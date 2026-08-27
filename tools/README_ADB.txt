SM-R860 SETUP — WINDOWS / ADB

1) Build the APK in Android Studio:
   Build > Build APK(s)
   APK path is normally:
   app\build\outputs\apk\debug\app-debug.apk

2) On the Watch 4:
   Settings > About watch > Software information > tap Software version repeatedly
   to enable Developer options.

3) Developer options:
   - ADB debugging: ON
   - Wireless debugging: ON
   - Pair/connect the watch to the PC using the IP:port shown by Wear OS.

4) Run PowerShell from this tools folder:
   .\setup_r860.ps1 -Watch "IP:PORT" -Apk "C:\path\to\app-debug.apk"

5) Test the counter. Only after it works, optionally run:
   .\lockdown_r860.ps1 -Watch "IP:PORT"

IMPORTANT:
- lockdown uses pm disable-user, not uninstall.
- It does NOT disable Wi-Fi/Bluetooth automatically so you don't lose recovery access.
- After everything is tested, manually turn off Wi-Fi, Bluetooth, NFC, location,
  raise-to-wake, notifications and health measurements.
- Keep Always-On Display enabled if you need the score to remain visible.
- Seven quick taps on the CENTER divider opens Android Settings.
- Long-press center divider: reset both scores.
- Long-press a score: reset that score.
- Left side of each player's view: -1. Score/right side: +1.
