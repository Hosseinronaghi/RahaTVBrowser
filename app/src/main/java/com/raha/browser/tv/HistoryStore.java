package com.raha.browser.tv;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

final class HistoryStore {
    private static final String PREFS = "raha_tv_browser";
    private static final String KEY = "recent_history";
    private static final int MAX = 10;
    private final SharedPreferences prefs;

    HistoryStore(@NonNull Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    void add(@NonNull String title, @NonNull String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) return;
        List<Entry> items = getAll();
        items.removeIf(item -> normalize(item.url).equalsIgnoreCase(normalize(url)));
        items.add(0, new Entry(title.trim().isEmpty() ? url : title.trim(), url, System.currentTimeMillis()));
        while (items.size() > MAX) items.remove(items.size() - 1);
        save(items);
    }

    @NonNull List<Entry> getAll() {
        List<Entry> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs.getString(KEY, "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o == null) continue;
                String url = o.optString("url", "");
                if (url.isEmpty()) continue;
                out.add(new Entry(o.optString("title", url), url, o.optLong("time", 0L)));
            }
        } catch (Exception ignored) { }
        return out;
    }

    void clear() { prefs.edit().remove(KEY).apply(); }

    private void save(List<Entry> items) {
        JSONArray a = new JSONArray();
        for (Entry e : items) {
            try {
                JSONObject o = new JSONObject();
                o.put("title", e.title); o.put("url", e.url); o.put("time", e.time); a.put(o);
            } catch (Exception ignored) { }
        }
        prefs.edit().putString(KEY, a.toString()).apply();
    }

    private String normalize(String s) {
        String x = s.trim();
        while (x.length() > 8 && x.endsWith("/")) x = x.substring(0, x.length() - 1);
        return x;
    }

    static final class Entry {
        final String title; final String url; final long time;
        Entry(String title, String url, long time) { this.title = title; this.url = url; this.time = time; }
    }
}
