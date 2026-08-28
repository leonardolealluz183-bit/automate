package com.riftking.mirrorcounter

import android.content.Context
import android.graphics.Color
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.MotionEvent
import android.view.WindowManager
import androidx.wear.ambient.AmbientLifecycleObserver
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30, 35])
@LooperMode(LooperMode.Mode.PAUSED)
class MainActivityTest {
    class TestActivity : MainActivity() {
        override fun createAmbientObserver(): AmbientLifecycleObserver =
            object : AmbientLifecycleObserver { override val isAmbient = false }
    }

    private lateinit var controller: ActivityController<TestActivity>
    private val activity get() = controller.get()
    private val view get() = activity.counterView

    @Before fun setUp() {
        RuntimeEnvironment.getApplication().getSharedPreferences("score", Context.MODE_PRIVATE)
            .edit().clear().commit()
        controller = Robolectric.buildActivity(TestActivity::class.java).setup()
        view.layout(0, 0, 396, 396)
    }

    @After fun tearDown() { controller.pause().stop().destroy() }

    private fun advance(ms: Long) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms)) }
    private fun event(action: Int, x: Float, y: Float, down: Long) {
        val e = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, y, 0)
        view.onTouchEvent(e)
        e.recycle()
    }
    private fun tap(x: Float, y: Float, duration: Long = 30) {
        val down = SystemClock.uptimeMillis()
        event(MotionEvent.ACTION_DOWN, x, y, down)
        advance(duration)
        event(MotionEvent.ACTION_UP, x, y, down)
    }

    @Test fun dimmedDisplayStaysOnAndFirstTapScores() {
        advance(CounterView.DIM_AFTER_MS)
        assertTrue(view.dimmed)
        assertEquals(CounterView.DIM_BRIGHTNESS, activity.window.attributes.screenBrightness)
        assertTrue(activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0)
        tap(198f, 297f)
        assertEquals(1, view.me)
        assertFalse(view.dimmed)
        assertEquals(CounterView.ACTIVE_BRIGHTNESS, activity.window.attributes.screenBrightness)
    }

    @Test fun eachTouchRestartsIdleTimeout() {
        advance(7_500)
        tap(198f, 297f)
        advance(500)
        assertFalse(view.dimmed)
        advance(7_500)
        assertTrue(view.dimmed)
    }

    @Test fun dimmedSessionHasNoOneMinuteOrOneHourExpiry() {
        advance(3_600_000)
        assertTrue(view.dimmed)
        assertEquals(CounterView.DIM_BRIGHTNESS, activity.window.attributes.screenBrightness)
        assertTrue(activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0)
        assertFalse(activity.isFinishing)
        tap(198f, 297f)
        assertEquals(1, view.me)
    }

    @Test fun flashIsLocalAndExpiresAfterTwoHundredMilliseconds() {
        tap(198f, 297f)
        assertEquals(CounterView.ADD_COLOR, view.scoreColor(false))
        assertEquals(Color.WHITE, view.scoreColor(true))
        advance(199)
        assertEquals(CounterView.ADD_COLOR, view.scoreColor(false))
        advance(1)
        assertEquals(Color.WHITE, view.scoreColor(false))
        tap(40f, 297f)
        assertEquals(0, view.me)
        assertEquals(CounterView.SUBTRACT_COLOR, view.scoreColor(false))
    }

    @Test fun rapidOppositeTapReplacesOldFlashTimeout() {
        tap(198f, 297f)
        advance(100)
        tap(40f, 297f)
        advance(70)
        assertEquals(CounterView.SUBTRACT_COLOR, view.scoreColor(false))
        advance(130)
        assertEquals(Color.WHITE, view.scoreColor(false))
    }

    @Test fun opponentControlsRemainRotatedAndScoresAreIndependent() {
        tap(40f, 99f)
        tap(198f, 297f)
        assertEquals(1, view.opponent)
        assertEquals(1, view.me)
        tap(356f, 99f)
        assertEquals(0, view.opponent)
        assertEquals(1, view.me)
        assertEquals(CounterView.SUBTRACT_COLOR, view.scoreColor(true))
    }

    @Test fun limitsDoNotFlashANonexistentChange() {
        tap(40f, 297f)
        assertEquals(0, view.me)
        assertEquals(Color.WHITE, view.scoreColor(false))
        repeat(99) { tap(198f, 297f, 1) }
        advance(200)
        tap(198f, 297f)
        assertEquals(99, view.me)
        assertEquals(Color.WHITE, view.scoreColor(false))
    }

    @Test fun resetGesturesStillResetOneOrBothPlayers() {
        tap(198f, 99f)
        tap(198f, 297f)
        tap(198f, 99f, 901)
        assertEquals(0, view.opponent)
        assertEquals(1, view.me)
        tap(198f, 99f)
        tap(198f, 198f, 1001)
        assertEquals(0, view.opponent)
        assertEquals(0, view.me)
        assertEquals(CounterView.SUBTRACT_COLOR, view.scoreColor(true))
        assertEquals(CounterView.SUBTRACT_COLOR, view.scoreColor(false))
    }

    @Test fun cancelledOrDraggedTouchesNeverScore() {
        val down = SystemClock.uptimeMillis()
        event(MotionEvent.ACTION_DOWN, 198f, 297f, down)
        event(MotionEvent.ACTION_CANCEL, 198f, 297f, down)
        event(MotionEvent.ACTION_UP, 198f, 297f, down)
        assertEquals(0, view.me)
        event(MotionEvent.ACTION_DOWN, 198f, 297f, down)
        event(MotionEvent.ACTION_UP, 300f, 297f, down)
        assertEquals(0, view.me)
    }

    @Test fun scoresSurviveActivityRecreation() {
        tap(198f, 99f)
        tap(198f, 297f)
        tap(198f, 297f)
        controller.recreate()
        view.layout(0, 0, 396, 396)
        assertEquals(1, view.opponent)
        assertEquals(2, view.me)
    }

    @Test fun sevenCenterTapsStillOpenSystemSettings() {
        repeat(7) { tap(198f, 198f) }
        assertEquals(Settings.ACTION_SETTINGS, shadowOf(activity).nextStartedActivity.action)
    }

    @Test fun backgroundReleasesDisplayAndCancelsPendingDimming() {
        tap(198f, 297f)
        controller.pause()
        assertEquals(0, activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        assertEquals(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE, activity.window.attributes.screenBrightness)
        advance(70_000)
        assertEquals(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE, activity.window.attributes.screenBrightness)
        assertEquals(Color.WHITE, view.scoreColor(false))
        controller.resume()
        assertFalse(view.dimmed)
        advance(CounterView.DIM_AFTER_MS)
        assertTrue(view.dimmed)
    }

    @Test fun actualSystemAmbientStillIgnoresTheWakeTouch() {
        view.setAmbientMode(true)
        tap(198f, 297f)
        assertEquals(0, view.me)
        view.setAmbientMode(false)
        tap(198f, 297f)
        assertEquals(1, view.me)
    }
}
