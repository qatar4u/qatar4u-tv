package net.qatar4u.tv.download

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlin.coroutines.coroutineContext

data class DownloadTarget(val url: String, val apk: Boolean)
enum class ResolveFailure { INPUT, BLOCKED, NOT_FOUND, REDIRECT, NETWORK }
class ResolveException(val reason: ResolveFailure) : IOException(reason.name)

object CodeResolver {
    internal data class Probe(val status: Int, val mime: String = "", val disposition: String = "",
        val location: String? = null, val html: String? = null)

    suspend fun resolve(input: String): DownloadTarget = withContext(Dispatchers.IO) {
        resolveWith(input, ::probe)
    }

    /** Same resolution path for production and deterministic HTTP/HTML regression fixtures. */
    internal suspend fun resolveWith(input: String, fetch: suspend (String) -> Probe): DownloadTarget {
        var next = try { CodeLinks.inputUrl(input) } catch (_: Exception) { throw ResolveException(ResolveFailure.INPUT) }
        val seen = mutableSetOf<String>()
        repeat(10) {
            coroutineContext.ensureActive()
            if (!seen.add(next)) throw ResolveException(ResolveFailure.REDIRECT)
            val response = fetch(next)
            if (response.status in listOf(401, 403, 429)) throw ResolveException(ResolveFailure.BLOCKED)
            if (response.status in listOf(404, 410)) throw ResolveException(ResolveFailure.NOT_FOUND)
            if (response.status in listOf(301, 302, 303, 307, 308)) {
                next = try {
                    CodeLinks.webUrl(URI(next).resolve(requireNotNull(response.location)).toString())
                } catch (_: Exception) { throw ResolveException(ResolveFailure.REDIRECT) }
            } else {
                if (response.status != 200 && response.status != 206) throw ResolveException(ResolveFailure.NETWORK)
                if (isShortener(next)) {
                    next = try {
                        CodeLinks.manualDestination(response.html.orEmpty()) ?: throw ResolveException(ResolveFailure.NOT_FOUND)
                    } catch (e: ResolveException) { throw e
                    } catch (_: Exception) { throw ResolveException(ResolveFailure.REDIRECT) }
                } else {
                    return DownloadTarget(next, CodeLinks.isApk(next, response.mime, response.disposition))
                }
            }
        }
        throw ResolveException(ResolveFailure.REDIRECT)
    }

    private fun isShortener(url: String) = URI(url).host.lowercase() in setOf("go.aftvnews.com", "aftv.news")

    private suspend fun probe(url: String): Probe {
        val connection = URL(CodeLinks.webUrl(url)).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.setRequestProperty("User-Agent", CodeLinks.USER_AGENT)
        try {
            val status = connection.responseCode
            val html = if (status == 200 && isShortener(url)) {
                connection.inputStream.use { stream ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(4096)
                    while (true) {
                        coroutineContext.ensureActive()
                        val n = stream.read(buffer)
                        if (n < 0) break
                        if (output.size() + n > 256 * 1024) throw ResolveException(ResolveFailure.REDIRECT)
                        output.write(buffer, 0, n)
                    }
                    output.toString("UTF-8")
                }
            } else null
            return Probe(status, connection.contentType.orEmpty(), connection.getHeaderField("Content-Disposition").orEmpty(),
                connection.getHeaderField("Location"), html)
        } finally { connection.disconnect() }
    }
}
