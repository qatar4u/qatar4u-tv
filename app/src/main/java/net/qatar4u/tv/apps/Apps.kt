package net.qatar4u.tv.apps

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.graphics.drawable.toBitmap

data class AppEntry(
    val pkg: String,
    val label: String,
    val component: ComponentName,
    /** True when the app declares a TV banner; the hub then shows it as a wide card. */
    val hasBanner: Boolean,
    val isEntertainment: Boolean,
    val isSystem: Boolean,
)

/** A streaming app the hub suggests when it is not installed (opens Google Play, never a side-load). */
data class Suggested(val pkg: String, val labelAr: String, val labelEn: String)

object Apps {

    /** The entertainment apps the hub knows, in their default order. */
    val ENTERTAINMENT = listOf(
        Suggested("com.google.android.youtube", "يوتيوب", "YouTube"),
        Suggested("com.netflix.mediaclient", "نتفليكس", "Netflix"),
        Suggested("net.mbc.shahid", "شاهد", "Shahid"),
        Suggested("com.osn.go", "OSN+", "OSN+"),
        Suggested("com.parsifal.starz", "ستارز بلاي", "STARZPLAY"),
        Suggested("org.videolan.vlc", "VLC", "VLC"),
        Suggested("org.xbmc.kodi", "Kodi", "Kodi"),
        Suggested("com.plexapp.android", "Plex", "Plex"),
    )
    private val entertainmentSet = ENTERTAINMENT.map { it.pkg }.toSet()

    /** Every launchable app except this one, phone and TV launch intents both counted. */
    fun installed(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val seen = LinkedHashMap<String, AppEntry>()
        for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val q = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (ri in pm.queryIntentActivities(q, 0)) {
                val ai = ri.activityInfo ?: continue
                if (ai.packageName == context.packageName || seen.containsKey(ai.packageName)) continue
                val appInfo = ai.applicationInfo
                if (!appInfo.enabled) continue
                seen[ai.packageName] = AppEntry(
                    pkg = ai.packageName,
                    label = ri.loadLabel(pm).toString(),
                    component = ComponentName(ai.packageName, ai.name),
                    hasBanner = appInfo.banner != 0 || ai.banner != 0,
                    // Known streaming apps, plus any app that declares itself a video app.
                    isEntertainment = ai.packageName in entertainmentSet || appInfo.category == ApplicationInfo.CATEGORY_VIDEO,
                    isSystem = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                )
            }
        }
        return seen.values.toList()
    }

    fun isInstalled(context: Context, pkg: String): Boolean = try {
        context.packageManager.getApplicationInfo(pkg, 0).enabled
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    /** Apply the owner's order: listed packages first in that order, then entertainment, then by name. */
    fun ordered(apps: List<AppEntry>, order: List<String>): List<AppEntry> {
        val rank = order.withIndex().associate { it.value to it.index }
        val ent = ENTERTAINMENT.withIndex().associate { it.value.pkg to it.index }
        return apps.sortedWith(
            compareBy<AppEntry>({ rank[it.pkg] ?: Int.MAX_VALUE }, { ent[it.pkg] ?: Int.MAX_VALUE }, { it.label.lowercase() })
        )
    }

    /** Move [pkg] one step in the visible order and return the new full order. */
    fun move(visible: List<String>, pkg: String, delta: Int): List<String> {
        val list = visible.toMutableList()
        val i = list.indexOf(pkg)
        if (i < 0) return list
        val j = (i + delta).coerceIn(0, list.lastIndex)
        list.removeAt(i)
        list.add(j, pkg)
        return list
    }

    fun launch(context: Context, app: AppEntry): Boolean {
        val pm = context.packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(app.pkg)
            ?: pm.getLaunchIntentForPackage(app.pkg)
            ?: Intent(Intent.ACTION_MAIN).setComponent(app.component)
        return start(context, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openInPlay(context: Context, pkg: String): Boolean =
        start(context, Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            || start(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    fun openAppInfo(context: Context, pkg: String): Boolean =
        start(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    /** Opens the system's own uninstall confirmation; the owner confirms there. */
    fun uninstall(context: Context, pkg: String): Boolean =
        start(context, Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            || openAppInfo(context, pkg)

    fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent); true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }

    fun icon(context: Context, pkg: String, sizePx: Int): Bitmap? = try {
        context.packageManager.getApplicationIcon(pkg).toBitmapSafe(sizePx, sizePx)
    } catch (e: Exception) {
        null
    }

    fun banner(context: Context, pkg: String, w: Int, h: Int): Bitmap? = try {
        context.packageManager.getApplicationBanner(pkg)?.toBitmapSafe(w, h)
    } catch (e: Exception) {
        null
    }

    private fun Drawable.toBitmapSafe(w: Int, h: Int): Bitmap =
        if (this is BitmapDrawable && bitmap != null) toBitmap(w, h) else {
            val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val c = Canvas(b)
            setBounds(0, 0, w, h)
            draw(c)
            b
        }

    /** Disabled packages, read only: the app cannot change another app's state, the owner does that. */
    fun disabledPackages(context: Context): List<String> =
        context.packageManager.getInstalledApplications(PackageManager.MATCH_DISABLED_COMPONENTS)
            .filter { !it.enabled }.map { it.packageName }.sorted()

    // ---- local library -------------------------------------------------------------------------------

    data class Video(val uri: Uri, val title: String, val durationMs: Long)

    fun videos(context: Context, limit: Int = 60): List<Video> {
        val out = ArrayList<Video>()
        val proj = arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.DURATION)
        runCatching {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, proj, null, null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC",
            )?.use { c ->
                val id = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val name = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dur = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                while (c.moveToNext() && out.size < limit) {
                    out += Video(
                        Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, c.getLong(id).toString()),
                        c.getString(name) ?: "",
                        c.getLong(dur),
                    )
                }
            }
        }
        return out
    }

    fun play(context: Context, v: Video): Boolean = start(
        context,
        Intent(Intent.ACTION_VIEW).setDataAndType(v.uri, "video/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
