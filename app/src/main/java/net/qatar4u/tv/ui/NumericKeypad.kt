package net.qatar4u.tv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import net.qatar4u.tv.R

/** Shared phone-style keypad: visual order stays 123 / 456 / 789 / Clear 0 Confirm in RTL too. */
@Composable
fun NumericKeypad(
    onDigit: (String) -> Unit,
    onClear: () -> Unit,
    onConfirm: () -> Unit,
    firstFocus: FocusRequester? = null,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(Modifier.widthIn(max = 360.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (row in listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("clear", "0", "confirm"))) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (key in row) {
                        val digit = key.length == 1
                        val label = when (key) {
                            "clear" -> stringResource(R.string.keypad_clear)
                            "confirm" -> stringResource(R.string.keypad_confirm)
                            else -> key
                        }
                        FocusCard(
                            Modifier.weight(1f).height(64.dp).then(
                                if (key == "1" && firstFocus != null) Modifier.focusRequester(firstFocus) else Modifier
                            ),
                            onClick = {
                                when (key) {
                                    "clear" -> onClear()
                                    "confirm" -> onConfirm()
                                    else -> onDigit(key)
                                }
                            },
                        ) {
                            Text(label, color = Q4u.TextPrimary,
                                style = if (digit) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.labelLarge,
                                maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
