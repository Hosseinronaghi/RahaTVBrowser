package com.raha.browser.tv;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/** High-contrast D-pad mouse cursor drawn above GeckoView. */
public final class CursorOverlayView extends View {
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint darkStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lightStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint clickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float cursorX;
    private float cursorY;
    private boolean initialized;
    private boolean clickFeedback;

    public CursorOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        setFocusable(true);
        setFocusableInTouchMode(true);

        fillPaint.setColor(0xFFFFB300);
        fillPaint.setStyle(Paint.Style.FILL);

        darkStrokePaint.setColor(0xFF050505);
        darkStrokePaint.setStyle(Paint.Style.STROKE);
        darkStrokePaint.setStrokeWidth(dp(7));
        darkStrokePaint.setStrokeJoin(Paint.Join.ROUND);

        lightStrokePaint.setColor(0xFFFFFFFF);
        lightStrokePaint.setStyle(Paint.Style.STROKE);
        lightStrokePaint.setStrokeWidth(dp(3));
        lightStrokePaint.setStrokeJoin(Paint.Join.ROUND);

        clickPaint.setColor(0xD9FFFFFF);
        clickPaint.setStyle(Paint.Style.STROKE);
        clickPaint.setStrokeWidth(dp(3));
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
        float s = dp(30);
        Path pointer = new Path();
        pointer.moveTo(cursorX, cursorY);
        pointer.lineTo(cursorX, cursorY + s);
        pointer.lineTo(cursorX + s * 0.28f, cursorY + s * 0.72f);
        pointer.lineTo(cursorX + s * 0.52f, cursorY + s * 1.12f);
        pointer.lineTo(cursorX + s * 0.70f, cursorY + s * 1.02f);
        pointer.lineTo(cursorX + s * 0.47f, cursorY + s * 0.64f);
        pointer.lineTo(cursorX + s * 0.86f, cursorY + s * 0.58f);
        pointer.close();

        canvas.drawPath(pointer, darkStrokePaint);
        canvas.drawPath(pointer, fillPaint);
        canvas.drawPath(pointer, lightStrokePaint);
        if (clickFeedback) {
            canvas.drawCircle(cursorX, cursorY, dp(20), clickPaint);
        }
    }

    public void moveBy(float dx, float dy) {
        cursorX += dx;
        cursorY += dy;
        clamp();
        invalidate();
    }

    public void showClickFeedback() {
        clickFeedback = true;
        invalidate();
        removeCallbacks(clearClickFeedback);
        postDelayed(clearClickFeedback, 140);
    }

    private final Runnable clearClickFeedback = () -> {
        clickFeedback = false;
        invalidate();
    };

    public float getCursorX() {
        return cursorX;
    }

    public float getCursorY() {
        return cursorY;
    }

    private void clamp() {
        float margin = dp(6);
        cursorX = Math.max(margin, Math.min(Math.max(margin, getWidth() - dp(34)), cursorX));
        cursorY = Math.max(margin, Math.min(Math.max(margin, getHeight() - dp(38)), cursorY));
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
