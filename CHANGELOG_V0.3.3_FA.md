# تغییرات RahaTVBrowser نسخه 0.3.3

- رفع خطای `buildReleasePreBundle` با جداسازی کامل Build فایل‌های APK و AAB.
- فعال بودن ABI Split فقط هنگام ساخت APKهای مستقیم.
- استفاده از `ndk.abiFilters` فقط هنگام ساخت AAB برای Google Play.
- محدودکردن خروجی‌ها به `arm64-v8a` و `armeabi-v7a`.
- ارتقای Android Gradle Plugin به 8.13.2 برای سازگاری بهتر با Kotlin 2.3 وابستگی‌های جدید GeckoView.
- ارتقای Gradle در GitHub Actions به 8.13.
- نسخه برنامه: `0.3.3` و `versionCode 10`.

نام Artifact بدون تغییر:

```text
raha-tv-browser-debug-and-unsigned-bundle
```
