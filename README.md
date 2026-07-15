# RahaTVBrowser

`RahaTVBrowser` is an open-source Android TV browser powered by Mozilla GeckoView and designed for D-pad, Bluetooth/USB mouse, and hardware keyboard use.

## Version 0.3.3

```text
App name: RahaTVBrowser
Application ID: com.raha.browser.tv
Minimum Android: Android 8.0 / API 26
GitHub artifact: raha-tv-browser-debug-and-unsigned-bundle
```

## Highlights

- Favorites-first home page
- Native favorite toggle button
- High-contrast D-pad virtual cursor
- TextureView rendering so the Android cursor overlay remains visible
- Mouse-style hover and click events for web video controls
- Bluetooth/USB mouse and keyboard support
- Desktop/mobile user-agent and viewport toggle
- Web autoplay permission handling
- TV media-key forwarding to HTML5/JW Player pages
- Full-screen web video handling
- `target=_blank` and `window.open()` links redirected into the current browser session
- Browser-level compatibility for pages using JW Player 8.46.1+, subject to the site's codecs, CORS, DRM, and device capabilities

## Size-optimized outputs

The build keeps only `arm64-v8a` and `armeabi-v7a`, removes emulator ABIs, enables R8/resource shrinking for compact/release builds, compresses native libraries, and generates no universal APK.

GitHub Actions produces:

```text
RahaTVBrowser-arm64-v8a-optimized-test.apk
RahaTVBrowser-armeabi-v7a-optimized-test.apk
RahaTVBrowser-release.aab
SHA256SUMS.txt
```

The optimized test APKs use a disposable debug certificate. The release AAB must be signed with the permanent publishing key before production distribution.

## Build

Requirements: JDK 17, Android SDK 36, and Gradle 8.11.1.

```bash
gradle :app:assembleCompact :app:bundleRelease
```

JW Player itself is not bundled or redistributed. RahaTVBrowser runs correctly embedded web players when their licensing, scripts, stream formats, CORS, HTTPS, codecs, and DRM requirements are satisfied.
