package net.qatar4u.tv.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

/**
 * Wallpapers live on the tablet: the ones bundled in the APK (assets/wallpapers) and pictures the owner
 * added from the tablet (copied into the app's own folder), so they work without the internet.
 */
object Wallpapers {

    data class Item(val name: String, val bundled: Boolean)

    private fun userDir(context: Context) = File(context.filesDir, "wallpapers").apply { mkdirs() }

    fun list(context: Context): List<Item> {
        val bundled = context.assets.list("wallpapers").orEmpty()
            .filter { it.endsWith(".jpg") || it.endsWith(".png") || it.endsWith(".webp") }
            .sorted().map { Item(it, true) }
        val mine = userDir(context).listFiles().orEmpty().map { it.name }.sorted().map { Item(it, false) }
        // The owner's own photos first, then the bundled ones.
        return mine + bundled
    }

    /** Index of the picture to show [now], changing every [minutes] (0: never), or the pinned one. */
    fun pick(items: List<Item>, pinned: String, minutes: Int, now: Long): Int {
        if (items.isEmpty()) return -1
        val p = items.indexOfFirst { it.name == pinned }
        if (p >= 0) return p
        if (minutes <= 0) return 0
        return ((now / (minutes * 60_000L)) % items.size).toInt()
    }

    private fun open(context: Context, item: Item): InputStream =
        if (item.bundled) context.assets.open("wallpapers/${item.name}") else File(userDir(context), item.name).inputStream()

    /** Decoded close to [maxW] x [maxH], never bigger than the screen needs. */
    suspend fun load(context: Context, item: Item, maxW: Int, maxH: Int): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            open(context, item).use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= maxW && bounds.outHeight / (sample * 2) >= maxH) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 }
            open(context, item).use { BitmapFactory.decodeStream(it, null, opts) }
        }.getOrNull()
    }

    /** Copy pictures the owner picked into the app folder. Returns how many were added. */
    suspend fun import(context: Context, uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        var n = 0
        for (u in uris) {
            runCatching {
                val name = "mine-" + System.currentTimeMillis() + "-" + n + ".jpg"
                context.contentResolver.openInputStream(u)?.use { input ->
                    File(userDir(context), name).outputStream().use { input.copyTo(it) }
                }
                n++
            }
        }
        n
    }

    fun delete(context: Context, item: Item): Boolean = !item.bundled && File(userDir(context), item.name).delete()
}
