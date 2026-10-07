package com.example.pdf

import com.example.data.local.entity.ReadingUnitEntity

data class RawPageBlock(
    val text: String,
    val topFraction: Float,
    val isHeaderCandidate: Boolean = false,
    val isFooterCandidate: Boolean = false
)

object ReadingUnitProcessor {

    /**
     * Divides extracted raw page text into logical reading units.
     * Preserves sentence boundaries, punctuation, numbers, and formulas.
     * Avoids breaking mid-formula or mid-acronym (e.g. "U.S.A.", "Dr.", "3.14").
     */
    fun processPageText(
        bookId: String,
        pageNumber: Int,
        rawText: String
    ): List<ReadingUnitEntity> {
        val cleanedText = cleanRawText(rawText)
        if (cleanedText.isBlank()) {
            return emptyList()
        }

        val paragraphs = cleanedText.split(Regex("(\r?\n){2,}"))
        val readingUnits = mutableListOf<ReadingUnitEntity>()
        var globalUnitIndex = 1

        paragraphs.forEachIndexed { pIndex, paragraph ->
            val trimmedParagraph = paragraph.trim()
            if (trimmedParagraph.isNotBlank()) {
                val sentences = splitIntoSentences(trimmedParagraph)
                for (sentence in sentences) {
                    val trimmedSentence = sentence.trim()
                    if (trimmedSentence.isNotBlank()) {
                        val isHeading = isProbableHeading(trimmedSentence)
                        val headingLevel = if (isHeading) {
                            if (trimmedSentence.length < 35 && !trimmedSentence.endsWith(".")) 1 else 2
                        } else 0

                        val unitId = "${bookId}_p${pageNumber}_u$globalUnitIndex"
                        readingUnits.add(
                            ReadingUnitEntity(
                                id = unitId,
                                bookId = bookId,
                                pageNumber = pageNumber,
                                paragraphIndex = pIndex + 1,
                                unitIndexInPage = globalUnitIndex,
                                englishText = trimmedSentence,
                                isHeading = isHeading,
                                headingLevel = headingLevel
                            )
                        )
                        globalUnitIndex++
                    }
                }
            }
        }

        return readingUnits
    }

    fun cleanRawText(text: String): String {
        return text
            // Replace soft hyphens and combine line-break hyphens e.g. "oper-\nating" -> "operating"
            .replace(Regex("(\\b\\w+)-\\r?\\n(\\w+\\b)"), "$1$2")
            // Remove standalone page numbers on a single line
            .replace(Regex("(?m)^\\s*(?:Page\\s+)?\\d{1,4}\\s*$"), "")
            // Normalize spaces
            .replace(Regex("[ \\t]+"), " ")
            .trim()
    }

    /**
     * Splits paragraph text into coherent sentences without breaking common abbreviations or decimal numbers.
     */
    fun splitIntoSentences(paragraph: String): List<String> {
        // Protect common abbreviations and decimals temporarily
        var protected = paragraph
            .replace("e.g.", "e_g_")
            .replace("i.e.", "i_e_")
            .replace("etc.", "etc_")
            .replace("Dr.", "Dr_")
            .replace("Mr.", "Mr_")
            .replace("Mrs.", "Mrs_")
            .replace("Prof.", "Prof_")
            .replace("Fig.", "Fig_")
            .replace("vs.", "vs_")
            .replace(Regex("(\\d+)\\.(\\d+)"), "$1_DECIMAL_$2") // decimal point

        // Split on terminal punctuation followed by space or newline
        val regex = Regex("(?<=[.?!])\\s+(?=[A-Z0-9\"'‘“])")
        val splits = protected.split(regex)

        val result = mutableListOf<String>()
        for (split in splits) {
            val restored = split
                .replace("e_g_", "e.g.")
                .replace("i_e_", "i.e.")
                .replace("etc_", "etc.")
                .replace("Dr_", "Dr.")
                .replace("Mr_", "Mr.")
                .replace("Mrs_", "Mrs.")
                .replace("Prof_", "Prof.")
                .replace("Fig_", "Fig.")
                .replace("vs_", "vs.")
                .replace("_DECIMAL_", ".")
                .trim()

            // If a sentence is unusually long (> 300 chars without punctuation), split on commas/connectors
            if (restored.length > 320) {
                val subParts = splitLongClause(restored)
                result.addAll(subParts)
            } else if (restored.isNotBlank()) {
                result.add(restored)
            }
        }

        return if (result.isEmpty() && paragraph.isNotBlank()) listOf(paragraph.trim()) else result
    }

    private fun splitLongClause(clause: String): List<String> {
        val parts = clause.split(Regex("(?<=[,;])\\s+"))
        val merged = mutableListOf<String>()
        var buffer = StringBuilder()

        for (part in parts) {
            if (buffer.length + part.length > 250 && buffer.isNotEmpty()) {
                merged.add(buffer.toString().trim())
                buffer = StringBuilder()
            }
            if (buffer.isNotEmpty()) buffer.append(" ")
            buffer.append(part)
        }
        if (buffer.isNotEmpty()) {
            merged.add(buffer.toString().trim())
        }
        return merged
    }

    private fun isProbableHeading(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length > 70) return false
        if (trimmed.startsWith("Chapter ", ignoreCase = true) ||
            trimmed.startsWith("Section ", ignoreCase = true) ||
            trimmed.startsWith("Unit ", ignoreCase = true) ||
            trimmed.startsWith("Part ", ignoreCase = true) ||
            trimmed.startsWith("#")
        ) {
            return true
        }
        // Title Case or uppercase with no trailing period
        val hasNoEndPunctuation = !trimmed.endsWith(".") && !trimmed.endsWith("?") && !trimmed.endsWith("!")
        val isShort = trimmed.length in 4..50
        val isMostlyUpper = trimmed.count { it.isUpperCase() } >= (trimmed.length / 3)
        return hasNoEndPunctuation && isShort && isMostlyUpper
    }
}
