package com.raha.browser.tv;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
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
            Log.w(TAG, "Could not load Vazirmatn. Falling back to default font.", error);
            return Typeface.DEFAULT;
        }
    }

    public static void applyTo(TextView textView) {
        if (textView != null) {
            textView.setTypeface(loadVazirmatn(textView.getContext()));
        }
    }
}
