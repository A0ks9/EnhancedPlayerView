package com.better.player;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.Player.Listener;
import androidx.media3.common.Player.PositionInfo;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.database.DatabaseProvider;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.cache.Cache;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.NoOpCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.TimeBar;
import androidx.mediarouter.media.MediaControlIntent;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@OptIn(markerClass = UnstableApi.class)
public class EnhancedPlayerView extends PlayerView implements View.OnClickListener, View.OnTouchListener, Listener, CustomDefaultTimeBar.ProgressChangeListener, View.OnLayoutChangeListener {

    private static final String ACTION_MEDIA_CONTROL = "media_control";
    private static final String EXTRA_CONTROL_TYPE = "control_type";
    private static final int REQUEST_BACK = 0;
    private static final int REQUEST_PLAYBACK = 1;
    private static final int REQUEST_FORWARD = 2;
    private static final int CONTROL_TYPE_BACK = 0;
    private static final int CONTROL_TYPE_PLAYBACK = 1;
    private static final int CONTROL_TYPE_FORWARD = 2;

    @RequiresApi(api = Build.VERSION_CODES.O)
    private final PictureInPictureParams.Builder pipParamsBuilder = new PictureInPictureParams.Builder();

    private final ArrayList<RemoteAction> remoteActions = new ArrayList<>();
    private final List<SpeedItems> speedItemsList = new ArrayList<>();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Handler hideControllerHandler = new Handler(Looper.getMainLooper());
    private GestureDetector gestureDetector;
    private boolean hideOnTouch = false;
    private ActivityResultLauncher<Intent> activityResultLauncher;
    private ExoPlayer exoPlayer;
    private DatabaseProvider databaseProvider;
    private Cache cache;
    private MediaRouter mediaRouter;
    private MediaRouter.Callback mediaRouterCallback;
    private MediaRouteSelector mediaRouteSelector;
    private int containerWidth = 0;
    private int containerHeight = 0;
    private int originalHeight;
    private int originalWidth;
    private int originalResizeMode;
    private int hideTimeout;
    private int additionalButtonImageId;
    private int[] newCoordinates = new int[2];
    private int[] oldCoordinates = new int[2];
    private boolean isLongClick = false;
    private boolean isClickOnRight = false;
    private boolean isPipMode = false;
    private boolean reversing = false;
    private boolean showAdditionalButton;
    private boolean showFloatingText;
    private boolean areViewsHidden = false;
    private boolean isOldLongClick;
    private boolean isFromEnhancedPlayerView = false;
    private float playbackSpeed = 1.0f;
    private CustomProgress volumeProgressBar;
    private ConstraintLayout volumeLayout, baseLayout;
    private CardView speedCardView, Speed2CardView, bottomControlCardView;
    private RecyclerView speedRecyclerView;
    private LinearLayout userIdLayout;
    private SpeedProgress speedProgressBar;
    private TextView speedTextView, userIdTextView, timeLeftTextView;
    private Volume volumeIcon;
    private ImageButton playPauseButton;
    private ImageView additionalImageView, shrinkImageView, moreSettingsImageView;
    private CustomDefaultTimeBar customTimeBar;
    private final BroadcastReceiver playbackBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !ACTION_MEDIA_CONTROL.equals(intent.getAction())) return;
            int controlType = intent.getIntExtra(EXTRA_CONTROL_TYPE, -1);
            handleMediaControl(controlType);
        }
    };
    private EnhancedPlayerListener listener;
    private SpeedRecyclerAdapter speedAdapter;

    public EnhancedPlayerView(Context context) {
        super(context, null);
    }

    public EnhancedPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);

    }

    public EnhancedPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null) {
            TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.EnhancedPlayerView, defStyleAttr, 0);
            try {
                hideTimeout = typedArray.getInteger(R.styleable.EnhancedPlayerView_hideTimeout, 5000);
                showAdditionalButton = typedArray.getBoolean(R.styleable.EnhancedPlayerView_showAdditionalButton, false);
                showFloatingText = typedArray.getBoolean(R.styleable.EnhancedPlayerView_showFloatingText, false);
                hideOnTouch = typedArray.getBoolean(R.styleable.EnhancedPlayerView_hideByTouching, true);
                additionalButtonImageId = typedArray.getResourceId(R.styleable.EnhancedPlayerView_additionalButtonImage, -1);
            } finally {
                typedArray.recycle();
            }
        }
        gestureDetector = new GestureDetector(context, new GestureListener());
        initializeViews();
    }

    @SuppressLint("DefaultLocale")
    private static String formatTime(long milliseconds) {
        if (milliseconds < 0) return "00:00";

        milliseconds /= 1000;
        long minutes = TimeUnit.SECONDS.toMinutes(milliseconds) % 60;
        long hours = TimeUnit.SECONDS.toHours(milliseconds) % 24;
        long days = TimeUnit.SECONDS.toDays(milliseconds);

        StringBuilder formattedTime = new StringBuilder();
        if (days > 0) formattedTime.append(String.format("%02d:", days));
        if (hours > 0) formattedTime.append(String.format("%02d:", hours));
        formattedTime.append(String.format("%02d:", minutes));
        formattedTime.append(String.format("%02d", milliseconds % 60));

        return formattedTime.toString();
    }

    @Override
    public void showController() {
        showAllViews();
    }

    @Override
    public boolean isControllerFullyVisible() {
        return bottomControlCardView.getVisibility() == View.VISIBLE;
    }

    @Override
    public int getControllerShowTimeoutMs() {
        return hideTimeout;
    }

    @Override
    public void setControllerShowTimeoutMs(int controllerShowTimeoutMs) {
        hideTimeout = controllerShowTimeoutMs;
    }

    @Override
    public void hideController() {
        hideAllViews(false);
    }

    @Override
    public boolean getControllerHideOnTouch() {
        return hideOnTouch;
    }

    @Override
    public void setControllerHideOnTouch(boolean controllerShowTimeoutMs) {
        hideOnTouch = controllerShowTimeoutMs;
    }

    @Override
    public void setResizeMode(int resizeMode) {
        super.setResizeMode(resizeMode);
        if (!isFromEnhancedPlayerView && originalResizeMode != resizeMode)
            originalResizeMode = resizeMode;
    }

    private void initializeViews() {
        baseLayout = findViewById(R.id.Base);
        playPauseButton = findViewById(R.id.exo_play);
        customTimeBar = findViewById(R.id.exo_progress);
        moreSettingsImageView = findViewById(R.id.more_settings);
        volumeProgressBar = findViewById(R.id.sound_progress);
        volumeIcon = findViewById(R.id.sound_icon);
        additionalImageView = findViewById(R.id.additional_button);
        shrinkImageView = findViewById(R.id.shrink);
        bottomControlCardView = findViewById(R.id.controls);
        volumeLayout = findViewById(R.id.volumes);
        timeLeftTextView = findViewById(R.id.timeLeft);
        Speed2CardView = findViewById(R.id.speed);
        speedCardView = findViewById(R.id.speedCard);
        speedRecyclerView = findViewById(R.id.speeds);
        speedTextView = findViewById(R.id.SpeedT);
        speedProgressBar = findViewById(R.id.progress);
        userIdTextView = findViewById(R.id.userID);
        userIdLayout = findViewById(R.id.UserID);
    }

    private void initialize() {
        if (additionalButtonImageId != -1)
            additionalImageView.setImageBitmap(BitmapFactory.decodeResource(getResources(), additionalButtonImageId));
        showCloseButton(showAdditionalButton);
        showFloatingText(showFloatingText);
        setOnTouchListener(this);

        customTimeBar.setPlayer(exoPlayer);
        speedProgressBar.setMaxSetup();
        speedProgressBar.SetupProgress(exoPlayer, speedTextView);

        if (hideTimeout != 0)
            hideControllerHandler.postDelayed(this::ControllerAnimation, hideTimeout);
        speedItemsList.add(new SpeedItems("0.5x", 0.5f, 0));
        speedItemsList.add(new SpeedItems("Normal", 1.0f, 1));
        speedItemsList.add(new SpeedItems("1.5x", 1.5f, 0));
        speedItemsList.add(new SpeedItems("2.0x", 2.0f, 0));

        speedAdapter = new SpeedRecyclerAdapter(speedItemsList, exoPlayer);
        speedAdapter.setSpeedProgress(speedProgressBar);
        speedRecyclerView.addItemDecoration(new RecyclerViewDivider(getContext(), R.drawable.item_devider));
        speedRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        speedRecyclerView.setAdapter(speedAdapter);
        speedProgressBar.setSpeedRecAdapter(speedAdapter);

        speedCardView.post(() -> {
            originalHeight = speedCardView.getMeasuredHeight();
            originalWidth = speedCardView.getMeasuredWidth();
            ReturnSpeedCardBack();
        });

        if (showFloatingText) {
            userIdLayout.post(() -> {
                containerWidth = userIdLayout.getMeasuredWidth();
                containerHeight = userIdLayout.getMeasuredHeight();
                animateTextPosition(containerWidth, containerHeight, userIdTextView);
            });
        }

        mediaRouter = MediaRouter.getInstance(getContext());
        mediaRouteSelector = new MediaRouteSelector.Builder().addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO).build();

        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        int streamType = AudioManager.STREAM_MUSIC;
        int maxVolume = audioManager.getStreamMaxVolume(streamType);
        int volume = audioManager.getStreamVolume(streamType);
        volumeProgressBar.setMax(maxVolume);
        volumeProgressBar.setProgress(volume);

        mediaRouterCallback = new MediaRouter.Callback() {
            @Override
            public void onRouteVolumeChanged(@NonNull MediaRouter router, @NonNull MediaRouter.RouteInfo info) {
                int volume = info.getVolume();
                volumeProgressBar.setProgress(volume);
                volumeIcon.setVolume(volume);
            }
        };

        registerVolumeObserver();

        volumeProgressBar.setProgressChangesListener(new CustomProgress.ProgressChangesListener() {

            @Override
            public void onProgressChanges(int progress) {
                audioManager.setStreamVolume(streamType, progress, 0);
                volumeIcon.setVolume(progress);
            }

            @Override
            public void onStartTrackingTouch() {
            }

            @Override
            public void onStopTrackingTouch() {
            }
        });

        customTimeBar.setProgressChangeListener(this);

        moreSettingsImageView.setOnClickListener(this);
        playPauseButton.setOnClickListener(this);
        additionalImageView.setOnClickListener(this);
        shrinkImageView.setOnClickListener(this);

        originalResizeMode = getResizeMode();
        addOnLayoutChangeListener(this);
    }

    private void handleMediaControl(int controlType) {
        switch (controlType) {
            case CONTROL_TYPE_BACK -> {
                int seekPosition = (int) exoPlayer.getCurrentPosition() - 10000;
                exoPlayer.seekTo(seekPosition);
                customTimeBar.setPosition(seekPosition);
            }
            case CONTROL_TYPE_PLAYBACK -> {
                if (exoPlayer.isPlaying()) {
                    exoPlayer.pause();
                    playPauseButton.setImageResource(R.drawable.play);
                    updatePictureInPictureActions(R.drawable.play);
                } else {
                    exoPlayer.play();
                    playPauseButton.setImageResource(R.drawable.pause);
                    updatePictureInPictureActions(R.drawable.pause);
                }
            }
            case CONTROL_TYPE_FORWARD -> {
                int seekPosition = (int) exoPlayer.getCurrentPosition() + 10000;
                exoPlayer.seekTo(seekPosition);
                customTimeBar.setPosition(seekPosition);
            }
        }
    }

    @Override
    public void setPlayer(Player player) {
        super.setPlayer(player);
        if (player != null) {
            this.exoPlayer = (ExoPlayer) player;
            initialize();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == playPauseButton.getId()) {
            if (VisibilityChecking()) ReturnSpeedCardBack();
            ValueAnimator scaleAni = ValueAnimator.ofFloat(1f, 0.9f);

            if (exoPlayer != null) {
                if (exoPlayer.isPlaying()) {
                    if (scaleAni.isRunning()) scaleAni.cancel();

                    scaleAni.addUpdateListener(animation -> {
                        playPauseButton.setScaleX((float) animation.getAnimatedValue());
                        playPauseButton.setScaleY((float) animation.getAnimatedValue());
                    });
                    scaleAni.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            if (reversing) {
                                reversing = false;
                            } else {
                                playPauseButton.setImageResource(R.drawable.play);
                                updatePictureInPictureActions(R.drawable.play);
                                reversing = true;
                                scaleAni.reverse();
                            }
                        }
                    });
                    scaleAni.setDuration(250);
                    scaleAni.setInterpolator(new AccelerateDecelerateInterpolator());
                    scaleAni.start();
                    exoPlayer.pause();
                } else {
                    if (scaleAni.isRunning()) scaleAni.cancel();

                    scaleAni.addUpdateListener(animation -> {
                        playPauseButton.setScaleX((float) animation.getAnimatedValue());
                        playPauseButton.setScaleY((float) animation.getAnimatedValue());
                    });
                    scaleAni.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            if (reversing) {
                                reversing = false;
                            } else {
                                playPauseButton.setImageResource(R.drawable.pause);
                                updatePictureInPictureActions(R.drawable.pause);
                                reversing = true;
                                scaleAni.reverse();
                            }
                        }
                    });
                    scaleAni.setDuration(250);
                    scaleAni.setInterpolator(new AccelerateDecelerateInterpolator());
                    scaleAni.start();
                    exoPlayer.play();
                }
            }
        } else if (v.getId() == shrinkImageView.getId()) {
            if (VisibilityChecking()) ReturnSpeedCardBack();
            if (getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Settings.canDrawOverlays(getContext())) {
                        if (isPipMode) {
                            leavePIP();
                        } else {
                            requestPIP();
                        }
                    } else {
                        requestPIPPermission();
                    }
                }
            }
        } else if (v.getId() == additionalImageView.getId()) {
            if (VisibilityChecking()) ReturnSpeedCardBack();
            if (listener != null) listener.onCloseClicked();
        } else if (v.getId() == moreSettingsImageView.getId()) {
            if (VisibilityChecking()) {
                ReturnSpeedCardBack();
            } else {
                if (speedAdapter != null) {
                    float chosenSpeed = speedAdapter.getChoseSpeed();
                    float speed = exoPlayer.getPlaybackParameters().speed;
                    boolean speedContains = speedItemsList.contains(new SpeedItems(speed));

                    if ((chosenSpeed != -1 && speedContains && chosenSpeed != speed) || (chosenSpeed == -1 && speedContains))
                        updateRecyclerViewData(chosenSpeed != -1 ? getSpeedItem(speedItemsList, speed) : -1);
                }

                @SuppressLint("Recycle") ValueAnimator VW = createValueAnimator(-3, originalWidth, false, 300, value -> {
                    int wd = (int) value.getAnimatedValue();
                    if (wd != -2 && wd != -1 && wd != 0) adjustLayoutParams(wd, null);
                });

                @SuppressLint("Recycle") ValueAnimator VH = createValueAnimator(-3, originalHeight, false, 300, value -> {
                    int hg = (int) value.getAnimatedValue();
                    if (hg != -2 && hg != -1 && hg != 0) adjustLayoutParams(null, hg);
                });

                @SuppressLint("Recycle") ValueAnimator VV = createValueAnimator(0.94f, 0.85f, true, 250, value -> {
                    ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) speedCardView.getLayoutParams();
                    params.verticalBias = (float) value.getAnimatedValue();
                    speedCardView.setLayoutParams(params);
                });

                @SuppressLint("Recycle") ValueAnimator VCH = createValueAnimator(0.89f, 0.9f, true, 250, value -> {
                    ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) speedCardView.getLayoutParams();
                    params.horizontalBias = (float) value.getAnimatedValue();
                    speedCardView.setLayoutParams(params);
                });

                VW.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        super.onAnimationEnd(animator);
                        animateScaling();
                    }

                    @Override
                    public void onAnimationStart(Animator animator) {
                        speedCardView.setVisibility(View.VISIBLE);
                    }
                });

                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(VW, VH, VV, VCH);
                animatorSet.start();
            }
        }
    }

    @SuppressLint("Recycle")
    private void ControllerAnimation() {
        if (!(VisibilityChecking() || volumeProgressBar.isDragging() || speedProgressBar.isDragging() || customTimeBar.isDragging())) {
            hideControllerHandler.removeCallbacksAndMessages(null);
            ValueAnimator hide = bottomControlCardView.getVisibility() == View.VISIBLE ? ValueAnimator.ofFloat(1f, 0f) : ValueAnimator.ofFloat(0f, 1f);
            hide.setDuration(300);
            if (hide.isRunning()) hide.cancel();

            View.OnClickListener click = this;
            View.OnTouchListener touch = this;
            handleClicksAndTouches(click, touch);
            showAllViews();

            hide.addUpdateListener(animation -> changeAllViewsAlpha((float) animation.getAnimatedValue()));
            hide.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if (!areViewsHidden) hideAllViews(true);
                    else
                        hideControllerHandler.postDelayed(() -> ControllerAnimation(), hideTimeout);
                    areViewsHidden = !areViewsHidden;
                    handleClicksAndTouches(click, touch);
                }
            });
            hide.start();
        }
    }

    private void handleClicksAndTouches(View.OnClickListener click, View.OnTouchListener touch) {
        setOnTouchListener(touch);
        moreSettingsImageView.setOnClickListener(click);
        playPauseButton.setOnClickListener(click);
        additionalImageView.setOnClickListener(click);
        shrinkImageView.setOnClickListener(click);
    }

    private void changeAllViewsAlpha(float alpha) {
        volumeLayout.setAlpha(alpha);
        if (alpha <= 0.74f) bottomControlCardView.setAlpha(alpha);
        shrinkImageView.setAlpha(alpha);
        additionalImageView.setAlpha(alpha);
        if (alpha <= 0.6f) playPauseButton.setAlpha(alpha);
        speedCardView.setAlpha(alpha);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (PerformChecks(event)) return true;
        else return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    private boolean PerformChecks(MotionEvent event) {
        float x = event.getX();
        int width = getWidth();

        if (VisibilityChecking()) {
            ReturnSpeedCardBack();
            return true;
        } else {
            Rect rect = new Rect();
            bottomControlCardView.getHitRect(rect);

            if (!rect.contains((int) event.getRawX(), (int) event.getRawY())) {
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN -> {
                        mainHandler.removeCallbacksAndMessages(null);
                        hideControllerHandler.removeCallbacksAndMessages(null);

                        isClickOnRight = x > (float) width / 2;

                        if (isClickOnRight) {
                            mainHandler.postDelayed(() -> {
                                isLongClick = true;
                                playbackSpeed = exoPlayer.getPlaybackParameters().speed;
                                if (speedAdapter != null)
                                    updateRecyclerViewData(getSpeedItem(speedItemsList, 2.0f));

                                exoPlayer.setPlaybackSpeed(2.0f);
                                AnimateAlpha(Speed2CardView, Speed2CardView.getAlpha(), 1f);
                            }, ViewConfiguration.getLongPressTimeout());
                        }
                    }

                    case MotionEvent.ACTION_MOVE -> {
                        if (isLongClick) {
                            if (event.getX() < (float) width / 2) {
                                isClickOnRight = false;
                                if (speedAdapter != null)
                                    updateRecyclerViewData(getSpeedItem(speedItemsList, playbackSpeed));
                                exoPlayer.setPlaybackSpeed(playbackSpeed);
                                AnimateAlpha(Speed2CardView, Speed2CardView.getAlpha(), 0f);
                            } else if (!isClickOnRight) {
                                isClickOnRight = true;
                                if (speedAdapter != null)
                                    updateRecyclerViewData(getSpeedItem(speedItemsList, 2.0f));
                                exoPlayer.setPlaybackSpeed(2.0f);
                                AnimateAlpha(Speed2CardView, Speed2CardView.getAlpha(), 1f);
                            }
                        }
                    }

                    case MotionEvent.ACTION_UP -> {
                        isClickOnRight = false;
                        mainHandler.removeCallbacksAndMessages(null);
                        isOldLongClick = isLongClick;
                        if (isLongClick) {
                            isLongClick = false;
                            if (speedAdapter != null)
                                updateRecyclerViewData(getSpeedItem(speedItemsList, playbackSpeed));
                            exoPlayer.setPlaybackSpeed(playbackSpeed);
                            AnimateAlpha(Speed2CardView, Speed2CardView.getAlpha(), 0f);
                            if (hideTimeout != 0)
                                hideControllerHandler.postDelayed(this::ControllerAnimation, hideTimeout);
                        }
                    }
                }
                return false;
            }
        }

        return true;
    }

    private int getSeekPosition() {
        return isClickOnRight ? (int) exoPlayer.getCurrentPosition() + 10000 : (int) exoPlayer.getCurrentPosition() - 10000;
    }

    public void initializeDatabaseProvider() {
        databaseProvider = new StandaloneDatabaseProvider(getContext());
    }

    public MediaSource EnableCaching(String mediaUrl) {
        MediaItem mediaItem = new MediaItem.Builder().setUri(Uri.parse(mediaUrl)).build();
        CacheDataSource.Factory cacheDataSourceFactory = new CacheDataSource.Factory().setCache(getInstance()).setUpstreamDataSourceFactory(new DefaultHttpDataSource.Factory()).setFlags(CacheDataSource.FLAG_BLOCK_ON_CACHE | CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);

        return new ProgressiveMediaSource.Factory(cacheDataSourceFactory).createMediaSource(mediaItem);
    }

    private Cache getInstance() {
        if (cache == null)
            cache = new SimpleCache(getContext().getCacheDir(), new NoOpCacheEvictor(), databaseProvider);
        return cache;
    }

    @Override
    public void onProgressChanged(long position) {
        updateRemainTime(position);
    }

    @Override
    public void onProgressChanges(TimeBar timeBar, long position) {
        exoPlayer.seekTo(position);
        updateRemainTime(position);
    }

    @Override
    public void onStartTrackingTouch(TimeBar timeBar, long position) {
        if (VisibilityChecking()) {
            ReturnSpeedCardBack();
        }
    }

    @Override
    public void onStopTrackingTouch(TimeBar timeBar, long position, boolean canceled) {
    }

    @Override
    public void onPositionDiscontinuity(@NonNull PositionInfo oldPosition, @NonNull PositionInfo newPosition, int reason) {
        updateProgress();
        Listener.super.onPositionDiscontinuity(oldPosition, newPosition, reason);
    }

    @Override
    public void onIsLoadingChanged(boolean isLoading) {
        updateProgress();
        Listener.super.onIsLoadingChanged(isLoading);
    }

    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        Listener.super.onIsPlayingChanged(isPlaying);
        updateProgress();
        if (isPlaying) updateRemainTime(exoPlayer.getCurrentPosition());
    }

    @Override
    public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
        Listener.super.onPlayWhenReadyChanged(playWhenReady, reason);
        updateProgress();
    }

    @Override
    public void onPlaybackStateChanged(int state) {
        Listener.super.onPlaybackStateChanged(state);
        boolean Playing = exoPlayer.isPlaying();

        updateProgress();
        timeLeftTextView.setText(formatTime(exoPlayer.getDuration()));

        if (state == Player.STATE_READY) {
            if (isPipMode) {
                if (Playing) {
                    updatePictureInPictureActions(R.drawable.pause);
                } else {
                    updatePictureInPictureActions(R.drawable.play);
                }
            }
        } else if (state == Player.STATE_ENDED) {
            if (isPipMode) {
                playPauseButton.setImageResource(R.drawable.play);
                updatePictureInPictureActions(R.drawable.play);
            }
        }
    }

    @Override
    public void onTimelineChanged(@NonNull Timeline timeline, int reason) {
        Listener.super.onTimelineChanged(timeline, reason);
        if (exoPlayer.getDuration() != C.TIME_UNSET) {
            updateProgress();
            timeLeftTextView.setText(formatTime(exoPlayer.getDuration()));
        }
    }

    private void updateProgress() {
        if (exoPlayer == null) return;

        long duration = exoPlayer.getDuration();
        long position = exoPlayer.getCurrentPosition();
        long bufferedPosition = exoPlayer.getBufferedPosition();

        customTimeBar.setDuration(duration);
        customTimeBar.setPosition(position);
        customTimeBar.setBufferedPosition(bufferedPosition);
    }

    private void updateRemainTime(long position) {
        timeLeftTextView.setText(formatTime(exoPlayer.getDuration() - position));
    }

    private ValueAnimator createValueAnimator(Object from, Object to, boolean Float, int duration, ValueAnimator.AnimatorUpdateListener updateListener) {
        ValueAnimator animator = Float ? ValueAnimator.ofFloat((float) from, (float) to) : ValueAnimator.ofInt((int) from, (int) to);
        animator.addUpdateListener(updateListener);
        animator.setDuration(duration);
        return animator;
    }

    private void adjustLayoutParams(Integer width, Integer height) {
        ViewGroup.LayoutParams params = speedCardView.getLayoutParams();
        if (width != null) params.width = width;
        if (height != null) params.height = height;
        speedCardView.setLayoutParams(params);
        baseLayout.requestLayout();
    }

    private void animateScaling() {
        ValueAnimator scaling = ValueAnimator.ofFloat(1.0f, 1.1f);
        scaling.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            speedCardView.setScaleX(scale);
            speedCardView.setScaleY(scale);
            baseLayout.requestLayout();
        });
        scaling.setDuration(150);
        scaling.setRepeatMode(ValueAnimator.REVERSE);
        scaling.setRepeatCount(1);
        scaling.start();
    }

    private void updatePictureInPictureActions(@DrawableRes int iconId) {
        final PendingIntent i = PendingIntent.getBroadcast(getContext(), EnhancedPlayerView.REQUEST_PLAYBACK, new Intent(ACTION_MEDIA_CONTROL).putExtra(EXTRA_CONTROL_TYPE, EnhancedPlayerView.CONTROL_TYPE_PLAYBACK), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final Icon icon = Icon.createWithResource(getContext(), iconId);
            try {
                remoteActions.set(EnhancedPlayerView.REQUEST_PLAYBACK, new RemoteAction(icon, "PlayBack", "PlayBack", i));
                pipParamsBuilder.setActions(remoteActions);
                ((Activity) getContext()).setPictureInPictureParams(pipParamsBuilder.build());
            } catch (Exception ignored) {
            }
        }
    }

    private void initializePictureInRetractions() {
        final PendingIntent i1 = PendingIntent.getBroadcast(getContext(), REQUEST_BACK, new Intent(ACTION_MEDIA_CONTROL).putExtra(EXTRA_CONTROL_TYPE, CONTROL_TYPE_BACK), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        final PendingIntent i2 = PendingIntent.getBroadcast(getContext(), REQUEST_PLAYBACK, new Intent(ACTION_MEDIA_CONTROL).putExtra(EXTRA_CONTROL_TYPE, CONTROL_TYPE_PLAYBACK), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        final PendingIntent i3 = PendingIntent.getBroadcast(getContext(), REQUEST_FORWARD, new Intent(ACTION_MEDIA_CONTROL).putExtra(EXTRA_CONTROL_TYPE, CONTROL_TYPE_FORWARD), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final Icon icon1 = Icon.createWithResource(getContext(), R.drawable.back);
            remoteActions.add(new RemoteAction(icon1, "Back", "Back", i1));

            final Icon icon2 = exoPlayer.isPlaying() ? Icon.createWithResource(getContext(), R.drawable.pause) : Icon.createWithResource(getContext(), R.drawable.play);
            remoteActions.add(new RemoteAction(icon2, "PlayBack", "PlayBack", i2));

            final Icon icon3 = Icon.createWithResource(getContext(), R.drawable.forward);
            remoteActions.add(new RemoteAction(icon3, "Forward", "Forward", i3));

            pipParamsBuilder.setActions(remoteActions);
            ((Activity) getContext()).setPictureInPictureParams(pipParamsBuilder.build());
        }
    }

    private void hideAllViews(boolean InVISIBLE) {
        if (InVISIBLE) {
            volumeLayout.setVisibility(View.INVISIBLE);
            bottomControlCardView.setVisibility(View.INVISIBLE);
            shrinkImageView.setVisibility(View.INVISIBLE);
            if (showAdditionalButton) additionalImageView.setVisibility(View.INVISIBLE);
            else additionalImageView.setVisibility(View.GONE);
            playPauseButton.setVisibility(View.INVISIBLE);
            speedCardView.setVisibility(View.INVISIBLE);
        } else {
            volumeLayout.setVisibility(View.GONE);
            bottomControlCardView.setVisibility(View.GONE);
            shrinkImageView.setVisibility(View.GONE);
            additionalImageView.setVisibility(View.GONE);
            playPauseButton.setVisibility(View.GONE);
            speedCardView.setVisibility(View.GONE);
        }
    }

    private void showAllViews() {
        volumeLayout.setVisibility(View.VISIBLE);
        bottomControlCardView.setVisibility(View.VISIBLE);
        shrinkImageView.setVisibility(View.VISIBLE);
        if (showAdditionalButton) additionalImageView.setVisibility(View.VISIBLE);
        playPauseButton.setVisibility(View.VISIBLE);
        speedCardView.setVisibility(View.VISIBLE);
    }

    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        isPipMode = isInPictureInPictureMode;

        if (isInPictureInPictureMode) {
            hideAllViews(false);
        } else {
            getContext().unregisterReceiver(playbackBroadcastReceiver);
            showAllViews();
        }
    }

    private void requestPIPPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (Settings.canDrawOverlays(getContext())) {
                Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getContext().getPackageName()));
                activityResultLauncher.launch(i);
            } else {
                requestPIP();
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void requestPIP() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                initializePictureInRetractions();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    getContext().registerReceiver(playbackBroadcastReceiver, new IntentFilter(ACTION_MEDIA_CONTROL), Context.RECEIVER_NOT_EXPORTED);
                } else {
                    getContext().registerReceiver(playbackBroadcastReceiver, new IntentFilter(ACTION_MEDIA_CONTROL));
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ((Activity) getContext()).enterPictureInPictureMode(pipParamsBuilder.setAutoEnterEnabled(true).setSeamlessResizeEnabled(true).build());
                } else {
                    ((Activity) getContext()).enterPictureInPictureMode(pipParamsBuilder.build());
                }
            } else {
                ((Activity) getContext()).enterPictureInPictureMode();
            }
        }
    }

    private void leavePIP() {
        ((Activity) getContext()).finishAndRemoveTask();
    }

    private void registerVolumeObserver() {
        mediaRouter.addCallback(mediaRouteSelector, mediaRouterCallback);
    }

    private void unregisterVolumeObserver() {
        mediaRouter.removeCallback(mediaRouterCallback);
    }

    private void updateRecyclerViewData(int index) {
        int ind = speedAdapter.getIndexOfChosenSpeed();
        if (index != -1) {
            if (ind != -1) {
                if (ind != index) {
                    speedAdapter.setChosen(ind, 0);
                    speedAdapter.setChosen(index, 1);
                }
            } else {
                speedAdapter.setChosen(index, 1);
            }
        } else {
            if (ind != -1) speedAdapter.setChosen(ind, 0);
        }
    }

    private void animateTextPosition(int ContainerWidth, int ContainerHeight, TextView textView) {
        final ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);

        newCoordinates = getRandomPosition(ContainerWidth, ContainerHeight, textView);

        animator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();

            if (oldCoordinates == null) {
                textView.setTranslationX(newCoordinates[0] * progress);
                textView.setTranslationY(newCoordinates[1] * progress);
            } else {
                textView.setTranslationX(oldCoordinates[0] + (newCoordinates[0] - oldCoordinates[0]) * progress);
                textView.setTranslationY(oldCoordinates[1] + (newCoordinates[1] - oldCoordinates[1]) * progress);
            }
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                oldCoordinates = newCoordinates;
                newCoordinates = getRandomPosition(ContainerWidth, ContainerHeight, textView);
                animator.start();
            }
        });
        animator.setDuration(5000);
        animator.start();
    }

    private int[] getRandomPosition(int width, int height, View view) {
        int availableWSpace = width - view.getWidth();
        int availableHSpace = height - view.getHeight();

        int randomX = (int) (Math.random() * availableWSpace);
        int randomY = (int) (Math.random() * availableHSpace);

        return new int[]{randomX, randomY};
    }

    private int getSpeedItem(List<SpeedItems> items, Object value) {
        int position = -1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Optional<SpeedItems> foundItem = speedItemsList.stream().filter(item -> item.getSpeed() == (float) value).findFirst();
            if (foundItem.isPresent()) position = items.indexOf(foundItem.get());
        } else {
            position = speedItemsList.indexOf(new SpeedItems((float) value));
        }
        return position;
    }

    private boolean VisibilityChecking() {
        ViewGroup.LayoutParams speedCardParams = speedCardView.getLayoutParams();
        return speedCardParams.width != -3 && speedCardParams.height != -3;
    }

    private void ReturnSpeedCardBack() {
        ViewGroup.LayoutParams speedCardParams = speedCardView.getLayoutParams();
        speedCardParams.width = -3;
        speedCardParams.height = -3;
        speedCardView.setLayoutParams(speedCardParams);
        baseLayout.requestLayout();
    }

    @SuppressLint("Recycle")
    private void AnimateAlpha(CardView view, float start, float end) {
        ValueAnimator v = ValueAnimator.ofFloat(start, end).setDuration(300);
        if (v.isRunning()) v.cancel();
        v.addUpdateListener(animation -> view.setAlpha((float) animation.getAnimatedValue()));
        v.start();
    }

    public void initializeActivityResult(ActivityResultRegistry register) {
        activityResultLauncher = register.register("PIP_Result", new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    if (Settings.canDrawOverlays(getContext())) requestPIP();
        });
    }

    public void onConfigurationChangedActivity(@NonNull Configuration newConfig) {
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            isFromEnhancedPlayerView = true;
            setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
            hideSystemUI();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            isFromEnhancedPlayerView = false;
            setResizeMode(originalResizeMode);
            showSystemUi();
        }
    }

    public void onDestroyActivity() {
        if (mediaRouter != null) unregisterVolumeObserver();
        cache.release();
        cache = null;
        try {
            getContext().unregisterReceiver(playbackBroadcastReceiver);
        } catch (IllegalArgumentException ignored) {
        }
    }

    public void onUserLeaveHintActivity() {
        if (exoPlayer.isPlaying()) requestPIP();
    }

    public void onPauseActivity() {
        if (mediaRouter != null) unregisterVolumeObserver();
    }

    public void onResumeActivity() {
        registerVolumeObserver();
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController window = ((Activity) getContext()).getWindow().getInsetsController();
            if (window != null) {
                window.hide(WindowInsets.Type.systemBars());
                window.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            Window window = ((Activity) getContext()).getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private void showSystemUi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController window = ((Activity) getContext()).getWindow().getInsetsController();
            if (window != null) window.show(WindowInsets.Type.systemBars());
        } else {
            Window window = ((Activity) getContext()).getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    public void changeFloatingText(String text) {
        userIdTextView.setText(text);
    }

    @Override
    public void onLayoutChange(View v, int oldLeft, int oldTop, int oldRight, int oldBottom, int newLeft, int newTop, int newRight, int newBottom) {
        final Rect sourceRectHint = new Rect();
        getGlobalVisibleRect(sourceRectHint);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            final PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder().setSourceRectHint(sourceRectHint);
            ((Activity) getContext()).setPictureInPictureParams(builder.build());
        }
    }

    public void showCloseButton(boolean show) {
        showAdditionalButton = show;
        additionalImageView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void showFloatingText(boolean show) {
        showFloatingText = show;
        userIdTextView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void addListener(EnhancedPlayerListener listener) {
        this.listener = listener;
    }

    public interface EnhancedPlayerListener {
        void onCloseClicked();
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDoubleTap(@NonNull MotionEvent event) {
            mainHandler.removeCallbacksAndMessages(null);
            hideControllerHandler.removeCallbacksAndMessages(null);

            int seekPosition = getSeekPosition();
            exoPlayer.seekTo(seekPosition);
            customTimeBar.setPosition(seekPosition);
            if (hideTimeout != 0)
                hideControllerHandler.postDelayed(EnhancedPlayerView.this::ControllerAnimation, hideTimeout);

            return super.onDoubleTap(event);
        }

        @Override
        public boolean onSingleTapConfirmed(@NonNull MotionEvent event) {
            if (!isOldLongClick && hideOnTouch) ControllerAnimation();
            else if (hideTimeout != 0)
                hideControllerHandler.postDelayed(EnhancedPlayerView.this::ControllerAnimation, hideTimeout);
            return super.onSingleTapConfirmed(event);
        }
    }
}
