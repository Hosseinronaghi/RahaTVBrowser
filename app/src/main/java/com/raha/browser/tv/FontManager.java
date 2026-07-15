package com.raha.browser.tv;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

/** Applies the optional bundled Vazirmatn font without making the build depend on a font binary. */
public final class FontManager {
    private FontManager() {}

    @Nullable
    public static Typeface loadVazirmatn(Context context) {
        int fontId = context.getResources().getIdentifier("vazirmatn", "font", context.getPackageName());
        if (fontId == 0) return null;
        try {
            return ResourcesCompat.getFont(context, fontId);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static void applyToTree(Context context, View root) {
        Typeface typeface = loadVazirmatn(context);
        if (typeface == null || root == null) return;
        applyRecursively(root, typeface);
    }

    private static void applyRecursively(View view, Typeface typeface) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            int style = textView.getTypeface() != null ? textView.getTypeface().getStyle() : Typeface.NORMAL;
            textView.setTypeface(typeface, style);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyRecursively(group.getChildAt(i), typeface);
            }
        }
    }
}
