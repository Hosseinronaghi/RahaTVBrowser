package com.raha.browser.tv;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public final class FontManager {

    private static final String TAG = "FontManager";

    private FontManager() {
        // Utility class
    }

    public static Typeface loadVazirmatn(Context context) {
        try {
            return context.getResources().getFont(R.font.vazirmatn);
        } catch (Exception error) {
            Log.w(
                    TAG,
                    "Could not load Vazirmatn. Falling back to default font.",
                    error
            );
            return Typeface.DEFAULT;
        }
    }

    public static void applyTo(TextView textView) {
        if (textView == null) {
            return;
        }

        textView.setTypeface(loadVazirmatn(textView.getContext()));
    }

    public static void applyToTree(Context context, View rootView) {
        if (context == null || rootView == null) {
            return;
        }

        Typeface typeface = loadVazirmatn(context);
        applyTypefaceRecursively(rootView, typeface);
    }

    private static void applyTypefaceRecursively(
            View view,
            Typeface typeface
    ) {
        if (view instanceof TextView) {
            ((TextView) view).setTypeface(typeface);
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;

            for (int index = 0; index < group.getChildCount(); index++) {
                applyTypefaceRecursively(
                        group.getChildAt(index),
                        typeface
                );
            }
        }
    }
}
