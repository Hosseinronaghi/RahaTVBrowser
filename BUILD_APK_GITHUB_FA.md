# ساخت APK با GitHub Actions

این روش به نصب Android Studio روی رایانه نیاز ندارد.

1. یک مخزن جدید در GitHub بسازید.
2. همه فایل‌های این پوشه را در شاخه `main` قرار دهید.
3. وارد تب **Actions** شوید و Workflow با نام **Android build** را باز کنید.
4. گزینه **Run workflow** را اجرا کنید.
5. پس از موفقیت Build، در پایین صفحه فایل Artifact با نام زیر را دریافت کنید:

```text
raha-browser-debug-and-unsigned-bundle
```

محتویات Artifact:

```text
app-debug.apk          نسخه آزمایشیِ امضاشده و قابل نصب
app-release.aab        Bundle بدون امضای انتشار
SHA256SUMS.txt         هش کنترل فایل‌ها
```

> فایل Debug برای نصب و آزمایش مناسب است، نه انتشار در Google Play. برای Play باید AAB را با کلید دائمی خودتان امضا کنید.

## ساخت روی Android Studio

- پروژه را باز کنید.
- Gradle JDK را روی Java 17 قرار دهید.
- برای APK آزمایشی: `Build > Build APK(s)`
- برای فایل Play: `Build > Generate Signed Bundle / APK > Android App Bundle`
