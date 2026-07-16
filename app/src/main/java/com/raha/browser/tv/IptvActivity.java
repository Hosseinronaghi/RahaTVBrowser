package com.raha.browser.tv;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

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

    private final List<Channel> channels = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ArrayAdapter<String> adapter;
    private EditText source;
    private final ActivityResultLauncher<String[]> filePicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), this::loadFile);

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_iptv);
        FontManager.apply(this, findViewById(android.R.id.content));
        source = findViewById(R.id.iptvSource);
        ListView list = findViewById(R.id.channelList);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        list.setAdapter(adapter);
        findViewById(R.id.iptvLoadUrl).setOnClickListener(v -> loadUrl(source.getText().toString()));
        findViewById(R.id.iptvLoadFile).setOnClickListener(v -> launchFilePicker());
        findViewById(R.id.iptvBack).setOnClickListener(v -> finish());
        list.setOnItemClickListener((parent, view, position, id) -> play(channels.get(position)));
    }

    private void launchFilePicker() {
        try {
            filePicker.launch(new String[]{"application/x-mpegURL", "application/vnd.apple.mpegurl", "audio/x-mpegurl", "text/plain", "*/*"});
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.file_picker_unavailable, Toast.LENGTH_LONG).show();
        }
    }

    private void loadUrl(String value) {
        String url = value == null ? "" : value.trim();
        if (!(url.startsWith("https://") || url.startsWith("http://"))) {
            Toast.makeText(this, R.string.invalid_url, Toast.LENGTH_SHORT).show();
            return;
        }
        executor.execute(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder().followRedirects(true).followSslRedirects(true).build();
                Request request = new Request.Builder().url(url)
                        .header("User-Agent", PlayerActivity.DEFAULT_USER_AGENT)
                        .header("Accept", "*/*")
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) throw new Exception("HTTP " + response.code());
                    String text = response.body().string();
                    if (text.length() > 8_000_000) throw new Exception(getString(R.string.playlist_too_large));
                    runOnUiThread(() -> parse(text));
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.iptv_load_failed, safeMessage(e)), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void loadFile(Uri uri) {
        if (uri == null) return;
        executor.execute(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)))) {
                StringBuilder text = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null && text.length() < 8_000_000) text.append(line).append('\n');
                runOnUiThread(() -> parse(text.toString()));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.iptv_load_failed, safeMessage(e)), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void parse(String text) {
        channels.clear();
        String pendingName = null;
        String pendingGroup = "";
        Map<String,String> pendingHeaders = new HashMap<>();

        for (String raw : text.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.startsWith("#EXTINF")) {
                int comma = line.indexOf(',');
                pendingName = comma >= 0 ? line.substring(comma + 1).trim() : getString(R.string.iptv_channel);
                pendingGroup = attribute(line, "group-title");
                String ua = attribute(line, "user-agent");
                String ref = attribute(line, "http-referrer");
                if (!ua.isBlank()) pendingHeaders.put("User-Agent", ua);
                if (!ref.isBlank()) pendingHeaders.put("Referer", ref);
            } else if (line.startsWith("#EXTVLCOPT:http-user-agent=")) {
                pendingHeaders.put("User-Agent", line.substring(line.indexOf('=') + 1).trim());
            } else if (line.startsWith("#EXTVLCOPT:http-referrer=") || line.startsWith("#EXTVLCOPT:http-referer=")) {
                pendingHeaders.put("Referer", line.substring(line.indexOf('=') + 1).trim());
            } else if (!line.isEmpty() && !line.startsWith("#") && (line.startsWith("http://") || line.startsWith("https://"))) {
                ParsedUrl parsed = parsePipeHeaders(line);
                Map<String,String> headers = new HashMap<>(pendingHeaders);
                headers.putAll(parsed.headers());
                channels.add(new Channel(pendingName == null ? parsed.url() : pendingName, parsed.url(), pendingGroup, headers));
                pendingName = null;
                pendingGroup = "";
                pendingHeaders = new HashMap<>();
            }
        }

        List<String> names = new ArrayList<>();
        for (Channel channel : channels) names.add((channel.group().isBlank() ? "" : channel.group() + " • ") + channel.name());
        adapter.clear();
        adapter.addAll(names);
        adapter.notifyDataSetChanged();
        if (channels.isEmpty()) Toast.makeText(this, R.string.no_channels, Toast.LENGTH_SHORT).show();
    }

    private record ParsedUrl(String url, Map<String,String> headers) {}

    private ParsedUrl parsePipeHeaders(String raw) {
        int pipe = raw.indexOf('|');
        if (pipe < 0) return new ParsedUrl(raw, Map.of());
        String url = raw.substring(0, pipe).trim();
        Map<String,String> headers = new HashMap<>();
        String query = raw.substring(pipe + 1);
        for (String part : query.split("&")) {
            int equals = part.indexOf('=');
            if (equals <= 0) continue;
            String key = Uri.decode(part.substring(0, equals)).trim();
            String value = Uri.decode(part.substring(equals + 1)).trim();
            if (key.equalsIgnoreCase("user-agent")) headers.put("User-Agent", value);
            else if (key.equalsIgnoreCase("referer") || key.equalsIgnoreCase("referrer")) headers.put("Referer", value);
            else if (key.equalsIgnoreCase("origin")) headers.put("Origin", value);
            else headers.put(key, value);
        }
        return new ParsedUrl(url, headers);
    }

    private String attribute(String line, String key) {
        String marker = key + "=\"";
        int start = line.toLowerCase(Locale.ROOT).indexOf(marker.toLowerCase(Locale.ROOT));
        if (start < 0) return "";
        start += marker.length();
        int end = line.indexOf('"', start);
        return end > start ? line.substring(start, end) : "";
    }

    private void play(Channel channel) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_URL, channel.url());
        intent.putExtra(PlayerActivity.EXTRA_TITLE, channel.name());
        intent.putExtra(PlayerActivity.EXTRA_HEADERS_JSON, PlayerActivity.headersToJson(channel.headers()));
        intent.putExtra(PlayerActivity.EXTRA_USER_AGENT, channel.headers().getOrDefault("User-Agent", PlayerActivity.DEFAULT_USER_AGENT));
        intent.putExtra(PlayerActivity.EXTRA_REFERER, channel.headers().get("Referer"));
        intent.putExtra(PlayerActivity.EXTRA_MIME_TYPE, mimeFor(channel.url()));
        startActivity(intent);
    }

    private String mimeFor(String url) {
        String value = url.toLowerCase(Locale.ROOT);
        if (value.contains(".m3u8")) return "application/x-mpegURL";
        if (value.contains(".mpd")) return "application/dash+xml";
        if (value.contains(".ts")) return "video/mp2t";
        return "";
    }

    private String safeMessage(Exception e) {
        return e.getMessage() == null || e.getMessage().isBlank() ? e.getClass().getSimpleName() : e.getMessage();
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
