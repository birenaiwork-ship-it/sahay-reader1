package com.example

import com.example.pdf.ReadingUnitProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testSentenceSplitting_preservesAbbreviationsAndDecimals() {
        val paragraph = "Dr. Smith explained that e.g. algorithm efficiency is 3.14 times faster. Control structures are crucial!"
        val sentences = ReadingUnitProcessor.splitIntoSentences(paragraph)

        assertEquals(2, sentences.size)
        assertEquals("Dr. Smith explained that e.g. algorithm efficiency is 3.14 times faster.", sentences[0])
        assertEquals("Control structures are crucial!", sentences[1])
    }

    @Test
    fun testTextCleaning_removesHyphensAndPageNumbers() {
        val raw = "This is an oper-\nating system textbook.\n14\nNew paragraph begins."
        val cleaned = ReadingUnitProcessor.cleanRawText(raw)

        assertTrue(cleaned.contains("operating system textbook."))
        assertFalse(cleaned.contains("\n14\n"))
    }

    @Test
    fun testReadingUnitProcessor_detectsHeadingsCorrectly() {
        val pageText = "Chapter 1: Basics of Computing\n\nComputers execute binary instructions."
        val units = ReadingUnitProcessor.processPageText("test_book", 1, pageText)

        assertEquals(2, units.size)
        assertTrue(units[0].isHeading)
        assertEquals("Chapter 1: Basics of Computing", units[0].englishText)
        assertEquals(1, units[0].headingLevel)
        assertFalse(units[1].isHeading)
    }

    @Test
    fun testReadingUnitProcessor_assignsStableIds() {
        val pageText = "Sentence one. Sentence two."
        val units = ReadingUnitProcessor.processPageText("book_abc", 3, pageText)

        assertEquals(2, units.size)
        assertEquals("book_abc_p3_u1", units[0].id)
        assertEquals("book_abc_p3_u2", units[1].id)
    }
}
