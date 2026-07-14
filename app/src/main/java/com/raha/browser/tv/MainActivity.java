package com.raha.browser.tv;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;

import java.net.URI;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final String HOME_URL = "https://www.google.com/";
    private static final String SEARCH_URL = "https://www.google.com/search?q=";

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

    private boolean canGoBack;
    private boolean canGoForward;
    private boolean cursorMode;
    private boolean fullScreen;
    private boolean pageLoading;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();
        configureBrowser();
        configureControls();

        String incoming = getIncomingUrl(getIntent());
        session.loadUri(incoming == null ? HOME_URL : incoming);
        geckoView.requestFocus();
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
    }

    private void configureBrowser() {
        GeckoRuntimeSettings settings = new GeckoRuntimeSettings.Builder()
                .javaScriptEnabled(true)
                .remoteDebuggingEnabled(false)
                .build();
        runtime = GeckoRuntime.create(this, settings);
        session = new GeckoSession();
        session.open(runtime);
        geckoView.setSession(session);
        geckoView.setFocusable(true);
        geckoView.setFocusableInTouchMode(true);

        session.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Override
            public void onLocationChange(@NonNull GeckoSession session,
                                         @Nullable String url,
                                         @NonNull List<GeckoSession.PermissionDelegate.ContentPermission> perms,
                                         @NonNull Boolean hasUserGesture) {
                if (url != null && !addressBar.hasFocus()) {
                    addressBar.setText(displayUrl(url));
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
                if (!success) {
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
                if (title != null && !title.isBlank()) {
                    setTitle(title);
                }
            }
        });
    }

    private void configureControls() {
        backButton.setOnClickListener(v -> goBack());
        forwardButton.setOnClickListener(v -> {
            if (canGoForward) session.goForward();
        });
        findViewById(R.id.homeButton).setOnClickListener(v -> session.loadUri(HOME_URL));
        reloadButton.setOnClickListener(v -> {
            if (pageLoading) session.stop(); else session.reload();
        });
        findViewById(R.id.goButton).setOnClickListener(v -> loadAddress());
        modeButton.setOnClickListener(v -> setCursorMode(!cursorMode));

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

    private void loadAddress() {
        String input = addressBar.getText().toString().trim();
        if (input.isEmpty()) return;
        session.loadUri(normalizeInput(input));
        addressBar.clearFocus();
        if (cursorMode) cursorOverlay.requestFocus(); else geckoView.requestFocus();
    }

    private String normalizeInput(String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://") ||
                lower.startsWith("about:") || lower.startsWith("file:")) {
            return input;
        }
        if (input.contains(" ") || !looksLikeHost(input)) {
            return SEARCH_URL + android.net.Uri.encode(input);
        }
        return "https://" + input;
    }

    private boolean looksLikeHost(String input) {
        try {
            URI uri = new URI("https://" + input);
            return uri.getHost() != null && uri.getHost().contains(".");
        } catch (Exception ignored) {
            return false;
        }
    }

    private String displayUrl(String url) {
        return url;
    }

    private void setCursorMode(boolean enabled) {
        cursorMode = enabled;
        cursorOverlay.setVisibility(enabled ? View.VISIBLE : View.GONE);
        modeButton.setText(enabled ? R.string.focus_mode : R.string.cursor_mode);
        modeButton.setContentDescription(getString(enabled ? R.string.focus_mode : R.string.cursor_mode));
        if (enabled) {
            cursorOverlay.requestFocus();
        } else {
            geckoView.requestFocus();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (handleBrowserKeyboardShortcut(event)) {
            return true;
        }

        if (cursorMode && cursorOverlay.hasFocus()) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                float step = event.getRepeatCount() > 0 ? dp(18) : dp(12);
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        cursorOverlay.moveBy(-step, 0);
                        return true;
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        cursorOverlay.moveBy(step, 0);
                        return true;
                    case KeyEvent.KEYCODE_DPAD_UP:
                        cursorOverlay.moveBy(0, -step);
                        return true;
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        cursorOverlay.moveBy(0, step);
                        return true;
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                    case KeyEvent.KEYCODE_ENTER:
                    case KeyEvent.KEYCODE_NUMPAD_ENTER:
                        clickAtCursor();
                        return true;
                    case KeyEvent.KEYCODE_BACK:
                        setCursorMode(false);
                        return true;
                    default:
                        break;
                }
            }

            // Keep hardware/Bluetooth keyboard typing available after a virtual-cursor click.
            // GeckoView owns the web page's focused input element even though the overlay owns
            // Android view focus, so key events must be forwarded explicitly.
            if (!isDpadNavigationKey(event.getKeyCode())) {
                return geckoView.dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
            }
        }

        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
            setCursorMode(!cursorMode);
            return true;
        }
        return super.dispatchKeyEvent(event);
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
            // The real system pointer is more accurate than the D-pad cursor. Hiding the overlay
            // also prevents it from sitting above GeckoView and interfering with mouse clicks.
            if (cursorMode) {
                setCursorMode(false);
            }
        }
    }

    private boolean handleBrowserKeyboardShortcut(@NonNull KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }

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
                session.loadUri(HOME_URL);
                return true;
            default:
                return false;
        }
    }

    private void focusAddressBar() {
        if (cursorMode) {
            setCursorMode(false);
        }
        addressBar.requestFocus();
        addressBar.selectAll();
    }

    private boolean isDpadNavigationKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                keyCode == KeyEvent.KEYCODE_ENTER ||
                keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER ||
                keyCode == KeyEvent.KEYCODE_BACK ||
                keyCode == KeyEvent.KEYCODE_MENU;
    }

    private void clickAtCursor() {
        long now = SystemClock.uptimeMillis();
        float x = cursorOverlay.getCursorX();
        float y = cursorOverlay.getCursorY();
        int source = InputDevice.SOURCE_TOUCHSCREEN;
        MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0);
        down.setSource(source);
        MotionEvent up = MotionEvent.obtain(now, now + 60, MotionEvent.ACTION_UP, x, y, 0);
        up.setSource(source);
        geckoView.dispatchTouchEvent(down);
        geckoView.dispatchTouchEvent(up);
        down.recycle();
        up.recycle();
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
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        goBack();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String url = getIncomingUrl(intent);
        if (url != null) session.loadUri(url);
    }

    @Nullable
    private String getIncomingUrl(@Nullable Intent intent) {
        if (intent == null || intent.getData() == null) return null;
        String scheme = intent.getData().getScheme();
        return ("http".equals(scheme) || "https".equals(scheme))
                ? intent.getData().toString()
                : null;
    }

    @Override
    protected void onDestroy() {
        if (session != null) {
            session.close();
        }
        super.onDestroy();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
