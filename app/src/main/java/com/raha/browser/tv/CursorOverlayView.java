package com.raha.browser.tv;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/** Draws a high-contrast virtual mouse pointer over GeckoView. */
public final class CursorOverlayView extends View {
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float cursorX;
    private float cursorY;
    private boolean initialized;

    public CursorOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        fillPaint.setColor(0xFFFFFFFF);
        fillPaint.setStyle(Paint.Style.FILL);
        strokePaint.setColor(0xFF111111);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(2));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (!initialized && w > 0 && h > 0) {
            cursorX = w / 2f;
            cursorY = h / 2f;
            initialized = true;
        }
        clamp();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float s = dp(26);
        Path pointer = new Path();
        pointer.moveTo(cursorX, cursorY);
        pointer.lineTo(cursorX, cursorY + s);
        pointer.lineTo(cursorX + s * 0.28f, cursorY + s * 0.72f);
        pointer.lineTo(cursorX + s * 0.52f, cursorY + s * 1.12f);
        pointer.lineTo(cursorX + s * 0.70f, cursorY + s * 1.02f);
        pointer.lineTo(cursorX + s * 0.47f, cursorY + s * 0.64f);
        pointer.lineTo(cursorX + s * 0.86f, cursorY + s * 0.58f);
        pointer.close();
        canvas.drawPath(pointer, fillPaint);
        canvas.drawPath(pointer, strokePaint);
    }

    public void moveBy(float dx, float dy) {
        cursorX += dx;
        cursorY += dy;
        clamp();
        invalidate();
    }

    public float getCursorX() {
        return cursorX;
    }

    public float getCursorY() {
        return cursorY;
    }

    private void clamp() {
        float margin = dp(4);
        cursorX = Math.max(margin, Math.min(Math.max(margin, getWidth() - dp(28)), cursorX));
        cursorY = Math.max(margin, Math.min(Math.max(margin, getHeight() - dp(32)), cursorY));
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
