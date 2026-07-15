# Static audit — 0.6.0

Checks completed before packaging:

- All XML resource files parsed successfully.
- Persian and English string key sets match.
- No `.tmp`, `.bak`, `.md`, or `.txt` files exist inside `app/src/main/res`.
- Internal Home uses `file:///android_asset/home/index.html`; UI displays `raha://home`.
- Internal Home is excluded from favorites/history.
- WebView file access, file-URL cross access, and mixed content are disabled.
- GitHub Actions validates resource extensions before Build.
- Application ID remains `com.raha.browser.tv`.
- Version is `0.6.0` / versionCode `16`.

Limitations:

- This package was statically reviewed but not compiled in the packaging environment because Android SDK/Gradle were unavailable there. GitHub Actions is the authoritative compile check.
- Android WebView private tabs are best-effort and do not provide a separate OS process/profile.
- USB visibility depends on the TV vendor's Storage Access Framework provider.
- 4K playback depends on hardware decoder support, stream codec/bitrate, network quality, and the installed System WebView/Media stack.
