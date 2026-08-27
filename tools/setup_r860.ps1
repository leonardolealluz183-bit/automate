param(
    [Parameter(Mandatory=$true)] [string]$Watch,
    [Parameter(Mandatory=$true)] [string]$Apk
)

$ErrorActionPreference = "Stop"

function ADB([string[]]$Args) {
    & adb @Args
    if ($LASTEXITCODE -ne 0) { throw "ADB failed: adb $($Args -join ' ')" }
}

Write-Host "Connecting to SM-R860 at $Watch..."
ADB @("connect", $Watch)

Write-Host "Saving current HOME component..."
$stockHome = (& adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME 2>$null | Select-Object -Last 1).Trim()
if ($stockHome) {
    Set-Content -Path "$PSScriptRoot\stock_home.txt" -Value $stockHome
    Write-Host "Stock HOME: $stockHome"
}

Write-Host "Installing Mirror Counter..."
ADB @("install", "-r", $Apk)

Write-Host "Selecting Mirror Counter as HOME..."
& adb shell cmd package set-home-activity com.riftking.mirrorcounter/.MainActivity

Write-Host "Applying low-overhead UI settings..."
& adb shell settings put global window_animation_scale 0
& adb shell settings put global transition_animation_scale 0
& adb shell settings put global animator_duration_scale 0
& adb shell settings put system screen_off_timeout 15000
& adb shell settings put secure wake_gesture_enabled 0
& adb shell cmd location set-location-enabled false

Write-Host "Starting counter..."
ADB @("shell", "am", "start", "-n", "com.riftking.mirrorcounter/.MainActivity")

Write-Host ""
Write-Host "DONE. Verify the counter works before running lockdown_r860.ps1."
Write-Host "Tip: enable Always-On Display on the watch so the two scores remain visible in Ambient mode."
