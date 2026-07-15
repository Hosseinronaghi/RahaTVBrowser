package com.raha.browser.tv;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

public class MainActivity extends AppCompatActivity {
    private static final int MAX_TABS = 5;
    private static final String MOBILE_UA = "Mozilla/5.0 (Linux; Android 13; TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36 RahaTVBrowser/0.5";
    private static final String DESKTOP_UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36 RahaTVBrowser/0.5";

    private final List<BrowserTab> tabs = new ArrayList<>();
    private final AtomicLong tabIds = new AtomicLong(1);
    private int currentTabIndex = -1;

    private FrameLayout webContainer;
    private EditText addressBar;
    private ProgressBar progressBar;
    private CursorOverlayView cursor;
    private BrowserStore store;
    private AppSettings settings;
    private String detectedMediaUrl;

    @Override protected void onCreate(Bundle savedInstanceState) {
        settings = new AppSettings(this);
        applySavedLocale();
        applySavedTheme();
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        setContentView(R.layout.activity_main);

        store = new BrowserStore(this);
        webContainer = findViewById(R.id.webContainer);
        addressBar = findViewById(R.id.addressBar);
        progressBar = findViewById(R.id.progressBar);
        cursor = findViewById(R.id.cursorOverlay);
        applyDirection(findViewById(R.id.root));
        bindToolbar();

        String initial = getIntent() != null && getIntent().getData() != null ? getIntent().getDataString() : null;
        createTab(false, initial == null ? homeUrl() : initial);
    }

