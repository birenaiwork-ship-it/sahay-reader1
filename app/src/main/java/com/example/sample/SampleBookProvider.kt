package com.example.sample

import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.PageEntity
import com.example.data.local.entity.ReadingUnitEntity
import com.example.data.local.entity.TranslationEntity
import com.example.data.repository.BookRepository
import com.example.data.repository.TranslationRepository

object SampleBookProvider {

    const val SAMPLE_BOOK_ID = "sample_book_cs101"

    suspend fun seedSampleBookIfNotPresent(
        context: android.content.Context,
        bookRepository: BookRepository,
        translationRepository: TranslationRepository
    ) {
        if (bookRepository.getBookById(SAMPLE_BOOK_ID) != null) {
            return
        }

        val sampleBook = BookEntity(
            id = SAMPLE_BOOK_ID,
            title = "Introduction to Computer Science",
            author = "National Council of Educational Research",
            pageCount = 3,
            currentPage = 1,
            currentReadingUnitId = "${SAMPLE_BOOK_ID}_p1_u1",
            lastReadTimestamp = System.currentTimeMillis(),
            isSampleBook = true,
            sourceUri = null,
            fileSize = 45000L
        )

        val pages = listOf(
            PageEntity(
                id = "${SAMPLE_BOOK_ID}_p1",
                bookId = SAMPLE_BOOK_ID,
                pageNumber = 1,
                isOcr = false,
                ocrConfidence = 1.0f,
                headerText = "Unit 1: Fundamentals of Computing",
                footerText = "Page 1",
                rawText = "Chapter 1: What is a Computer?\n\nComputers are electronic devices that process data into useful information.\nA computer system consists of hardware components and software instructions.\nHardware includes the Central Processing Unit (CPU), memory (RAM), and storage devices.\nThe CPU executes instructions and performs mathematical calculations.\nInput devices like keyboards allow users to feed data into the machine."
            ),
            PageEntity(
                id = "${SAMPLE_BOOK_ID}_p2",
                bookId = SAMPLE_BOOK_ID,
                pageNumber = 2,
                isOcr = false,
                ocrConfidence = 1.0f,
                headerText = "Unit 2: Problem Solving",
                footerText = "Page 2",
                rawText = "Chapter 2: Algorithms and Logic\n\nAn algorithm is a step-by-step sequence of instructions designed to solve a problem.\nIn computer programming, algorithms must be unambiguous and terminate after finite steps.\nThe efficiency of an algorithm is evaluated using Big-O notation, such as O(n) or O(n log n).\nControl structures include sequential execution, conditional branching, and iterative loops.\nProper indentation and clear variable names make source code accessible to everyone."
            ),
            PageEntity(
                id = "${SAMPLE_BOOK_ID}_p3",
                bookId = SAMPLE_BOOK_ID,
                pageNumber = 3,
                isOcr = false,
                ocrConfidence = 1.0f,
                headerText = "Unit 3: Emerging Technologies",
                footerText = "Page 3",
                rawText = "Chapter 3: Artificial Intelligence and Networks\n\nThe Internet is a global network of interconnected computers communicating via standard protocols.\nArtificial Intelligence refers to computer systems capable of performing tasks that typically require human cognition.\nMachine learning models learn patterns from training data to make predictions.\nSpeech recognition and synthesis allow visually impaired students to read educational materials.\nTechnology empowers students to achieve independence and knowledge."
            )
        )

        val readingUnits = mutableListOf<ReadingUnitEntity>()
        val translations = mutableListOf<TranslationEntity>()

        // Page 1 Units
        val p1Units = listOf(
            Pair("Chapter 1: What is a Computer?", "ଅଧ୍ୟାୟ ୧: କମ୍ପ୍ୟୁଟର କ'ଣ?"),
            Pair("Computers are electronic devices that process data into useful information.", "କମ୍ପ୍ୟୁଟର ହେଉଛି ଇଲେକ୍ଟ୍ରୋନିକ୍ ଯନ୍ତ୍ର ଯାହା ତଥ୍ୟକୁ ଉପଯୋଗୀ ସୂଚନାରେ ପରିଣତ କରେ।"),
            Pair("A computer system consists of hardware components and software instructions.", "ଏକ କମ୍ପ୍ୟୁଟର ସିଷ୍ଟମ୍ ହାର୍ଡୱେର୍ ଉପାଦାନ ଏବଂ ସଫ୍ଟୱେର୍ ନିର୍ଦ୍ଦେଶକୁ ନେଇ ଗଠିତ।"),
            Pair("Hardware includes the Central Processing Unit, memory, and storage devices.", "ହାର୍ଡୱେର୍ ମଧ୍ୟରେ ସେଣ୍ଟ୍ରାଲ୍ ପ୍ରୋସେସିଂ ୟୁନିଟ୍, ମେମୋରୀ ଏବଂ ଷ୍ଟୋରେଜ୍ ଡିଭାଇସ୍ ଅନ୍ତର୍ଭୁକ୍ତ।"),
            Pair("The CPU executes instructions and performs mathematical calculations.", "ସିପିୟୁ ନିର୍ଦ୍ଦେଶାବଳୀ କାର୍ଯ୍ୟକାରୀ କରେ ଏବଂ ଗାଣିତିକ ଗଣନା କରେ।"),
            Pair("Input devices like keyboards allow users to feed data into the machine.", "କୀବୋର୍ଡ୍ ଭଳି ଇନପୁଟ୍ ଉପକରଣ ବ୍ୟବହାରକାରୀଙ୍କୁ ମେସିନରେ ତଥ୍ୟ ଦେବାକୁ ଅନୁମତି ଦିଏ।")
        )

        p1Units.forEachIndexed { idx, pair ->
            val uId = "${SAMPLE_BOOK_ID}_p1_u${idx + 1}"
            readingUnits.add(
                ReadingUnitEntity(
                    id = uId,
                    bookId = SAMPLE_BOOK_ID,
                    pageNumber = 1,
                    paragraphIndex = if (idx == 0) 1 else 2,
                    unitIndexInPage = idx + 1,
                    englishText = pair.first,
                    isHeading = idx == 0,
                    headingLevel = if (idx == 0) 1 else 0
                )
            )
            translations.add(
                TranslationEntity(
                    readingUnitId = uId,
                    bookId = SAMPLE_BOOK_ID,
                    sourceText = pair.first,
                    odiaText = pair.second,
                    status = TranslationEntity.STATUS_TRANSLATED
                )
            )
        }

        // Page 2 Units
        val p2Units = listOf(
            Pair("Chapter 2: Algorithms and Logic", "ଅଧ୍ୟାୟ ୨: ଆଲଗୋରିଦମ୍ ଏବଂ ଲଜିକ୍"),
            Pair("An algorithm is a step-by-step sequence of instructions designed to solve a problem.", "ଆଲଗୋରିଦମ୍ ହେଉଛି କୌଣସି ସମସ୍ୟାର ସମାଧାନ ପାଇଁ ପ୍ରସ୍ତୁତ ଏକ କ୍ରମାନ୍ୱୟ ନିର୍ଦ୍ଦେଶାବଳୀ।"),
            Pair("In computer programming, algorithms must be unambiguous and terminate after finite steps.", "କମ୍ପ୍ୟୁଟର ପ୍ରୋଗ୍ରାମିଂରେ, ଆଲଗୋରିଦମ୍ ସ୍ପଷ୍ଟ ହେବା ଆବଶ୍ୟକ ଏବଂ ନିର୍ଦ୍ଦିଷ୍ଟ ପଦକ୍ଷେପ ପରେ ଶେଷ ହେବା ଆବଶ୍ୟକ।"),
            Pair("The efficiency of an algorithm is evaluated using Big-O notation, such as O(n) or O(n log n).", "ଆଲଗୋରିଦମ୍ର ଦକ୍ଷତା ବିଗ୍-ଓ ସଙ୍କେତ ବ୍ୟବହାର କରି ମୂଲ୍ୟାଙ୍କନ କରାଯାଏ।"),
            Pair("Control structures include sequential execution, conditional branching, and iterative loops.", "ନିୟନ୍ତ୍ରଣ ଗଠନରେ କ୍ରମିକ ନିଷ୍ପାଦନ, ସର୍ତ୍ତମୂଳକ ଶାଖା ଏବଂ ପୁନରାବୃତ୍ତି ଲୁପ୍ ଅନ୍ତର୍ଭୁକ୍ତ।"),
            Pair("Proper indentation and clear variable names make source code accessible to everyone.", "ସଠିକ୍ ଇଣ୍ଡେଣ୍ଟେସନ୍ ଏବଂ ସ୍ପଷ୍ଟ ଭେରିଏବଲ୍ ନାମ ସୋର୍ସ କୋଡ୍ ସମସ୍ତଙ୍କ ପାଇଁ ସୁବୋଧ୍ୟ କରେ।")
        )

        p2Units.forEachIndexed { idx, pair ->
            val uId = "${SAMPLE_BOOK_ID}_p2_u${idx + 1}"
            readingUnits.add(
                ReadingUnitEntity(
                    id = uId,
                    bookId = SAMPLE_BOOK_ID,
                    pageNumber = 2,
                    paragraphIndex = if (idx == 0) 1 else 2,
                    unitIndexInPage = idx + 1,
                    englishText = pair.first,
                    isHeading = idx == 0,
                    headingLevel = if (idx == 0) 1 else 0
                )
            )
            translations.add(
                TranslationEntity(
                    readingUnitId = uId,
                    bookId = SAMPLE_BOOK_ID,
                    sourceText = pair.first,
                    odiaText = pair.second,
                    status = TranslationEntity.STATUS_TRANSLATED
                )
            )
        }

        // Page 3 Units
        val p3Units = listOf(
            Pair("Chapter 3: Artificial Intelligence and Networks", "ଅଧ୍ୟାୟ ୩: କୃତ୍ରିମ ବୁଦ୍ଧିମତା ଏବଂ ନେଟୱାର୍କ"),
            Pair("The Internet is a global network of interconnected computers communicating via standard protocols.", "ଇଣ୍ଟରନେଟ୍ ହେଉଛି ମାନକ ପ୍ରୋଟୋକଲ୍ ମାଧ୍ୟମରେ ଯୋଗାଯୋଗ କରୁଥିବା ପାରସ୍ପରିକ ସଂଯୁକ୍ତ କମ୍ପ୍ୟୁଟରଗୁଡ଼ିକର ବିଶ୍ୱବ୍ୟାପୀ ନେଟୱାର୍କ।"),
            Pair("Artificial Intelligence refers to computer systems capable of performing tasks that typically require human cognition.", "କୃତ୍ରିମ ବୁଦ୍ଧିମତା (AI) ଏପରି କମ୍ପ୍ୟୁଟର ସିଷ୍ଟମକୁ ବୁଝାଏ ଯାହା ମାନବ ବୁଦ୍ଧିମତା ଆବଶ୍ୟକ କରୁଥିବା କାର୍ଯ୍ୟ କରିପାରେ।"),
            Pair("Machine learning models learn patterns from training data to make predictions.", "ମେସିନ୍ ଲର୍ନିଂ ମଡେଲ୍ଗୁଡିକ ଭବିଷ୍ୟବାଣୀ କରିବା ପାଇଁ ପ୍ରଶିକ୍ଷଣ ତଥ୍ୟରୁ ଢାଞ୍ଚା ଶିଖନ୍ତି।"),
            Pair("Speech recognition and synthesis allow visually impaired students to read educational materials.", "ଭଏସ୍ ରିକଗ୍ନିସନ୍ ଏବଂ ସିନ୍ଥେସିସ୍ ଦୃଷ୍ଟିହୀନ ଛାତ୍ରଛାତ୍ରୀମାନଙ୍କୁ ଶିକ୍ଷଣୀୟ ବିଷୟବସ୍ତୁ ପଢ଼ିବାକୁ ସାହାଯ୍ୟ କରେ।"),
            Pair("Technology empowers students to achieve independence and knowledge.", "ପ୍ରଯୁକ୍ତିବିଦ୍ୟା ଛାତ୍ରଛାତ୍ରୀମାନଙ୍କୁ ଆତ୍ମନିର୍ଭରଶୀଳତା ଏବଂ ଜ୍ଞାନ ହାସଲ କରିବାକୁ ସଶକ୍ତ କରେ।")
        )

        p3Units.forEachIndexed { idx, pair ->
            val uId = "${SAMPLE_BOOK_ID}_p3_u${idx + 1}"
            readingUnits.add(
                ReadingUnitEntity(
                    id = uId,
                    bookId = SAMPLE_BOOK_ID,
                    pageNumber = 3,
                    paragraphIndex = if (idx == 0) 1 else 2,
                    unitIndexInPage = idx + 1,
                    englishText = pair.first,
                    isHeading = idx == 0,
                    headingLevel = if (idx == 0) 1 else 0
                )
            )
            translations.add(
                TranslationEntity(
                    readingUnitId = uId,
                    bookId = SAMPLE_BOOK_ID,
                    sourceText = pair.first,
                    odiaText = pair.second,
                    status = TranslationEntity.STATUS_TRANSLATED
                )
            )
        }

        bookRepository.saveBook(sampleBook, pages, readingUnits)
        val db = com.example.data.local.AppDatabase.getInstance(context)
        db.translationDao().insertTranslations(translations)
    }
}
