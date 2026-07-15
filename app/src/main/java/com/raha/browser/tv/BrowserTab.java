package com.raha.browser.tv;

import org.mozilla.geckoview.GeckoSession;

final class BrowserTab {
    final long id;
    final GeckoSession session;
    final boolean privateMode;
    String title = "New tab";
    String url;
    boolean home;
    boolean canGoBack;
    boolean canGoForward;

    BrowserTab(long id, GeckoSession session, boolean privateMode) {
        this.id = id;
        this.session = session;
        this.privateMode = privateMode;
    }
}
