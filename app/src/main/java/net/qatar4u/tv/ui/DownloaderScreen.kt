package net.qatar4u.tv.ui

import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import net.qatar4u.tv.R
import net.qatar4u.tv.download.ApkDownloads
import net.qatar4u.tv.download.CodeResolver
import net.qatar4u.tv.download.DownloadTarget

@Composable
fun DownloaderScreen(onToast: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var input by rememberSaveable { mutableStateOf("") }
    var resolving by remember { mutableStateOf(false) }
    var starting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var target by remember { mutableStateOf<DownloadTarget?>(null) }
    var download by remember { mutableStateOf<ApkDownloads.Snapshot?>(null) }
    val failure = stringResource(R.string.download_error)
    val installFailure = stringResource(R.string.download_install_error)
    val active = starting || download?.status in listOf(DownloadManager.STATUS_PENDING,
        DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PAUSED)

    fun resolve() {
        if (resolving || input.isBlank()) return
        val requestedInput = input
        resolving = true
        error = false
        target = null
        scope.launch {
            try {
                val resolved = withTimeout(30_000L) { CodeResolver.resolve(requestedInput) }
                if (input == requestedInput) target = resolved
            } catch (_: TimeoutCancellationException) { error = true
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { error = true
            } finally { resolving = false }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            download = withContext(Dispatchers.IO) { runCatching { ApkDownloads.snapshot(context) }.getOrNull() }
            delay(1000)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.download_help), color = Q4u.TextSecondary)
        OutlinedTextField(
            value = input,
            onValueChange = { input = it.take(2048); target = null; error = false },
            label = { Text(stringResource(R.string.download_input)) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )
        // TV users can enter the complete numeric code without an on-screen keyboard.
        for (row in listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("⌫", "0", "✓"))) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (key in row) FocusButton(key, Modifier.width(72.dp), onClick = {
                    when (key) {
                        "⌫" -> { input = input.dropLast(1); target = null }
                        "✓" -> resolve()
                        else -> { if (input.length < 12) input += key; target = null }
                    }
                })
            }
        }
        FocusButton(stringResource(if (resolving) R.string.download_resolving else R.string.download_resolve), onClick = { resolve() })
        if (error) Text(failure, color = Q4u.Danger)
        target?.let { resolved ->
            Text(resolved.url, color = Q4u.TextPrimary)
            if (!resolved.apk) {
                Text(stringResource(R.string.download_webpage), color = Q4u.TextSecondary)
                FocusButton(stringResource(R.string.download_open_page), onClick = {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(resolved.url))) }
                        .onFailure { onToast(failure) }
                })
            } else if (!active) {
                FocusButton(stringResource(R.string.download_start), onClick = {
                    if (!starting) {
                        starting = true
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { ApkDownloads.start(context, resolved) }
                                download = withContext(Dispatchers.IO) { ApkDownloads.snapshot(context) }
                            } catch (e: CancellationException) { throw e
                            } catch (_: Exception) { onToast(failure)
                            } finally { starting = false }
                        }
                    }
                })
            }
        }
        download?.let { state ->
            val status = when (state.status) {
                DownloadManager.STATUS_SUCCESSFUL -> R.string.download_ready
                DownloadManager.STATUS_FAILED -> R.string.download_failed
                DownloadManager.STATUS_PAUSED -> R.string.download_paused
                else -> R.string.download_running
            }
            Text(stringResource(status), color = Q4u.TextPrimary)
            if (state.total > 0) {
                val percent = (state.bytes.toDouble() / state.total * 100).toInt().coerceIn(0, 100)
                Text("$percent%", color = LocalAccent.current)
            }
            if (active) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            if (state.status == DownloadManager.STATUS_SUCCESSFUL) {
                FocusButton(stringResource(R.string.download_install), onClick = {
                    runCatching { ApkDownloads.install(context) }.onFailure { onToast(installFailure) }
                })
                Text(stringResource(R.string.download_install_help), color = Q4u.TextSecondary)
            }
            FocusButton(stringResource(if (active) R.string.download_cancel else R.string.download_delete), onClick = {
                runCatching { ApkDownloads.remove(context); download = null }.onFailure { onToast(failure) }
            })
        }
    }
}
