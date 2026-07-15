package com.raha.browser.tv;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

final class BrowserStore {
    record Entry(String title, String url) {}
    private final SharedPreferences prefs;
    BrowserStore(Context context) { prefs = context.getSharedPreferences("browser_store", Context.MODE_PRIVATE); }
    List<Entry> favorites() { return read("favorites", 60); }
    List<Entry> history() { return read("history", 10); }
    boolean isFavorite(String url) { for (Entry e : favorites()) if (e.url().equals(url)) return true; return false; }
    void toggleFavorite(String title, String url) {
        if (!isPublicWebUrl(url)) return;
        List<Entry> list = new ArrayList<>(favorites());
        boolean removed = list.removeIf(e -> e.url().equals(url));
        if (!removed) list.add(0, new Entry(cleanTitle(title, url), url));
        write("favorites", list, 60);
    }
    void addHistory(String title, String url) {
        if (!isPublicWebUrl(url)) return;
        List<Entry> list = new ArrayList<>(history());
        list.removeIf(e -> e.url().equals(url));
        list.add(0, new Entry(cleanTitle(title, url), url));
        write("history", list, 10);
    }
    void clearHistory() { prefs.edit().remove("history").apply(); }
    private boolean isPublicWebUrl(String url) { return url != null && (url.startsWith("https://") || url.startsWith("http://")); }
    private List<Entry> read(String key, int max) {
        List<Entry> out = new ArrayList<>();
        try { JSONArray a = new JSONArray(prefs.getString(key, "[]")); for (int i=0;i<Math.min(a.length(),max);i++){ JSONObject o=a.getJSONObject(i); out.add(new Entry(o.optString("title"),o.optString("url"))); }} catch(Exception ignored){}
        return out;
    }
    private void write(String key,List<Entry> list,int max){ JSONArray a=new JSONArray(); try{ for(int i=0;i<Math.min(list.size(),max);i++){ JSONObject o=new JSONObject();o.put("title",list.get(i).title());o.put("url",list.get(i).url());a.put(o);} }catch(Exception ignored){} prefs.edit().putString(key,a.toString()).apply(); }
    private String cleanTitle(String title,String url){ return title==null||title.isBlank()?url:title; }
}
