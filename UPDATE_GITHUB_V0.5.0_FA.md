# بروزرسانی GitHub به نسخه 0.5.0

این نسخه بازنویسی کامل است. محتویات Repository را با محتویات پوشه `RahaTVBrowser` جایگزین کنید، اما این موارد را حفظ کنید:

1. فایل فونت شخصی شما: `app/src/main/res/font/vazirmatn.ttf`
2. GitHub Secrets مربوط به امضای ثابت
3. نام Repository فعلی

فایل Workflow باید دقیقاً در مسیر `.github/workflows/android.yml` باشد.

Commit پیشنهادی:

`Rewrite RahaTVBrowser 0.5.0 with WebView and Media3`

پس از Build سبز، Artifact زیر را دانلود کنید:

`raha-tv-browser-debug-and-unsigned-bundle`
