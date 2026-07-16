package com.raha.browser.tv;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.CaptionStyleCompat;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.SubtitleView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
    private PlayerView playerView;
    private String mediaUrl;
    private String mediaMime;
    private final List<MediaItem.SubtitleConfiguration> externalSubtitles = new ArrayList<>();
    private float subtitleSize = 0.0533f;
    private int resizeModeIndex = 0;

    private final ActivityResultLauncher<String[]> subtitlePicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), this::attachSubtitle);

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mediaUrl = getIntent().getStringExtra(EXTRA_URL);
        if (mediaUrl == null || mediaUrl.isBlank()) { finish(); return; }
        mediaMime = getIntent().getStringExtra(EXTRA_MIME_TYPE);
        if (isImage(mediaUrl, mediaMime)) { showImage(mediaUrl); return; }

        setContentView(R.layout.activity_player);
        FontManager.apply(this, findViewById(android.R.id.content));
        playerView = findViewById(R.id.playerView);
        TextView title = findViewById(R.id.playerTitle);
        title.setText(getIntent().getStringExtra(EXTRA_TITLE));
        findViewById(R.id.playerBack).setOnClickListener(v -> finish());
        findViewById(R.id.playerSubtitle).setOnClickListener(v -> subtitlePicker.launch(new String[]{"application/x-subrip","text/vtt","application/ttml+xml","text/plain","*/*"}));
        findViewById(R.id.playerSettings).setOnClickListener(v -> showPlayerSettings());

        buildPlayer();
        applySubtitleStyle();
    }

    private void buildPlayer() {
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
        playerView.setPlayer(player);
        playerView.setUseController(true);
        playerView.setControllerAutoShow(true);
        setMediaItem(true);
        player.addListener(new Player.Listener() {
            @Override public void onPlayerError(PlaybackException error) {
                String detail = error.getErrorCodeName();
                Throwable cause = error.getCause();
                if (cause != null && cause.getMessage() != null) detail += ": " + cause.getMessage();
                Toast.makeText(PlayerActivity.this, getString(R.string.playback_failed, detail), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setMediaItem(boolean play) {
        long position = player == null ? 0 : player.getCurrentPosition();
        MediaItem.Builder builder = new MediaItem.Builder().setUri(Uri.parse(mediaUrl));
        String mime = mediaMime == null || mediaMime.isBlank() ? inferMime(mediaUrl) : normalizeMime(mediaMime);
        if (mime != null && !mime.isBlank()) builder.setMimeType(mime);
        if (!externalSubtitles.isEmpty()) builder.setSubtitleConfigurations(externalSubtitles);
        player.setMediaItem(builder.build(), position);
        player.prepare();
        if (play) player.play();
    }

    private void attachSubtitle(Uri uri) {
        if (uri == null) return;
        try { getContentResolver().takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
        String mime = getContentResolver().getType(uri);
        if (mime == null || mime.isBlank()) mime = subtitleMime(uri.toString());
        MediaItem.SubtitleConfiguration config = new MediaItem.SubtitleConfiguration.Builder(uri)
                .setMimeType(mime)
                .setLanguage(Locale.getDefault().getLanguage())
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .setLabel(getString(R.string.external_subtitle))
                .build();
        externalSubtitles.clear();
        externalSubtitles.add(config);
        setMediaItem(true);
        Toast.makeText(this, R.string.subtitle_loaded, Toast.LENGTH_SHORT).show();
    }

    private void showPlayerSettings() {
        String[] items = {getString(R.string.playback_speed), getString(R.string.aspect_ratio), getString(R.string.subtitle_size), getString(R.string.subtitle_style)};
        new AlertDialog.Builder(this).setTitle(R.string.player_settings).setItems(items, (d, which) -> {
            if (which == 0) chooseSpeed();
            else if (which == 1) chooseResizeMode();
            else if (which == 2) chooseSubtitleSize();
            else chooseSubtitleStyle();
        }).show();
    }

    private void chooseSpeed() {
        String[] labels = {"0.5×","0.75×","1×","1.25×","1.5×","2×"};
        float[] speeds = {.5f,.75f,1f,1.25f,1.5f,2f};
        new AlertDialog.Builder(this).setTitle(R.string.playback_speed).setItems(labels, (d,w) -> player.setPlaybackSpeed(speeds[w])).show();
    }

    private void chooseResizeMode() {
        String[] labels = {getString(R.string.fit), getString(R.string.fill), getString(R.string.zoom)};
        int[] modes = {androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT, androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL, androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM};
        new AlertDialog.Builder(this).setTitle(R.string.aspect_ratio).setSingleChoiceItems(labels, resizeModeIndex, (d,w) -> { resizeModeIndex=w; playerView.setResizeMode(modes[w]); d.dismiss(); }).show();
    }

    private void chooseSubtitleSize() {
        String[] labels = {getString(R.string.small), getString(R.string.medium), getString(R.string.large), getString(R.string.extra_large)};
        float[] sizes = {.042f,.0533f,.066f,.08f};
        new AlertDialog.Builder(this).setTitle(R.string.subtitle_size).setItems(labels, (d,w) -> { subtitleSize=sizes[w]; applySubtitleStyle(); }).show();
    }

    private void chooseSubtitleStyle() {
        String[] labels = {getString(R.string.subtitle_white_black), getString(R.string.subtitle_yellow_black), getString(R.string.subtitle_white_transparent)};
        new AlertDialog.Builder(this).setTitle(R.string.subtitle_style).setItems(labels, (d,w) -> applySubtitleStyle(w)).show();
    }

    private void applySubtitleStyle() { applySubtitleStyle(0); }
    private void applySubtitleStyle(int styleIndex) {
        SubtitleView subtitleView = playerView.getSubtitleView();
        if (subtitleView == null) return;
        Typeface typeface = FontManager.vazirmatn(this);
        int foreground = styleIndex == 1 ? Color.YELLOW : Color.WHITE;
        int background = styleIndex == 2 ? Color.TRANSPARENT : 0xB3000000;
        subtitleView.setApplyEmbeddedStyles(false);
        subtitleView.setApplyEmbeddedFontSizes(false);
        subtitleView.setFractionalTextSize(subtitleSize);
        subtitleView.setStyle(new CaptionStyleCompat(foreground, background, Color.TRANSPARENT, CaptionStyleCompat.EDGE_TYPE_OUTLINE, Color.BLACK, typeface));
    }

    private void showImage(String url) {
        ImageView image = new ImageView(this);
        image.setBackgroundColor(Color.BLACK);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setImageURI(Uri.parse(url));
        image.setOnClickListener(v -> finish());
        setContentView(image, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private String subtitleMime(String raw) {
        String value = raw.toLowerCase(Locale.ROOT);
        if (value.contains(".vtt")) return MimeTypes.TEXT_VTT;
        if (value.contains(".ttml") || value.contains(".xml")) return MimeTypes.APPLICATION_TTML;
        return MimeTypes.APPLICATION_SUBRIP;
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
        try { Uri uri = Uri.parse(referer); if (uri.getScheme() == null || uri.getAuthority() == null) return null; return uri.getScheme() + "://" + uri.getAuthority(); }
        catch (Exception e) { return null; }
    }

    private String value(String key, String fallback) { String v=getIntent().getStringExtra(key); return v==null||v.isBlank()?fallback:v; }
    private void put(Map<String,String> map, String key, String value) { if (value != null && !value.isBlank()) map.put(key, value); }

    private Map<String,String> parseHeaders(String json) {
        Map<String,String> headers = new HashMap<>();
        if (json == null || json.isBlank()) return headers;
        try { JSONObject object = new JSONObject(json); Iterator<String> keys = object.keys(); while (keys.hasNext()) { String key=keys.next(); String value=object.optString(key,""); if(!key.isBlank()&&!value.isBlank()) headers.put(key,value); } }
        catch (Exception ignored) {}
        return headers;
    }

    static String headersToJson(Map<String,String> headers) { return new JSONObject(headers == null ? Map.of() : headers).toString(); }

    @Override public void onBackPressed() { finish(); }
    @Override protected void onStop() { super.onStop(); if (player != null) player.pause(); }
    @Override protected void onDestroy() { if (player != null) { player.release(); player = null; } super.onDestroy(); }
}
