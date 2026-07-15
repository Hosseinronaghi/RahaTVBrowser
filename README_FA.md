# RahaTVBrowser 0.5.0

بازنویسی کامل مرورگر Android TV با معماری سبک مشابه مرورگرهای TV مدرن:

- موتور مرور: Android System WebView / Chromium
- پخش آنلاین و محلی: AndroidX Media3 / ExoPlayer
- شناسه برنامه: `com.raha.browser.tv`
- حداقل Android: 8.0 / API 26
- دو APK جدا: arm64-v8a و armeabi-v7a

## امکانات

- صفحه خانه فارسی/انگلیسی با Favoriteهای متراکم و ۱۰ بازدید اخیر
- تغییر مستقل زبان فارسی/انگلیسی و جهت RTL/LTR/خودکار
- Dark / Light / System theme
- حداکثر ۵ تب، تب خصوصی و بستن تب
- Desktop/Mobile mode با User-Agent و viewport متفاوت
- نشانگر D-pad، موس و کیبورد Bluetooth/USB
- تشخیص ویدئوهای HTML5 و JW Player با JavaScript Bridge
- رهگیری URLهای HLS/DASH/MP4 و بازکردن با Player داخلی
- انتقال Cookie، Referer و User-Agent به Player
- Buffer بزرگ‌تر، adaptive track selection و SurfaceView برای کاهش لگ
- File manager سبک با Storage Access Framework برای USB
- پخش ویدئو، صوت و نمایش عکس محلی

## فونت وزیرمتن

فایل فونت به‌دلیل محدودیت توزیع داخل بسته قرار نگرفته است. فایل موجود خودتان را در مسیر زیر حفظ کنید:

`app/src/main/res/font/vazirmatn.ttf`

سپس در `styles.xml` مقدار `fontFamily` را از `sans-serif` به `@font/vazirmatn` تغییر دهید.

## Build

فایل `.github/workflows/android.yml` خروجی‌های زیر را می‌سازد:

- `RahaTVBrowser-arm64-v8a.apk`
- `RahaTVBrowser-armeabi-v7a.apk`
- `RahaTVBrowser-release.aab`
- `SHA256SUMS.txt`

Artifact:

`raha-tv-browser-debug-and-unsigned-bundle`
