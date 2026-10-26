package net.qatar4u.tv.ui

import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextAlign
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
import net.qatar4u.tv.download.CodeLinks
import net.qatar4u.tv.download.CodeResolver
import net.qatar4u.tv.download.DownloadTarget
import net.qatar4u.tv.download.ResolveException
import net.qatar4u.tv.download.ResolveFailure

@Composable
fun DownloaderScreen(onToast: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var input by rememberSaveable { mutableStateOf("") }
    var resolving by remember { mutableStateOf(false) }
    var starting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<ResolveFailure?>(null) }
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
        error = null
        target = null
        scope.launch {
            try {
                val resolved = withTimeout(30_000L) { CodeResolver.resolve(requestedInput) }
                if (input == requestedInput) target = resolved
            } catch (_: TimeoutCancellationException) { error = ResolveFailure.NETWORK
            } catch (e: CancellationException) { throw e
            } catch (e: ResolveException) { error = e.reason
            } catch (_: Exception) { error = ResolveFailure.NETWORK
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
            onValueChange = { input = it.take(2048); target = null; error = null },
            label = { Text(stringResource(R.string.download_input)) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            textStyle = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Ltr, textAlign = TextAlign.Left),
        )
        NumericKeypad(
            onDigit = { key ->
                if (input.length < 12) input += key
                target = null; error = null
            },
            onClear = { input = ""; target = null; error = null },
            onConfirm = { resolve() },
        )
        if (resolving) Text(stringResource(R.string.download_resolving), color = Q4u.TextSecondary)
        error?.let { reason ->
            Text(stringResource(when (reason) {
                ResolveFailure.INPUT -> R.string.download_invalid_input
                ResolveFailure.BLOCKED -> R.string.download_blocked
                ResolveFailure.NOT_FOUND -> R.string.download_not_found
                ResolveFailure.REDIRECT -> R.string.download_redirect_error
                ResolveFailure.NETWORK -> R.string.download_network_error
            }), color = Q4u.Danger)
            if (reason != ResolveFailure.INPUT) FocusButton(stringResource(R.string.download_open_page), onClick = {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(CodeLinks.inputUrl(input)))) }
                    .onFailure { onToast(failure) }
            })
        }
        target?.let { resolved ->
            Text(resolved.url, color = Q4u.TextPrimary, style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr))
            if (resolved.url.startsWith("http:")) Text(stringResource(R.string.download_http), color = LocalAccent.current)
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
            if (state.status == DownloadManager.STATUS_FAILED) Text(stringResource(R.string.download_failure_reason, state.reason), color = Q4u.Danger)
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
