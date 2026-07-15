# Security notes

- Safe Browsing is enabled when supported by the installed Android System WebView.
- WebView file access and universal file-URL access are disabled.
- Mixed active content is blocked in browser pages.
- JavaScript bridges expose only narrow one-way callbacks and no filesystem API.
- History excludes internal home pages and private tabs.
- IPTV URLs and playlists are user-supplied; the app ships with no channels.
- HTTP IPTV streams may require cleartext capability. Prefer HTTPS playlists and streams.
- System WebView security depends on the TV vendor/Google Play update channel. Keep Android System WebView current.
- Private mode is best-effort on Android WebView because CookieManager is process-wide; do not treat it as a hardened anonymity boundary.
