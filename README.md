# LaZine Prayer

**LaZine Prayer** is a free, open-source Android app for prayer times, built with Kotlin and Jetpack Compose.

## 🇪🇬 العربية

تطبيق مجاني ومفتوح المصدر لمواقيت الصلاة، بواجهة عربية وإنجليزية وحسابات تعمل دون اتصال بالإنترنت.

- مجاني بالكامل، بدون إعلانات أو اشتراكات.
- لا يحتوي على أدوات إعلانات أو تحليلات أو تتبّع مدمجة.
- لا يطلب إذن الإنترنت؛ حساب المواقيت يتم محليًا على الجهاز.
- إذن التشغيل الذي يظهر للمستخدم أثناء الاستخدام هو إذن الإشعارات على إصدارات أندرويد المدعومة.
- يمكن مراجعة الصلاحيات وكود المصدر علنًا.

**توضيح الصلاحيات:** يعلن التطبيق أيضًا عن صلاحيات نظامية لجدولة التنبيهات الدقيقة، واستقبال إعادة تشغيل الجهاز، وتشغيل خدمة الأذان في المقدمة. هذه ليست كلها نوافذ أذونات تشغيل يوافق عليها المستخدم؛ وقد يعرض أندرويد صفحة إعداد خاصة بالتنبيهات الدقيقة حسب إصدار النظام.

## 🇬🇧 English

A free and open-source prayer-times app with Arabic and English support and fully offline prayer-time calculations.

- Free to use, with no ads or subscriptions.
- No built-in advertising, analytics, or tracking SDKs.
- Does not request the Internet permission; prayer-time calculations run locally on the device.
- The runtime permission prompt shown by the app is for notifications on supported Android versions.
- Permissions and source code are available for public review.

**Permission note:** The app also declares Android system permissions for exact alarms, receiving device boot events, and running the Adhan foreground service. These are not all runtime permission dialogs; Android may show a separate special-access settings page for exact alarms depending on the OS version.

## 🔎 Security & transparency

The source code and Android manifest are public so anyone can inspect the app's behavior and declared permissions. Google Play Protect screenshots, when included in the `Screenshots/` folder, document the scan result for the exact APK and scan date shown. A scan result is useful evidence, but it is not an absolute guarantee that every build is 100% risk-free.

## App details

- Package: `com.zine.prayer`
- Minimum Android: 8.0 (API 26)
- Offline solar calculations; no Internet permission.
- Alexandria, Cairo and Makkah presets.
- Arabic/English interface, calculation method, Asr school and minute adjustments.
- Separate notification channels for full Adhan, ordinary, short-tone and silent alerts.
- Alarm rescheduling after boot, system time and timezone changes.

## Build

```bash
gradle :app:assembleDebug
```

The GitHub Actions workflow builds the APK from the `main` branch and publishes it in GitHub Releases. The release contains the APK; GitHub also provides source-code archives automatically.
