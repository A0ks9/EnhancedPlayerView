package com.better.player;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.ExoPlayer;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@UnstableApi
public class SpeedProgress extends ProgressBar {

    private final float[] Speeds = new float[]{0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f, 1.6f, 1.7f, 1.8f, 1.9f, 2f, 2.1f, 2.2f, 2.3f, 2.4f, 2.5f};
    private final int[] speeds = new int[24];
    private List<SpeedItems> RecSpeeds;
    private int max;
    private SpeedProgressListener listener;
    private SpeedRecyclerAdapter adapter;
    private Paint paint;
    private ExoPlayer player;
    private TextView textview;
    private ValueAnimator animateProgress;
    private boolean isRecyclerView = false;
    private boolean isDragging = false;

    public SpeedProgress(@NonNull Context context) {
        super(context);
        init();
    }

    public SpeedProgress(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public SpeedProgress(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isEnabled() || !isRecyclerView) {
            int action = event.getAction();
            isRecyclerView = false;
            switch (action) {
                case MotionEvent.ACTION_DOWN -> {
                    setProgress(calculateProgress(event.getX()));
                    isDragging = true;
                    if (listener != null) listener.onStartTrackingTouch();
                }

                case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    setProgress(calculateProgress(event.getX()));
                    isDragging = false;
                    if (listener != null) listener.onStopTrackingTouch();
                }

                case MotionEvent.ACTION_MOVE -> {
                    int progress = calculateProgress(event.getX());
                    setProgress(progress);
                    isDragging = true;
                    if (listener != null) listener.onProgressChanges(progress);
                }
            }
        }
        return true;
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.parseColor("#0096FF"));
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);

        setMaxSetup();
    }

    @Override
    protected synchronized void onDraw(@NonNull Canvas canvas) {
        float position = Util.constrainValue((float) getProgress() / getMax(), 0, 1) * getWidth();
        canvas.drawRect(0, 0, position, getHeight(), paint);
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

    public void setSpeedProgressListener(SpeedProgressListener listener) {
        this.listener = listener;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public synchronized void setProgress(int progress) {
        super.setProgress(progress);
        if (findIndex(speeds, progress) != -1 && player != null) {
            float Speed = getSpeed(speeds, progress);
            float ActualSpeed = player.getPlaybackParameters().speed;
            if (Speed != ActualSpeed) {
                textview.setText(Speed + "x");
                player.setPlaybackSpeed(Speed);
                if (adapter != null && !isRecyclerView)
                    updateRecyclerViewData(getSpeedItem(RecSpeeds, Speed));
            }
        }
        invalidate();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public synchronized void setProgress(int progress, boolean fromUser) {
        super.setProgress(progress, fromUser);
        if (findIndex(speeds, progress) != -1 && player != null) {
            float Speed = getSpeed(speeds, progress);
            float ActualSpeed = player.getPlaybackParameters().speed;
            if (Speed != ActualSpeed) {
                textview.setText(Speed + "x");
                player.setPlaybackSpeed(Speed);
                if (adapter != null && !isRecyclerView)
                    updateRecyclerViewData(getSpeedItem(RecSpeeds, Speed));
            }
        }
        invalidate();
    }

    private int getSpeedItem(List<SpeedItems> items, Object value) {
        int position = -1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Optional<SpeedItems> foundItem = RecSpeeds.stream().filter(item -> item.getSpeed() == (float) value).findFirst();
            if (foundItem.isPresent()) position = items.indexOf(foundItem.get());
        } else position = RecSpeeds.indexOf(new SpeedItems((float) value));
        return position;
    }

    private void updateRecyclerViewData(int index) {
        int ind = adapter.getIndexOfChosenSpeed();
        if (index != -1) {
            if (ind != -1) {
                if (ind != index) {
                    adapter.setChosen(ind, 0);
                    adapter.setChosen(index, 1);
                }
            } else adapter.setChosen(index, 1);
        } else if (ind != -1) adapter.setChosen(ind, 0);
    }

    private int findIndex(int[] arr, int val) {
        if (arr == null) return -1;

        int index = -1;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            index = IntStream.range(0, arr.length).filter(i -> arr[i] <= val && (i == arr.length - 1 || arr[i + 1] > val)).findFirst().orElse(-1);
        } else {
            for (int i = 0; i < arr.length; i++)
                if (arr[i] <= val && (i == arr.length - 1 || arr[i + 1] > val)) index = i;
        }
        return index;
    }

    @Override
    public synchronized void setMax(int max) {
        super.setMax(max);
        this.max = max;
        invalidate();
    }

    public void setMaxSetup() {
        int miniPer = (int) (((max / 24.0) / max) * 100.0);
        for (int i = 0; i < 24; i++)
            speeds[i] = (i == 0) ? miniPer : speeds[i - 1] + miniPer;
    }

    public void SetupProgress(ExoPlayer player, TextView textview) {
        this.textview = textview;
        this.player = player;

        setProgress(speeds[Arrays.binarySearch(Speeds, player.getPlaybackParameters().speed)]);
    }

    public void setSpeedRecAdapter(SpeedRecyclerAdapter adapter) {
        this.adapter = adapter;
        RecSpeeds = adapter.getSpeedsData();
    }

    private float getSpeed(int[] arr, int val) {
        return Speeds[findIndex(arr, val)];
    }

    public boolean isDragging() {
        return isDragging;
    }

    public void animateSpeedProgress(float targetSpeed) {
        if (animateProgress != null && animateProgress.isRunning()) animateProgress.cancel();
        isRecyclerView = true;

        int index = Arrays.binarySearch(Speeds, targetSpeed);
        animateProgress = ValueAnimator.ofInt(getProgress(), speeds[index]);
        animateProgress.addUpdateListener((animation) -> setProgress((int) animation.getAnimatedValue()));
        animateProgress.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                isRecyclerView = false;
            }
        });
        animateProgress.setDuration(800);
        animateProgress.start();
    }

    public interface SpeedProgressListener {
        void onProgressChanges(int progress);

        void onStartTrackingTouch();

        void onStopTrackingTouch();
    }
}
