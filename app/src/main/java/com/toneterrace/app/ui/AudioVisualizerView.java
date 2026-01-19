package com.toneterrace.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class AudioVisualizerView extends View {

    private byte[] mBytes;
    private float[] mPoints;
    private Paint mPaint = new Paint();
    private int mSpectrumCount = 60; // How many bars to show

    public AudioVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint.setStrokeWidth(8f);
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.parseColor("#03DAC5")); // Teal color
        mPaint.setStyle(Paint.Style.FILL);
    }

    public void updateVisualizer(byte[] bytes) {
        mBytes = bytes;
        invalidate(); // Request a redraw
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mBytes == null) {
            return;
        }

        if (mPoints == null || mPoints.length < mBytes.length * 4) {
            mPoints = new float[mBytes.length * 4];
        }

        int width = getWidth();
        int height = getHeight();

        // Simple visualization: Draw vertical bars based on waveform data
        // The Visualizer API returns bytes (-128 to 127). We normalize them to fit height.

        int barWidth = width / mSpectrumCount;

        for (int i = 0; i < mSpectrumCount; i++) {
            // Pick a data point from the byte array (skip some to fit the count)
            int index = (i * mBytes.length) / mSpectrumCount;
            byte value = mBytes[index];

            // Convert byte to height (0 to 255 -> 0 to height)
            float barHeight = ((float) (value + 128) / 256f) * height;

            float left = i * barWidth;
            float top = (height - barHeight) / 2; // Center vertically
            float right = left + (barWidth - 2); // -2 for gap
            float bottom = top + barHeight;

            canvas.drawRect(left, top, right, bottom, mPaint);
        }
    }
}