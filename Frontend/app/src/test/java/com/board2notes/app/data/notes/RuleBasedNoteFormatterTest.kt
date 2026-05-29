package com.board2notes.app.data.notes

import com.board2notes.app.domain.model.OcrResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleBasedNoteFormatterTest {
    @Test
    fun formatterCleansWhitespaceAndDropsConsecutiveDuplicates() {
        val formatter = RuleBasedNoteFormatter()
        val result = formatter.format(
            OcrResult(
                rawText = "  Fonksiyonlar   \nFonksiyonlar\nLimit  süreklilik\n\n Türev   kuralları ",
                lines = emptyList(),
                words = emptyList(),
                confidence = null,
                elapsedMs = 10L
            )
        )

        assertEquals("Fonksiyonlar", result.title)
        assertTrue(result.body.contains("Limit süreklilik"))
        assertEquals(1, Regex("Fonksiyonlar").findAll(result.body).count())
    }
}
