# تغییرات RahaTVBrowser نسخه 0.3.2

- رفع خطای کامپایل `MotionEvent.setActionButton` در `MainActivity.java`.
- بازطراحی کلیک نشانگر مجازی با توالی استاندارد `ACTION_DOWN` و `ACTION_UP`.
- حفظ `BUTTON_PRIMARY` در رویداد پایین‌رفتن دکمه برای ارسال کلیک چپ به GeckoView.
- افزایش نسخه به `versionCode 9` و `versionName 0.3.2`.

هشدار مربوط به ناتوانی AGP در strip کردن برخی کتابخانه‌های Native GeckoView علت شکست Build نیست؛ کتابخانه‌ها بدون تغییر بسته‌بندی می‌شوند.
