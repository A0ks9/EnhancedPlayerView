package com.better.player;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.DefaultTimeBar;
import androidx.media3.ui.TimeBar;

@UnstableApi
public class CustomDefaultTimeBar extends DefaultTimeBar {

    private final AnimatorSet animatorSet = new AnimatorSet();
    private final Paint[] paints = new Paint[3];
    private int originalHeight;
    private int expandedHeight;
    private int PH;
    private int OPH;
    private boolean maxed = false;
    private boolean touched = false;
    private boolean isDragging = false;
    private long maxProgress;
    private ExoPlayer player;
    private ProgressChangeListener listener;

    public CustomDefaultTimeBar(Context context) {
        super(context);
        init();
    }

    public CustomDefaultTimeBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomDefaultTimeBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        addListener(new OnScrubListener() {
            @Override
            public void onScrubStart(@NonNull TimeBar timeBar, long position) {
                touched = true;
                isDragging = true;
                animateSizeChange(expandedHeight, null);
                if (listener != null) listener.onStartTrackingTouch(timeBar, position);
            }

            @Override
            public void onScrubMove(@NonNull TimeBar timeBar, long position) {
                isDragging = true;
                if (listener != null) listener.onProgressChanges(timeBar, position);
            }

            @Override
            public void onScrubStop(@NonNull TimeBar timeBar, long position, boolean canceled) {
                touched = false;
                isDragging = false;
                animateSizeChange(originalHeight, null);
                if (listener != null) listener.onStopTrackingTouch(timeBar, position, canceled);
            }
        });

        paints[0] = createPaint(Color.parseColor("#F0FFFF"));
        paints[1] = createPaint(Color.parseColor("#0096FF"));
        paints[2] = createPaint(Color.parseColor("#A7C7E7"));
    }

    private Paint createPaint(int color) {
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        return paint;
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        if (oldWidth == 0 && oldHeight == 0) {
            originalHeight = height;
            PH = pxToDp(30);
            OPH = PH * getHeight() / originalHeight;

            expandedHeight = (int) (originalHeight * 1.25);
        }
    }

    @Override
    public synchronized void onDraw(@NonNull Canvas canvas) {
        drawRoundedProgress(canvas);
    }

    private void drawRoundedProgress(Canvas canvas) {
        float radius = 360f;
        int h = PH * getHeight() / originalHeight;
        int height = h;
        if (!(h == OPH)) height = (int) ((PH * getHeight() / originalHeight) * 1.25);
        int width = getWidth();

        if (player != null) {
            long bufferedPosition = player.getBufferedPosition();
            long playedPosition = player.getCurrentPosition();
            maxProgress = player.getDuration();

            float[] positions = {Util.constrainValue((float) bufferedPosition / maxProgress, 0, 1) * width, Util.constrainValue((float) playedPosition / maxProgress, 0, 1) * width};

            int startY = (getHeight() - height) / 2;

            canvas.drawRoundRect(0, startY, width, startY + height, radius, radius, paints[0]);
            if (!maxed && !touched)
                canvas.drawRoundRect(0, startY, positions[0], startY + height, radius, radius, paints[2]);
            canvas.drawRoundRect(0, startY, positions[1], startY + height, radius, radius, paints[1]);
        }
    }
    
    public void setDefaultTimeBar(DefaultTimeBar defaultTimeBar) {
        
    }

    private int pxToDp(int px) {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        return Math.round(px / ((float) displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    private void animateSizeChange(int targetHeight, Runnable endAction) {
        if (animatorSet.isRunning()) animatorSet.cancel();

        ValueAnimator animators = createAnimator(getHeight(), targetHeight);

        animatorSet.playTogether(animators);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.setDuration(250);
        if (endAction != null) {
            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    endAction.run();
                }
            });
        }
        animatorSet.start();
    }

    private ValueAnimator createAnimator(int start, int end) {
        ValueAnimator animator = ValueAnimator.ofInt(start, end);
        animator.addUpdateListener(animation -> {
            ViewGroup.LayoutParams layoutParams = getLayoutParams();
            layoutParams.height = (int) animation.getAnimatedValue();
            setLayoutParams(layoutParams);
            requestLayout();
            invalidate();
        });
        return animator;
    }

    public void setPlayer(ExoPlayer player) {
        this.player = player;
    }

    @Override
    public synchronized void setDuration(long duration) {
        super.setDuration(duration);
        maxProgress = duration;
    }

    @Override
    public synchronized void setBufferedPosition(long position) {
        super.setBufferedPosition(position);
        if (position != player.getDuration()) invalidate();
        else maxed = true;
    }

    @Override
    public synchronized void setPosition(long position) {
        super.setPosition(position);
        if (listener != null) listener.onProgressChanged(position);
        invalidate();
    }

    public boolean isDragging() {
        return isDragging;
    }

    public void setProgressChangeListener(ProgressChangeListener listener) {
        this.listener = listener;
    }

    public interface ProgressChangeListener {
        void onProgressChanged(long position);

        void onProgressChanges(TimeBar timebar, long progress);

        void onStartTrackingTouch(TimeBar timebar, long position);

        void onStopTrackingTouch(TimeBar timebar, long position, boolean canceled);
    }
}
