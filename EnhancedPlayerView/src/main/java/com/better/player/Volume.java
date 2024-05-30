package com.better.player;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.AudioManager;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

public class Volume extends AppCompatImageView {

    private static final int NUM_CIRCLES = 3;
    private final Paint[] paints = new Paint[NUM_CIRCLES];
    private final ValueAnimator[] animator = new ValueAnimator[NUM_CIRCLES];
    private final ValueAnimator[] valueAnimator = new ValueAnimator[2];
    private final double[] volumes = new double[5];
    private final boolean[] changedColor = new boolean[NUM_CIRCLES];
    private Paint bitmapPaint;
    private Bitmap bitmap;
    private boolean drawCircles = true;
    private boolean initializing = true;
    private double currentVolume;
    private double maxVolume;
    private double volumePercentage;
    private int paddingTop;
    private int paddingBottom;
    private int bitmapTop;
    private int bitmapStart;
    private int bitmapWidth;
    private int bitmapHeight;
    private int x = 0;

    public Volume(Context context) {
        super(context);
        init();
    }

    public Volume(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Volume(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        for (int i = 0; i < NUM_CIRCLES; i++)
            paints[i] = createPaint();

        bitmapPaint = new Paint();
        bitmapPaint.setColor(Color.BLACK);
        bitmapPaint.setAlpha((int) (255 * 0.7f));

        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        int streamType = AudioManager.STREAM_MUSIC;
        maxVolume = audioManager.getStreamMaxVolume(streamType);
        currentVolume = audioManager.getStreamVolume(streamType);

        setMaxVolume();
        setVolume(currentVolume);
    }

    private Paint createPaint() {
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAntiAlias(true);
        return paint;
    }

    @SuppressLint({"DrawAllocation", "UseCompatLoadingForDrawables"})
    @Override
    protected void onDraw(Canvas canvas) {
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.volu);
            bitmap = Bitmap.createScaledBitmap(bitmap, bitmapWidth, bitmapHeight, true);
        }

        if (drawCircles) {
            canvas.drawBitmap(bitmap, x, bitmapTop, bitmapPaint);

            if (x == 0) {
                int availableWidth = getWidth() - bitmapWidth;
                int circleSpaceY = bitmap.getHeight() / 4;
                int circleRadius = availableWidth / NUM_CIRCLES;

                int circle1LeftX = bitmapWidth - circleRadius;
                int circle1RightX = circle1LeftX + circleRadius;
                int circle1TopY = (circleSpaceY * 2) + paddingTop;
                int circle1BottomY = getHeight() - (paddingBottom + (circleSpaceY * 2));

                RectF rectF1 = new RectF(circle1LeftX, circle1TopY, circle1RightX, circle1BottomY);
                canvas.drawArc(rectF1, -90f, 180f, false, paints[0]);

                int circle2RightX = circle1RightX + circleRadius;
                int circle2TopY = (int) (circleSpaceY * 1.4) + paddingTop;
                int circle2BottomY = getHeight() - (paddingTop + (int) (circleSpaceY * 1.4));

                RectF rectF2 = new RectF(circle1RightX, circle2TopY, circle2RightX, circle2BottomY);
                canvas.drawArc(rectF2, -90f, 180f, false, paints[1]);

                int circle3RightX = circle2RightX + circleRadius;
                int circle3TopY = (int) (circleSpaceY * 0.8) + paddingTop;
                int circle3BottomY = getHeight() - (paddingTop + (int) (circleSpaceY * 0.8));

                RectF rectF3 = new RectF(circle2RightX, circle3TopY, circle3RightX, circle3BottomY);
                canvas.drawArc(rectF3, -90f, 180f, false, paints[2]);
            }
        } else {
            canvas.drawBitmap(bitmap, x, bitmapTop, bitmapPaint);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        int paddingLeft = getPaddingStart();
        int paddingRight = getPaddingEnd();
        paddingTop = getPaddingTop();
        paddingBottom = getPaddingBottom();

        bitmapWidth = (int) ((getWidth() - paddingLeft - paddingRight) / 1.4);
        bitmapHeight = (int) ((getHeight() - paddingTop - paddingBottom) / 1.4);
        bitmapTop = (getHeight() - bitmapHeight) / 2;
        bitmapStart = (getWidth() - bitmapWidth) / 2;
    }

