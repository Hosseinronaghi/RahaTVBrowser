package com.raha.browser.tv;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

final class FontManager {
    private FontManager() {}

    static void apply(Context context, View root) {
        Typeface typeface = load(context);
        if (typeface != null) applyRecursive(root, typeface);
    }

    static Typeface vazirmatn(Context context) { return load(context); }

    private static Typeface load(Context context) {
        try {
            int id = context.getResources().getIdentifier("vazirmatn", "font", context.getPackageName());
            if (id != 0) {
                Typeface font = ResourcesCompat.getFont(context, id);
                if (font != null) return font;
            }
        } catch (Exception ignored) {}
        try {
            return Typeface.createFromAsset(context.getAssets(), "vazirmatn.ttf");
        } catch (Exception ignored) {
            try { return Typeface.createFromAsset(context.getAssets(), "fonts/vazirmatn.ttf"); }
            catch (Exception ignoredAgain) { return null; }
        }
    }

    private static void applyRecursive(View view, Typeface typeface) {
        if (view instanceof TextView textView) textView.setTypeface(typeface);
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) applyRecursive(group.getChildAt(i), typeface);
        }
    }
}
