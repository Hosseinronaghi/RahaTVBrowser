# ساخت APK نسخه 0.3.1 با GitHub Actions

Workflow پروژه:

```text
.github/workflows/android.yml
```

نام آن در تب Actions:

```text
RahaTVBrowser build
```

## اجرا

1. وارد Repository با نام `RahaTVBrowser` شوید.
2. تب **Actions** را باز کنید.
3. از ستون سمت چپ **RahaTVBrowser build** را انتخاب کنید.
4. روی **Run workflow** بزنید و شاخه `main` را اجرا کنید.
5. پس از سبزشدن Build، همان اجرا را باز کنید.
6. پایین صفحه از بخش **Artifacts** این مورد را دانلود کنید:

```text
raha-tv-browser-debug-and-unsigned-bundle
```

## فایل‌های داخل Artifact

```text
RahaTVBrowser-arm64-v8a-optimized-test.apk
RahaTVBrowser-armeabi-v7a-optimized-test.apk
RahaTVBrowser-release.aab
SHA256SUMS.txt
```

برای اغلب Android TVهای جدید، فایل `arm64-v8a` مناسب است. اگر نصب نشد و دستگاه 32 بیتی بود، فایل `armeabi-v7a` را امتحان کنید.

## نکته امضا

APKهای آزمایشی قابل نصب‌اند، اما کلید آن‌ها کلید موقت Debug در Runner گیت‌هاب است. ممکن است برای نصب Build جدید مجبور شوید نسخه قبلی آزمایشی را حذف کنید. برای انتشار و به‌روزرسانی پایدار باید Keystore دائمی بسازید و آن را با GitHub Secrets یا Android Studio مدیریت کنید.
