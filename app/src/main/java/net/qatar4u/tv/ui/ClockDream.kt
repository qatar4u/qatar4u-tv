package net.qatar4u.tv.ui

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextClock

/** The system screensaver: a dim gold clock that moves a little every minute. */
class ClockDream : DreamService() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var clock: TextClock
    private var step = 0

    private val drift = object : Runnable {
        override fun run() {
            val root = clock.parent as? FrameLayout ?: return
            val w = (root.width - clock.width).coerceAtLeast(1)
            val h = (root.height - clock.height).coerceAtLeast(1)
            step++
            clock.translationX = ((step * 37) % 100) / 100f * w
            clock.translationY = ((step * 53) % 100) / 100f * h
            handler.postDelayed(this, 60_000)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = false
        clock = TextClock(this).apply {
            setTextColor(Color.parseColor("#C8D9B76E"))
            textSize = 96f
            format12Hour = "h:mm"
            format24Hour = "HH:mm"
        }
        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        root.addView(clock, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.START))
        setContentView(root)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        handler.post(drift)
    }

    override fun onDreamingStopped() {
        handler.removeCallbacks(drift)
        super.onDreamingStopped()
    }
}
