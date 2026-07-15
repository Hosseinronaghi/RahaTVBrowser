# امضای ثابت و Update شدن APK

شناسه نسخه آزمایشی به دلیل حفظ سازگاری با خروجی‌های Compact قبلی:

```text
com.raha.browser.tv.debug
```

برای اینکه نسخه‌های بعدی روی نسخه قبلی نصب شوند، همه Buildها باید با یک Keystore ثابت امضا شوند.

## 1. ساخت Keystore فقط یک‌بار

در رایانه‌ای که JDK دارد:

```bash
keytool -genkeypair -v -keystore raha-upload.jks -alias raha -keyalg RSA -keysize 4096 -validity 10000
```

از فایل و رمزها نسخه پشتیبان امن بگیرید و Keystore را داخل Repository قرار ندهید.

## 2. تبدیل به Base64

Linux/macOS:

```bash
base64 -w 0 raha-upload.jks > raha-upload.base64.txt
```

PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("raha-upload.jks")) | Set-Content raha-upload.base64.txt
```

## 3. ثبت GitHub Secrets

Repository → Settings → Secrets and variables → Actions → New repository secret

```text
RAHA_KEYSTORE_BASE64     محتوای فایل Base64
RAHA_KEYSTORE_PASSWORD   رمز Keystore
RAHA_KEY_ALIAS            raha
RAHA_KEY_PASSWORD         رمز کلید
```

Workflow به‌صورت خودکار از همین کلید برای هر دو APK و AAB استفاده می‌کند.

## نکته نسخه فعلی

اگر نسخه 0.3.3 با کلید Debug موقتی GitHub نصب شده باشد، احتمالاً 0.4.0 امضاشده روی آن Update نمی‌شود و برای اولین مهاجرت باید نسخه قبلی را حذف کنید. بعد از نصب نخستین نسخه‌ای که با Keystore ثابت ساخته شده، تمام نسخه‌های آینده با افزایش `versionCode` مستقیماً Update خواهند شد.
