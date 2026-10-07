package com.example.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.example.data.local.entity.PageEntity
import com.example.data.local.entity.ReadingUnitEntity
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

sealed class ExtractionProgress {
    data class Status(val message: String, val currentPage: Int = 0, val totalPages: Int = 0, val percent: Int = 0) : ExtractionProgress()
    data class Success(val pages: List<PageEntity>, val readingUnits: List<ReadingUnitEntity>) : ExtractionProgress()
    data class Error(val message: String, val throwable: Throwable? = null) : ExtractionProgress()
}

class PdfTextExtractor(private val context: Context) {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractFromUri(
        bookId: String,
        uri: Uri,
        onProgress: (ExtractionProgress) -> Unit
    ): Result<Pair<List<PageEntity>, List<ReadingUnitEntity>>> = withContext(Dispatchers.IO) {
        var pfd: ParcelFileDescriptor? = null
        val booksDir = File(context.filesDir, "books").apply { mkdirs() }
        val permanentPdfFile = File(booksDir, "${bookId}.pdf")

        try {
            onProgress(ExtractionProgress.Status("Opening book.", 0, 0, 5))

            // Copy to app's secure storage for permanent offline access
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(permanentPdfFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Could not read PDF stream from selected location"))

            pfd = ParcelFileDescriptor.open(permanentPdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            if (pfd == null) {
                return@withContext Result.failure(Exception("Could not open PDF file descriptor"))
            }

            val renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount
            if (pageCount == 0) {
                renderer.close()
                return@withContext Result.failure(Exception("PDF file contains no pages"))
            }

            onProgress(ExtractionProgress.Status("Extracting text.", 0, pageCount, 10))

            val allPages = mutableListOf<PageEntity>()
            val allUnits = mutableListOf<ReadingUnitEntity>()

            for (pageIndex in 0 until pageCount) {
                val pageNumber = pageIndex + 1
                val progressPercent = 10 + ((pageIndex.toFloat() / pageCount.toFloat()) * 80).toInt()

                val page = renderer.openPage(pageIndex)
                // Render at high resolution for crisp OCR precision
                val scale = 2.0f
                val width = (page.width * scale).toInt().coerceAtLeast(600)
                val height = (page.height * scale).toInt().coerceAtLeast(800)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                onProgress(
                    ExtractionProgress.Status(
                        message = "OCR is being used to read the scanned pages.",
                        currentPage = pageNumber,
                        totalPages = pageCount,
                        percent = progressPercent
                    )
                )

                // Run OCR with ML Kit
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val ocrResult = try {
                    val task = textRecognizer.process(inputImage)
                    Tasks.await(task, 15, TimeUnit.SECONDS)
                } catch (e: Exception) {
                    null
                } finally {
                    bitmap.recycle()
                }

                val extractedRawText = ocrResult?.text ?: ""
                var headerCandidate: String? = null
                var footerCandidate: String? = null

                // Detect headers and footers from block positions
                ocrResult?.textBlocks?.let { blocks ->
                    val sorted = blocks.sortedBy { it.boundingBox?.top ?: 0 }
                    if (sorted.isNotEmpty()) {
                        val first = sorted.first()
                        val top = first.boundingBox?.top ?: 0
                        if (top < (height * 0.12f)) {
                            headerCandidate = first.text.trim()
                        }
                        val last = sorted.last()
                        val bottom = last.boundingBox?.bottom ?: height
                        if (bottom > (height * 0.88f)) {
                            footerCandidate = last.text.trim()
                        }
                    }
                }

                // Process into reading units
                val units = ReadingUnitProcessor.processPageText(bookId, pageNumber, extractedRawText)
                val fallbackUnits = if (units.isEmpty()) {
                    listOf(
                        ReadingUnitEntity(
                            id = "${bookId}_p${pageNumber}_u1",
                            bookId = bookId,
                            pageNumber = pageNumber,
                            paragraphIndex = 1,
                            unitIndexInPage = 1,
                            englishText = "[Blank or unscannable page $pageNumber]",
                            isHeading = false,
                            headingLevel = 0
                        )
                    )
                } else {
                    units
                }

                val pageEntity = PageEntity(
                    id = "${bookId}_p$pageNumber",
                    bookId = bookId,
                    pageNumber = pageNumber,
                    isOcr = true,
                    ocrConfidence = 0.95f,
                    headerText = headerCandidate,
                    footerText = footerCandidate,
                    rawText = extractedRawText
                )

                allPages.add(pageEntity)
                allUnits.addAll(fallbackUnits)
            }

            renderer.close()

            onProgress(ExtractionProgress.Status("Preparing reading lines.", pageCount, pageCount, 95))
            onProgress(ExtractionProgress.Success(allPages, allUnits))

            Result.success(Pair(allPages, allUnits))
        } catch (e: Exception) {
            permanentPdfFile.delete()
            onProgress(ExtractionProgress.Error("Failed to extract book text: ${e.localizedMessage}", e))
            Result.failure(e)
        } finally {
            try {
                pfd?.close()
            } catch (_: Exception) {}
        }
    }
}
