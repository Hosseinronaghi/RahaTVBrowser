package com.raha.browser.tv;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

public class MainActivity extends AppCompatActivity {
    public static final String HOME_URL = "file:///android_asset/home/index.html";
    private static final String FRIENDLY_HOME = "raha://home";
    private static final int MAX_TABS = 5;
    private static final String MOBILE_UA = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36 RahaTVBrowser/0.6";
    private static final String DESKTOP_UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36 RahaTVBrowser/0.6";

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

    private final ActivityResultLauncher<Intent> voiceLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                ArrayList<String> values = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (values == null || values.isEmpty()) return;
                handleVoice(values.get(0));
            });

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
        cursor.setVisibility(View.GONE);
        cursor.setSpeed(settings.pointerSpeed());
        FontManager.apply(this, findViewById(android.R.id.content));
        applyDirection(findViewById(R.id.root));
        bindToolbar();
        updateToolbarModes();
        String initial = getIntent() != null && getIntent().getData() != null ? getIntent().getDataString() : HOME_URL;
        createTab(false, initial);
    }

    private void applySavedTheme() {
        int mode = switch (settings.theme()) {
            case "light" -> AppCompatDelegate.MODE_NIGHT_NO;
            case "system" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            default -> AppCompatDelegate.MODE_NIGHT_YES;
        };
        AppCompatDelegate.setDefaultNightMode(mode);
    }
    private void applySavedLocale() {
        String lang = settings.language();
        AppCompatDelegate.setApplicationLocales(("fa".equals(lang)||"en".equals(lang)) ? LocaleListCompat.forLanguageTags(lang) : LocaleListCompat.getEmptyLocaleList());
    }
    private void applyDirection(View root) {
        String d=settings.direction(); int dir=View.LAYOUT_DIRECTION_LOCALE;
        if("rtl".equals(d))dir=View.LAYOUT_DIRECTION_RTL; else if("ltr".equals(d))dir=View.LAYOUT_DIRECTION_LTR;
        root.setLayoutDirection(dir); addressBar.setLayoutDirection(View.LAYOUT_DIRECTION_LTR); addressBar.setTextDirection(View.TEXT_DIRECTION_LTR);
    }

    private void bindToolbar() {
        findViewById(R.id.backButton).setOnClickListener(v->goBack());
        findViewById(R.id.forwardButton).setOnClickListener(v->{WebView w=currentWeb();if(w!=null&&w.canGoForward())w.goForward();});
        findViewById(R.id.homeButton).setOnClickListener(v->showHome());
        findViewById(R.id.reloadButton).setOnClickListener(v->{WebView w=currentWeb();if(w!=null)w.reload();});
        findViewById(R.id.favoriteButton).setOnClickListener(v->toggleFavorite());
        findViewById(R.id.desktopButton).setOnClickListener(v->setDesktop(!settings.desktop()));
        findViewById(R.id.themeButton).setOnClickListener(v->toggleTheme());
        findViewById(R.id.voiceButton).setOnClickListener(v->startVoice());
        findViewById(R.id.playerButton).setOnClickListener(v->openDetectedMedia());
        findViewById(R.id.tabsButton).setOnClickListener(v->showTabsDialog());
        findViewById(R.id.privateButton).setOnClickListener(v->createTab(true,HOME_URL));
        findViewById(R.id.iptvButton).setOnClickListener(v->startActivity(new Intent(this,IptvActivity.class)));
        findViewById(R.id.filesButton).setOnClickListener(v->startActivity(new Intent(this,LocalMediaActivity.class)));
        findViewById(R.id.settingsButton).setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));
        findViewById(R.id.closeButton).setOnClickListener(v->closeCurrentTab());
        addressBar.setOnFocusChangeListener((v,has)->{if(has&&isHome(currentTab()==null?null:currentTab().url))addressBar.selectAll();});
        addressBar.setOnEditorActionListener((v,id,e)->{if(id==EditorInfo.IME_ACTION_GO||id==EditorInfo.IME_ACTION_SEARCH||(e!=null&&e.getKeyCode()==KeyEvent.KEYCODE_ENTER)){loadInput(addressBar.getText().toString());return true;}return false;});
    }

    private void createTab(boolean privateMode,String url){
        if(tabs.size()>=MAX_TABS){Toast.makeText(this,R.string.max_tabs,Toast.LENGTH_SHORT).show();return;}
        WebView web=new WebView(this); configureWebView(web,privateMode); BrowserTab tab=new BrowserTab(tabIds.getAndIncrement(),web,privateMode); tabs.add(tab); switchToTab(tabs.size()-1); web.loadUrl(url==null?HOME_URL:url);
    }

    private void configureWebView(WebView web,boolean privateMode){
        web.setBackgroundColor(Color.TRANSPARENT); web.setFocusable(true); web.setFocusableInTouchMode(true); web.setLayerType(View.LAYER_TYPE_HARDWARE,null);
        web.setOnTouchListener((v,event)->{ if ((event.getSource() & InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE || event.getToolType(0) == MotionEvent.TOOL_TYPE_MOUSE) cursor.setVisibility(View.GONE); return false; });
        WebSettings ws=web.getSettings(); ws.setJavaScriptEnabled(true); ws.setDomStorageEnabled(!privateMode); ws.setDatabaseEnabled(!privateMode); ws.setMediaPlaybackRequiresUserGesture(false); ws.setAllowFileAccess(false); ws.setAllowContentAccess(true); ws.setAllowFileAccessFromFileURLs(false); ws.setAllowUniversalAccessFromFileURLs(false); ws.setBuiltInZoomControls(true); ws.setDisplayZoomControls(false); ws.setSupportMultipleWindows(true); ws.setJavaScriptCanOpenWindowsAutomatically(true); ws.setUseWideViewPort(settings.desktop()); ws.setLoadWithOverviewMode(settings.desktop()); ws.setTextZoom(settings.desktop()?85:100); ws.setUserAgentString(settings.desktop()?DESKTOP_UA:MOBILE_UA); ws.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW); ws.setCacheMode(privateMode?WebSettings.LOAD_NO_CACHE:WebSettings.LOAD_DEFAULT); ws.setSaveFormData(!privateMode);
        CookieManager.getInstance().setAcceptCookie(true); CookieManager.getInstance().setAcceptThirdPartyCookies(web,!privateMode);
        if(WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING)) WebViewCompat.startSafeBrowsing(this,null);
        web.addJavascriptInterface(new VideoBridge(url->runOnUiThread(()->{if(isPlayableUrl(url))detectedMediaUrl=url;})),"RahaVideo");
        web.addJavascriptInterface(new HomeBridge((action,value)->runOnUiThread(()->handleHomeAction(action,value))),"RahaHome");
        web.setWebChromeClient(new WebChromeClient(){
            @Override public void onProgressChanged(WebView view,int p){progressBar.setProgress(p);progressBar.setVisibility(p>=100?View.GONE:View.VISIBLE);}
            @Override public boolean onCreateWindow(WebView view,boolean dialog,boolean gesture,android.os.Message resultMsg){if(tabs.size()>=MAX_TABS)return false;WebView popup=new WebView(MainActivity.this);configureWebView(popup,privateMode);BrowserTab t=new BrowserTab(tabIds.getAndIncrement(),popup,privateMode);tabs.add(t);((WebView.WebViewTransport)resultMsg.obj).setWebView(popup);resultMsg.sendToTarget();switchToTab(tabs.size()-1);return true;}
        });
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest req){String u=req.getUrl().toString();if(isPlayableUrl(u)){detectedMediaUrl=u;openPlayer(u,req.getRequestHeaders().get("Referer"));return true;}return !(u.startsWith("http://")||u.startsWith("https://")||u.startsWith("file:///android_asset/"));}
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest req){String u=req.getUrl().toString();if(isPlayableUrl(u))detectedMediaUrl=u;return super.shouldInterceptRequest(view,req);}
            @Override public void onPageFinished(WebView view,String url){BrowserTab t=findTab(view);if(t==null)return;t.url=url;t.title=isHome(url)?getString(R.string.home_title):(view.getTitle()==null||view.getTitle().isBlank()?shortUrl(url):view.getTitle());if(t==currentTab()){addressBar.setText(isHome(url)?FRIENDLY_HOME:url);updateFavoriteIcon();}if(!t.privateMode&&!isHome(url))store.addHistory(t.title,url);if(isHome(url))injectHome(view);else { injectViewportMode(view); injectVideoDetector(view); }}
        });
        web.setOnGenericMotionListener((v,event)->{if((event.getSource()&InputDevice.SOURCE_MOUSE)==InputDevice.SOURCE_MOUSE)cursor.setVisibility(View.GONE);return false;});
    }

    private BrowserTab findTab(WebView v){for(BrowserTab t:tabs)if(t.webView==v)return t;return null;}
    private void injectHome(WebView web){
        try{
            JSONObject o=new JSONObject(); o.put("lang",currentLanguageIsPersian()?"fa":"en"); o.put("dir",resolvedRtl()?"rtl":"ltr"); o.put("theme",resolvedTheme()); o.put("favorites",entriesJson(store.favorites())); o.put("history",entriesJson(store.history()));
            web.evaluateJavascript("window.RahaHomePage&&RahaHomePage.render("+o+");",null);
        }catch(Exception ignored){}
    }
    private JSONArray entriesJson(List<BrowserStore.Entry> list)throws Exception{JSONArray a=new JSONArray();for(BrowserStore.Entry e:list){JSONObject o=new JSONObject();o.put("title",e.title());o.put("url",e.url());a.put(o);}return a;}
    private void handleHomeAction(String action,String value){switch(action){case"open"->loadInput(value);case"search"->loadInput(value);case"voice"->startVoice();case"newtab"->createTab(false,HOME_URL);case"files"->startActivity(new Intent(this,LocalMediaActivity.class));case"iptv"->startActivity(new Intent(this,IptvActivity.class));case"settings"->startActivity(new Intent(this,SettingsActivity.class));}}


    private void injectViewportMode(WebView web){
        boolean desktop=settings.desktop();
        String content=desktop?"width=1280, initial-scale=1":"width=device-width, initial-scale=1, maximum-scale=5";
        String js="(function(){var m=document.querySelector('meta[name=viewport]');if(!m){m=document.createElement('meta');m.name='viewport';document.head&&document.head.appendChild(m);}if(m)m.setAttribute('content',"+JSONObject.quote(content)+");document.documentElement.style.webkitTextSizeAdjust='100%';})();";
        web.evaluateJavascript(js,null);
    }

    private void injectVideoDetector(WebView web){String js="javascript:(function(){try{function s(u){if(u&&typeof u==='string'&&!u.startsWith('blob:'))RahaVideo.onVideoFound(u);}document.querySelectorAll('video,source').forEach(v=>s(v.currentSrc||v.src));if(window.jwplayer){try{for(var i=0;i<8;i++){var j=window.jwplayer(i);if(j&&j.getPlaylistItem){var p=j.getPlaylistItem();if(p){s(p.file);(p.sources||[]).forEach(x=>s(x.file));}}}}catch(e){}}new MutationObserver(()=>document.querySelectorAll('video,source').forEach(v=>s(v.currentSrc||v.src))).observe(document.documentElement,{childList:true,subtree:true,attributes:true});}catch(e){}})();";web.evaluateJavascript(js,null);}

    private void switchToTab(int index){if(index<0||index>=tabs.size())return;BrowserTab old=currentTab();if(old!=null)old.webView.onPause();webContainer.removeAllViews();currentTabIndex=index;WebView web=tabs.get(index).webView;if(web.getParent()!=null)((ViewGroup)web.getParent()).removeView(web);webContainer.addView(web,new FrameLayout.LayoutParams(-1,-1));web.onResume();addressBar.setText(isHome(tabs.get(index).url)?FRIENDLY_HOME:tabs.get(index).url);web.requestFocus();detectedMediaUrl=null;updateFavoriteIcon();}
    @Nullable private BrowserTab currentTab(){return currentTabIndex>=0&&currentTabIndex<tabs.size()?tabs.get(currentTabIndex):null;}
    @Nullable private WebView currentWeb(){BrowserTab t=currentTab();return t==null?null:t.webView;}

    private void showTabsDialog(){
        String[] items=new String[tabs.size()+1];for(int i=0;i<tabs.size();i++){BrowserTab t=tabs.get(i);items[i]=(i==currentTabIndex?"● ":"")+(t.privateMode?getString(R.string.private_prefix)+" ":"")+t.title+"\n"+(isHome(t.url)?FRIENDLY_HOME:shortUrl(t.url));}items[tabs.size()]=getString(R.string.new_tab);
        new AlertDialog.Builder(this).setTitle(R.string.tabs).setItems(items,(d,w)->{if(w==tabs.size())createTab(false,HOME_URL);else switchToTab(w);}).setNeutralButton(R.string.close_current_tab,(d,w)->closeCurrentTab()).show();
    }
    private void closeCurrentTab(){if(tabs.isEmpty()){finish();return;}BrowserTab t=tabs.remove(currentTabIndex);t.webView.stopLoading();t.webView.loadUrl("about:blank");t.webView.clearHistory();t.webView.removeAllViews();t.webView.destroy();if(t.privateMode){CookieManager.getInstance().removeSessionCookies(null);}if(tabs.isEmpty())createTab(false,HOME_URL);else switchToTab(Math.max(0,currentTabIndex-1));}
    private void showHome(){WebView w=currentWeb();if(w!=null)w.loadUrl(HOME_URL);}
    private boolean isHome(String u){return u!=null&&(u.equals(HOME_URL)||u.startsWith(HOME_URL+"#")||u.equals(FRIENDLY_HOME));}

    private void loadInput(String input){String v=input==null?"":input.trim();if(v.isEmpty()||v.equals(FRIENDLY_HOME)){showHome();return;}String u;if(v.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*"))u=v;else if(v.contains(".")&&!v.contains(" "))u="https://"+v;else u="https://www.google.com/search?q="+URLEncoder.encode(v,StandardCharsets.UTF_8);WebView w=currentWeb();if(w!=null)w.loadUrl(u);}
    private void toggleFavorite(){BrowserTab t=currentTab();if(t==null||isHome(t.url)||t.privateMode)return;store.toggleFavorite(t.title,t.url);updateFavoriteIcon();}
    private void updateFavoriteIcon(){Button b=findViewById(R.id.favoriteButton);BrowserTab t=currentTab();b.setText(t!=null&&store.isFavorite(t.url)?"★":"☆");}
    private void setDesktop(boolean desktop){
        settings.put("desktop",desktop);
        for(BrowserTab t:tabs){
            WebSettings ws=t.webView.getSettings();
            ws.setUserAgentString(desktop?DESKTOP_UA:MOBILE_UA);
            ws.setUseWideViewPort(desktop);
            ws.setLoadWithOverviewMode(desktop);
            ws.setTextZoom(desktop?85:100);
            ws.setSupportZoom(true);
        }
        updateToolbarModes();
        Toast.makeText(this,desktop?R.string.desktop_mode_enabled:R.string.mobile_mode_enabled,Toast.LENGTH_SHORT).show();
        WebView w=currentWeb();
        if(w!=null){
            String url=w.getUrl();
            w.stopLoading();
            w.clearCache(false);
            if(url==null||url.isBlank()) url=HOME_URL;
            w.loadUrl(url);
        }
    }

    private void toggleTheme(){
        String next="dark".equals(resolvedTheme())?"light":"dark";
        settings.put("theme",next);
        AppCompatDelegate.setDefaultNightMode("light".equals(next)?AppCompatDelegate.MODE_NIGHT_NO:AppCompatDelegate.MODE_NIGHT_YES);
        recreate();
    }

    private void updateToolbarModes(){
        Button desktop=findViewById(R.id.desktopButton);
        if(desktop!=null) desktop.setText(settings.desktop()?"M":"D");
        Button theme=findViewById(R.id.themeButton);
        if(theme!=null) theme.setText("light".equals(resolvedTheme())?"☀":"☾");
    }

    private void openDetectedMedia(){if(detectedMediaUrl==null||detectedMediaUrl.isBlank()){WebView w=currentWeb();if(w!=null)injectVideoDetector(w);Toast.makeText(this,R.string.no_media_detected,Toast.LENGTH_SHORT).show();return;}openPlayer(detectedMediaUrl,currentTab()==null?null:currentTab().url);}
    private void openPlayer(String url,String referer){Intent i=new Intent(this,PlayerActivity.class);i.putExtra(PlayerActivity.EXTRA_URL,url);i.putExtra(PlayerActivity.EXTRA_REFERER,referer);i.putExtra(PlayerActivity.EXTRA_USER_AGENT,currentWeb()==null?MOBILE_UA:currentWeb().getSettings().getUserAgentString());i.putExtra(PlayerActivity.EXTRA_MAX_HEIGHT,settings.preferredHeight());String c=CookieManager.getInstance().getCookie(url);if(c!=null)i.putExtra(PlayerActivity.EXTRA_COOKIE,c);startActivity(i);}
    private boolean isPlayableUrl(String u){if(u==null)return false;String x=u.toLowerCase(Locale.ROOT);return x.contains(".m3u8")||x.contains(".mpd")||x.matches(".*\\.(mp4|m4v|webm|mkv|mp3|aac|m4a|flac|ogg|wav|ts|m2ts)(\\?.*)?$");}

    private void startVoice(){try{Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);String l=settings.voiceLanguage();if("auto".equals(l))l=currentLanguageIsPersian()?"fa-IR":"en-US";i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,l);i.putExtra(RecognizerIntent.EXTRA_PROMPT,getString(R.string.voice_prompt));voiceLauncher.launch(i);}catch(ActivityNotFoundException e){Toast.makeText(this,R.string.voice_unavailable,Toast.LENGTH_LONG).show();}}
    private void handleVoice(String phrase){if(!settings.voiceCommands()){loadInput(phrase);return;}VoiceCommandHandler.Result r=VoiceCommandHandler.parse(phrase);switch(r.action()){case SEARCH->loadInput(r.query());case HOME->showHome();case BACK->goBack();case FORWARD->{WebView w=currentWeb();if(w!=null&&w.canGoForward())w.goForward();}case RELOAD->{WebView w=currentWeb();if(w!=null)w.reload();}case NEW_TAB->createTab(false,HOME_URL);case CLOSE_TAB->closeCurrentTab();case PRIVATE->createTab(true,HOME_URL);case SETTINGS->startActivity(new Intent(this,SettingsActivity.class));case LIGHT->{settings.put("theme","light");recreate();}case DARK->{settings.put("theme","dark");recreate();}case DESKTOP->setDesktop(true);case MOBILE->setDesktop(false);case YOUTUBE->loadInput("https://www.youtube.com");case GOOGLE->loadInput("https://www.google.com");case SOUNDCLOUD->loadInput("https://soundcloud.com");case CHATGPT->loadInput("https://chatgpt.com");case IPTV->startActivity(new Intent(this,IptvActivity.class));case FILES->startActivity(new Intent(this,LocalMediaActivity.class));default->Toast.makeText(this,R.string.command_not_available,Toast.LENGTH_SHORT).show();}}

    private void goBack(){WebView w=currentWeb();if(w!=null&&w.canGoBack())w.goBack();else if(tabs.size()>1)closeCurrentTab();else new AlertDialog.Builder(this).setMessage(R.string.exit_confirm).setPositiveButton(R.string.exit,(d,x)->finish()).setNegativeButton(android.R.string.cancel,null).show();}
    @Override public void onBackPressed(){goBack();}
    @Override protected void onResume(){super.onResume();if(settings!=null){cursor.setSpeed(settings.pointerSpeed());applySavedTheme();updateToolbarModes();}WebView w=currentWeb();if(w!=null&&isHome(w.getUrl()))w.reload();}
    @Override protected void onDestroy(){for(BrowserTab t:tabs){t.webView.stopLoading();t.webView.destroy();}super.onDestroy();}
    @Override public boolean dispatchKeyEvent(KeyEvent e){
        if(e.getAction()==KeyEvent.ACTION_DOWN){
            if(e.isCtrlPressed()&&(e.getKeyCode()==KeyEvent.KEYCODE_L||e.getKeyCode()==KeyEvent.KEYCODE_K)){addressBar.requestFocus();addressBar.selectAll();return true;}
            if(e.isCtrlPressed()&&e.getKeyCode()==KeyEvent.KEYCODE_R){WebView w=currentWeb();if(w!=null)w.reload();return true;}
            if(e.isAltPressed()&&e.getKeyCode()==KeyEvent.KEYCODE_DPAD_LEFT){goBack();return true;}
            if(e.getKeyCode()==KeyEvent.KEYCODE_SEARCH){startVoice();return true;}
            View focused=getCurrentFocus();
            boolean webFocused=focused instanceof WebView || cursor.getVisibility()==View.VISIBLE;
            if(webFocused){float step=cursor.speed();WebView w=currentWeb();switch(e.getKeyCode()){
                case KeyEvent.KEYCODE_PAGE_UP,KeyEvent.KEYCODE_CHANNEL_UP -> {if(w!=null)w.scrollBy(0,-360);return true;}
                case KeyEvent.KEYCODE_PAGE_DOWN,KeyEvent.KEYCODE_CHANNEL_DOWN -> {if(w!=null)w.scrollBy(0,360);return true;}
                case KeyEvent.KEYCODE_DPAD_LEFT-> {cursor.setVisibility(View.VISIBLE);cursor.move(-step,0);return true;}
                case KeyEvent.KEYCODE_DPAD_RIGHT-> {cursor.setVisibility(View.VISIBLE);cursor.move(step,0);return true;}
                case KeyEvent.KEYCODE_DPAD_UP-> {cursor.setVisibility(View.VISIBLE);if(cursor.getCursorY()<95&&w!=null)w.scrollBy(0,-220);else cursor.move(0,-step);return true;}
                case KeyEvent.KEYCODE_DPAD_DOWN-> {cursor.setVisibility(View.VISIBLE);if(cursor.getCursorY()>cursor.getHeight()-80&&w!=null)w.scrollBy(0,220);else cursor.move(0,step);return true;}
                case KeyEvent.KEYCODE_DPAD_CENTER,KeyEvent.KEYCODE_ENTER-> {clickCursor();return true;}
            }}
        }
        return super.dispatchKeyEvent(e);
    }
    @Override public boolean dispatchGenericMotionEvent(MotionEvent event){ if((event.getSource()&InputDevice.SOURCE_MOUSE)==InputDevice.SOURCE_MOUSE) cursor.setVisibility(View.GONE); return super.dispatchGenericMotionEvent(event);}
    private void clickCursor(){WebView w=currentWeb();if(w==null)return;cursor.showClickFeedback();long now=android.os.SystemClock.uptimeMillis();float x=cursor.getCursorX();float y=cursor.getCursorY()-findViewById(R.id.toolbar).getHeight();MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,x,y,0);MotionEvent up=MotionEvent.obtain(now,now+55,MotionEvent.ACTION_UP,x,y,0);w.dispatchTouchEvent(down);w.dispatchTouchEvent(up);down.recycle();up.recycle();}
    private boolean currentLanguageIsPersian(){return getResources().getConfiguration().getLocales().get(0).getLanguage().equals("fa");}
    private boolean resolvedRtl(){String d=settings.direction();if("rtl".equals(d))return true;if("ltr".equals(d))return false;return currentLanguageIsPersian();}
    private String resolvedTheme(){if(!"system".equals(settings.theme()))return settings.theme();int m=getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK;return m==android.content.res.Configuration.UI_MODE_NIGHT_YES?"dark":"light";}
    private String shortUrl(String u){try{Uri x=Uri.parse(u);String h=x.getHost();return h==null?u:h;}catch(Exception e){return u;}}
}
