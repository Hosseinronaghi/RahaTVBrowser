package com.raha.browser.tv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final String SEARCH_URL = "https://www.google.com/search?q=";
    private static final String PREFS = "raha_tv_browser";
    private static final String KEY_DESKTOP = "desktop_mode";
    private static final String KEY_CURSOR_SPEED = "cursor_speed";
    private static final int MAX_TABS = 5;

    private GeckoView geckoView;
    private GeckoRuntime runtime;
    private EditText addressBar;
    private ProgressBar progressBar;
    private LinearLayout toolbar;
    private CursorOverlayView cursorOverlay;
    private Button backButton, forwardButton, reloadButton, favoriteButton, desktopButton, modeButton;
    private Button tabsButton, settingsButton, privateButton, playerButton, closeButton;

    private final List<BrowserTab> tabs = new ArrayList<>();
    private BrowserTab activeTab;
    private long nextTabId = 1L;
    private FavoriteStore favoriteStore;
    private HistoryStore historyStore;
    private SharedPreferences preferences;
    private boolean desktopMode;
    private boolean cursorMode;
    private boolean fullScreen;
    private boolean pageLoading;
    private float cursorStep;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        FontManager.applyToTree(this, findViewById(android.R.id.content));
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        desktopMode = preferences.getBoolean(KEY_DESKTOP, true);
        cursorStep = preferences.getFloat(KEY_CURSOR_SPEED, 18f);
        favoriteStore = new FavoriteStore(this);
        historyStore = new HistoryStore(this);
        bindViews();
        createRuntime();
        configureControls();
        updateDesktopButton();

        String incoming = getIncomingUrl(getIntent());
        createTab(false, incoming, true);
        cursorOverlay.post(() -> setCursorMode(true));
    }

    private void bindViews() {
        geckoView = findViewById(R.id.geckoView); addressBar = findViewById(R.id.addressBar);
        progressBar = findViewById(R.id.progressBar); toolbar = findViewById(R.id.toolbar);
        cursorOverlay = findViewById(R.id.cursorOverlay); backButton = findViewById(R.id.backButton);
        forwardButton = findViewById(R.id.forwardButton); reloadButton = findViewById(R.id.reloadButton);
        favoriteButton = findViewById(R.id.favoriteButton); desktopButton = findViewById(R.id.desktopButton);
        modeButton = findViewById(R.id.modeButton); tabsButton = findViewById(R.id.tabsButton);
        settingsButton = findViewById(R.id.settingsButton); privateButton = findViewById(R.id.privateButton);
        playerButton = findViewById(R.id.playerButton); closeButton = findViewById(R.id.closeButton);
    }

    private void createRuntime() {
        runtime = GeckoRuntime.create(this, new GeckoRuntimeSettings.Builder()
                .javaScriptEnabled(true).remoteDebuggingEnabled(false).build());
        geckoView.setViewBackend(GeckoView.BACKEND_TEXTURE_VIEW);
        geckoView.setFocusable(true); geckoView.setFocusableInTouchMode(true); geckoView.setKeepScreenOn(true);
    }

    private BrowserTab createTab(boolean privateMode, @Nullable String initialUrl, boolean activate) {
        if (tabs.size() >= MAX_TABS) {
            Toast.makeText(this, R.string.tab_limit, Toast.LENGTH_SHORT).show();
            return activeTab;
        }
        GeckoSessionSettings settings = new GeckoSessionSettings.Builder()
                .usePrivateMode(privateMode)
                .useTrackingProtection(true)
                .userAgentMode(desktopMode ? GeckoSessionSettings.USER_AGENT_MODE_DESKTOP : GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
                .viewportMode(desktopMode ? GeckoSessionSettings.VIEWPORT_MODE_DESKTOP : GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
                .suspendMediaWhenInactive(true)
                .build();
        GeckoSession session = new GeckoSession(settings);
        BrowserTab tab = new BrowserTab(nextTabId++, session, privateMode);
        attachDelegates(tab);
        session.open(runtime);
        tabs.add(tab);
        if (activate) activateTab(tab);
        if (initialUrl == null || initialUrl.trim().isEmpty()) showHome(tab); else loadWebUri(tab, initialUrl);
        updateTabsButton();
        return tab;
    }

    private void attachDelegates(@NonNull BrowserTab tab) {
        tab.session.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Nullable @Override public GeckoResult<AllowOrDeny> onLoadRequest(@NonNull GeckoSession session,
                    @NonNull GeckoSession.NavigationDelegate.LoadRequest request) {
                if (isDirectMedia(request.uri)) {
                    openNativePlayer(request.uri);
                    return GeckoResult.deny();
                }
                if (request.target == GeckoSession.NavigationDelegate.TARGET_WINDOW_NEW && isWebUrl(request.uri)) {
                    createTab(tab.privateMode, request.uri, true);
                    return GeckoResult.deny();
                }
                if (isWebUrl(request.uri)) tab.home = false;
                return null;
            }

            @Override public void onLocationChange(@NonNull GeckoSession session, @Nullable String url,
                    @NonNull List<GeckoSession.PermissionDelegate.ContentPermission> perms,
                    @NonNull Boolean hasUserGesture) {
                if (url == null) return;
                if (tab.home && url.startsWith("data:text/html")) { tab.url = null; }
                else if (isWebUrl(url)) { tab.home = false; tab.url = url; }
                if (tab == activeTab) updateActiveUi();
            }

            @Override public void onCanGoBack(@NonNull GeckoSession session, boolean value) {
                tab.canGoBack = value; if (tab == activeTab) updateNavigationButtons();
            }
            @Override public void onCanGoForward(@NonNull GeckoSession session, boolean value) {
                tab.canGoForward = value; if (tab == activeTab) updateNavigationButtons();
            }
        });

        tab.session.setProgressDelegate(new GeckoSession.ProgressDelegate() {
            @Override public void onPageStart(@NonNull GeckoSession session, @NonNull String url) {
                if (tab != activeTab) return;
                pageLoading = true; progressBar.setVisibility(View.VISIBLE); progressBar.setProgress(5);
                reloadButton.setText("×");
            }
            @Override public void onProgressChange(@NonNull GeckoSession session, int progress) {
                if (tab == activeTab) progressBar.setProgress(progress);
            }
            @Override public void onPageStop(@NonNull GeckoSession session, boolean success) {
                if (tab == activeTab) {
                    pageLoading = false; progressBar.setProgress(100);
                    progressBar.postDelayed(() -> progressBar.setVisibility(View.GONE), 160);
                    reloadButton.setText("↻");
                }
                if (success && !tab.privateMode && !tab.home && tab.url != null) {
                    historyStore.add(tab.title, tab.url);
                }
            }
        });

        tab.session.setContentDelegate(new GeckoSession.ContentDelegate() {
            @Override public void onFullScreen(@NonNull GeckoSession session, boolean isFullScreen) {
                if (tab == activeTab) setBrowserFullScreen(isFullScreen);
            }
            @Override public void onTitleChange(@NonNull GeckoSession session, @Nullable String title) {
                if (title != null && !title.trim().isEmpty()) tab.title = title.trim();
                if (tab == activeTab) { setTitle(tab.privateMode ? "Private · " + tab.title : tab.title); updateTabsButton(); }
            }
        });
    }

    private void activateTab(@NonNull BrowserTab tab) {
        if (activeTab != null && activeTab != tab) activeTab.session.setActive(false);
        activeTab = tab;
        tab.session.setActive(true);
        geckoView.setSession(tab.session);
        updateActiveUi();
        updateTabsButton();
    }

    private void configureControls() {
        backButton.setOnClickListener(v -> goBack());
        forwardButton.setOnClickListener(v -> { if (activeTab != null && activeTab.canGoForward) activeTab.session.goForward(); });
        findViewById(R.id.homeButton).setOnClickListener(v -> showHome(activeTab));
        reloadButton.setOnClickListener(v -> { if (activeTab == null) return; if (pageLoading) activeTab.session.stop(); else activeTab.session.reload(); });
        findViewById(R.id.goButton).setOnClickListener(v -> loadAddress());
        favoriteButton.setOnClickListener(v -> toggleFavorite());
        desktopButton.setOnClickListener(v -> toggleDesktopMode());
        modeButton.setOnClickListener(v -> setCursorMode(!cursorMode));
        tabsButton.setOnClickListener(v -> showTabsDialog());
        settingsButton.setOnClickListener(v -> showSettingsDialog());
        privateButton.setOnClickListener(v -> createTab(true, null, true));
        playerButton.setOnClickListener(v -> { if (activeTab != null && activeTab.url != null) openNativePlayer(activeTab.url); });
        closeButton.setOnClickListener(v -> closeCurrentTab());
        addressBar.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) { loadAddress(); return true; }
            return false;
        });
    }

    private void showHome(@Nullable BrowserTab tab) {
        if (tab == null) return;
        tab.home = true; tab.url = null; tab.title = getString(R.string.app_name);
        String html = buildHomeHtml(favoriteStore.getAll(), historyStore.getAll(), tab.privateMode);
        tab.session.loadUri("data:text/html;charset=utf-8," + Uri.encode(html));
        if (tab == activeTab) updateActiveUi();
    }

    private String buildHomeHtml(List<FavoriteStore.Favorite> favorites, List<HistoryStore.Entry> history, boolean privateMode) {
        StringBuilder cards = new StringBuilder();
        if (favorites.isEmpty()) cards.append("<div class='empty'>").append(escapeHtml(getString(R.string.no_favorites))).append("</div>");
        for (FavoriteStore.Favorite f : favorites) {
            cards.append("<a class='card' href='").append(escapeHtml(f.url)).append("'><span class='ico'>★</span><span>")
                    .append(escapeHtml(f.title)).append("</span></a>");
        }
        StringBuilder recent = new StringBuilder();
        if (!privateMode) for (HistoryStore.Entry h : history) {
            recent.append("<a class='recent' href='").append(escapeHtml(h.url)).append("'>")
                    .append(escapeHtml(h.title)).append("<small>").append(escapeHtml(shortHost(h.url))).append("</small></a>");
        }
        return "<!doctype html><html lang='fa' dir='rtl'><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>" +
                "<style>*{box-sizing:border-box}body{margin:0;background:#0b1020;color:#f6f8ff;font-family:Arial,sans-serif;padding:28px 40px}h1{font-size:27px;margin:0 0 18px}h2{font-size:18px;color:#b9c6dd;margin:25px 0 12px}.grid{display:grid;grid-template-columns:repeat(8,minmax(0,1fr));gap:10px}.card{height:88px;background:#18233a;color:white;text-decoration:none;border-radius:12px;padding:10px;display:flex;flex-direction:column;justify-content:center;align-items:center;text-align:center;font-size:13px;overflow:hidden}.card:focus,.recent:focus{outline:3px solid #78aefc;transform:scale(1.04)}.ico{font-size:23px;color:#ffbd3e;margin-bottom:5px}.search{display:flex;gap:10px}.search input{flex:1;padding:14px 18px;border:0;border-radius:12px;font-size:17px}.search button{padding:0 25px;border:0;border-radius:12px;background:#3974d7;color:white}.recentGrid{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:9px}.recent{background:#131d31;color:white;text-decoration:none;border-radius:10px;padding:12px;font-size:13px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.recent small{display:block;color:#8fa0bd;margin-top:5px;overflow:hidden;text-overflow:ellipsis}.empty{color:#95a5bf;padding:20px 0}.private{color:#d7a6ff}</style></head><body>" +
                "<h1>RahaTVBrowser" + (privateMode ? " <span class='private'>· Private</span>" : "") + "</h1>" +
                "<form class='search' action='https://www.google.com/search'><input name='q' placeholder='" + escapeHtml(getString(R.string.address_hint)) + "'><button>Search</button></form>" +
                "<h2>" + escapeHtml(getString(R.string.favorites)) + "</h2><div class='grid'>" + cards + "</div>" +
                (privateMode ? "" : "<h2>" + escapeHtml(getString(R.string.recent_visits)) + "</h2><div class='recentGrid'>" + recent + "</div>") +
                "</body></html>";
    }

    private String shortHost(String url) { try { return new URI(url).getHost(); } catch (Exception e) { return url; } }
    private String escapeHtml(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"); }

    private void loadAddress() {
        if (activeTab == null) return;
        String input = addressBar.getText().toString().trim();
        if (!input.isEmpty()) loadWebUri(activeTab, normalizeInput(input));
        addressBar.clearFocus(); geckoView.requestFocus();
    }

    private void loadWebUri(@NonNull BrowserTab tab, @NonNull String uri) {
        String normalized = normalizeInput(uri);
        if (isDirectMedia(normalized)) { openNativePlayer(normalized); return; }
        tab.home = false; tab.url = normalized; tab.session.loadUri(normalized);
        if (tab == activeTab) updateActiveUi();
    }

    private String normalizeInput(String input) {
        String s = input.trim();
        if (s.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) return s;
        if (looksLikeHost(s)) return "https://" + s;
        return SEARCH_URL + Uri.encode(s);
    }
    private boolean looksLikeHost(String input) { return !input.contains(" ") && (input.contains(".") || input.startsWith("localhost")); }
    private boolean isWebUrl(@Nullable String url) { if (url == null) return false; String x = url.toLowerCase(Locale.ROOT); return x.startsWith("http://") || x.startsWith("https://"); }
    private boolean isDirectMedia(@Nullable String url) { if (url == null) return false; String x = url.toLowerCase(Locale.ROOT).split("\\?")[0]; return x.endsWith(".m3u8") || x.endsWith(".mpd") || x.endsWith(".mp4") || x.endsWith(".webm") || x.endsWith(".mkv"); }

    private void openNativePlayer(@NonNull String url) {
        Intent i = new Intent(this, PlayerActivity.class); i.putExtra(PlayerActivity.EXTRA_URL, url); startActivity(i);
    }

    private void toggleFavorite() {
        if (activeTab == null || activeTab.url == null || activeTab.privateMode) {
            Toast.makeText(this, R.string.favorite_private_blocked, Toast.LENGTH_SHORT).show(); return;
        }
        boolean added = favoriteStore.toggle(activeTab.title, activeTab.url);
        Toast.makeText(this, added ? R.string.favorite_added : R.string.favorite_removed, Toast.LENGTH_SHORT).show();
        updateFavoriteButton();
    }

    private void updateFavoriteButton() {
        boolean saved = activeTab != null && activeTab.url != null && favoriteStore.contains(activeTab.url);
        favoriteButton.setText(saved ? "★" : "☆");
        favoriteButton.setEnabled(activeTab != null && !activeTab.privateMode && activeTab.url != null);
    }

    private void toggleDesktopMode() {
        desktopMode = !desktopMode; preferences.edit().putBoolean(KEY_DESKTOP, desktopMode).apply();
        for (BrowserTab tab : tabs) {
            tab.session.getSettings().setUserAgentMode(desktopMode ? GeckoSessionSettings.USER_AGENT_MODE_DESKTOP : GeckoSessionSettings.USER_AGENT_MODE_MOBILE);
            tab.session.getSettings().setViewportMode(desktopMode ? GeckoSessionSettings.VIEWPORT_MODE_DESKTOP : GeckoSessionSettings.VIEWPORT_MODE_MOBILE);
            if (!tab.home) tab.session.reload();
        }
        updateDesktopButton();
        Toast.makeText(this, desktopMode ? R.string.desktop_enabled : R.string.mobile_enabled, Toast.LENGTH_SHORT).show();
    }

    private void updateDesktopButton() { desktopButton.setText(desktopMode ? "D" : "M"); desktopButton.setContentDescription(getString(desktopMode ? R.string.desktop_mode : R.string.mobile_mode)); }

    private void showTabsDialog() {
        String[] labels = new String[tabs.size() + 2];
        for (int i = 0; i < tabs.size(); i++) labels[i] = (tabs.get(i).privateMode ? "◉ " : "") + (i + 1) + ". " + tabs.get(i).title;
        labels[tabs.size()] = getString(R.string.new_tab); labels[tabs.size() + 1] = getString(R.string.new_private_tab);
        new AlertDialog.Builder(this).setTitle(getString(R.string.tabs)).setItems(labels, (d, which) -> {
            if (which < tabs.size()) activateTab(tabs.get(which));
            else if (which == tabs.size()) createTab(false, null, true);
            else createTab(true, null, true);
        }).show();
    }

    private void showSettingsDialog() {
        String[] items = { getString(R.string.toggle_desktop), getString(R.string.cursor_speed), getString(R.string.clear_history), getString(R.string.close_all_tabs) };
        new AlertDialog.Builder(this).setTitle(R.string.settings).setItems(items, (d, which) -> {
            if (which == 0) toggleDesktopMode();
            else if (which == 1) showCursorSpeedDialog();
            else if (which == 2) { historyStore.clear(); Toast.makeText(this, R.string.history_cleared, Toast.LENGTH_SHORT).show(); if (activeTab != null && activeTab.home) showHome(activeTab); }
            else closeAllTabs();
        }).show();
    }

    private void showCursorSpeedDialog() {
        String[] choices = { getString(R.string.slow), getString(R.string.normal), getString(R.string.fast) };
        new AlertDialog.Builder(this).setTitle(R.string.cursor_speed).setItems(choices, (d, which) -> {
            cursorStep = which == 0 ? 11f : which == 1 ? 18f : 28f;
            preferences.edit().putFloat(KEY_CURSOR_SPEED, cursorStep).apply();
        }).show();
    }

    private void closeCurrentTab() {
        if (activeTab == null) return;
        int index = tabs.indexOf(activeTab);
        activeTab.session.close(); tabs.remove(activeTab);
        if (tabs.isEmpty()) { createTab(false, null, true); return; }
        activateTab(tabs.get(Math.max(0, Math.min(index, tabs.size() - 1))));
    }

    private void closeAllTabs() {
        for (BrowserTab tab : new ArrayList<>(tabs)) tab.session.close();
        tabs.clear(); createTab(false, null, true);
    }

    private void updateTabsButton() { tabsButton.setText(String.valueOf(tabs.size())); }

    private void updateActiveUi() {
        if (activeTab == null) return;
        if (!addressBar.hasFocus()) addressBar.setText(activeTab.home || activeTab.url == null ? "" : activeTab.url);
        updateNavigationButtons(); updateFavoriteButton();
        privateButton.setAlpha(activeTab.privateMode ? 1f : .72f);
        setTitle(activeTab.privateMode ? "Private · " + activeTab.title : activeTab.title);
    }

    private void updateNavigationButtons() {
        boolean back = activeTab != null && activeTab.canGoBack; boolean forward = activeTab != null && activeTab.canGoForward;
        backButton.setEnabled(back); backButton.setAlpha(back ? 1f : .42f);
        forwardButton.setEnabled(forward); forwardButton.setAlpha(forward ? 1f : .42f);
    }

    private void setCursorMode(boolean enabled) {
        cursorMode = enabled; cursorOverlay.setVisibility(enabled ? View.VISIBLE : View.GONE);
        modeButton.setText(enabled ? "◎" : "✥");
        if (enabled) { cursorOverlay.bringToFront(); cursorOverlay.requestFocus(); hoverAtCursor(); }
        else geckoView.requestFocus();
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (handleKeyboardShortcut(event)) return true;
        if (isMediaPlaybackKey(event.getKeyCode()) && activeTab != null) return geckoView.dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
        if (cursorMode && cursorOverlay.hasFocus()) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                float step = dp(event.getRepeatCount() > 0 ? cursorStep * 1.45f : cursorStep);
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_DPAD_LEFT: moveCursor(-step, 0); return true;
                    case KeyEvent.KEYCODE_DPAD_RIGHT: moveCursor(step, 0); return true;
                    case KeyEvent.KEYCODE_DPAD_UP: moveCursor(0, -step); return true;
                    case KeyEvent.KEYCODE_DPAD_DOWN: moveCursor(0, step); return true;
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                    case KeyEvent.KEYCODE_ENTER:
                    case KeyEvent.KEYCODE_NUMPAD_ENTER: clickAtCursor(); return true;
                    case KeyEvent.KEYCODE_PAGE_UP:
                    case KeyEvent.KEYCODE_CHANNEL_UP: scrollWebPage(1f); return true;
                    case KeyEvent.KEYCODE_PAGE_DOWN:
                    case KeyEvent.KEYCODE_CHANNEL_DOWN: scrollWebPage(-1f); return true;
                    case KeyEvent.KEYCODE_BACK: setCursorMode(false); return true;
                    default: break;
                }
            }
            if (isDpadNavigationKey(event.getKeyCode())) return true;
            return geckoView.dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_MENU) { setCursorMode(!cursorMode); return true; }
        return super.dispatchKeyEvent(event);
    }

    private boolean handleKeyboardShortcut(KeyEvent e) {
        if (e.getAction() != KeyEvent.ACTION_DOWN) return false;
        if (e.isCtrlPressed() && (e.getKeyCode() == KeyEvent.KEYCODE_L || e.getKeyCode() == KeyEvent.KEYCODE_K)) { focusAddressBar(); return true; }
        if (e.isCtrlPressed() && e.getKeyCode() == KeyEvent.KEYCODE_T) { createTab(false, null, true); return true; }
        if (e.isCtrlPressed() && e.getKeyCode() == KeyEvent.KEYCODE_W) { closeCurrentTab(); return true; }
        if (e.isCtrlPressed() && e.getKeyCode() == KeyEvent.KEYCODE_R && activeTab != null) { activeTab.session.reload(); return true; }
        if (e.isAltPressed() && e.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) { goBack(); return true; }
        if (e.getKeyCode() == KeyEvent.KEYCODE_F6 || e.getKeyCode() == KeyEvent.KEYCODE_SEARCH) { focusAddressBar(); return true; }
        return false;
    }

    private void moveCursor(float dx, float dy) { cursorOverlay.moveBy(dx, dy); hoverAtCursor(); }
    private void hoverAtCursor() { if (activeTab == null) return; MotionEvent e = obtainMouseEvent(MotionEvent.ACTION_HOVER_MOVE, cursorOverlay.getCursorX(), cursorOverlay.getCursorY(), 0); activeTab.session.getPanZoomController().onMotionEvent(e); e.recycle(); }
    private void clickAtCursor() {
        if (activeTab == null) return; cursorOverlay.showClickFeedback(); float x = cursorOverlay.getCursorX(), y = cursorOverlay.getCursorY(); long t = SystemClock.uptimeMillis();
        MotionEvent d = obtainMouseEvent(MotionEvent.ACTION_DOWN, x, y, MotionEvent.BUTTON_PRIMARY, t, t);
        MotionEvent u = obtainMouseEvent(MotionEvent.ACTION_UP, x, y, 0, t, t + 70);
        activeTab.session.getPanZoomController().onMouseEvent(d); activeTab.session.getPanZoomController().onMouseEvent(u); d.recycle(); u.recycle();
    }
    private void scrollWebPage(float amount) { if (activeTab == null) return; MotionEvent e = obtainMouseScrollEvent(cursorOverlay.getCursorX(), cursorOverlay.getCursorY(), amount); activeTab.session.getPanZoomController().onMotionEvent(e); e.recycle(); }

    private MotionEvent obtainMouseScrollEvent(float x, float y, float amount) {
        long now = SystemClock.uptimeMillis(); MotionEvent.PointerProperties p = new MotionEvent.PointerProperties(); p.id = 0; p.toolType = MotionEvent.TOOL_TYPE_MOUSE;
        MotionEvent.PointerCoords c = new MotionEvent.PointerCoords(); c.x = x; c.y = y; c.setAxisValue(MotionEvent.AXIS_VSCROLL, amount);
        return MotionEvent.obtain(now, now, MotionEvent.ACTION_SCROLL, 1, new MotionEvent.PointerProperties[]{p}, new MotionEvent.PointerCoords[]{c}, 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_MOUSE, 0);
    }
    private MotionEvent obtainMouseEvent(int action, float x, float y, int buttons) { long n = SystemClock.uptimeMillis(); return obtainMouseEvent(action, x, y, buttons, n, n); }
    private MotionEvent obtainMouseEvent(int action, float x, float y, int buttons, long down, long time) {
        MotionEvent.PointerProperties p = new MotionEvent.PointerProperties(); p.id = 0; p.toolType = MotionEvent.TOOL_TYPE_MOUSE;
        MotionEvent.PointerCoords c = new MotionEvent.PointerCoords(); c.x = x; c.y = y; c.pressure = action == MotionEvent.ACTION_UP ? 0f : 1f; c.size = 1f;
        return MotionEvent.obtain(down, time, action, 1, new MotionEvent.PointerProperties[]{p}, new MotionEvent.PointerCoords[]{c}, 0, buttons, 1f, 1f, 0, 0, InputDevice.SOURCE_MOUSE, 0);
    }

    @Override public boolean dispatchGenericMotionEvent(MotionEvent event) { prepareForPhysicalMouse(event); return super.dispatchGenericMotionEvent(event); }
    @Override public boolean dispatchTouchEvent(MotionEvent event) { prepareForPhysicalMouse(event); return super.dispatchTouchEvent(event); }
    private void prepareForPhysicalMouse(MotionEvent e) { if ((e.getSource() & InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE && cursorMode) setCursorMode(false); }

    private boolean isMediaPlaybackKey(int k) { return k == KeyEvent.KEYCODE_MEDIA_PLAY || k == KeyEvent.KEYCODE_MEDIA_PAUSE || k == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || k == KeyEvent.KEYCODE_MEDIA_STOP || k == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD || k == KeyEvent.KEYCODE_MEDIA_REWIND; }
    private boolean isDpadNavigationKey(int k) { return k == KeyEvent.KEYCODE_DPAD_LEFT || k == KeyEvent.KEYCODE_DPAD_RIGHT || k == KeyEvent.KEYCODE_DPAD_UP || k == KeyEvent.KEYCODE_DPAD_DOWN || k == KeyEvent.KEYCODE_DPAD_CENTER; }
    private void focusAddressBar() { setCursorMode(false); addressBar.requestFocus(); addressBar.selectAll(); }

    private void setBrowserFullScreen(boolean enabled) {
        fullScreen = enabled;
        // SurfaceView gives the hardware video path the best chance of smooth 1080p/4K playback.
        // TextureView is restored outside fullscreen so the D-pad cursor can remain above web content.
        try { geckoView.setViewBackend(enabled ? GeckoView.BACKEND_SURFACE_VIEW : GeckoView.BACKEND_TEXTURE_VIEW); } catch (RuntimeException ignored) { }
        toolbar.setVisibility(enabled ? View.GONE : View.VISIBLE); progressBar.setVisibility(enabled ? View.GONE : progressBar.getVisibility()); cursorOverlay.setVisibility(enabled ? View.GONE : (cursorMode ? View.VISIBLE : View.GONE));
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController(); if (c != null) { if (enabled) c.hide(WindowInsets.Type.systemBars()); else c.show(WindowInsets.Type.systemBars()); }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(enabled ? View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY : View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    private void goBack() {
        if (fullScreen) { setBrowserFullScreen(false); return; }
        if (activeTab != null && activeTab.canGoBack) activeTab.session.goBack(); else if (tabs.size() > 1) closeCurrentTab(); else showExitDialog();
    }
    private void showExitDialog() { new AlertDialog.Builder(this).setMessage(R.string.exit_question).setPositiveButton(R.string.close, (d,w)->finish()).setNegativeButton(android.R.string.cancel, null).show(); }

    @Nullable private String getIncomingUrl(@Nullable Intent intent) { if (intent == null || intent.getData() == null) return null; String s = intent.getData().toString(); return isWebUrl(s) ? s : null; }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); setIntent(intent); String u = getIncomingUrl(intent); if (u != null) createTab(false, u, true); }
    @Override protected void onResume() { super.onResume(); if (activeTab != null) activeTab.session.setActive(true); }
    @Override protected void onPause() { if (activeTab != null) activeTab.session.setActive(false); super.onPause(); }
    @Override protected void onDestroy() { for (BrowserTab tab : tabs) tab.session.close(); if (runtime != null) runtime.shutdown(); super.onDestroy(); }
    @Override public void onBackPressed() { goBack(); }
    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }
}
