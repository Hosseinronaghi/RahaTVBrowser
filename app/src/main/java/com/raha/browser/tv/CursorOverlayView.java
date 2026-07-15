package com.raha.browser.tv;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class CursorOverlayView extends View {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float x = 420f;
    private float y = 280f;
    private boolean visible;
    private float clickPulse;
    private float speed = 28f;

    public CursorOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        fill.setColor(0xFFF7FAFC);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(3f);
        stroke.setColor(0xFF0D1B2A);
    }

    public void setSpeed(float value) { speed = Math.max(8f, Math.min(60f, value)); }
    public float speed() { return speed; }

    public void move(float dx, float dy) {
        visible = true;
        x = Math.max(8f, Math.min(getWidth() - 24f, x + dx));
        y = Math.max(64f, Math.min(getHeight() - 24f, y + dy));
        invalidate();
    }

    public void hideCursor() {
        visible = false;
        invalidate();
    }

    public void showCursor() {
        visible = true;
        invalidate();
    }

    public float getCursorX() { return x; }
    public float getCursorY() { return y; }

    public void showClickFeedback() {
        clickPulse = 1f;
        animate().setDuration(180).withEndAction(() -> {
            clickPulse = 0f;
            invalidate();
        }).start();
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!visible) return;
        Path p = new Path();
        p.moveTo(x, y);
        p.lineTo(x, y + 32);
        p.lineTo(x + 9, y + 24);
        p.lineTo(x + 17, y + 40);
        p.lineTo(x + 24, y + 36);
        p.lineTo(x + 16, y + 21);
        p.lineTo(x + 30, y + 20);
        p.close();
        canvas.drawPath(p, fill);
        canvas.drawPath(p, stroke);
        if (clickPulse > 0f) {
            canvas.drawCircle(x + 6, y + 8, 22f, stroke);
        }
    }
}
