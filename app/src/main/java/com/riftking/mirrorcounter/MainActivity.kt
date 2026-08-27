package com.riftking.mirrorcounter

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.wear.ambient.AmbientLifecycleObserver

class MainActivity : ComponentActivity() {

    private lateinit var counterView: CounterView
    private lateinit var ambientObserver: AmbientLifecycleObserver

    private val ambientCallback = object : AmbientLifecycleObserver.AmbientLifecycleCallback {
        override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
            counterView.setAmbientMode(true)
        }

        override fun onExitAmbient() {
            counterView.setAmbientMode(false)
        }

        override fun onUpdateAmbient() {
            counterView.ambientTick()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Do NOT use FLAG_KEEP_SCREEN_ON. Wear OS is allowed to enter low-power Ambient mode.
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )

        counterView = CounterView(this)
        setContentView(counterView)

        ambientObserver = AmbientLifecycleObserver(this, mainExecutor, ambientCallback)
        lifecycle.addObserver(ambientObserver)

        // The dedicated-device build intentionally ignores Back.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })
    }

    override fun onDestroy() {
        lifecycle.removeObserver(ambientObserver)
        super.onDestroy()
    }

    fun openSystemSettings() {
        startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private class CounterView(private val host: MainActivity) : View(host) {

    private val prefs = host.getSharedPreferences("score", android.content.Context.MODE_PRIVATE)
    private var opponent = prefs.getInt("opponent", 0).coerceIn(0, 99)
    private var me = prefs.getInt("me", 0).coerceIn(0, 99)

    private var ambient = false
    private var burnInStep = 0

    private var downX = 0f
    private var downY = 0f
    private var downAt = 0L
    private var downWasAmbient = false

    private var dividerTapCount = 0
    private var lastDividerTapAt = 0L

    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("sans", android.graphics.Typeface.BOLD)
    }

    private val controlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(190, 190, 190)
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL)
    }

    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(70, 70, 70)
        strokeWidth = 2f
    }

    init {
        setBackgroundColor(Color.BLACK)
        isFocusable = true
        isFocusableInTouchMode = true
        keepScreenOn = false
    }

    fun setAmbientMode(enabled: Boolean) {
        ambient = enabled
        invalidate()
    }

    fun ambientTick() {
        burnInStep = (burnInStep + 1) % 7
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)

        val w = width.toFloat()
        val h = height.toFloat()
        val mid = h / 2f
        val offset = if (ambient) burnOffset() else 0f

        if (!ambient) {
            dividerPaint.color = Color.rgb(55, 55, 55)
            canvas.drawLine(w * 0.30f, mid, w * 0.70f, mid, dividerPaint)
        }

        drawHalf(canvas, RectF(0f, 0f, w, mid), opponent, rotated = true, offset)
        drawHalf(canvas, RectF(0f, mid, w, h), me, rotated = false, offset)
    }

    private fun drawHalf(canvas: Canvas, rect: RectF, value: Int, rotated: Boolean, offset: Float) {
        canvas.save()
        if (rotated) canvas.rotate(180f, rect.centerX(), rect.centerY())

        val cx = rect.centerX() + offset
        val cy = rect.centerY() + offset

        scorePaint.textSize = width * if (ambient) 0.205f else 0.225f
        scorePaint.color = if (ambient) Color.rgb(92, 92, 92) else Color.WHITE
        scorePaint.isAntiAlias = !ambient

        val fm = scorePaint.fontMetrics
        val baseline = cy - (fm.ascent + fm.descent) / 2f
        canvas.drawText(value.toString(), cx, baseline, scorePaint)

        if (!ambient) {
            controlPaint.textSize = width * 0.095f
            controlPaint.color = Color.rgb(155, 155, 155)
            controlPaint.isAntiAlias = true
            val cfm = controlPaint.fontMetrics
            val controlBaseline = cy - (cfm.ascent + cfm.descent) / 2f
            canvas.drawText("−", rect.left + width * 0.18f, controlBaseline, controlPaint)
            canvas.drawText("+", rect.right - width * 0.18f, controlBaseline, controlPaint)
        }

        canvas.restore()
    }

    private fun burnOffset(): Float {
        // Tiny movement to reduce static-pixel burn-in in Ambient mode.
        val steps = floatArrayOf(-3f, -2f, -1f, 0f, 1f, 2f, 3f)
        return steps[burnInStep]
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                downAt = android.os.SystemClock.uptimeMillis()
                downWasAmbient = ambient
                return true
            }

            MotionEvent.ACTION_UP -> {
                // First touch while AOD/Ambient is active only wakes the watch.
                if (downWasAmbient) return true

                val now = android.os.SystemClock.uptimeMillis()
                val duration = now - downAt
                val moved = kotlin.math.hypot(event.x - downX, event.y - downY)
                if (moved > width * 0.10f) return true

                val mid = height / 2f
                val dividerBand = height * 0.075f

                if (kotlin.math.abs(event.y - mid) <= dividerBand) {
                    if (duration >= 1000L) {
                        opponent = 0
                        me = 0
                        save()
                        hapticStrong()
                        invalidate()
                    } else {
                        // Hidden escape hatch: 7 quick taps on the center divider opens Settings.
                        dividerTapCount = if (now - lastDividerTapAt <= 1800L) dividerTapCount + 1 else 1
                        lastDividerTapAt = now
                        if (dividerTapCount >= 7) {
                            dividerTapCount = 0
                            host.openSystemSettings()
                        }
                    }
                    return true
                }

                val topHalf = event.y < mid
                var localX = event.x
                if (topHalf) localX = width - localX // compensate for the 180° top layout

                if (duration >= 900L && localX in width * 0.34f..width * 0.66f) {
                    if (topHalf) opponent = 0 else me = 0
                    save()
                    hapticStrong()
                    invalidate()
                    return true
                }

                val delta = if (localX < width * 0.34f) -1 else +1
                if (topHalf) opponent = (opponent + delta).coerceIn(0, 99)
                else me = (me + delta).coerceIn(0, 99)

                save()
                hapticTick()
                invalidate()
                return true
            }
        }
        return true
    }

    private fun save() {
        prefs.edit().putInt("opponent", opponent).putInt("me", me).apply()
    }

    private fun hapticTick() {
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    private fun hapticStrong() {
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}
