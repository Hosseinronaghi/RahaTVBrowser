# Raha Browser

An open-source Android TV browser MVP powered by Mozilla GeckoView.

The public app name is **Raha Browser**.

## Included in v0.2.2

- GeckoView web engine
- Android TV / Leanback launcher entry
- D-pad friendly toolbar
- Virtual cursor controlled by D-pad + OK
- Back, forward, home and reload
- URL entry and Google search
- Full-screen web video handling
- External HTTP/HTTPS link handling
- Persian and English app strings
- A pinned multi-ABI GeckoView dependency for reproducible MVP builds

## Requirements

- Android Studio with JDK 17
- Android SDK 35
- Internet access during the first Gradle sync

## Build

```bash
gradle :app:assembleDebug
```

Debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Unsigned release bundle (sign it with your permanent release key before Play upload):

```bash
gradle :app:bundleRelease
```

Output:

```text
app/build/outputs/bundle/release/app-release.aab
```

## Important before publishing

1. Keep the permanent application ID `com.raha.browser.tv` unchanged after the first Play release.
2. Review the final icon, TV banner, and store listing assets.
3. Add bookmarks, history, downloads, permissions and privacy controls before calling it production-ready.
4. Test every screen with only a D-pad on real Android TV hardware.
5. Keep GeckoView updated because it is a security-critical browser engine.

See `BUILD_STATUS_FA.md` for the exact validation status of this delivery.

## Physical mouse and keyboard

USB and Bluetooth mice are passed directly to GeckoView for pointer movement, hover, clicking, wheel scrolling, and text selection. If the D-pad virtual cursor is active, the first real mouse event disables it automatically. Hardware keyboards can type into web forms and use Ctrl+L/Ctrl+K, Ctrl+R, Alt+Left/Right, F6, Search, and supported browser media keys.
