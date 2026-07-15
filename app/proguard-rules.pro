-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.raha.browser.tv.VideoBridge { *; }
-dontwarn org.conscrypt.**
-dontwarn okhttp3.**
