# به‌روزرسانی GitHub به RahaTVBrowser 0.4.0

1. محتویات پوشه `RahaTVBrowser` را در ریشه Repository جایگزین کنید.
2. فایل مخفی Workflow را جداگانه کنترل کنید:

```text
.github/workflows/android.yml
```

3. Commit:

```text
Update RahaTVBrowser to 0.4.0
```

4. در Actions اجرای `RahaTVBrowser build` را باز کنید.
5. Artifact:

```text
raha-tv-browser-debug-and-unsigned-bundle
```

6. داخل Artifact:

```text
RahaTVBrowser-arm64-v8a.apk
RahaTVBrowser-armeabi-v7a.apk
RahaTVBrowser-release.aab
SHA256SUMS.txt
```

برای بیشتر تلویزیون‌های جدید `arm64-v8a` را نصب کنید. برای دستگاه‌های ۳۲ بیتی از `armeabi-v7a` استفاده کنید.
