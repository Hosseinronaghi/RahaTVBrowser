# RahaTVBrowser

`RahaTVBrowser` یک مرورگر متن‌باز مخصوص Android TV است که با **Mozilla GeckoView** ساخته شده و برای ریموت، موس و کیبورد طراحی شده است.

## مشخصات نسخه 0.3.3

```text
نام برنامه: RahaTVBrowser
شناسه برنامه: com.raha.browser.tv
حداقل اندروید: Android 8.0 / API 26
Artifact گیت‌هاب: raha-tv-browser-debug-and-unsigned-bundle
```

## امکانات این نسخه

- صفحه خانه مبتنی بر علاقه‌مندی‌ها به‌جای بازکردن مستقیم Google
- دکمه ستاره برای افزودن یا حذف صفحه فعلی از علاقه‌مندی‌ها
- جست‌وجوی Google در صفحه خانه و نوار نشانی
- نشانگر بزرگ و پرکنتراست برای کنترل با D-pad
- استفاده از `TextureView` برای دیده‌شدن نشانگر روی محتوای GeckoView
- کلیک واقعی موس برای دکمه‌های پخش، لینک‌ها و کنترل‌های وب
- پشتیبانی از موس و کیبورد Bluetooth/USB
- دکمه تغییر بین نمای Desktop و Mobile
- اجازه پخش خودکار صوتی و بی‌صدا در سطح GeckoView
- ارسال کلیدهای Play/Pause، Stop، Forward و Rewind ریموت به صفحه وب
- پشتیبانی از Fullscreen و جلوگیری از خاموش‌شدن صفحه هنگام مرور و پخش
- بازکردن لینک‌های `target=_blank` و `window.open()` در همان نشست مرورگر
- سازگاری مرورگری با صفحات مبتنی بر JW Player 8.46.1 و نسخه‌های جدیدتر، در محدوده فرمت، کدک، CORS و DRM پشتیبانی‌شده توسط سایت و دستگاه

## کنترل با ریموت

```text
جهت‌ها: حرکت نشانگر
OK / Enter: کلیک
Back در حالت نشانگر: خروج از حالت نشانگر
Menu: تغییر بین نشانگر و فوکوس
Page/Channel Up و Down: اسکرول
Play/Pause و کلیدهای رسانه: ارسال به پخش‌کننده صفحه
```

## کاهش شدید حجم

GeckoView یک موتور کامل مرورگر و شامل کتابخانه‌های Native است؛ بنابراین همچنان از WebView بزرگ‌تر خواهد بود. برای کاهش عملی حجم این نسخه:

- فقط ABIهای واقعی تلویزیون یعنی `arm64-v8a` و `armeabi-v7a` نگه داشته شده‌اند.
- `x86` و `x86_64` از APK و AAB حذف شده‌اند.
- APK جداگانه برای هر معماری ساخته می‌شود.
- R8 و Resource Shrinking در خروجی Compact و Release فعال است.
- کتابخانه‌های Native داخل APK فشرده می‌شوند.
- Universal APK ساخته نمی‌شود.
- Google Play از AAB برای تحویل اجزای مخصوص دستگاه استفاده می‌کند.

## خروجی GitHub Actions

Workflow با نام `RahaTVBrowser build` این فایل‌ها را تولید می‌کند:

```text
RahaTVBrowser-arm64-v8a-optimized-test.apk
RahaTVBrowser-armeabi-v7a-optimized-test.apk
RahaTVBrowser-release.aab
SHA256SUMS.txt
```

APKهای `optimized-test` با کلید آزمایشی Debug امضا می‌شوند و برای نصب مستقیم و تست هستند. AAB برای انتشار نهایی باید با کلید دائمی پروژه آماده و امضا شود.

## انتخاب APK مناسب

بیشتر تلویزیون‌های جدید:

```text
RahaTVBrowser-arm64-v8a-optimized-test.apk
```

تلویزیون‌های 32 بیتی قدیمی‌تر:

```text
RahaTVBrowser-armeabi-v7a-optimized-test.apk
```

## ساخت محلی

نیازمندی‌ها:

- JDK 17
- Android SDK 36
- Gradle 8.11.1
- اینترنت برای دریافت GeckoView در نخستین Build

```bash
gradle :app:assembleCompact :app:bundleRelease
```

## محدودیت‌های پخش

این پروژه خود کتابخانه تجاری JW Player را داخل APK قرار نمی‌دهد؛ بلکه صفحات وبی را که JW Player را قانونی و صحیح بارگذاری می‌کنند اجرا می‌کند. پخش نهایی به کدک دستگاه، نوع Stream، تنظیمات CORS، گواهی HTTPS، DRM، مجوزهای سرویس و کیفیت پیاده‌سازی سایت وابسته است.

وضعیت اعتبارسنجی در `BUILD_STATUS_FA.md` و روش به‌روزرسانی GitHub در `UPDATE_GITHUB_V0.3.3_FA.md` آمده است.

## نسخه 0.4.0

این نسخه تب‌ها، Private Mode، History، Settings و Player داخلی Media3 را اضافه می‌کند و دو APK جداگانه ARM تولید می‌کند. راهنمای Update امضاشده در `SIGNING_AND_UPDATE_FA.md` است.

## فونت رابط در نسخه 0.4.1

نسخه 0.4.1 آماده استفاده از Vazirmatn است. فایل فونت را با نام `vazirmatn.ttf` در مسیر `app/src/main/res/font/` قرار دهید. اگر فایل وجود نداشته باشد، برنامه از فونت پیش‌فرض Android استفاده می‌کند. جزئیات در `FONT_VAZIRMATN_SETUP_FA.md` آمده است.
