# وضعیت Build و اعتبارسنجی RahaTVBrowser 0.3.3

## انجام‌شده

- نام برنامه در منابع متنی: `RahaTVBrowser`
- شناسه: `com.raha.browser.tv`
- `compileSdk 36`، `targetSdk 35` و `minSdk 26`
- Android Gradle Plugin 8.10.1، Gradle 8.11.1 و Java 17
- ABIهای مجاز: `arm64-v8a` و `armeabi-v7a`
- حذف خروجی Universal، x86 و x86_64
- R8 و Resource Shrinking برای Compact و Release
- Artifact دقیق: `raha-tv-browser-debug-and-unsigned-bundle`
- بررسی Parse شدن فایل‌های XML
- بررسی مسیر Package و namespace
- صفحه خانه علاقه‌مندی‌ها و ذخیره محلی آن‌ها
- Desktop/Mobile mode
- نشانگر TextureView، Hover و کلیک Mouse
- Autoplay و مجوز Media Key System در GeckoView
- مدیریت لینک‌های پنجره جدید

## نیازمند تست عملی روی دستگاه

- اندازه دقیق هر APK پس از Build در GitHub
- پخش JW Player روی سایت واقعی موردنظر
- HLS/DASH/MP4 با کدک‌های مختلف
- Fullscreen، Play/Pause و Seek با مدل‌های متفاوت ریموت
- DRM/Widevine روی هر مدل تلویزیون
- رفتار CORS و Cookie در سرویس‌های پخش واقعی
- نصب روی Android TVهای 32 و 64 بیتی

## صداقت Build

در محیط تولید این بسته Android SDK و وابستگی‌های GeckoView برای کامپایل محلی در دسترس نبودند؛ بنابراین APK در همین محیط ساخته و نصب نشده است. Workflow پروژه برای Build واقعی روی GitHub Actions آماده شده و باید نتیجه نهایی آن روی تلویزیون آزمایش شود.
