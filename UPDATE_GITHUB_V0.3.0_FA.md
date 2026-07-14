# به‌روزرسانی Repository به RahaTVBrowser 0.3.0

## روش ساده با رابط وب GitHub

1. ZIP نسخه 0.3.0 را Extract کنید.
2. در Repository وارد تب **Code** شوید.
3. **Add file → Upload files** را بزنید.
4. محتویات پوشه پروژه را آپلود کنید. فایل‌های هم‌نام جایگزین و فایل جدید `FavoriteStore.java` افزوده می‌شود.
5. پیام Commit را این بگذارید:

```text
Update RahaTVBrowser to 0.3.0
```

به‌دلیل مخفی‌بودن `.github` در ویندوز، فایل Workflow را جداگانه ویرایش کنید:

```text
.github/workflows/android.yml
```

محتوای آن باید با فایل همین بسته یکسان باشد و نام Artifact دقیقاً این باشد:

```text
raha-tv-browser-debug-and-unsigned-bundle
```

بعد از Commit، Build خودکار آغاز می‌شود. اجرای جدید باید مرحله زیر را نشان دهد:

```text
Build compact per-ABI test APKs and optimized release AAB
```

## فایل‌های مهم تغییرکرده

```text
app/build.gradle.kts
app/src/main/java/com/raha/browser/tv/MainActivity.java
app/src/main/java/com/raha/browser/tv/CursorOverlayView.java
app/src/main/java/com/raha/browser/tv/FavoriteStore.java
app/src/main/res/layout/activity_main.xml
app/src/main/res/values/strings.xml
app/src/main/res/values-fa/strings.xml
app/src/main/AndroidManifest.xml
.github/workflows/android.yml
```
