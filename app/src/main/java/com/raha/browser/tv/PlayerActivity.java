package com.raha.browser.tv;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public final class PlayerActivity extends Activity {
    public static final String EXTRA_URL = "media_url";
    private ExoPlayer player;
    private PlayerView playerView;
    private String mediaUrl;

    @Override protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_player);
        FontManager.applyToTree(this, findViewById(android.R.id.content));
        playerView = findViewById(R.id.playerView);
        mediaUrl = getIntent().getStringExtra(EXTRA_URL);
        if (mediaUrl == null || mediaUrl.trim().isEmpty()) finish();
    }

    @Override protected void onStart() {
        super.onStart();
        if (mediaUrl == null || isFinishing()) return;
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(Uri.parse(mediaUrl)));
        player.prepare();
        player.play();
        playerView.requestFocus();
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (player != null && event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    if (player.isPlaying()) player.pause(); else player.play(); return true;
                case KeyEvent.KEYCODE_MEDIA_REWIND:
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    player.seekTo(Math.max(0, player.getCurrentPosition() - 10000)); return true;
                case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    player.seekTo(player.getCurrentPosition() + 10000); return true;
                default: break;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override protected void onStop() {
        if (player != null) { playerView.setPlayer(null); player.release(); player = null; }
        super.onStop();
    }
}
