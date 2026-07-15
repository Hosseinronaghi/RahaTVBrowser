# RahaTVBrowser 0.6.0

مرورگر متن‌باز Android TV با موتور System WebView و پخش‌کننده Media3.

## امکانات

- صفحه خانه Asset-based با نشانی داخلی تمیز `raha://home`
- میان‌بر Google، YouTube، SoundCloud، ChatGPT و Wikipedia
- علاقه‌مندی‌ها و ۱۰ بازدید اخیر
- حداکثر ۵ تب و بستن تب فعلی
- حالت خصوصی با عدم ثبت تاریخچه و پاک‌سازی Session Cookie هنگام بستن تب
- پوسته روشن، تیره و هماهنگ با سیستم
- فارسی و انگلیسی با جهت خودکار، راست‌چین یا چپ‌چین
- جستجو و فرمان صوتی فارسی و انگلیسی
- حالت موبایل و دسکتاپ
- نشانگر D-pad و موس/کیبورد USB یا Bluetooth
- شناسایی اولیه HTML5/JW Player و انتقال لینک‌های مستقیم به Media3
- پخش HLS، DASH، MP4، WebM، MKV و صدا
- مرور رسانه‌های فلش/USB از طریق Storage Access Framework
- IPTV با Playlistهای M3U/M3U8 کاربر
- خروجی arm64-v8a، armeabi-v7a، Universal و AAB

## ساخت

فایل‌ها را در ریشه Repository قرار دهید و Workflow زیر را اجرا کنید:

`.github/workflows/android.yml`

Artifact:

`raha-tv-browser-debug-and-unsigned-bundle`

## شناسه

`com.raha.browser.tv`

## فونت وزیرمتن

به‌دلیل توزیع‌نشدن فایل فونت در بسته، رابط با فونت سیستم Build می‌شود. برای افزودن وزیرمتن فایل مجاز خود را در `app/src/main/assets/fonts/vazirmatn.ttf` قرار دهید و FontManager سفارشی اضافه کنید. صفحه خانه از Vazirmatn در صورت موجودبودن در CSS استفاده می‌کند و در غیر این صورت fallback دارد.
