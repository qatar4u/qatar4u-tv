package net.qatar4u.tv.download

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

object ApkDownloads {
    private const val MIME = "application/vnd.android.package-archive"
    private fun manager(context: Context) = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private fun prefs(context: Context) = context.getSharedPreferences("apk_download", Context.MODE_PRIVATE)

    data class Snapshot(val id: Long, val status: Int, val bytes: Long, val total: Long)

    fun start(context: Context, target: DownloadTarget): Long {
        require(target.apk)
        val previous = snapshot(context)
        require(previous?.status !in listOf(DownloadManager.STATUS_PENDING, DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PAUSED))
        val request = DownloadManager.Request(Uri.parse(CodeLinks.https(target.url)))
            .setTitle("Qatar4u TV · APK")
            .setMimeType(MIME)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "app-${System.currentTimeMillis()}.apk")
        val id = manager(context).enqueue(request)
        if (previous != null) manager(context).remove(previous.id)
        prefs(context).edit().putLong("id", id).apply()
        return id
    }

    fun snapshot(context: Context): Snapshot? {
        val id = prefs(context).getLong("id", -1)
        if (id < 0) return null
        manager(context).query(DownloadManager.Query().setFilterById(id))?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            fun long(column: String) = cursor.getLong(cursor.getColumnIndexOrThrow(column))
            return Snapshot(id, long(DownloadManager.COLUMN_STATUS).toInt(),
                long(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR), long(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
        }
        return null
    }

    fun remove(context: Context) {
        val id = prefs(context).getLong("id", -1)
        if (id >= 0) manager(context).remove(id)
        prefs(context).edit().remove("id").apply()
    }

    /** Opens the OS permission screen or installer; installation always needs the owner's confirmation. */
    fun install(context: Context) {
        val snapshot = requireNotNull(snapshot(context))
        require(snapshot.status == DownloadManager.STATUS_SUCCESSFUL)
        val local = manager(context).query(DownloadManager.Query().setFilterById(snapshot.id)).use { cursor ->
            require(cursor.moveToFirst())
            cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
        }
        val uri = Uri.parse(local)
        require(uri.scheme == "file")
        val file = File(requireNotNull(uri.path)).canonicalFile
        val directory = requireNotNull(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)).canonicalFile
        require(file.parentFile == directory && file.isFile)
        requireNotNull(context.packageManager.getPackageArchiveInfo(file.path, 0)) { "invalid APK" }
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
            return
        }
        val shared = FileProvider.getUriForFile(context, "${context.packageName}.downloads", file)
        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(shared, MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }
}
