param(
    [Parameter(Mandatory=$true)] [string]$Watch
)

$ErrorActionPreference = "Continue"
& adb connect $Watch | Out-Host

# Reversible only: DISABLE, never uninstall. This list intentionally avoids Android core,
# System UI, Settings, Bluetooth/Wi-Fi stacks, package manager and Google Play Services.
$packages = @(
    "com.samsung.android.bixby.agent",
    "com.samsung.android.bixby.wakeup",
    "com.samsung.android.samsungpay.gear",
    "com.samsung.android.wear.shealth",
    "com.samsung.android.shealthmonitor",
    "com.google.android.apps.maps",
    "com.samsung.android.gallery.watch",
    "com.samsung.android.calendar",
    "com.samsung.android.watch.worldclock",
    "com.samsung.android.app.reminder",
    "com.samsung.android.messaging",
    "com.google.android.apps.messaging",
    "com.samsung.android.watch.cameracontroller",
    "com.google.android.wearable.assistant",
    "com.google.android.apps.walletnfcrel",
    "com.microsoft.office.outlook",
    "com.samsung.android.watch.budscontroller",
    "com.samsung.android.wearable.music",
    "com.samsung.android.wear.calculator",
    "com.samsung.android.wear.voicerecorder",
    "com.android.vending"
)

Write-Host "Disabling nonessential apps (reversible)..."
foreach ($p in $packages) {
    $exists = & adb shell pm path $p 2>$null
    if ($exists) {
        Write-Host "  disable $p"
        & adb shell pm disable-user --user 0 $p | Out-Host
    }
}

& adb shell settings put global window_animation_scale 0
& adb shell settings put global transition_animation_scale 0
& adb shell settings put global animator_duration_scale 0
& adb shell settings put system screen_off_timeout 15000
& adb shell settings put secure wake_gesture_enabled 0
& adb shell cmd location set-location-enabled false

Write-Host ""
Write-Host "Lockdown applied. Wi-Fi/Bluetooth were deliberately NOT disabled by script so you keep an ADB recovery path."
Write-Host "After testing, you can turn Wi-Fi, Bluetooth and NFC off manually in Settings."
Write-Host "Hidden Settings escape: tap the center divider 7 times quickly."
