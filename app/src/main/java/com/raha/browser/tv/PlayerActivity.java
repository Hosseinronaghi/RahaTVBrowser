package com.raha.browser.tv;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.PlayerView;

import java.util.HashMap;
import java.util.Map;

public class PlayerActivity extends AppCompatActivity {
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_REFERER = "referer";
    public static final String EXTRA_USER_AGENT = "user_agent";
    public static final String EXTRA_COOKIE = "cookie";
    public static final String EXTRA_MAX_HEIGHT = "max_height";

    private ExoPlayer player;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String url = getIntent().getStringExtra(EXTRA_URL);
        if (url == null || url.isBlank()) { finish(); return; }
        if (isImage(url)) { showImage(url); return; }

        setContentView(R.layout.activity_player);
        PlayerView view = findViewById(R.id.playerView);
        int maxHeight = getIntent().getIntExtra(EXTRA_MAX_HEIGHT, 1080);

        DefaultTrackSelector trackSelector = new DefaultTrackSelector(this);
        DefaultTrackSelector.Parameters.Builder params = trackSelector.buildUponParameters()
                .setAllowVideoMixedMimeTypeAdaptiveness(true)
                .setAllowVideoNonSeamlessAdaptiveness(true)
                .setExceedRendererCapabilitiesIfNecessary(false);
        if (maxHeight > 0) params.setMaxVideoSize(Integer.MAX_VALUE, maxHeight);
        trackSelector.setParameters(params);

        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(20_000, 90_000, 1_500, 4_000)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build();

        DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15_000)
                .setReadTimeoutMs(30_000)
                .setUserAgent(value(EXTRA_USER_AGENT, "RahaTVBrowser/0.5"));

        Map<String, String> headers = new HashMap<>();
        put(headers, "Referer", getIntent().getStringExtra(EXTRA_REFERER));
        put(headers, "Cookie", getIntent().getStringExtra(EXTRA_COOKIE));
        headers.put("Accept", "*/*");
        http.setDefaultRequestProperties(headers);

        DefaultDataSource.Factory data = new DefaultDataSource.Factory(this, http);
        player = new ExoPlayer.Builder(this)
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .setMediaSourceFactory(new androidx.media3.exoplayer.source.DefaultMediaSourceFactory(data))
                .build();
        view.setPlayer(player);
        view.setUseController(true);

        MediaItem.Builder item = new MediaItem.Builder().setUri(Uri.parse(url));
        String mime = inferMime(url);
        if (mime != null) item.setMimeType(mime);
        player.setMediaItem(item.build());
        player.prepare();
        player.setPlayWhenReady(true);
        player.addListener(new Player.Listener() {
            @Override public void onPlayerError(androidx.media3.common.PlaybackException error) {
                Toast.makeText(PlayerActivity.this, error.getErrorCodeName(), Toast.LENGTH_LONG).show();
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

    private String inferMime(String url) {
        String u = url.toLowerCase();
        if (u.contains(".m3u8")) return MimeTypes.APPLICATION_M3U8;
        if (u.contains(".mpd")) return MimeTypes.APPLICATION_MPD;
        if (u.contains(".mp3")) return MimeTypes.AUDIO_MPEG;
        if (u.contains(".mp4") || u.contains(".m4v")) return MimeTypes.VIDEO_MP4;
        if (u.contains(".webm")) return MimeTypes.VIDEO_WEBM;
        return null;
    }

    private boolean isImage(String url) {
        String u = url.toLowerCase();
        return u.matches(".*\\.(jpg|jpeg|png|webp|gif|bmp)(\\?.*)?$");
    }
    private String value(String key, String fallback) { String v = getIntent().getStringExtra(key); return v == null ? fallback : v; }
    private void put(Map<String,String> map, String key, String value) { if (value != null && !value.isBlank()) map.put(key, value); }

    @Override protected void onStop() {
        super.onStop();
        if (player != null) player.pause();
    }
    @Override protected void onDestroy() {
        if (player != null) { player.release(); player = null; }
        super.onDestroy();
    }
}
