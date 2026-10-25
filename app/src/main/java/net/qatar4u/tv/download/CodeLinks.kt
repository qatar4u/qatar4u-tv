package net.qatar4u.tv.download

import java.net.URI

/** Public AFTVnews short codes are links, not uploaded APK files. No scripts are executed. */
object CodeLinks {
    fun inputUrl(input: String): String {
        val value = input.trim().map { c ->
            if (c in '٠'..'٩') ('0'.code + (c - '٠')).toChar()
            else if (c in '۰'..'۹') ('0'.code + (c - '۰')).toChar() else c
        }.joinToString("")
        if (value.matches(Regex("[0-9]{1,12}"))) return "https://go.aftvnews.com/$value"
        return https(input.trim())
    }

    fun https(value: String): String {
        val uri = URI(value)
        require(uri.scheme.equals("https", true) && !uri.host.isNullOrBlank() && uri.rawUserInfo == null)
        return uri.toASCIIString()
    }

    /** Restrict extraction to the site's documented manual-redirect block, never advertising links. */
    fun manualDestination(html: String): String? {
        val block = Regex("""<div\b[^>]*\bid\s*=\s*["']redirect_noscript["'][^>]*>(.*?)</div>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(html)?.groupValues?.get(1) ?: return null
        val href = Regex("""<a\b[^>]*\bhref\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(block)?.groupValues?.get(1) ?: return null
        return https(href.replace("&amp;", "&").replace("&#038;", "&").replace("&#38;", "&"))
    }
}
