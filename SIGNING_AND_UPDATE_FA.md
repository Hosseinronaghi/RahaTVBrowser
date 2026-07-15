# امضای ثابت و Update

برای نصب نسخه جدید روی نسخه قبلی باید package id و keystore ثابت بمانند. شناسه این پروژه `com.raha.browser.tv` است.

GitHub Secrets لازم:
- RAHA_KEYSTORE_BASE64
- RAHA_KEYSTORE_PASSWORD
- RAHA_KEY_ALIAS
- RAHA_KEY_PASSWORD

پس از تنظیم این چهار Secret، همه نسخه‌های Compact و Release با همان کلید امضا می‌شوند. اگر نسخه قبلی با کلید دیگری نصب شده، فقط یک بار باید حذف و نسخه دارای امضای دائمی نصب شود.
