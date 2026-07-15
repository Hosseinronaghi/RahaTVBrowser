package com.raha.browser.tv;

import android.webkit.JavascriptInterface;

final class HomeBridge {
    interface Listener { void onAction(String action, String value); }
    private final Listener listener;
    HomeBridge(Listener listener) { this.listener = listener; }
    @JavascriptInterface public void action(String action, String value) { listener.onAction(action == null ? "" : action, value == null ? "" : value); }
}
