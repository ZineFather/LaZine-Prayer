# Lenzin Prayer

Standalone Android prayer-times application. Kotlin + Jetpack Compose.

- Package: `com.zine.prayer`
- Minimum Android: 8.0 (API 26); Android 12 (API 31) is within the declared support range.
- Offline solar calculations; no network permission is declared.
- Alexandria, Cairo and Makkah presets.
- Arabic/English interface, calculation method, Asr school and minute adjustments.
- Separate notification channels for alarm-style, ordinary and silent alerts.
- Alarm rescheduling after boot, system time and timezone changes.
- GitHub Actions builds a debug APK from `main`.

## Known limitation
The build workflow bundles a 42-second Adhan recording from Wikimedia Commons under CC0 1.0 (uploader: Aishatu98; source: https://commons.wikimedia.org/wiki/File:Adhan.ogg). The ADHAN notification channel uses this local resource; it does not require runtime internet. The notification includes a stop-sound action. Android notification-channel behavior and full playback duration still require physical-device testing.

## Build
`gradle :app:assembleDebug`
