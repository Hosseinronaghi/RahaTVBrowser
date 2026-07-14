# تغییرات RahaTVBrowser 0.3.0

| درخواست | پیاده‌سازی |
|---|---|
| کاهش شدید حجم | فقط ARM 32/64، APK جدا برای ABI، حذف x86/x86_64 و Universal، Compact Build با R8 و Resource Shrinking، فشرده‌سازی Native libs |
| پخش آنلاین | JavaScript، Autoplay، Fullscreen، Media Key System و نگه‌داشتن صفحه روشن فعال است |
| دکمه فیوریت | دکمه `☆/★` در نوار ابزار و ذخیره محلی با SharedPreferences |
| خانه بر پایه فیوریت‌ها | صفحه خانه پویا با کارت‌های علاقه‌مندی و جست‌وجوی Google |
| نشانگر دیده نمی‌شود | GeckoView از SurfaceView به TextureView منتقل شد و نشانگر با Elevation و کنتراست بالا نمایش داده می‌شود |
| کلیک پخش کار نمی‌کند | Hover و توالی کامل Mouse Down/Button Press/Button Release/Up به PanZoomController ارسال می‌شود |
| حالت دسکتاپ | دکمه Desktop/Mobile با تغییر هم‌زمان User Agent و Viewport |
| JW Player 8.46.1+ | اجرای صفحات دارای JW Player، Autoplay و کلیدهای رسانه؛ بدون توزیع خود کتابخانه تجاری JW |
| لینک پخش در پنجره جدید | `target=_blank`، `window.open()` و Popupهای HTTP/HTTPS به نشست فعلی هدایت می‌شوند |

## نکته تست

نسخه 0.3.0 باید پس از Build روی همان سایت‌ها و Streamهای واقعی شما آزمایش شود. سازگاری کامل DRM، کدک، CORS و پخش زنده صرفاً از روی سورس قابل تضمین نیست.
