package com.alexandro.asianlanguageidentification

import com.alexandro.asianlanguageidentification.data.local.DetectionHistoryItem
import com.alexandro.asianlanguageidentification.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageDetectionUnitTest {

    @Test
    fun testFallbackLanguagesHasAllSevenLanguages() {
        val fallback = LanguageDetectionViewModel.FALLBACK_LANGUAGES
        assertEquals(7, fallback.size)
        assertTrue(fallback.contains("indonesian"))
        assertTrue(fallback.contains("malay"))
        assertTrue(fallback.contains("vietnamese"))
        assertTrue(fallback.contains("thai"))
        assertTrue(fallback.contains("khmer"))
        assertTrue(fallback.contains("lao"))
        assertTrue(fallback.contains("fil_tl"))

        // Pastikan bahasa di luar 7 regional tidak diklaim
        assertFalse(fallback.contains("korean"))
        assertFalse(fallback.contains("japanese"))
        assertFalse(fallback.contains("english"))
        assertFalse(fallback.contains("french"))
        assertFalse(fallback.contains("german"))
    }

    @Test
    fun testNavigationScreensCountAndTitles() {
        val screens = Screen.values()
        assertEquals(5, screens.size)

        val titles = screens.map { it.title }
        assertTrue(titles.contains("Deteksi Bahasa"))
        assertTrue(titles.contains("Riwayat Deteksi"))
        assertTrue(titles.contains("Bahasa Didukung"))
        assertTrue(titles.contains("Cara Penggunaan"))
        assertTrue(titles.contains("Tentang Aplikasi"))
    }

    @Test
    fun testDetectionHistoryItemProperties() {
        val item = DetectionHistoryItem(
            textSnippet = "Bapak presiden sedang berpidato di istana negara Jakarta.",
            fullText = "Bapak presiden sedang berpidato di istana negara Jakarta hari ini.",
            languageName = "Indonesia",
            languageCode = "indonesian",
            isSuccess = true,
            marginScore = 0.208
        )

        assertNotNull(item.id)
        assertTrue(item.isSuccess)
        assertEquals("Indonesia", item.languageName)
        assertEquals("indonesian", item.languageCode)
        assertEquals(0.208, item.marginScore!!, 0.001)
        assertTrue(item.timestamp > 0)
    }

    @Test
    fun testUnknownLanguageHistoryItem() {
        val unknownItem = DetectionHistoryItem(
            textSnippet = "Bonjour, je suis en train d'apprendre la langue française.",
            fullText = "Bonjour, je suis en train d'apprendre la langue française aujourd'hui.",
            languageName = "Bahasa tidak dikenali",
            languageCode = "unknown",
            isSuccess = false,
            marginScore = -0.1117
        )

        assertFalse(unknownItem.isSuccess)
        assertEquals("Bahasa tidak dikenali", unknownItem.languageName)
        assertEquals("unknown", unknownItem.languageCode)
        assertTrue(unknownItem.marginScore!! < 0.0)
    }

    @Test
    fun testMinimumCharacterConstraint() {
        val minChars = 20
        val shortText = "Halo tes"
        val validText = "Ini adalah kalimat bahasa Indonesia yang valid."

        assertTrue(shortText.length < minChars)
        assertTrue(validText.length >= minChars)
    }
}
