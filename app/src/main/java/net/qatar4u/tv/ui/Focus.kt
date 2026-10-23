package net.qatar4u.tv.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Something the remote can land on: a gold frame and a slight zoom when focused, OK or a tap to activate,
 * and Menu (or holding OK) for the secondary action.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FocusCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    background: Color = Q4u.Surface.copy(alpha = 0.82f),
    focusedScale: Float = 1.06f,
    borderWidth: Dp = 3.dp,
    onFocus: (Boolean) -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    var held by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) focusedScale else 1f, tween(140), label = "scale")
    val accent = LocalAccent.current
    val source = remember { MutableInteractionSource() }
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(if (focused) Modifier.shadow(18.dp, shape, ambientColor = accent, spotColor = accent) else Modifier)
            .clip(shape)
            .background(background)
            .border(BorderStroke(if (focused) borderWidth else 1.dp, if (focused) accent else Color.White.copy(alpha = 0.08f)), shape)
            .onFocusChanged { focused = it.isFocused; onFocus(it.isFocused) }
            .onPreviewKeyEvent { e ->
                val long = onLongClick ?: return@onPreviewKeyEvent false
                val ok = e.key == Key.DirectionCenter || e.key == Key.Enter || e.key == Key.NumPadEnter
                val menu = e.key == Key.Menu || e.key.nativeKeyCode == android.view.KeyEvent.KEYCODE_MENU
                when {
                    menu && e.type == KeyEventType.KeyUp -> { long(); true }
                    ok && e.type == KeyEventType.KeyDown && e.nativeKeyEvent.repeatCount == 1 -> { held = true; long(); true }
                    ok && e.type == KeyEventType.KeyDown && e.nativeKeyEvent.repeatCount > 1 -> true
                    // The release after a hold must not also open the app.
                    ok && e.type == KeyEventType.KeyUp && held -> { held = false; true }
                    else -> false
                }
            }
            .combinedClickable(
                interactionSource = source,
                indication = null,
                onLongClick = onLongClick,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) { content(focused) }
}

/** A plain text button for settings and dialogs, with the same focus treatment. */
@Composable
fun FocusButton(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onFocus: (Boolean) -> Unit = {},
    onClick: () -> Unit,
) {
    val accent = LocalAccent.current
    FocusCard(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        background = if (selected) accent.copy(alpha = 0.22f) else Q4u.Surface.copy(alpha = 0.9f),
        focusedScale = 1.03f,
        onFocus = onFocus,
        onClick = onClick,
    ) { f ->
        Text(
            text,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            color = if (f || selected) Q4u.TextPrimary else Q4u.TextSecondary,
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}
