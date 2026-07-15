package com.raha.browser.tv;

import android.webkit.JavascriptInterface;

public final class VideoBridge {
    interface Listener { void onVideoFound(String url); }
    private final Listener listener;
    VideoBridge(Listener listener) { this.listener = listener; }

    @JavascriptInterface public void onVideoFound(String url) {
        if (url != null && !url.isBlank()) listener.onVideoFound(url);
    }
}
