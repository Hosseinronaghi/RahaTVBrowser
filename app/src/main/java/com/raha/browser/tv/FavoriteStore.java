package com.raha.browser.tv;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Lightweight persistent favorite storage without adding another dependency. */
final class FavoriteStore {
    private static final String PREFS_NAME = "raha_tv_browser";
    private static final String KEY_FAVORITES = "favorites";
    private static final int MAX_FAVORITES = 40;

    private final SharedPreferences preferences;

    FavoriteStore(@NonNull Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    List<Favorite> getAll() {
        List<Favorite> result = new ArrayList<>();
        String raw = preferences.getString(KEY_FAVORITES, "[]");
        try {
            JSONArray array = new JSONArray(raw == null ? "[]" : raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item == null) continue;
                String url = item.optString("url", "").trim();
                if (url.isEmpty()) continue;
                String title = item.optString("title", url).trim();
                result.add(new Favorite(title.isEmpty() ? url : title, url));
            }
        } catch (Exception ignored) {
            // A malformed preference should never prevent the browser from starting.
        }
        return result;
    }

    boolean contains(@NonNull String url) {
        for (Favorite item : getAll()) {
            if (sameUrl(item.url, url)) return true;
        }
        return false;
    }

    boolean toggle(@NonNull String title, @NonNull String url) {
        List<Favorite> items = getAll();
        boolean removed = false;
        for (int i = items.size() - 1; i >= 0; i--) {
            if (sameUrl(items.get(i).url, url)) {
                items.remove(i);
                removed = true;
            }
        }

        if (!removed) {
            String safeTitle = title.trim().isEmpty() ? url : title.trim();
            items.add(0, new Favorite(safeTitle, url));
            while (items.size() > MAX_FAVORITES) {
                items.remove(items.size() - 1);
            }
        }
        save(items);
        return !removed;
    }

    private void save(@NonNull List<Favorite> items) {
        JSONArray array = new JSONArray();
        for (Favorite item : items) {
            try {
                JSONObject object = new JSONObject();
                object.put("title", item.title);
                object.put("url", item.url);
                array.put(object);
            } catch (Exception ignored) {
                // Skip only the invalid item and preserve the rest.
            }
        }
        preferences.edit().putString(KEY_FAVORITES, array.toString()).apply();
    }

    private boolean sameUrl(@NonNull String left, @NonNull String right) {
        return trimTrailingSlash(left).equalsIgnoreCase(trimTrailingSlash(right));
    }

    @NonNull
    private String trimTrailingSlash(@NonNull String value) {
        String output = value.trim();
        while (output.length() > 8 && output.endsWith("/")) {
            output = output.substring(0, output.length() - 1);
        }
        return output;
    }

    static final class Favorite {
        final String title;
        final String url;

        Favorite(@NonNull String title, @NonNull String url) {
            this.title = title;
            this.url = url;
        }
    }
}
