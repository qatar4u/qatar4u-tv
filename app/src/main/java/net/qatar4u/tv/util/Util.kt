package net.qatar4u.tv.util

import android.content.Context
import android.content.res.Configuration
import java.security.MessageDigest
import java.util.Locale

object Pin {
    /** Salted SHA-256, so the four digits are never stored as they are. */
    fun hash(pin: String): String {
        val d = MessageDigest.getInstance("SHA-256").digest(("qatar4u-tv:" + pin).toByteArray())
        return d.joinToString("") { "%02x".format(it) }
    }

    fun check(pin: String, hash: String) = hash.isNotEmpty() && hash(pin) == hash
}

object Dex {
    /** Samsung DeX state from Samsung's configuration fields; false on other devices. */
    fun isOn(context: Context): Boolean = runCatching {
        val cfg = context.resources.configuration
        val c = cfg.javaClass
        val enabled = c.getField("SEM_DESKTOP_MODE_ENABLED").getInt(c)
        c.getField("semDesktopModeEnabled").getInt(cfg) == enabled
    }.getOrDefault(false)
}

object Lang {
    private const val PREFS = "lang"

    /** The chosen language is mirrored here so the activity can apply it before it draws anything. */
    fun get(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("lang", "ar") ?: "ar"

    fun set(context: Context, lang: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("lang", lang).apply()
    }

    fun wrap(base: Context): Context {
        val locale = Locale(get(base))
        Locale.setDefault(locale)
        val cfg = Configuration(base.resources.configuration)
        cfg.setLocale(locale)
        cfg.setLayoutDirection(locale)
        return base.createConfigurationContext(cfg)
    }
}
