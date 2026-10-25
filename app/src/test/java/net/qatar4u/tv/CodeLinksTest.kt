package net.qatar4u.tv

import net.qatar4u.tv.download.CodeLinks
import org.junit.Assert.*
import org.junit.Test

class CodeLinksTest {
    @Test fun codesAndArabicDigits() {
        assertEquals("https://go.aftvnews.com/12345", CodeLinks.inputUrl(" ١٢٣٤٥ "))
        assertEquals("https://go.aftvnews.com/123", CodeLinks.inputUrl("۱۲۳"))
        assertEquals("https://example.com/app.apk", CodeLinks.inputUrl("https://example.com/app.apk"))
    }
    @Test fun onlyManualRedirectBlockIsUsed() {
        val html = """<a href="https://ads.example/app.apk">Ad</a>
            <div id="redirect_noscript">Manual link:<br><a href="https://example.com/app.apk?a=1&amp;b=2">Download</a></div>"""
        assertEquals("https://example.com/app.apk?a=1&b=2", CodeLinks.manualDestination(html))
        assertNull(CodeLinks.manualDestination("<script>window.location='https://example.com/app.apk'</script>"))
    }
    @Test fun unsafeAndMalformedInputsAreRejected() {
        for (input in listOf("", "123x", "javascript:alert(1)", "file:///tmp/a.apk", "http://example.com/a.apk", "https://user:pass@example.com/a.apk", "https:///bad")) {
            assertTrue("must reject $input", runCatching { CodeLinks.inputUrl(input) }.isFailure)
        }
    }
    @Test fun badManualDestinationCannotBecomeExecutableScript() {
        assertTrue(runCatching { CodeLinks.manualDestination("""<div id="redirect_noscript"><a href="javascript:alert(1)">bad</a></div>""") }.isFailure)
    }
}
