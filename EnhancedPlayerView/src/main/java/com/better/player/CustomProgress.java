package com.better.player;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.google.android.material.progressindicator.LinearProgressIndicator;

public class CustomProgress extends LinearProgressIndicator {

    private progressChangesListener listener;
    private boolean isDragging = false;

    public CustomProgress(Context context) {
        super(context);
    }

    public CustomProgress(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomProgress(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN -> {
                isDragging = true;
                if (listener != null) listener.onStartTrackingTouch();
                return true;
            }
            case MotionEvent.ACTION_MOVE -> {
                int progress = calculateProgress(event.getX());
                setProgress(progress);
                isDragging = true;
                if (listener != null) listener.onProgressChanges(progress);
            }
            case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false;
                if (listener != null) listener.onStopTrackingTouch();
            }
            default -> {
                return false;
            }
        }
        return super.onTouchEvent(event);
    }

    private int calculateProgress(float x) {
        final int paddingLeft = super.getPaddingLeft();
        final int paddingRight = super.getPaddingRight();
        final int width = super.getWidth();

        final int available = width - paddingLeft - paddingRight;
        float value = x - paddingLeft;

        final float scale = value < 0 || available == 0 ? 0.0f : value > available ? 1.0f : value / (float) available;
        return (int) (scale * getMax());
    }

    public boolean isDragging() {
        return isDragging;
    }

    public void setProgressChangesListener(progressChangesListener listener) {
        this.listener = listener;
    }

    public interface progressChangesListener {
        void onProgressChanges(int progress);

        void onStartTrackingTouch();

        void onStopTrackingTouch();
    }
}