    private void applySavedTheme() {
        String theme = settings.theme();
        int mode = switch (theme) {
            case "light" -> AppCompatDelegate.MODE_NIGHT_NO;
            case "system" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            default -> AppCompatDelegate.MODE_NIGHT_YES;
        };
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    private void applySavedLocale() {
        String lang = settings.language();
        if ("fa".equals(lang) || "en".equals(lang)) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang));
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
        }
    }

    private void applyDirection(View root) {
        String dir = settings.direction();
        int layout = View.LAYOUT_DIRECTION_LOCALE;
        if ("rtl".equals(dir)) layout = View.LAYOUT_DIRECTION_RTL;
        if ("ltr".equals(dir)) layout = View.LAYOUT_DIRECTION_LTR;
        root.setLayoutDirection(layout);
        addressBar.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        addressBar.setTextDirection(View.TEXT_DIRECTION_LTR);
    }

    private void bindToolbar() {
        findViewById(R.id.backButton).setOnClickListener(v -> goBack());
        findViewById(R.id.forwardButton).setOnClickListener(v -> { WebView w = currentWeb(); if (w != null && w.canGoForward()) w.goForward(); });
        findViewById(R.id.homeButton).setOnClickListener(v -> showHome());
        findViewById(R.id.reloadButton).setOnClickListener(v -> { WebView w = currentWeb(); if (w != null) w.reload(); });
        findViewById(R.id.favoriteButton).setOnClickListener(v -> toggleFavorite());
        findViewById(R.id.desktopButton).setOnClickListener(v -> toggleDesktop());
        findViewById(R.id.playerButton).setOnClickListener(v -> openDetectedMedia());
        findViewById(R.id.tabsButton).setOnClickListener(v -> showTabsDialog());
        findViewById(R.id.privateButton).setOnClickListener(v -> createTab(true, homeUrl()));
        findViewById(R.id.filesButton).setOnClickListener(v -> startActivity(new Intent(this, LocalMediaActivity.class)));
        findViewById(R.id.settingsButton).setOnClickListener(v -> showSettings());
        findViewById(R.id.closeButton).setOnClickListener(v -> closeCurrentTab());

        addressBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loadInput(addressBar.getText().toString());
                return true;
            }
            return false;
        });
    }

    private void createTab(boolean privateMode, String url) {
        if (tabs.size() >= MAX_TABS) {
            Toast.makeText(this, "Maximum " + MAX_TABS + " tabs", Toast.LENGTH_SHORT).show();
            return;
        }
        WebView web = new WebView(this);
        configureWebView(web, privateMode);
        BrowserTab tab = new BrowserTab(tabIds.getAndIncrement(), web, privateMode);
        tabs.add(tab);
        switchToTab(tabs.size() - 1);
        web.loadUrl(url);
    }

    private void configureWebView(WebView web, boolean privateMode) {
        web.setBackgroundColor(Color.TRANSPARENT);
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        web.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(!privateMode);
        ws.setDatabaseEnabled(!privateMode);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setAllowFileAccess(true);
        ws.setAllowContentAccess(true);
        ws.setBuiltInZoomControls(true);
        ws.setDisplayZoomControls(false);
        ws.setLoadWithOverviewMode(settings.desktop());
        ws.setUseWideViewPort(settings.desktop());
        ws.setUserAgentString(settings.desktop() ? DESKTOP_UA : MOBILE_UA);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        ws.setCacheMode(privateMode ? WebSettings.LOAD_NO_CACHE : WebSettings.LOAD_DEFAULT);

        if (privateMode) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
        }

        web.addJavascriptInterface(new VideoBridge(url -> runOnUiThread(() -> {
            if (isPlayableUrl(url)) detectedMediaUrl = url;
        })), "RahaVideo");

        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }

            @Override public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
                if (tabs.size() >= MAX_TABS) return false;
                WebView popup = new WebView(MainActivity.this);
                configureWebView(popup, privateMode);
                BrowserTab tab = new BrowserTab(tabIds.getAndIncrement(), popup, privateMode);
                tabs.add(tab);
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(popup);
                resultMsg.sendToTarget();
                switchToTab(tabs.size() - 1);
                return true;
            }
        });

        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (isPlayableUrl(url)) {
                    detectedMediaUrl = url;
                    openPlayer(url, request.getRequestHeaders().get("Referer"));
                    return true;
                }
                return false;
            }

            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (isPlayableUrl(url)) detectedMediaUrl = url;
                return super.shouldInterceptRequest(view, request);
            }

            @Override public void onPageFinished(WebView view, String url) {
                BrowserTab t = currentTab();
                if (t != null && t.webView == view) {
                    t.url = url;
                    t.title = view.getTitle() == null ? url : view.getTitle();
                    addressBar.setText(url.startsWith("file:///android_asset") ? "" : url);
                    if (!t.privateMode) store.addHistory(t.title, url);
                    updateFavoriteIcon();
                }
                injectVideoDetector(view);
            }
        });

        web.setOnGenericMotionListener((v, event) -> {
            if ((event.getSource() & android.view.InputDevice.SOURCE_MOUSE) == android.view.InputDevice.SOURCE_MOUSE) cursor.hideCursor();
            return false;
        });
    }

    private void injectVideoDetector(WebView web) {
        String js = "javascript:(function(){try{" +
                "function send(u){if(u&&typeof u==='string'&&!u.startsWith('blob:')) RahaVideo.onVideoFound(u);}" +
                "document.querySelectorAll('video,source').forEach(function(v){send(v.currentSrc||v.src);});" +
                "if(window.jwplayer){try{var j=window.jwplayer();var p=j.getPlaylistItem&&j.getPlaylistItem();if(p){send(p.file);if(p.sources)p.sources.forEach(function(s){send(s.file);});}}catch(e){}}" +
                "var oldOpen=window.open;window.open=function(u){send(u);return oldOpen.apply(this,arguments);};" +
                "new MutationObserver(function(){document.querySelectorAll('video,source').forEach(function(v){send(v.currentSrc||v.src);});}).observe(document.documentElement,{childList:true,subtree:true,attributes:true});" +
                "}catch(e){}})();";
        web.evaluateJavascript(js, null);
    }

    private void switchToTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        webContainer.removeAllViews();
        currentTabIndex = index;
        WebView web = tabs.get(index).webView;
        if (web.getParent() != null) ((ViewGroup) web.getParent()).removeView(web);
        webContainer.addView(web, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        addressBar.setText(tabs.get(index).url);
        web.requestFocus();
        detectedMediaUrl = null;
        updateFavoriteIcon();
    }

    @Nullable private BrowserTab currentTab() {
        return currentTabIndex >= 0 && currentTabIndex < tabs.size() ? tabs.get(currentTabIndex) : null;
    }
    @Nullable private WebView currentWeb() { BrowserTab t = currentTab(); return t == null ? null : t.webView; }

    private void showTabsDialog() {
        String[] items = new String[tabs.size() + 1];
        for (int i = 0; i < tabs.size(); i++) {
            BrowserTab t = tabs.get(i);
            items[i] = (t.privateMode ? "◐ " : "") + (i == currentTabIndex ? "• " : "") + t.title;
        }
        items[tabs.size()] = getString(R.string.new_tab);
        new AlertDialog.Builder(this).setTitle(R.string.tabs).setItems(items, (d, which) -> {
            if (which == tabs.size()) createTab(false, homeUrl()); else switchToTab(which);
        }).show();
    }

    private void closeCurrentTab() {
        if (tabs.isEmpty()) { finish(); return; }
        BrowserTab t = tabs.remove(currentTabIndex);
        t.webView.stopLoading();
        t.webView.destroy();
        if (tabs.isEmpty()) createTab(false, homeUrl());
        else switchToTab(Math.max(0, currentTabIndex - 1));
    }

    private void showHome() {
        WebView w = currentWeb();
        if (w != null) w.loadUrl(homeUrl());
    }

    private String homeUrl() {
        String html = buildHomeHtml();
        return "data:text/html;charset=utf-8," + Uri.encode(html);
    }

    private String buildHomeHtml() {
        boolean fa = currentLanguageIsPersian();
        boolean rtl = resolvedRtl();
        String title = fa ? "جستجوگر تلویزیونی رها" : "Raha TVBROWSER";
        String search = fa ? "جستجو" : "Search";
        String fav = fa ? "علاقه‌مندی‌ها" : "Favorites";
        String recent = fa ? "بازدیدهای اخیر" : "Recent visits";
        StringBuilder cards = new StringBuilder();
        for (BrowserStore.Entry e : store.favorites()) cards.append(card(e));
        if (cards.length() == 0) cards.append("<div class='empty'>").append(fa ? "با دکمه ستاره سایت‌ها را اضافه کنید" : "Add sites with the star button").append("</div>");
        StringBuilder hist = new StringBuilder();
        for (BrowserStore.Entry e : store.history()) hist.append(card(e));
        return "<!doctype html><html dir='" + (rtl ? "rtl" : "ltr") + "'><head><meta name='viewport' content='width=device-width,initial-scale=1'>" +
                "<style>body{margin:0;background:#07131f;color:#f7fafc;font-family:sans-serif;padding:34px}h1{font-size:28px;margin:0 0 22px}.search{display:flex;gap:8px;margin-bottom:28px}.search input{flex:1;padding:14px;border-radius:12px;border:0;font-size:20px}.search button{padding:12px 24px;border:0;border-radius:12px;background:#4da3ff;color:#06111b;font-weight:bold}.grid{display:grid;grid-template-columns:repeat(8,minmax(0,1fr));gap:12px}.card{display:block;background:#142a3b;color:#fff;text-decoration:none;border-radius:12px;padding:14px;min-height:48px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.card:focus{outline:3px solid #74b9ff;transform:scale(1.04)}h2{font-size:20px;margin:22px 0 12px}.empty{opacity:.65}</style></head><body>" +
                "<h1>" + esc(title) + "</h1><form class='search' onsubmit='location.href=\"https://www.google.com/search?q=\"+encodeURIComponent(q.value);return false'><input name='q' placeholder='" + esc(search) + "'><button>" + esc(search) + "</button></form>" +
                "<h2>" + esc(fav) + "</h2><div class='grid'>" + cards + "</div><h2>" + esc(recent) + "</h2><div class='grid'>" + hist + "</div></body></html>";
    }

    private String card(BrowserStore.Entry e) {
        return "<a class='card' href='" + escAttr(e.url()) + "' title='" + escAttr(e.url()) + "'>" + esc(e.title()) + "</a>";
    }

    private String esc(String s) { return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
    private String escAttr(String s) { return esc(s).replace("'", "&#39;").replace("\"", "&quot;"); }

    private void loadInput(String input) {
        String value = input == null ? "" : input.trim();
        if (value.isEmpty()) return;
        String url;
        if (value.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) url = value;
        else if (value.contains(".") && !value.contains(" ")) url = "https://" + value;
        else url = "https://www.google.com/search?q=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
        WebView w = currentWeb(); if (w != null) w.loadUrl(url);
    }

    private void toggleFavorite() {
        BrowserTab t = currentTab();
        if (t == null || t.url == null || t.url.isBlank() || t.url.startsWith("data:")) return;
        store.toggleFavorite(t.title, t.url);
        updateFavoriteIcon();
    }

    private void updateFavoriteIcon() {
        Button b = findViewById(R.id.favoriteButton);
        BrowserTab t = currentTab();
        b.setText(t != null && store.isFavorite(t.url) ? "★" : "☆");
    }

    private void toggleDesktop() {
        boolean desktop = !settings.desktop();
        settings.put("desktop", desktop);
        for (BrowserTab t : tabs) {
            WebSettings ws = t.webView.getSettings();
            ws.setUserAgentString(desktop ? DESKTOP_UA : MOBILE_UA);
            ws.setUseWideViewPort(desktop);
            ws.setLoadWithOverviewMode(desktop);
        }
        ((Button)findViewById(R.id.desktopButton)).setText(desktop ? "M" : "D");
        WebView w = currentWeb(); if (w != null) w.reload();
    }

    private void openDetectedMedia() {
        if (detectedMediaUrl == null || detectedMediaUrl.isBlank()) {
            WebView w = currentWeb();
            if (w != null) injectVideoDetector(w);
            Toast.makeText(this, R.string.invalid_url, Toast.LENGTH_SHORT).show();
            return;
        }
        openPlayer(detectedMediaUrl, currentTab() == null ? null : currentTab().url);
    }

    private void openPlayer(String url, String referer) {
        Intent i = new Intent(this, PlayerActivity.class);
        i.putExtra(PlayerActivity.EXTRA_URL, url);
        i.putExtra(PlayerActivity.EXTRA_REFERER, referer);
        i.putExtra(PlayerActivity.EXTRA_USER_AGENT, settings.desktop() ? DESKTOP_UA : MOBILE_UA);
        i.putExtra(PlayerActivity.EXTRA_COOKIE, CookieManager.getInstance().getCookie(url));
        i.putExtra(PlayerActivity.EXTRA_MAX_HEIGHT, settings.preferredHeight());
        startActivity(i);
    }

    private boolean isPlayableUrl(String url) {
        if (url == null) return false;
        String u = url.toLowerCase(Locale.ROOT);
        return u.contains(".m3u8") || u.contains(".mpd") || u.matches(".*\\.(mp4|m4v|webm|mkv|mov|avi|ts|m2ts|mp3|m4a|aac|flac|ogg|wav)(\\?.*)?$");
    }

    private void showSettings() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(28, 8, 28, 8);

        String[] themes = {getString(R.string.dark), getString(R.string.light), getString(R.string.system)};
        String[] languages = {getString(R.string.auto), getString(R.string.persian), getString(R.string.english)};
        String[] directions = {getString(R.string.auto), getString(R.string.rtl), getString(R.string.ltr)};
        String[] qualities = {"Auto", "720p", "1080p", "2160p / 4K"};

        Button theme = settingButton(getString(R.string.theme) + ": " + settings.theme());
        theme.setOnClickListener(v -> choose(themes, indexOf(settings.theme(), new String[]{"dark","light","system"}), which -> { settings.put("theme", new String[]{"dark","light","system"}[which]); recreate(); }));
        Button lang = settingButton(getString(R.string.language) + ": " + settings.language());
        lang.setOnClickListener(v -> choose(languages, indexOf(settings.language(), new String[]{"auto","fa","en"}), which -> { settings.put("language", new String[]{"auto","fa","en"}[which]); applySavedLocale(); recreate(); }));
        Button direction = settingButton(getString(R.string.direction) + ": " + settings.direction());
        direction.setOnClickListener(v -> choose(directions, indexOf(settings.direction(), new String[]{"auto","rtl","ltr"}), which -> { settings.put("direction", new String[]{"auto","rtl","ltr"}[which]); recreate(); }));
        Button quality = settingButton(getString(R.string.video_quality));
        quality.setOnClickListener(v -> choose(qualities, 2, which -> settings.put("preferred_height", new int[]{0,720,1080,2160}[which])));
        Button clear = settingButton(getString(R.string.clear_history));
        clear.setOnClickListener(v -> { store.clearHistory(); Toast.makeText(this, R.string.clear_history, Toast.LENGTH_SHORT).show(); });

        TextView speedLabel = new TextView(this); speedLabel.setText(R.string.pointer_speed); speedLabel.setTextColor(Color.WHITE); speedLabel.setPadding(8,16,8,4);
        SeekBar speed = new SeekBar(this); speed.setMax(50); speed.setProgress((int)settings.pointerSpeed());
        speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean f) { settings.put("pointer_speed", Math.max(8, p)); }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });

        layout.addView(theme); layout.addView(lang); layout.addView(direction); layout.addView(quality); layout.addView(clear); layout.addView(speedLabel); layout.addView(speed);
        new AlertDialog.Builder(this).setTitle(R.string.settings).setView(layout).setPositiveButton(android.R.string.ok, null).show();
    }

    private Button settingButton(String text) {
        Button b = new Button(this); b.setText(text); b.setAllCaps(false); b.setFocusable(true); return b;
    }
    private interface Choice { void selected(int which); }
    private void choose(String[] items, int checked, Choice choice) {
        new AlertDialog.Builder(this).setSingleChoiceItems(items, checked, (d,w) -> { choice.selected(w); d.dismiss(); }).show();
    }
    private int indexOf(String v, String[] a) { for (int i=0;i<a.length;i++) if (a[i].equals(v)) return i; return 0; }

    private boolean currentLanguageIsPersian() {
        String lang = settings.language();
        return "fa".equals(lang) || ("auto".equals(lang) && getResources().getConfiguration().getLocales().get(0).getLanguage().equals("fa"));
    }
    private boolean resolvedRtl() {
        if ("rtl".equals(settings.direction())) return true;
        if ("ltr".equals(settings.direction())) return false;
        return currentLanguageIsPersian();
    }

    private void goBack() {
        WebView w = currentWeb();
        if (w != null && w.canGoBack()) w.goBack(); else closeCurrentTab();
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.isCtrlPressed() && event.getKeyCode() == KeyEvent.KEYCODE_L) { addressBar.requestFocus(); addressBar.selectAll(); return true; }
            if (event.isCtrlPressed() && event.getKeyCode() == KeyEvent.KEYCODE_R) { WebView w=currentWeb(); if(w!=null)w.reload(); return true; }
            if (event.isAltPressed() && event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) { goBack(); return true; }
            if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) { openDetectedMedia(); return true; }
            if (event.getKeyCode() == KeyEvent.KEYCODE_MENU) { findViewById(R.id.settingsButton).requestFocus(); return true; }
            if (currentWeb() != null && currentWeb().hasFocus()) {
                float step = settings.pointerSpeed();
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_DPAD_LEFT -> { cursor.move(-step, 0); return true; }
                    case KeyEvent.KEYCODE_DPAD_RIGHT -> { cursor.move(step, 0); return true; }
                    case KeyEvent.KEYCODE_DPAD_UP -> { cursor.move(0, -step); return true; }
                    case KeyEvent.KEYCODE_DPAD_DOWN -> { cursor.move(0, step); return true; }
                    case KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> { clickCursor(); return true; }
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void clickCursor() {
        WebView w = currentWeb(); if (w == null) return;
        cursor.showClickFeedback();
        int[] loc = new int[2]; w.getLocationOnScreen(loc);
        float localX = cursor.getCursorX() - loc[0];
        float localY = cursor.getCursorY() - loc[1];
        long now = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, localX, localY, 0);
        MotionEvent up = MotionEvent.obtain(now, now + 60, MotionEvent.ACTION_UP, localX, localY, 0);
        w.dispatchTouchEvent(down); w.dispatchTouchEvent(up);
        down.recycle(); up.recycle();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getData() != null) createTab(false, intent.getDataString());
    }

    @Override protected void onDestroy() {
        for (BrowserTab t : tabs) t.webView.destroy();
        super.onDestroy();
    }
}
