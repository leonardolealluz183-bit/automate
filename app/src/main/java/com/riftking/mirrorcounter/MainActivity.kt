package com.riftking.mirrorcounter

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.wear.ambient.AmbientLifecycleObserver

open class MainActivity : ComponentActivity() {
    internal lateinit var counterView: CounterView
    private lateinit var ambientObserver: AmbientLifecycleObserver

    private val ambientCallback = object : AmbientLifecycleObserver.AmbientLifecycleCallback {
        override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
            counterView.setAmbientMode(true)
        }
        override fun onExitAmbient() { counterView.setAmbientMode(false) }
        override fun onUpdateAmbient() { counterView.ambientTick() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        counterView = CounterView(this)
        setContentView(counterView)
        ambientObserver = createAmbientObserver()
        lifecycle.addObserver(ambientObserver)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })
    }

    // Wear's hardware bridge is supplied by the watch OS; tests replace only that bridge.
    protected open fun createAmbientObserver(): AmbientLifecycleObserver =
        AmbientLifecycleObserver(this, mainExecutor, ambientCallback)

    override fun onResume() {
        super.onResume()
        // User-approved tabletop strategy. This is NOT hardware low-power Ambient mode.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        counterView.resumeSession()
    }

    override fun onPause() {
        counterView.pauseSession()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setAppBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
        super.onPause()
    }

    override fun onDestroy() {
        counterView.pauseSession()
        lifecycle.removeObserver(ambientObserver)
        super.onDestroy()
    }

    internal fun setAppBrightness(value: Float) {
        window.attributes = window.attributes.apply { screenBrightness = value }
    }

    fun openSystemSettings() {
        startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

internal class CounterView(private val host: MainActivity) : View(host) {
    private val prefs = host.getSharedPreferences("score", android.content.Context.MODE_PRIVATE)
    internal var opponent = prefs.getInt("opponent", 0).coerceIn(0, 99)
        private set
    internal var me = prefs.getInt("me", 0).coerceIn(0, 99)
        private set
    internal var dimmed = false
        private set
    private var ambient = false
    private var running = false
    private var burnInStep = 0
    private var downX = 0f
    private var downY = 0f
    private var downAt = 0L
    private var downWasAmbient = false
    private var trackingTouch = false
    private var dividerTapCount = 0
    private var lastDividerTapAt = 0L
    private var opponentFlash = Color.WHITE
    private var meFlash = Color.WHITE
    private val handler = Handler(Looper.getMainLooper())
    private val feedback = TouchFeedback(host)
    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("sans", android.graphics.Typeface.BOLD)
    }
    private val fontMetrics = Paint.FontMetrics()
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(55, 55, 55)
        strokeWidth = 2f
    }
    private val clearOpponentFlash = Runnable { opponentFlash = Color.WHITE; invalidate() }
    private val clearMeFlash = Runnable { meFlash = Color.WHITE; invalidate() }
    private val shiftPixels = object : Runnable {
        override fun run() {
            if (!running || !dimmed || ambient) return
            burnInStep = (burnInStep + 1) % 7
            invalidate()
            // One redraw per minute, never a frame/animation loop or a wakeup alarm.
            handler.postDelayed(this, PIXEL_SHIFT_MS)
        }
    }
    private val dimDisplay = Runnable {
        if (running && !ambient) {
            dimmed = true
            host.setAppBrightness(DIM_BRIGHTNESS)
            invalidate()
            handler.removeCallbacks(shiftPixels)
            handler.postDelayed(shiftPixels, PIXEL_SHIFT_MS)
        }
    }

    init {
        setBackgroundColor(Color.BLACK)
        isFocusable = true
        isFocusableInTouchMode = true
        isClickable = true
        isSoundEffectsEnabled = false // TouchFeedback owns the single short beep.
    }

    internal fun resumeSession() {
        running = true
        feedback.start()
        if (ambient) {
            host.setAppBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
        } else {
            noteInteraction()
        }
    }

    internal fun pauseSession() {
        running = false
        trackingTouch = false
        handler.removeCallbacksAndMessages(null)
        opponentFlash = Color.WHITE
        meFlash = Color.WHITE
        feedback.stop()
    }

    private fun noteInteraction() {
        if (!running || ambient) return
        handler.removeCallbacks(dimDisplay)
        handler.removeCallbacks(shiftPixels)
        dimmed = false
        host.setAppBrightness(ACTIVE_BRIGHTNESS)
        handler.postDelayed(dimDisplay, DIM_AFTER_MS)
        invalidate()
    }

    fun setAmbientMode(enabled: Boolean) {
        ambient = enabled
        handler.removeCallbacksAndMessages(null)
        opponentFlash = Color.WHITE
        meFlash = Color.WHITE
        if (enabled) {
            host.setAppBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
        } else if (running) {
            noteInteraction()
        }
        invalidate()
    }

    fun ambientTick() {
        if (!running || !ambient) return
        burnInStep = (burnInStep + 1) % 7
        invalidate()
    }

    internal fun scoreColor(topHalf: Boolean): Int = when {
        ambient -> Color.rgb(92, 92, 92)
        dimmed -> Color.rgb(190, 190, 190)
        topHalf -> opponentFlash
        else -> meFlash
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)
        val w = width.toFloat()
        val h = height.toFloat()
        if (!ambient && !dimmed) {
            canvas.drawLine(w * 0.30f, h / 2f, w * 0.70f, h / 2f, dividerPaint)
        }
        // Keep the hit regions fixed. Move only the drawn scores a few pixels.
        val offset = if (ambient || dimmed) (burnInStep - 3).toFloat() else 0f
        drawHalf(canvas, w / 2f, h / 4f, opponent, true, offset)
        drawHalf(canvas, w / 2f, h * 0.75f, me, false, offset)
    }

    private fun drawHalf(canvas: Canvas, cx: Float, cy: Float, value: Int, topHalf: Boolean, offset: Float) {
        canvas.save()
        if (topHalf) canvas.rotate(180f, cx, cy)
        scorePaint.textSize = width * if (ambient) 0.205f else 0.225f
        scorePaint.color = scoreColor(topHalf)
        scorePaint.isAntiAlias = !ambient
        scorePaint.getFontMetrics(fontMetrics)
        val baseline = cy + offset - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(value.toString(), cx + offset, baseline, scorePaint)
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!running) return true
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                trackingTouch = true
                downX = event.x
                downY = event.y
                downAt = SystemClock.uptimeMillis()
                downWasAmbient = ambient
                noteInteraction() // A dim tabletop touch wakes AND scores on release.
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> {
                trackingTouch = false
            }
            MotionEvent.ACTION_UP -> {
                if (!trackingTouch) return true
                trackingTouch = false
                if (downWasAmbient || ambient) return true // Only true OS ambient needs a wake-only touch.
                noteInteraction()
                val duration = SystemClock.uptimeMillis() - downAt
                if (kotlin.math.hypot(event.x - downX, event.y - downY) > width * 0.10f) return true
                performClick()
                handleTap(event.x, event.y, duration)
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun handleTap(x: Float, y: Float, duration: Long) {
        val now = SystemClock.uptimeMillis()
        val mid = height / 2f
        if (kotlin.math.abs(y - mid) <= height * 0.075f) {
            if (duration >= 1000L) {
                val topChanged = opponent != 0
                val bottomChanged = me != 0
                opponent = 0
                me = 0
                dividerTapCount = 0
                save()
                if (topChanged) flash(true, -1)
                if (bottomChanged) flash(false, -1)
                feedback.play(reset = true)
            } else {
                dividerTapCount = if (now - lastDividerTapAt <= 1800L) dividerTapCount + 1 else 1
                lastDividerTapAt = now
                if (dividerTapCount >= 7) {
                    dividerTapCount = 0
                    host.openSystemSettings()
                }
            }
            return
        }
        dividerTapCount = 0
        val topHalf = y < mid
        val localX = if (topHalf) width - x else x
        val oldValue = if (topHalf) opponent else me
        val reset = duration >= 900L && localX in width * 0.34f..width * 0.66f
        val delta = if (localX < width * 0.34f) -1 else 1
        val newValue = if (reset) 0 else (oldValue + delta).coerceIn(0, 99)
        if (topHalf) opponent = newValue else me = newValue
        if (newValue != oldValue) {
            save()
            flash(topHalf, newValue - oldValue)
        }
        // A boundary tap is still acknowledged, but never flashes a nonexistent score change.
        feedback.play(reset)
    }

    private fun flash(topHalf: Boolean, delta: Int) {
        val color = if (delta > 0) ADD_COLOR else SUBTRACT_COLOR
        val clear = if (topHalf) clearOpponentFlash else clearMeFlash
        if (topHalf) opponentFlash = color else meFlash = color
        handler.removeCallbacks(clear)
        handler.postDelayed(clear, FLASH_MS)
        invalidate()
    }

    private fun save() {
        prefs.edit().putInt("opponent", opponent).putInt("me", me).apply()
    }

    companion object {
        internal const val DIM_AFTER_MS = 8_000L
        internal const val FLASH_MS = 200L
        internal const val PIXEL_SHIFT_MS = 60_000L
        internal const val ACTIVE_BRIGHTNESS = 0.35f
        internal const val DIM_BRIGHTNESS = 0.05f
        internal val ADD_COLOR = Color.rgb(76, 220, 125)
        internal val SUBTRACT_COLOR = Color.rgb(255, 95, 100)
    }
}
