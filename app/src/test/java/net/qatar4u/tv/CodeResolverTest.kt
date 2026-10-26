package net.qatar4u.tv

import kotlinx.coroutines.runBlocking
import net.qatar4u.tv.download.CodeResolver
import net.qatar4u.tv.download.CodeResolver.Probe
import net.qatar4u.tv.download.ResolveException
import net.qatar4u.tv.download.ResolveFailure
import org.junit.Assert.*
import org.junit.Test

class CodeResolverTest {
    @Test fun shortCodeToRedirectToAttachment() = runBlocking {
        val requested = mutableListOf<String>()
        val result = CodeResolver.resolveWith("١٢٣٤٥") { url ->
            requested += url
            when (url) {
                "https://go.aftvnews.com/12345" -> Probe(200, html = """<div id="redirect_noscript"><a href="https://example.com/start">Continue</a></div>""")
                "https://example.com/start" -> Probe(302, location = "/download")
                "https://example.com/download" -> Probe(200, "application/zip", "attachment; filename=\"app.apk\"")
                else -> error("unexpected URL $url")
            }
        }
        assertEquals(3, requested.size)
        assertEquals("https://example.com/download", result.url)
        assertTrue(result.apk)
    }
    @Test fun httpApkWorksAndHtmlStaysAWebpage() = runBlocking {
        assertTrue(CodeResolver.resolveWith("http://example.com/app.apk") { Probe(200, "application/vnd.android.package-archive") }.apk)
        assertFalse(CodeResolver.resolveWith("https://example.com/app.apk") { Probe(200, "text/html") }.apk)
    }
    @Test fun failuresAreSpecificAndLoopsAreBounded() = runBlocking {
        for ((status, expected) in listOf(403 to ResolveFailure.BLOCKED, 404 to ResolveFailure.NOT_FOUND, 500 to ResolveFailure.NETWORK)) {
            val exception = runCatching { CodeResolver.resolveWith("https://example.com/a") { Probe(status) } }.exceptionOrNull()
            assertEquals(expected, (exception as ResolveException).reason)
        }
        var calls = 0
        val exception = runCatching { CodeResolver.resolveWith("https://example.com/a") {
            calls++; Probe(302, location = "/a")
        } }.exceptionOrNull()
        assertEquals(ResolveFailure.REDIRECT, (exception as ResolveException).reason)
        assertEquals(1, calls)
    }
    @Test fun unsupportedRedirectCannotLaunchOtherSchemes() = runBlocking {
        val exception = runCatching { CodeResolver.resolveWith("https://example.com/a") {
            Probe(302, location = "file:///tmp/app.apk")
        } }.exceptionOrNull()
        assertEquals(ResolveFailure.REDIRECT, (exception as ResolveException).reason)
    }
}
