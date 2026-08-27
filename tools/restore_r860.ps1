param(
    [Parameter(Mandatory=$true)] [string]$Watch
)

$ErrorActionPreference = "Continue"
& adb connect $Watch | Out-Host

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

foreach ($p in $packages) {
    $exists = & adb shell pm path $p 2>$null
    if ($exists) { & adb shell pm enable --user 0 $p | Out-Host }
}

$homeFile = "$PSScriptRoot\stock_home.txt"
if (Test-Path $homeFile) {
    $stockHome = (Get-Content $homeFile -Raw).Trim()
    if ($stockHome) {
        Write-Host "Restoring HOME: $stockHome"
        & adb shell cmd package set-home-activity $stockHome | Out-Host
    }
}

& adb shell settings put secure wake_gesture_enabled 1
& adb shell cmd location set-location-enabled true
Write-Host "Restore complete. Mirror Counter remains installed but is no longer forced as HOME if stock_home.txt was available."
