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
The ADHAN channel currently uses the device's default alarm sound. A recorded Adhan audio file and a foreground playback/stop service are not yet included. Exact-alarm and notification behavior must still be tested on a physical Android 12 device.

## Build
`gradle :app:assembleDebug`
