package com.raha.browser.tv;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.PlayerView;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class PlayerActivity extends AppCompatActivity {
    public static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Linux; Android 13; Android TV) AppleWebKit/537.36 Chrome/124.0 Safari/537.36 RahaTVBrowser/0.6";
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_REFERER = "referer";
    public static final String EXTRA_USER_AGENT = "user_agent";
    public static final String EXTRA_COOKIE = "cookie";
    public static final String EXTRA_MAX_HEIGHT = "max_height";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_MIME_TYPE = "mime_type";
    public static final String EXTRA_HEADERS_JSON = "headers_json";

    private ExoPlayer player;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String url = getIntent().getStringExtra(EXTRA_URL);
        if (url == null || url.isBlank()) { finish(); return; }
        String explicitMime = getIntent().getStringExtra(EXTRA_MIME_TYPE);
        if (isImage(url, explicitMime)) { showImage(url); return; }

        setContentView(R.layout.activity_player);
        PlayerView view = findViewById(R.id.playerView);
        int maxHeight = getIntent().getIntExtra(EXTRA_MAX_HEIGHT, new AppSettings(this).preferredHeight());

        DefaultTrackSelector trackSelector = new DefaultTrackSelector(this);
        DefaultTrackSelector.Parameters.Builder parameters = trackSelector.buildUponParameters()
                .setAllowVideoMixedMimeTypeAdaptiveness(true)
                .setAllowVideoNonSeamlessAdaptiveness(true)
                .setExceedRendererCapabilitiesIfNecessary(true);
        if (maxHeight > 0) parameters.setMaxVideoSize(Integer.MAX_VALUE, maxHeight);
        trackSelector.setParameters(parameters);

        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(15_000, 120_000, 1_000, 3_000)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build();

        Map<String,String> headers = parseHeaders(getIntent().getStringExtra(EXTRA_HEADERS_JSON));
        put(headers, "Referer", getIntent().getStringExtra(EXTRA_REFERER));
        put(headers, "Origin", originOf(getIntent().getStringExtra(EXTRA_REFERER)));
        put(headers, "Cookie", getIntent().getStringExtra(EXTRA_COOKIE));
        headers.putIfAbsent("Accept", "*/*");
        headers.putIfAbsent("Connection", "keep-alive");

        String userAgent = value(EXTRA_USER_AGENT, headers.getOrDefault("User-Agent", DEFAULT_USER_AGENT));
        headers.remove("User-Agent");
        DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(20_000)
                .setReadTimeoutMs(60_000)
                .setUserAgent(userAgent)
                .setDefaultRequestProperties(headers);

        DefaultDataSource.Factory dataSource = new DefaultDataSource.Factory(this, http);
        player = new ExoPlayer.Builder(this)
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .setMediaSourceFactory(new androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSource))
                .build();
        view.setPlayer(player);
        view.setUseController(true);
        view.setControllerAutoShow(true);

        MediaItem.Builder mediaItem = new MediaItem.Builder().setUri(Uri.parse(url));
        String mime = explicitMime == null || explicitMime.isBlank() ? inferMime(url) : normalizeMime(explicitMime);
        if (mime != null && !mime.isBlank()) mediaItem.setMimeType(mime);
        player.setMediaItem(mediaItem.build());
        player.prepare();
        player.play();
        player.addListener(new Player.Listener() {
            @Override public void onPlayerError(PlaybackException error) {
                String detail = error.getErrorCodeName();
                Throwable cause = error.getCause();
                if (cause != null && cause.getMessage() != null) detail += ": " + cause.getMessage();
                Toast.makeText(PlayerActivity.this, getString(R.string.playback_failed, detail), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showImage(String url) {
        ImageView image = new ImageView(this);
        image.setBackgroundColor(Color.BLACK);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setImageURI(Uri.parse(url));
        setContentView(image, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private String inferMime(String rawUrl) {
        String url = rawUrl.toLowerCase(Locale.ROOT);
        if (url.contains(".m3u8")) return MimeTypes.APPLICATION_M3U8;
        if (url.contains(".mpd")) return MimeTypes.APPLICATION_MPD;
        if (url.contains(".mp3")) return MimeTypes.AUDIO_MPEG;
        if (url.contains(".mp4") || url.contains(".m4v")) return MimeTypes.VIDEO_MP4;
        if (url.contains(".webm")) return MimeTypes.VIDEO_WEBM;
        if (url.contains(".ts") || url.contains(".m2ts")) return MimeTypes.VIDEO_MP2T;
        return null;
    }

    private String normalizeMime(String mime) {
        if (mime.equalsIgnoreCase("application/x-mpegURL") || mime.equalsIgnoreCase("application/vnd.apple.mpegurl") || mime.equalsIgnoreCase("audio/x-mpegurl")) return MimeTypes.APPLICATION_M3U8;
        if (mime.equalsIgnoreCase("application/dash+xml")) return MimeTypes.APPLICATION_MPD;
        return mime;
    }

    private boolean isImage(String url, String mime) {
        if (mime != null && mime.startsWith("image/")) return true;
        return url.toLowerCase(Locale.ROOT).matches(".*\\.(jpg|jpeg|png|webp|gif|bmp)(\\?.*)?$");
    }

    private String originOf(String referer) {
        try {
            Uri uri = Uri.parse(referer);
            if (uri.getScheme() == null || uri.getAuthority() == null) return null;
            return uri.getScheme() + "://" + uri.getAuthority();
        } catch (Exception e) { return null; }
    }

    private String value(String key, String fallback) {
        String value = getIntent().getStringExtra(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private void put(Map<String,String> map, String key, String value) {
        if (value != null && !value.isBlank()) map.put(key, value);
    }

    private Map<String,String> parseHeaders(String json) {
        Map<String,String> headers = new HashMap<>();
        if (json == null || json.isBlank()) return headers;
        try {
            JSONObject object = new JSONObject(json);
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = object.optString(key, "");
                if (!key.isBlank() && !value.isBlank()) headers.put(key, value);
            }
        } catch (Exception ignored) {}
        return headers;
    }

    static String headersToJson(Map<String,String> headers) {
        return new JSONObject(headers == null ? Map.of() : headers).toString();
    }

    @Override protected void onStop() {
        super.onStop();
        if (player != null) player.pause();
    }

    @Override protected void onDestroy() {
        if (player != null) { player.release(); player = null; }
        super.onDestroy();
    }
}
