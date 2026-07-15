package com.raha.browser.tv;

import android.webkit.WebView;

final class BrowserTab {
    final long id;
    final WebView webView;
    final boolean privateMode;
    String title;
    String url;

    BrowserTab(long id, WebView webView, boolean privateMode) {
        this.id = id;
        this.webView = webView;
        this.privateMode = privateMode;
        this.title = privateMode ? "Private" : "Home";
        this.url = MainActivity.HOME_URL;
    }
}
