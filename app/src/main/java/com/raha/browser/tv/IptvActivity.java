package com.raha.browser.tv;

import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class IptvActivity extends AppCompatActivity {
    private record Channel(String name, String url, String group, Map<String,String> headers) {}

    private static final String PREFS = "iptv_sources";
    private static final String KEY_SOURCES = "sources";
    private final List<Channel> allChannels = new ArrayList<>();
    private final List<Channel> visibleChannels = new ArrayList<>();
    private final List<String> savedSources = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ArrayAdapter<String> channelAdapter;
    private ArrayAdapter<String> sourceAdapter;
    private EditText source;
    private TextView nowPlaying;
    private ExoPlayer player;
    private final ActivityResultLauncher<String[]> filePicker = registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::loadFile);

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_iptv);
        FontManager.apply(this, findViewById(android.R.id.content));

        source = findViewById(R.id.iptvSource);
        nowPlaying = findViewById(R.id.iptvNowPlaying);
        ListView channelList = findViewById(R.id.channelList);
        ListView playlistList = findViewById(R.id.playlistList);
        EditText search = findViewById(R.id.channelSearch);
        PlayerView playerView = findViewById(R.id.iptvPlayer);

        channelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        sourceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, savedSources);
        channelList.setAdapter(channelAdapter);
        playlistList.setAdapter(sourceAdapter);
        loadSavedSources();

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        findViewById(R.id.iptvLoadUrl).setOnClickListener(v -> loadUrl(source.getText().toString(), true));
        findViewById(R.id.iptvLoadFile).setOnClickListener(v -> launchFilePicker());
        findViewById(R.id.iptvBack).setOnClickListener(v -> finish());
        playlistList.setOnItemClickListener((p,v,pos,id) -> { source.setText(savedSources.get(pos)); loadUrl(savedSources.get(pos), false); });
        playlistList.setOnItemLongClickListener((p,v,pos,id) -> { savedSources.remove(pos); saveSources(); sourceAdapter.notifyDataSetChanged(); return true; });
        channelList.setOnItemClickListener((p,v,pos,id) -> play(visibleChannels.get(pos)));
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int b,int c){ filterChannels(s.toString()); }
            public void afterTextChanged(Editable e){}
        });
    }

    private void launchFilePicker() {
        try { filePicker.launch(new String[]{"application/x-mpegURL","application/vnd.apple.mpegurl","audio/x-mpegurl","text/plain","*/*"}); }
        catch (ActivityNotFoundException | SecurityException e) { Toast.makeText(this, R.string.file_picker_unavailable, Toast.LENGTH_LONG).show(); }
    }

    private void loadSavedSources() {
        savedSources.clear();
        try {
            JSONArray a = new JSONArray(getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_SOURCES, "[]"));
            for (int i=0;i<a.length();i++) { String v=a.optString(i,""); if(!v.isBlank()) savedSources.add(v); }
        } catch (Exception ignored) {}
        sourceAdapter.notifyDataSetChanged();
    }

    private void rememberSource(String url) {
        savedSources.remove(url);
        savedSources.add(0,url);
        while(savedSources.size()>20) savedSources.remove(savedSources.size()-1);
        saveSources();
        sourceAdapter.notifyDataSetChanged();
    }

    private void saveSources() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_SOURCES, new JSONArray(savedSources).toString()).apply();
    }

    private void loadUrl(String value, boolean remember) {
        String url = value == null ? "" : value.trim();
        if (!(url.startsWith("https://") || url.startsWith("http://"))) { Toast.makeText(this, R.string.invalid_url, Toast.LENGTH_SHORT).show(); return; }
        if(remember) rememberSource(url);
        executor.execute(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder().followRedirects(true).followSslRedirects(true).build();
                Request request = new Request.Builder().url(url).header("User-Agent", PlayerActivity.DEFAULT_USER_AGENT).header("Accept", "*/*").build();
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) throw new Exception("HTTP " + response.code());
                    String text = response.body().string();
                    if (text.length() > 8_000_000) throw new Exception(getString(R.string.playlist_too_large));
                    runOnUiThread(() -> parse(text));
                }
            } catch (Exception e) { runOnUiThread(() -> Toast.makeText(this, getString(R.string.iptv_load_failed, safeMessage(e)), Toast.LENGTH_LONG).show()); }
        });
    }

    private void loadFile(Uri uri) {
        if (uri == null) return;
        executor.execute(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)))) {
                StringBuilder text = new StringBuilder(); String line;
                while ((line = reader.readLine()) != null && text.length() < 8_000_000) text.append(line).append('\n');
                runOnUiThread(() -> parse(text.toString()));
            } catch (Exception e) { runOnUiThread(() -> Toast.makeText(this, getString(R.string.iptv_load_failed, safeMessage(e)), Toast.LENGTH_LONG).show()); }
        });
    }

    private void parse(String text) {
        allChannels.clear();
        String pendingName = null, pendingGroup = "";
        Map<String,String> pendingHeaders = new HashMap<>();
        for (String raw : text.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.startsWith("#EXTINF")) {
                int comma=line.indexOf(','); pendingName=comma>=0?line.substring(comma+1).trim():getString(R.string.iptv_channel);
                pendingGroup=attribute(line,"group-title");
                String ua=attribute(line,"user-agent"), ref=attribute(line,"http-referrer");
                if(!ua.isBlank())pendingHeaders.put("User-Agent",ua); if(!ref.isBlank())pendingHeaders.put("Referer",ref);
            } else if(line.startsWith("#EXTVLCOPT:http-user-agent=")) pendingHeaders.put("User-Agent",line.substring(line.indexOf('=')+1).trim());
            else if(line.startsWith("#EXTVLCOPT:http-referrer=")||line.startsWith("#EXTVLCOPT:http-referer=")) pendingHeaders.put("Referer",line.substring(line.indexOf('=')+1).trim());
            else if(!line.isEmpty()&&!line.startsWith("#")&&(line.startsWith("http://")||line.startsWith("https://"))) {
                ParsedUrl parsed=parsePipeHeaders(line); Map<String,String> headers=new HashMap<>(pendingHeaders); headers.putAll(parsed.headers());
                allChannels.add(new Channel(pendingName==null?parsed.url():pendingName,parsed.url(),pendingGroup,headers));
                pendingName=null; pendingGroup=""; pendingHeaders=new HashMap<>();
            }
        }
        filterChannels("");
        if(allChannels.isEmpty()) Toast.makeText(this,R.string.no_channels,Toast.LENGTH_SHORT).show();
    }

    private void filterChannels(String query) {
        String q=query==null?"":query.trim().toLowerCase(Locale.ROOT);
        visibleChannels.clear();
        for(Channel c:allChannels) if(q.isEmpty()||c.name().toLowerCase(Locale.ROOT).contains(q)||c.group().toLowerCase(Locale.ROOT).contains(q)) visibleChannels.add(c);
        List<String> names=new ArrayList<>();
        for(Channel c:visibleChannels) names.add((c.group().isBlank()?"":c.group()+" • ")+c.name());
        channelAdapter.clear(); channelAdapter.addAll(names); channelAdapter.notifyDataSetChanged();
    }

    private void play(Channel channel) {
        Map<String,String> headers=new HashMap<>(channel.headers());
        String userAgent=headers.remove("User-Agent"); if(userAgent==null||userAgent.isBlank())userAgent=PlayerActivity.DEFAULT_USER_AGENT;
        DefaultHttpDataSource.Factory http=new DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true).setUserAgent(userAgent).setDefaultRequestProperties(headers);
        DefaultDataSource.Factory dataSource=new DefaultDataSource.Factory(this,http);
        DefaultMediaSourceFactory mediaSourceFactory=new DefaultMediaSourceFactory(dataSource);
        MediaItem.Builder item=new MediaItem.Builder().setUri(channel.url());
        String mime=mimeFor(channel.url()); if(!mime.isBlank())item.setMimeType(mime);
        player.setMediaSource(mediaSourceFactory.createMediaSource(item.build()));
        player.prepare(); player.play();
        nowPlaying.setText(channel.name());
    }

    private record ParsedUrl(String url, Map<String,String> headers) {}
    private ParsedUrl parsePipeHeaders(String raw) {
        int pipe=raw.indexOf('|'); if(pipe<0)return new ParsedUrl(raw,Map.of());
        String url=raw.substring(0,pipe).trim(); Map<String,String> headers=new HashMap<>();
        for(String part:raw.substring(pipe+1).split("&")){int equals=part.indexOf('=');if(equals<=0)continue;String key=Uri.decode(part.substring(0,equals)).trim(),value=Uri.decode(part.substring(equals+1)).trim();if(key.equalsIgnoreCase("user-agent"))headers.put("User-Agent",value);else if(key.equalsIgnoreCase("referer")||key.equalsIgnoreCase("referrer"))headers.put("Referer",value);else if(key.equalsIgnoreCase("origin"))headers.put("Origin",value);else headers.put(key,value);} return new ParsedUrl(url,headers);
    }
    private String attribute(String line,String key){String marker=key+"=\"";int start=line.toLowerCase(Locale.ROOT).indexOf(marker.toLowerCase(Locale.ROOT));if(start<0)return"";start+=marker.length();int end=line.indexOf('"',start);return end>start?line.substring(start,end):"";}
    private String mimeFor(String url){String v=url.toLowerCase(Locale.ROOT);if(v.contains(".m3u8"))return MimeTypes.APPLICATION_M3U8;if(v.contains(".mpd"))return MimeTypes.APPLICATION_MPD;if(v.contains(".ts"))return MimeTypes.VIDEO_MP2T;return"";}
    private String safeMessage(Exception e){return e.getMessage()==null||e.getMessage().isBlank()?e.getClass().getSimpleName():e.getMessage();}

    @Override public void onBackPressed(){finish();}
    @Override protected void onStop(){super.onStop();if(player!=null)player.pause();}
    @Override protected void onDestroy(){executor.shutdownNow();if(player!=null)player.release();super.onDestroy();}
}
