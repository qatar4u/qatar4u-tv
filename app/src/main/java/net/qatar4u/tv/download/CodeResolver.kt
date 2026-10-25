package net.qatar4u.tv.download

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlin.coroutines.coroutineContext

data class DownloadTarget(val url: String, val apk: Boolean)

object CodeResolver {
    suspend fun resolve(input: String): DownloadTarget = withContext(Dispatchers.IO) {
        var next = CodeLinks.inputUrl(input)
        val seen = mutableSetOf<String>()
        repeat(10) {
            coroutineContext.ensureActive()
            require(seen.add(next)) { "redirect loop" }
            val connection = URL(CodeLinks.https(next)).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("User-Agent", "Qatar4uTV/1.5 (Android)")
            try {
                val status = connection.responseCode
                if (status in listOf(301, 302, 303, 307, 308)) {
                    val location = requireNotNull(connection.getHeaderField("Location"))
                    next = CodeLinks.https(URI(next).resolve(location).toString())
                } else {
                    require(status == 200) { "HTTP $status" }
                    val mime = connection.contentType.orEmpty().substringBefore(';').lowercase()
                    val host = URI(next).host.lowercase()
                    if (host == "go.aftvnews.com" || host == "aftv.news") {
                        val html = connection.inputStream.use { stream ->
                            val output = ByteArrayOutputStream()
                            val buffer = ByteArray(4096)
                            while (true) {
                                coroutineContext.ensureActive()
                                val n = stream.read(buffer)
                                if (n < 0) break
                                require(output.size() + n <= 256 * 1024) { "page too large" }
                                output.write(buffer, 0, n)
                            }
                            output.toString("UTF-8")
                        }
                        next = requireNotNull(CodeLinks.manualDestination(html)) { "code unavailable" }
                    } else {
                        val isHtml = mime.contains("html")
                        val apk = !isHtml && (mime == "application/vnd.android.package-archive" ||
                            mime == "application/octet-stream" || URI(next).path.orEmpty().endsWith(".apk", true))
                        return@withContext DownloadTarget(next, apk)
                    }
                }
            } finally {
                connection.disconnect()
            }
        }
        error("too many redirects")
    }
}
