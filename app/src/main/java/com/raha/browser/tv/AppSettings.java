package com.raha.browser.tv;

import android.content.Context;
import android.content.SharedPreferences;

final class AppSettings {
    private final SharedPreferences p;
    AppSettings(Context c) { p = c.getSharedPreferences("settings", Context.MODE_PRIVATE); }
    String language() { return p.getString("language", "auto"); }
    String direction() { return p.getString("direction", "auto"); }
    String theme() { return p.getString("theme", "dark"); }
    String voiceLanguage() { return p.getString("voice_language", "auto"); }
    boolean voiceCommands() { return p.getBoolean("voice_commands", true); }
    boolean desktop() { return p.getBoolean("desktop", false); }
    boolean restoreTabs() { return p.getBoolean("restore_tabs", true); }
    float pointerSpeed() { return p.getFloat("pointer_speed", 28f); }
    int preferredHeight() { return p.getInt("preferred_height", 0); }
    void put(String k, String v) { p.edit().putString(k, v).apply(); }
    void put(String k, boolean v) { p.edit().putBoolean(k, v).apply(); }
    void put(String k, float v) { p.edit().putFloat(k, v).apply(); }
    void put(String k, int v) { p.edit().putInt(k, v).apply(); }
}