    public void setVolume(double volume) {
        currentVolume = volume;
        volumePercentage = (volume / maxVolume) * 100.0;
        drawCircles = volume != 0;
        if (drawCircles) {
            if (initializing) {
                initializeCircles();
                invalidate();
                initializing = false;
            } else {
                if (x != 0) {
                    if (valueAnimator[1] != null && !valueAnimator[1].isRunning())
                        AnimateBitmapX(bitmapStart, 0);
                    else if (valueAnimator[1] == null) AnimateBitmapX(bitmapStart, 0);
                } else {
                    initializeCircles();
                    invalidate();
                }
            }
        } else {
            if (initializing) {
                x = bitmapStart;
                invalidate();
            } else {
                if (x == 0) {
                    if (valueAnimator[1] != null && !valueAnimator[1].isRunning())
                        AnimateBitmapX(0, bitmapStart);
                    else if (valueAnimator[1] == null) AnimateBitmapX(0, bitmapStart);
                }
            }
        }
    }

    private void AnimateBitmapX(int start, int end) {
        if (valueAnimator[1] != null) {
            if (valueAnimator[1].isRunning()) {
                valueAnimator[1].cancel();
                valueAnimator[1].setIntValues(x, end);
            } else {
                valueAnimator[1] = ValueAnimator.ofInt(start, end);
            }
        } else {
            valueAnimator[1] = ValueAnimator.ofInt(start, end);
        }

        valueAnimator[1].addUpdateListener(animation -> {
            x = (int) animation.getAnimatedValue();
            invalidate();
        });

        if (drawCircles) {
            valueAnimator[1].addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    initializeCircles();
                    invalidate();
                }
            });
        }

        valueAnimator[1].setDuration(250);
        valueAnimator[1].start();
    }

    private void initializeCircles() {
        for (int i = 0; i < NUM_CIRCLES; i++) {
            if ((i == 0 && volumePercentage < volumes[1] || i == 1 && volumePercentage < volumes[2] || i == 2 && volumePercentage < volumes[3]) && !changedColor[i])
                updatePaintAlpha(i, 0.5f);
            else if ((i == 0 && volumePercentage >= volumes[1] || i == 1 && volumePercentage >= volumes[2] || i == 2 && volumePercentage >= volumes[3]) && changedColor[i])
                updatePaintAlpha(i, 0.7f);
        }
    }

    private void updatePaintAlpha(int index, float alpha) {
        if (alpha >= 0 && alpha <= 1) {
            float startAlpha = paints[index].getAlpha() / 255f;
            if (animator[index] != null && animator[index].isRunning()) {
                animator[index].cancel();
            } else if (animator[index] == null) {
                animator[index] = ValueAnimator.ofFloat(startAlpha, alpha);
                animator[index].setDuration(700);
                animator[index].addUpdateListener(animation -> {
                    paints[index].setAlpha((int) ((float) animation.getAnimatedValue() * 255));
                    invalidate();
                });
            }
            animator[index].setFloatValues(startAlpha, alpha);
            animator[index].start();

            changedColor[index] = !changedColor[index];
        }
    }

    public void setMaxVolume() {
        double minVolumePer = ((maxVolume / 5.0) / maxVolume) * 100.0;
        for (int i = 0; i < 5; i++)
            volumes[i] = (i == 0) ? minVolumePer : volumes[i - 1] + minVolumePer;
        volumePercentage = (currentVolume / maxVolume) * 100.0;
    }
}
