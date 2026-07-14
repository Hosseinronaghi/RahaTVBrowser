package com.raha.browser.tv;

import android.app.Activity;
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
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final String SEARCH_URL = "https://www.google.com/search?q=";
    private static final String PREFS_NAME = "raha_tv_browser";
    private static final String KEY_DESKTOP_MODE = "desktop_mode";

    private GeckoView geckoView;
    private GeckoSession session;
    private GeckoRuntime runtime;
    private EditText addressBar;
    private ProgressBar progressBar;
    private LinearLayout toolbar;
    private CursorOverlayView cursorOverlay;
    private Button backButton;
    private Button forwardButton;
    private Button reloadButton;
    private Button modeButton;
    private Button favoriteButton;
    private Button desktopButton;

    private FavoriteStore favoriteStore;
    private SharedPreferences preferences;

    private boolean canGoBack;
    private boolean canGoForward;
    private boolean cursorMode;
    private boolean desktopMode;
    private boolean fullScreen;
    private boolean pageLoading;
    private boolean homePage;

    @Nullable private String currentUrl;
    @Nullable private String currentTitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        desktopMode = preferences.getBoolean(KEY_DESKTOP_MODE, true);
        favoriteStore = new FavoriteStore(this);

        bindViews();
        configureBrowser();
        configureControls();
        updateDesktopButton();

        String incoming = getIncomingUrl(getIntent());
        if (incoming == null) {
            showHome();
        } else {
            loadWebUri(incoming);
        }

        // TV browsing is cursor-first. The cursor is visible immediately and remains optional.
        cursorOverlay.post(() -> setCursorMode(true));
    }

    private void bindViews() {
        geckoView = findViewById(R.id.geckoView);
        addressBar = findViewById(R.id.addressBar);
        progressBar = findViewById(R.id.progressBar);
        toolbar = findViewById(R.id.toolbar);
        cursorOverlay = findViewById(R.id.cursorOverlay);
        backButton = findViewById(R.id.backButton);
        forwardButton = findViewById(R.id.forwardButton);
        reloadButton = findViewById(R.id.reloadButton);
        modeButton = findViewById(R.id.modeButton);
        favoriteButton = findViewById(R.id.favoriteButton);
        desktopButton = findViewById(R.id.desktopButton);
    }

    private void configureBrowser() {
        GeckoRuntimeSettings runtimeSettings = new GeckoRuntimeSettings.Builder()
                .javaScriptEnabled(true)
                .remoteDebuggingEnabled(false)
                .build();
        runtime = GeckoRuntime.create(this, runtimeSettings);

        GeckoSessionSettings sessionSettings = new GeckoSessionSettings.Builder()
                .userAgentMode(desktopMode
                        ? GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
                        : GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
                .viewportMode(desktopMode
                        ? GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
                        : GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
                .suspendMediaWhenInactive(false)
                .build();

        session = new GeckoSession(sessionSettings);
        session.open(runtime);
        // SurfaceView is faster, but it can cover ordinary Android overlay Views.
        // TextureView keeps the high-contrast D-pad cursor visible above web content.
        geckoView.setViewBackend(GeckoView.BACKEND_TEXTURE_VIEW);
        geckoView.setSession(session);
        geckoView.setFocusable(true);
        geckoView.setFocusableInTouchMode(true);
        geckoView.setKeepScreenOn(true);

        session.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Nullable
            @Override
            public GeckoResult<AllowOrDeny> onLoadRequest(
                    @NonNull GeckoSession session,
                    @NonNull GeckoSession.NavigationDelegate.LoadRequest request) {
                // Streaming sites frequently use target=_blank/window.open for the actual player.
                // Keep the navigation inside RahaTVBrowser so the player is not silently lost.
                if (request.target == GeckoSession.NavigationDelegate.TARGET_WINDOW_NEW
                        && isWebUrl(request.uri)) {
                    loadWebUri(request.uri);
                    return GeckoResult.deny();
                }
                if (isWebUrl(request.uri)) {
                    homePage = false;
                }
                return null;
            }

            @Override
            public void onLocationChange(@NonNull GeckoSession session,
                                         @Nullable String url,
                                         @NonNull List<GeckoSession.PermissionDelegate.ContentPermission> perms,
                                         @NonNull Boolean hasUserGesture) {
                if (url == null) return;
                if (homePage && url.startsWith("data:text/html")) {
                    addressBar.setText("");
                    currentUrl = null;
                    updateFavoriteButton();
                    return;
                }
                if (isWebUrl(url)) {
                    homePage = false;
                    currentUrl = url;
                    if (!addressBar.hasFocus()) {
                        addressBar.setText(url);
                    }
                    updateFavoriteButton();
                }
            }

            @Override
            public void onCanGoBack(@NonNull GeckoSession session, boolean value) {
                canGoBack = value;
                backButton.setEnabled(value);
                backButton.setAlpha(value ? 1f : 0.45f);
            }

            @Override
            public void onCanGoForward(@NonNull GeckoSession session, boolean value) {
                canGoForward = value;
                forwardButton.setEnabled(value);
                forwardButton.setAlpha(value ? 1f : 0.45f);
            }
        });

        session.setProgressDelegate(new GeckoSession.ProgressDelegate() {
            @Override
            public void onPageStart(@NonNull GeckoSession session, @NonNull String url) {
                pageLoading = true;
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(5);
                reloadButton.setText("×");
                reloadButton.setContentDescription(getString(R.string.stop));
            }

            @Override
            public void onProgressChange(@NonNull GeckoSession session, int progress) {
                progressBar.setProgress(progress);
            }

            @Override
            public void onPageStop(@NonNull GeckoSession session, boolean success) {
                pageLoading = false;
                progressBar.setProgress(100);
                progressBar.postDelayed(() -> progressBar.setVisibility(View.GONE), 180);
                reloadButton.setText("↻");
                reloadButton.setContentDescription(getString(R.string.reload));
                if (!success && !homePage) {
                    Toast.makeText(MainActivity.this, R.string.page_failed, Toast.LENGTH_SHORT).show();
                }
            }
        });

        session.setContentDelegate(new GeckoSession.ContentDelegate() {
            @Override
            public void onFullScreen(@NonNull GeckoSession session, boolean isFullScreen) {
                setBrowserFullScreen(isFullScreen);
            }

            @Override
            public void onTitleChange(@NonNull GeckoSession session, @Nullable String title) {
                if (homePage) {
                    currentTitle = getString(R.string.app_name);
                    setTitle(R.string.app_name);
                    return;
                }
                if (title != null && !title.trim().isEmpty()) {
                    currentTitle = title;
                    setTitle(title);
                }
            }
        });

        session.setPromptDelegate(new GeckoSession.PromptDelegate() {
            @Nullable
            @Override
            public GeckoResult<GeckoSession.PromptDelegate.PromptResponse> onPopupPrompt(
                    @NonNull GeckoSession session,
                    @NonNull GeckoSession.PromptDelegate.PopupPrompt prompt) {
                // Some streaming pages open the player from script rather than a direct tap.
                // Move safe HTTP(S) popup targets into the current single-session browser.
                if (isWebUrl(prompt.targetUri)) {
                    loadWebUri(prompt.targetUri);
                    return GeckoResult.fromValue(prompt.confirm(AllowOrDeny.DENY));
                }
                return GeckoResult.fromValue(prompt.confirm(AllowOrDeny.DENY));
            }
        });

        session.setPermissionDelegate(new GeckoSession.PermissionDelegate() {
            @Nullable
            @Override
            public GeckoResult<Integer> onContentPermissionRequest(
                    @NonNull GeckoSession session,
                    @NonNull GeckoSession.PermissionDelegate.ContentPermission permission) {
                switch (permission.permission) {
                    case GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE:
                    case GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_INAUDIBLE:
                    case GeckoSession.PermissionDelegate.PERMISSION_MEDIA_KEY_SYSTEM_ACCESS:
                    case GeckoSession.PermissionDelegate.PERMISSION_PERSISTENT_STORAGE:
                        return GeckoResult.fromValue(
                                GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW);
                    default:
                        return null;
                }
            }
        });
    }

    private void configureControls() {
        backButton.setOnClickListener(v -> goBack());
        forwardButton.setOnClickListener(v -> {
            if (canGoForward) session.goForward();
        });
        findViewById(R.id.homeButton).setOnClickListener(v -> showHome());
        reloadButton.setOnClickListener(v -> {
            if (pageLoading) session.stop(); else session.reload();
        });
        findViewById(R.id.goButton).setOnClickListener(v -> loadAddress());
        modeButton.setOnClickListener(v -> setCursorMode(!cursorMode));
        favoriteButton.setOnClickListener(v -> toggleFavorite());
        desktopButton.setOnClickListener(v -> toggleDesktopMode());

        addressBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loadAddress();
                return true;
            }
            return false;
        });

        geckoView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && cursorMode) cursorOverlay.requestFocus();
        });
    }

    private void showHome() {
        homePage = true;
        currentUrl = null;
        currentTitle = getString(R.string.app_name);
        setTitle(R.string.app_name);
        addressBar.setText("");
        updateFavoriteButton();
        String html = buildHomeHtml(favoriteStore.getAll());
        session.loadUri("data:text/html;charset=utf-8," + Uri.encode(html));
        if (cursorMode) cursorOverlay.requestFocus();
    }

    @NonNull
    private String buildHomeHtml(@NonNull List<FavoriteStore.Favorite> favorites) {
        StringBuilder cards = new StringBuilder();
        if (favorites.isEmpty()) {
            cards.append("<div class='empty'><b>هنوز سایتی به علاقه‌مندی‌ها اضافه نشده است.</b>")
                    .append("<span>یک سایت را باز کنید و دکمه ★ نوار بالا را بزنید.</span></div>");
        } else {
            for (FavoriteStore.Favorite item : favorites) {
                String host = Uri.parse(item.url).getHost();
                if (host == null || host.trim().isEmpty()) host = item.url;
                String title = item.title == null || item.title.trim().isEmpty() ? host : item.title;
                String icon = title.substring(0, 1).toUpperCase(Locale.ROOT);
                cards.append("<a class='card' href='")
                        .append(escapeHtml(item.url))
                        .append("'><span class='icon'>")
                        .append(escapeHtml(icon))
                        .append("</span><span class='name'>")
                        .append(escapeHtml(title))
                        .append("</span><span class='host'>")
                        .append(escapeHtml(host))
                        .append("</span></a>");
            }
        }

        return "<!doctype html><html lang='fa' dir='rtl'><head>"
                + "<meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<title>RahaTVBrowser</title><style>"
                + ":root{color-scheme:dark;--bg:#090e1b;--panel:#121a2c;--card:#192541;--accent:#68a4ff;--text:#f7faff;--muted:#aebbd1}"
                + "*{box-sizing:border-box}body{margin:0;min-height:100vh;padding:6vh 6vw;background:radial-gradient(circle at 50% -20%,#20477f 0,#090e1b 48%);font-family:system-ui,sans-serif;color:var(--text)}"
                + ".wrap{max-width:1500px;margin:auto}.brand{display:flex;align-items:center;gap:18px;margin-bottom:5vh}.logo{width:70px;height:70px;border-radius:22px;background:var(--accent);display:grid;place-items:center;color:#07101f;font-size:34px;font-weight:900}.brand h1{font-size:38px;margin:0;direction:ltr}.brand p{font-size:19px;color:var(--muted);margin:7px 0 0}"
                + "form{display:flex;direction:ltr;gap:12px;margin-bottom:5vh}input{flex:1;height:64px;border:3px solid transparent;border-radius:18px;background:#fff;color:#101828;font-size:22px;padding:0 22px;outline:none}input:focus{border-color:var(--accent)}button{min-width:130px;border:3px solid transparent;border-radius:18px;background:var(--accent);color:#07101f;font-size:20px;font-weight:850}button:focus{border-color:#fff;outline:none;transform:scale(1.04)}"
                + "h2{font-size:27px;margin:0 0 22px}.grid{display:grid;grid-template-columns:repeat(5,minmax(170px,1fr));gap:20px;direction:ltr}.card{display:flex;min-height:155px;flex-direction:column;text-decoration:none;background:linear-gradient(145deg,#22345a,#121b30);border:4px solid transparent;border-radius:23px;padding:22px;color:var(--text);transition:.12s}.card:focus,.card:hover{outline:none;border-color:#fff;transform:scale(1.045);box-shadow:0 16px 42px #0009}.icon{display:grid;place-items:center;width:48px;height:48px;border-radius:15px;background:var(--accent);color:#07101f;font-size:25px;font-weight:900}.name{font-size:21px;font-weight:800;margin-top:auto;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.host{font-size:15px;color:var(--muted);margin-top:6px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.empty{display:flex;min-height:180px;flex-direction:column;justify-content:center;align-items:center;gap:13px;border:2px dashed #6e83a8;border-radius:24px;color:var(--muted);font-size:20px}.empty b{color:var(--text);font-size:24px}.hint{margin-top:5vh;color:var(--muted);font-size:17px}"
                + "@media(max-width:1100px){.grid{grid-template-columns:repeat(3,1fr)}.brand h1{font-size:32px}}"
                + "</style></head><body><div class='wrap'><header class='brand'><div class='logo'>R</div><div><h1>RahaTVBrowser</h1><p>علاقه‌مندی‌ها و جست‌وجوی سریع</p></div></header>"
                + "<form action='https://www.google.com/search' method='get'><input name='q' aria-label='جست‌وجو' placeholder='جست‌وجو در وب…'><button type='submit'>جست‌وجو</button></form>"
                + "<h2>علاقه‌مندی‌ها</h2><main class='grid'>" + cards + "</main>"
                + "<div class='hint'>ریموت: حرکت نشانگر با جهت‌ها، کلیک با OK، تغییر حالت با Menu. موس و کیبورد بلوتوثی نیز پشتیبانی می‌شوند.</div>"
                + "</div></body></html>";
    }

    @NonNull
    private String escapeHtml(@NonNull String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private void loadAddress() {
        String input = addressBar.getText().toString().trim();
        if (input.isEmpty()) return;
        loadWebUri(normalizeInput(input));
        addressBar.clearFocus();
        if (cursorMode) cursorOverlay.requestFocus(); else geckoView.requestFocus();
    }

    private void loadWebUri(@NonNull String uri) {
        homePage = false;
        currentUrl = uri;
        session.loadUri(uri);
        updateFavoriteButton();
    }

    @NonNull
    private String normalizeInput(@NonNull String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://") ||
                lower.startsWith("about:") || lower.startsWith("file:")) {
            return input;
        }
        if (input.contains(" ") || !looksLikeHost(input)) {
            return SEARCH_URL + Uri.encode(input);
        }
        return "https://" + input;
    }

    private boolean looksLikeHost(@NonNull String input) {
        try {
            URI uri = new URI("https://" + input);
            return uri.getHost() != null && uri.getHost().contains(".");
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isWebUrl(@Nullable String url) {
        if (url == null) return false;
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private void toggleFavorite() {
        if (!isWebUrl(currentUrl)) return;
        String title = currentTitle;
        if (title == null || title.trim().isEmpty()) {
            String host = Uri.parse(currentUrl).getHost();
            title = host == null ? currentUrl : host;
        }
        boolean added = favoriteStore.toggle(title, currentUrl);
        updateFavoriteButton();
        Toast.makeText(this, added ? R.string.favorite_added : R.string.favorite_removed,
                Toast.LENGTH_SHORT).show();
    }

    private void updateFavoriteButton() {
        boolean valid = isWebUrl(currentUrl) && !homePage;
        favoriteButton.setEnabled(valid);
        favoriteButton.setAlpha(valid ? 1f : 0.45f);
        boolean favorite = valid && favoriteStore.contains(currentUrl);
        favoriteButton.setText(favorite ? "★" : "☆");
        favoriteButton.setContentDescription(getString(
                favorite ? R.string.remove_favorite : R.string.add_favorite));
    }

    private void toggleDesktopMode() {
        desktopMode = !desktopMode;
        session.getSettings().setUserAgentMode(desktopMode
                ? GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
                : GeckoSessionSettings.USER_AGENT_MODE_MOBILE);
        session.getSettings().setViewportMode(desktopMode
                ? GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
                : GeckoSessionSettings.VIEWPORT_MODE_MOBILE);
        preferences.edit().putBoolean(KEY_DESKTOP_MODE, desktopMode).apply();
        updateDesktopButton();
        Toast.makeText(this,
                desktopMode ? R.string.desktop_enabled : R.string.mobile_enabled,
                Toast.LENGTH_SHORT).show();
        if (homePage) showHome(); else session.reload();
    }

    private void updateDesktopButton() {
        desktopButton.setText(desktopMode ? R.string.desktop_mode : R.string.mobile_mode);
        desktopButton.setContentDescription(getString(
                desktopMode ? R.string.desktop_enabled : R.string.mobile_enabled));
    }

    private void setCursorMode(boolean enabled) {
        cursorMode = enabled;
        cursorOverlay.setVisibility(enabled ? View.VISIBLE : View.GONE);
        modeButton.setText(enabled ? R.string.focus_mode : R.string.cursor_mode);
        modeButton.setContentDescription(getString(enabled
                ? R.string.focus_mode : R.string.cursor_mode));
        if (enabled) {
            cursorOverlay.bringToFront();
            cursorOverlay.invalidate();
            cursorOverlay.requestFocus();
            hoverAtCursor();
        } else {
            geckoView.requestFocus();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (handleBrowserKeyboardShortcut(event)) {
            return true;
        }

        // Send common TV remote playback keys directly to Gecko/JW/HTML5 media.
        if (isMediaPlaybackKey(event.getKeyCode())) {
            return geckoView.dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
        }

        // When web content owns focus, the first D-pad movement enters visible cursor mode.
        if (!cursorMode && event.getAction() == KeyEvent.ACTION_DOWN
                && isDirectionalKey(event.getKeyCode())
                && (geckoView.hasFocus() || findViewById(R.id.browserFrame).hasFocus())) {
            setCursorMode(true);
        }

        if (cursorMode && cursorOverlay.hasFocus()) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                float step = event.getRepeatCount() > 0 ? dp(24) : dp(16);
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        moveCursor(-step, 0);
                        return true;
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        moveCursor(step, 0);
                        return true;
                    case KeyEvent.KEYCODE_DPAD_UP:
                        moveCursor(0, -step);
                        return true;
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        moveCursor(0, step);
                        return true;
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                    case KeyEvent.KEYCODE_ENTER:
                    case KeyEvent.KEYCODE_NUMPAD_ENTER:
                        clickAtCursor();
                        return true;
                    case KeyEvent.KEYCODE_PAGE_UP:
                    case KeyEvent.KEYCODE_CHANNEL_UP:
                        scrollWebPage(1f);
                        return true;
                    case KeyEvent.KEYCODE_PAGE_DOWN:
                    case KeyEvent.KEYCODE_CHANNEL_DOWN:
                        scrollWebPage(-1f);
                        return true;
                    case KeyEvent.KEYCODE_BACK:
                        setCursorMode(false);
                        return true;
                    default:
                        break;
                }
            }

            // Consume D-pad key-up events too, avoiding a second native focus click.
            if (isDpadNavigationKey(event.getKeyCode())) {
                return true;
            }

            // Keep Bluetooth/USB keyboard typing available while cursor is active.
            return geckoView.dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
        }

        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
            setCursorMode(!cursorMode);
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void moveCursor(float dx, float dy) {
        cursorOverlay.moveBy(dx, dy);
        hoverAtCursor();
    }

    private void hoverAtCursor() {
        MotionEvent hover = obtainMouseEvent(MotionEvent.ACTION_HOVER_MOVE,
                cursorOverlay.getCursorX(), cursorOverlay.getCursorY(), 0);
        session.getPanZoomController().onMotionEvent(hover);
        hover.recycle();
    }

    private void clickAtCursor() {
        cursorOverlay.showClickFeedback();
        float x = cursorOverlay.getCursorX();
        float y = cursorOverlay.getCursorY();
        long downTime = SystemClock.uptimeMillis();

        MotionEvent down = obtainMouseEvent(MotionEvent.ACTION_DOWN, x, y,
                MotionEvent.BUTTON_PRIMARY, downTime, downTime);
        session.getPanZoomController().onMouseEvent(down);

        MotionEvent press = obtainMouseEvent(MotionEvent.ACTION_BUTTON_PRESS, x, y,
                MotionEvent.BUTTON_PRIMARY, downTime, downTime + 10);
        press.setActionButton(MotionEvent.BUTTON_PRIMARY);
        session.getPanZoomController().onMouseEvent(press);

        MotionEvent release = obtainMouseEvent(MotionEvent.ACTION_BUTTON_RELEASE, x, y,
                0, downTime, downTime + 55);
        release.setActionButton(MotionEvent.BUTTON_PRIMARY);
        session.getPanZoomController().onMouseEvent(release);

        MotionEvent up = obtainMouseEvent(MotionEvent.ACTION_UP, x, y,
                0, downTime, downTime + 60);
        session.getPanZoomController().onMouseEvent(up);

        down.recycle();
        press.recycle();
        release.recycle();
        up.recycle();
    }

    private void scrollWebPage(float amount) {
        MotionEvent event = obtainMouseScrollEvent(
                cursorOverlay.getCursorX(), cursorOverlay.getCursorY(), amount);
        session.getPanZoomController().onMotionEvent(event);
        event.recycle();
    }

    @NonNull
    private MotionEvent obtainMouseScrollEvent(float x, float y, float amount) {
        long now = SystemClock.uptimeMillis();
        MotionEvent.PointerProperties properties = new MotionEvent.PointerProperties();
        properties.id = 0;
        properties.toolType = MotionEvent.TOOL_TYPE_MOUSE;

        MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
        coords.x = x;
        coords.y = y;
        coords.setAxisValue(MotionEvent.AXIS_VSCROLL, amount);

        return MotionEvent.obtain(
                now,
                now,
                MotionEvent.ACTION_SCROLL,
                1,
                new MotionEvent.PointerProperties[]{properties},
                new MotionEvent.PointerCoords[]{coords},
                0,
                0,
                1f,
                1f,
                0,
                0,
                InputDevice.SOURCE_MOUSE,
                0);
    }

    @NonNull
    private MotionEvent obtainMouseEvent(int action, float x, float y, int buttonState) {
        long now = SystemClock.uptimeMillis();
        return obtainMouseEvent(action, x, y, buttonState, now, now);
    }

    @NonNull
    private MotionEvent obtainMouseEvent(int action, float x, float y, int buttonState,
                                         long downTime, long eventTime) {
        MotionEvent.PointerProperties properties = new MotionEvent.PointerProperties();
        properties.id = 0;
        properties.toolType = MotionEvent.TOOL_TYPE_MOUSE;

        MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
        coords.x = x;
        coords.y = y;
        coords.pressure = buttonState == 0 ? 0f : 1f;
        coords.size = 1f;

        return MotionEvent.obtain(
                downTime,
                eventTime,
                action,
                1,
                new MotionEvent.PointerProperties[]{properties},
                new MotionEvent.PointerCoords[]{coords},
                0,
                buttonState,
                1f,
                1f,
                0,
                0,
                InputDevice.SOURCE_MOUSE,
                0);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        prepareForPhysicalMouse(event);
        return super.dispatchGenericMotionEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        prepareForPhysicalMouse(event);
        return super.dispatchTouchEvent(event);
    }

    private void prepareForPhysicalMouse(@NonNull MotionEvent event) {
        if (event.isFromSource(InputDevice.SOURCE_MOUSE) ||
                (event.getPointerCount() > 0 && event.getToolType(0) == MotionEvent.TOOL_TYPE_MOUSE)) {
            if (cursorMode) {
                setCursorMode(false);
            }
        }
    }

    private boolean handleBrowserKeyboardShortcut(@NonNull KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) return false;

        boolean ctrlOrMeta = event.isCtrlPressed() || event.isMetaPressed();
        if (ctrlOrMeta && (event.getKeyCode() == KeyEvent.KEYCODE_L ||
                event.getKeyCode() == KeyEvent.KEYCODE_K)) {
            focusAddressBar();
            return true;
        }
        if (ctrlOrMeta && event.getKeyCode() == KeyEvent.KEYCODE_R) {
            if (pageLoading) session.stop(); else session.reload();
            return true;
        }
        if (event.isAltPressed() && event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {
            goBack();
            return true;
        }
        if (event.isAltPressed() && event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (canGoForward) session.goForward();
            return true;
        }

        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_F6:
            case KeyEvent.KEYCODE_SEARCH:
                focusAddressBar();
                return true;
            case KeyEvent.KEYCODE_FORWARD:
                if (canGoForward) session.goForward();
                return true;
            case KeyEvent.KEYCODE_REFRESH:
                if (pageLoading) session.stop(); else session.reload();
                return true;
            case KeyEvent.KEYCODE_EXPLORER:
                showHome();
                return true;
            default:
                return false;
        }
    }

    private void focusAddressBar() {
        if (cursorMode) setCursorMode(false);
        addressBar.requestFocus();
        addressBar.selectAll();
    }

    private boolean isDirectionalKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                keyCode == KeyEvent.KEYCODE_DPAD_DOWN;
    }

    private boolean isMediaPlaybackKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_STOP:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                return true;
            default:
                return false;
        }
    }

    private boolean isDpadNavigationKey(int keyCode) {
        return isDirectionalKey(keyCode) ||
                keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                keyCode == KeyEvent.KEYCODE_ENTER ||
                keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER ||
                keyCode == KeyEvent.KEYCODE_BACK ||
                keyCode == KeyEvent.KEYCODE_MENU ||
                keyCode == KeyEvent.KEYCODE_PAGE_UP ||
                keyCode == KeyEvent.KEYCODE_PAGE_DOWN ||
                keyCode == KeyEvent.KEYCODE_CHANNEL_UP ||
                keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN;
    }

    private void setBrowserFullScreen(boolean enabled) {
        fullScreen = enabled;
        toolbar.setVisibility(enabled ? View.GONE : View.VISIBLE);
        progressBar.setVisibility(enabled ? View.GONE : progressBar.getVisibility());
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                if (enabled) {
                    controller.hide(WindowInsets.Type.systemBars());
                    controller.setSystemBarsBehavior(
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                } else {
                    controller.show(WindowInsets.Type.systemBars());
                }
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(enabled
                    ? View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    : View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    private void goBack() {
        if (fullScreen) {
            session.exitFullScreen();
            return;
        }
        if (canGoBack) {
            session.goBack();
        } else if (!homePage) {
            showHome();
        } else {
            finish();
        }
    }

    @Nullable
    private String getIncomingUrl(@Nullable Intent intent) {
        if (intent == null || intent.getData() == null) return null;
        String scheme = intent.getData().getScheme();
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
            return intent.getData().toString();
        }
        return null;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String incoming = getIncomingUrl(intent);
        if (incoming != null) loadWebUri(incoming);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (session != null) session.setActive(true);
    }

    @Override
    protected void onPause() {
        if (session != null) session.setActive(false);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (session != null) session.close();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        goBack();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
