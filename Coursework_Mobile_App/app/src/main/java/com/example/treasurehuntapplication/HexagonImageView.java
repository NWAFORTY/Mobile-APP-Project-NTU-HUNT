package com.example.treasurehuntapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * {@link AppCompatImageView} clipped to a regular hexagon, with an optional stroke border.
 */
public class HexagonImageView extends AppCompatImageView {

    private final Path hexPath = new Path();
    private float strokeWidthPx;
    private int strokeColor = Color.GRAY;
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public HexagonImageView(Context context) {
        super(context);
        init();
    }

    public HexagonImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HexagonImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setScaleType(ScaleType.CENTER_CROP);
        strokePaint.setStyle(Paint.Style.STROKE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        buildHexPath(w, h);
    }

    private void buildHexPath(int w, int h) {
        hexPath.reset();
        float cx = w / 2f;
        float cy = h / 2f;
        float inset = Math.max(0f, strokeWidthPx / 2f);
        float r = Math.min(w, h) / 2f - inset;
        if (r <= 0f) {
            return;
        }
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI / 3 * i - Math.PI / 6;
            float x = (float) (cx + r * Math.cos(angle));
            float y = (float) (cy + r * Math.sin(angle));
            if (i == 0) {
                hexPath.moveTo(x, y);
            } else {
                hexPath.lineTo(x, y);
            }
        }
        hexPath.close();
    }

    public void setStrokeWidthDp(float dp) {
        strokeWidthPx = dp * getResources().getDisplayMetrics().density;
        strokePaint.setStrokeWidth(strokeWidthPx);
        buildHexPath(getWidth(), getHeight());
        invalidate();
    }

    public void setStrokeColor(int color) {
        strokeColor = color;
        strokePaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (hexPath.isEmpty()) {
            buildHexPath(getWidth(), getHeight());
        }
        canvas.save();
        canvas.clipPath(hexPath);
        super.onDraw(canvas);
        canvas.restore();
        strokePaint.setColor(strokeColor);
        strokePaint.setStrokeWidth(strokeWidthPx);
        canvas.drawPath(hexPath, strokePaint);
    }
}
