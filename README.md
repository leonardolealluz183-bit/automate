# Mirror Counter — SM-R860 Appliance Build

Purpose-built for Samsung Galaxy Watch4 **SM-R860 (40 mm Bluetooth, 396×396)** as a dedicated two-player score counter.

## Build with GitHub Actions

The repository root must contain `settings.gradle.kts`, `app/`, `gradlew`,
`gradle/wrapper/` and `.github/workflows/build.yml` (not the project ZIP).
The workflow runs on pushes, pull requests and manual dispatch, using Ubuntu,
Temurin JDK 17, Gradle 8.13, Android SDK 36 and Build Tools 35.0.0.

It runs `./gradlew assembleDebug`, Android lint and APK signature verification.
After a successful run, download the `MirrorCounter-R860-debug` artifact from
Actions. It contains `MirrorCounter-R860-debug.apk`, its SHA-256 checksum,
package metadata and the source commit/run identifiers.

The APK is debug-signed for sideloading; it is not a Play Store release.
A fresh CI runner may use a different debug signing key, so do not assume that
future APKs can replace an installed one without resolving signing continuity.
Do not uninstall an existing counter without first recording its scores.

Local build, with JDK 17 and the Android SDK installed:

```sh
./gradlew assembleDebug lintDebug --no-daemon --stacktrace
```

Before changing HOME or running any setup/lockdown script, install and launch
the app normally and verify controls, persistence, ambient mode and the hidden
Settings exit on the real watch. No device configuration has been changed by
the build preparation. See `BUILD_STATUS.md` for the actual validation status.

## What is different from the first build

- No Compose UI: one native custom `View`, no animation loop, no network code.
- No `FLAG_KEEP_SCREEN_ON`.
- Implements Wear OS `AmbientLifecycleObserver` so the system can enter low-power Ambient/AOD mode.
- Ambient mode draws only two dim gray scores on pure black OLED background.
- `+`, `−`, divider and nonessential pixels disappear in Ambient mode.
- Tiny periodic score-position shift reduces burn-in risk.
- Top score is rotated 180° for the opponent.
- Saves scores locally.
- Can be selected as the Android HOME activity, making the watch behave like a dedicated counter.
- Back is ignored while the counter is active.
- Hidden recovery path: seven rapid taps on the center divider opens system Settings.

## Controls

For each player's orientation:
- left region: −1
- score/right region: +1
- long press the score: reset that player
- long press center divider: reset both
- 7 quick taps center divider: open Android Settings

## Battery strategy

The app intentionally lets Wear OS transition out of the interactive display into Ambient/AOD. In Ambient mode only the scores remain, with all other pixels black. This is the main battery-saving mechanism; background app removal is secondary.

For an always-visible score, enable Always-On Display. For maximum battery life, disable AOD and wake the display only when checking/updating the score.

## Dedicated-device setup

See `tools/README_ADB.txt`. `setup_r860.ps1` makes the counter the HOME target and applies low-overhead settings. `lockdown_r860.ps1` can reversibly disable a conservative list of nonessential apps. It deliberately uses `pm disable-user`, never `pm uninstall`.

`restore_r860.ps1` re-enables those packages and restores the HOME component saved during setup.
