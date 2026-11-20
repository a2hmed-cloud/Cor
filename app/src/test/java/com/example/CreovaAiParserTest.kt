package com.example

import com.example.data.catalog.CuratedCatalog
import com.example.data.repository.CreovaAiRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CreovaAiParserTest {

    @Test
    fun `parseStructuredResponse preserves verified catalog urls and strips hallucinated urls`() {
        val rawJson = """
            {
              "summary": "أحسن أداة مجانية بدون علامة مائية هي DaVinci Resolve.",
              "workflowSteps": [
                "جهز الفيديوهات في Media Pool",
                "اعمل Cut و Color Grading"
              ],
              "picks": [
                {
                  "name": "DaVinci Resolve",
                  "type": "tool",
                  "pricing": "freemium",
                  "reason": "قوي جداً في المونتاج وتصحيح الألوان",
                  "verifiedUrl": "https://www.blackmagicdesign.com/products/davinciresolve"
                },
                {
                  "name": "Unknown Fake App",
                  "type": "tool",
                  "pricing": "free",
                  "reason": "اقتراح غير موجود",
                  "verifiedUrl": "https://fake-hallucinated-tool.example/download.exe"
                }
              ]
            }
        """.trimIndent()

        val parsed = CreovaAiRepository.parseStructuredResponse(
            rawJson = rawJson,
            verifiedTools = CuratedCatalog.verifiedTools,
            verifiedResources = CuratedCatalog.curatedResources
        )

        assertEquals(2, parsed.workflowSteps.size)
        assertEquals(2, parsed.picks.size)
        assertEquals(
            "https://www.blackmagicdesign.com/products/davinciresolve",
            parsed.picks[0].verifiedUrl
        )
        assertEquals("", parsed.picks[1].verifiedUrl)
    }
}
