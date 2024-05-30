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
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Outline;
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
import android.view.ViewOutlineProvider;
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

import eightbitlab.com.blurview.BlurView;

@OptIn(markerClass = UnstableApi.class)
public final class EnhancedPlayerView extends PlayerView implements View.OnClickListener, View.OnTouchListener, Listener, CustomDefaultTimeBar.ProgressChangeListener, View.OnLayoutChangeListener {

    private static final String ACTION_MEDIA_CONTROL = "media_control";
    private static final String EXTRA_CONTROL_TYPE = "control_type";
    private static final int REQUEST_BACK = 0;
    private static final int REQUEST_PLAYBACK = 1;
    private static final int REQUEST_FORWARD = 2;
    private static final int CONTROL_TYPE_BACK = 0;
    private static final int CONTROL_TYPE_PLAYBACK = 1;
    private static final int CONTROL_TYPE_FORWARD = 2;

    @RequiresApi(api = Build.VERSION_CODES.O)
    private final PictureInPictureParams.Builder pictureParams = new PictureInPictureParams.Builder();

    private final ArrayList<RemoteAction> actions = new ArrayList<>();
    private final List<SpeedItems> speeds = new ArrayList<>();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Handler handler2 = new Handler(Looper.getMainLooper());
    private final GestureDetector gestureDetector;
    private boolean hideONTouch = false;
    private ActivityResultLauncher<Intent> ActivityResult;
    private ExoPlayer player;
    private DatabaseProvider databaseProvider;
    private Cache cache;
    private MediaRouter mediaRouter;
    private MediaRouter.Callback Callback;
    private MediaRouteSelector selector;
    private int containerWidth = 0;
    private int containerHeight = 0;
    private int OHeight;
    private int OWidth;
    private int originalResizeMode;
    private int controllerShowTime;
    private int[] New = new int[2];
    private int[] oldXY = new int[2];
    private boolean isLongClick = false;
    private boolean isClickOnRight = false;
    private boolean isPiP = false;
    private boolean reversing = false;
    private boolean showClose;
    private boolean showFloatingText;
    private boolean hiddenViews = false;
    private boolean oldLongClick;
    private boolean isFromEnhancedPlayerView = false;
    private float FSpeed = 1.0f;
    private CustomProgress SoundProgress;
    private ConstraintLayout volumes, Base;
    private CardView Speed, bottomControls;
    private BlurView SpeedCard;
    private RecyclerView Speeds;
    private LinearLayout UserID;
    private SpeedProgress progress;
    private TextView SpeedT, UID, TimeLeft;
    private Volume SoundIcon;
    private ImageButton Play;
    private ImageView close, shrink, MoreSettings;
    private CustomDefaultTimeBar Progress;
    private final BroadcastReceiver playbackBroadCastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            if (i == null || !ACTION_MEDIA_CONTROL.equals(i.getAction())) return;

            final int controlType = i.getIntExtra(EXTRA_CONTROL_TYPE, -1);
            switch (controlType) {
                case CONTROL_TYPE_BACK -> {
                    int seekPosition = (int) player.getCurrentPosition() - 10000;
                    player.seekTo(seekPosition);
                    Progress.setPosition(seekPosition);
                }

                case CONTROL_TYPE_PLAYBACK -> {
                    if (player.isPlaying()) {
                        player.pause();
                        Play.setImageResource(R.drawable.play);
                        updatePictureInPictureActions(R.drawable.play);
                    } else {
                        player.play();
                        Play.setImageResource(R.drawable.pause);
                        updatePictureInPictureActions(R.drawable.pause);
                    }
                }

                case CONTROL_TYPE_FORWARD -> {
                    int seekPosition = (int) player.getCurrentPosition() + 10000;
                    player.seekTo(seekPosition);
                    Progress.setPosition(seekPosition);
                }
            }
        }
    };
    private EnhancedPlayerListener listener;
    private SpeedRecyclerAdapter adapter;

    public EnhancedPlayerView(Context context) {
        super(context, null);
        gestureDetector = new GestureDetector(context, new GestureListener());
        //initializeViews();
    }

    public EnhancedPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.EnhancedPlayerView);
        controllerShowTime = typedArray.getInteger(R.styleable.EnhancedPlayerView_hide_time_out, 5000);
        showClose = typedArray.getBoolean(R.styleable.EnhancedPlayerView_show_close, false);
        showFloatingText = typedArray.getBoolean(R.styleable.EnhancedPlayerView_show_floating_text, false);
        hideONTouch = typedArray.getBoolean(R.styleable.EnhancedPlayerView_hide_by_touching, true);
        typedArray.recycle();

        gestureDetector = new GestureDetector(context, new GestureListener());
        //initializeViews();
    }

    public EnhancedPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.EnhancedPlayerView);
        controllerShowTime = typedArray.getInteger(R.styleable.EnhancedPlayerView_hide_time_out, 5000);
        showClose = typedArray.getBoolean(R.styleable.EnhancedPlayerView_show_close, false);
        showFloatingText = typedArray.getBoolean(R.styleable.EnhancedPlayerView_show_floating_text, false);
        hideONTouch = typedArray.getBoolean(R.styleable.EnhancedPlayerView_hide_by_touching, true);
        typedArray.recycle();

        gestureDetector = new GestureDetector(context, new GestureListener());
        //initializeViews();
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
        return bottomControls.getVisibility() == View.VISIBLE;
    }

    @Override
    public int getControllerShowTimeoutMs() {
        return controllerShowTime;
    }

    @Override
    public void setControllerShowTimeoutMs(int controllerShowTimeoutMs) {
        controllerShowTime = controllerShowTimeoutMs;
    }

    @Override
    public void hideController() {
        hideAllViews(false);
    }

    @Override
    public boolean getControllerHideOnTouch() {
        return hideONTouch;
    }

    @Override
    public void setControllerHideOnTouch(boolean controllerHideOnTouch) {
        hideONTouch = controllerHideOnTouch;
    }

    @Override
    public void setResizeMode(int resizeMode) {
        super.setResizeMode(resizeMode);
        if (!isFromEnhancedPlayerView && originalResizeMode != resizeMode)
            originalResizeMode = resizeMode;
    }

    @SuppressLint("DiscouragedApi")
    public void initializeViews(Resources r, String PackageName) {
        Base = findViewById(r.getIdentifier("Base", "id", PackageName));
        Play = findViewById(r.getIdentifier("exo_play", "id", PackageName));
        Progress = findViewById(r.getIdentifier("exo_progress", "id", PackageName));
        MoreSettings = findViewById(r.getIdentifier("more_settings", "id", PackageName));
        SoundProgress = findViewById(r.getIdentifier("sound_progress", "id", PackageName));
        SoundIcon = findViewById(r.getIdentifier("sound_icon", "id", PackageName));
        close = findViewById(r.getIdentifier("close", "id", PackageName));
        shrink = findViewById(r.getIdentifier("shrink", "id", PackageName));
        bottomControls = findViewById(r.getIdentifier("controls", "id", PackageName));
        volumes = findViewById(r.getIdentifier("volumes", "id", PackageName));
        TimeLeft = findViewById(r.getIdentifier("timeLeft", "id", PackageName));
        Speed = findViewById(r.getIdentifier("speed", "id", PackageName));
        SpeedCard = findViewById(r.getIdentifier("speedCard", "id", PackageName));
        Speeds = findViewById(r.getIdentifier("speeds", "id", PackageName));
        SpeedT = findViewById(r.getIdentifier("SpeedT", "id", PackageName));
        progress = findViewById(r.getIdentifier("progress", "id", PackageName));
        UID = findViewById(r.getIdentifier("userID", "id", PackageName));
        UserID = findViewById(r.getIdentifier("UserID", "id", PackageName));
    }

    private void initialize() {
        showCloseButton(showClose);
        showFloatingText(showFloatingText);
        setOnTouchListener(this);

        Progress.setPlayer(player);
        progress.setMaxSetup();
        progress.SetupProgress(player, SpeedT);

        if (controllerShowTime != 0)
            handler2.postDelayed(this::ControllerAnimation, controllerShowTime);
        speeds.add(new SpeedItems("0.5x", 0.5f, 0));
        speeds.add(new SpeedItems("Normal", 1.0f, 1));
        speeds.add(new SpeedItems("1.5x", 1.5f, 0));
        speeds.add(new SpeedItems("2.0x", 2.0f, 0));

        adapter = new SpeedRecyclerAdapter(speeds, player);
        adapter.setSpeedProgress(progress);
        Speeds.addItemDecoration(new RecyclerViewDivider(getContext(), R.drawable.item_devider));
        Speeds.setLayoutManager(new LinearLayoutManager(getContext()));
        Speeds.setAdapter(adapter);
        progress.setSpeedRecAdapter(adapter);

        SpeedCard.post(() -> {
            OHeight = SpeedCard.getMeasuredHeight();
            OWidth = SpeedCard.getMeasuredWidth();
            ReturnSpeedCardBack();
        });

        if (showFloatingText) {
            UserID.post(() -> {
                containerWidth = UserID.getMeasuredWidth();
                containerHeight = UserID.getMeasuredHeight();
                animateTextPosition(containerWidth, containerHeight, UID);
            });
        }

        final View view = ((Activity) getContext()).getWindow().getDecorView();
        ViewGroup rootView = view.findViewById(android.R.id.content);
        SpeedCard.setupWith(rootView).setFrameClearDrawable(view.getBackground()).setBlurRadius(5f).setBlurAutoUpdate(true).setOverlayColor(Color.parseColor("#e2e2e2"));

        SpeedCard.setClipToOutline(true);
        SpeedCard.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                SpeedCard.getBackground().getOutline(outline);
                outline.setAlpha(1f);
            }
        });

        mediaRouter = MediaRouter.getInstance(getContext());
        selector = new MediaRouteSelector.Builder().addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO).build();

        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        int streamType = AudioManager.STREAM_MUSIC;
        int maxVolume = audioManager.getStreamMaxVolume(streamType);
        int volume = audioManager.getStreamVolume(streamType);
        SoundProgress.setMax(maxVolume);
        SoundProgress.setProgress(volume);

        Callback = new MediaRouter.Callback() {
            @Override
            public void onRouteVolumeChanged(@NonNull MediaRouter router, @NonNull MediaRouter.RouteInfo info) {
                int volume = info.getVolume();
                SoundProgress.setProgress(volume);
                SoundIcon.setVolume(volume);
            }
        };

        registerVolumeObserver();

        SoundProgress.setProgressChangesListener(new CustomProgress.progressChangesListener() {

            @Override
            public void onProgressChanges(int progress) {
                audioManager.setStreamVolume(streamType, progress, 0);
                SoundIcon.setVolume(progress);
            }

            @Override
            public void onStartTrackingTouch() {
            }

            @Override
            public void onStopTrackingTouch() {
            }
        });

        Progress.setProgressChangeListener(this);

        MoreSettings.setOnClickListener(this);
        Play.setOnClickListener(this);
        close.setOnClickListener(this);
        shrink.setOnClickListener(this);

        originalResizeMode = getResizeMode();
        addOnLayoutChangeListener(this);
    }

    @Override
    public void setPlayer(Player player) {
        super.setPlayer(player);
        if (player != null) {
            this.player = (ExoPlayer) player;
            initialize();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == Play.getId()) {
            if (VisibilityChecking()) ReturnSpeedCardBack();
            ValueAnimator scaleAni = ValueAnimator.ofFloat(1f, 0.9f);

            if (player != null) {
                if (player.isPlaying()) {
                    if (scaleAni.isRunning()) scaleAni.cancel();

                    scaleAni.addUpdateListener(animation -> {
                        Play.setScaleX((float) animation.getAnimatedValue());
                        Play.setScaleY((float) animation.getAnimatedValue());
                    });
                    scaleAni.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            if (reversing) {
                                reversing = false;
                            } else {
                                Play.setImageResource(R.drawable.play);
                                updatePictureInPictureActions(R.drawable.play);
                                reversing = true;
                                scaleAni.reverse();
                            }
                        }
                    });
                    scaleAni.setDuration(250);
                    scaleAni.setInterpolator(new AccelerateDecelerateInterpolator());
                    scaleAni.start();
                    player.pause();
                } else {
                    if (scaleAni.isRunning()) scaleAni.cancel();

                    scaleAni.addUpdateListener(animation -> {
                        Play.setScaleX((float) animation.getAnimatedValue());
                        Play.setScaleY((float) animation.getAnimatedValue());
                    });
                    scaleAni.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            if (reversing) {
                                reversing = false;
                            } else {
                                Play.setImageResource(R.drawable.pause);
                                updatePictureInPictureActions(R.drawable.pause);
                                reversing = true;
                                scaleAni.reverse();
                            }
                        }
                    });
                    scaleAni.setDuration(250);
                    scaleAni.setInterpolator(new AccelerateDecelerateInterpolator());
                    scaleAni.start();
                    player.play();
                }
            }
        } else if (v.getId() == shrink.getId()) {
            if (VisibilityChecking()) ReturnSpeedCardBack();
            if (getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Settings.canDrawOverlays(getContext())) {
                        if (isPiP) {
                            leavePIP();
                        } else {
                            requestPIP();
                        }
                    } else {
                        requestPIPPermission();
                    }
                }
            }
        } else if (v.getId() == close.getId()) {
            if (VisibilityChecking()) ReturnSpeedCardBack();
            if (listener != null) listener.onCloseClicked();
        } else if (v.getId() == MoreSettings.getId()) {
            if (VisibilityChecking()) {
                ReturnSpeedCardBack();
            } else {
                if (adapter != null) {
                    float chosenSpeed = adapter.getChoseSpeed();
                    float speed = player.getPlaybackParameters().speed;
                    boolean speedContains = speeds.contains(new SpeedItems(speed));

                    if ((chosenSpeed != -1 && speedContains && chosenSpeed != speed) || (chosenSpeed == -1 && speedContains))
                        updateRecyclerViewData(chosenSpeed != -1 ? getSpeedItem(speeds, speed) : -1);
                }

                @SuppressLint("Recycle") ValueAnimator VW = createValueAnimator(-3, OWidth, false, 300, value -> {
                    int wd = (int) value.getAnimatedValue();
                    if (wd != -2 && wd != -1 && wd != 0) adjustLayoutParams(wd, null);
                });

                @SuppressLint("Recycle") ValueAnimator VH = createValueAnimator(-3, OHeight, false, 300, value -> {
                    int hg = (int) value.getAnimatedValue();
                    if (hg != -2 && hg != -1 && hg != 0) adjustLayoutParams(null, hg);
                });

                @SuppressLint("Recycle") ValueAnimator VV = createValueAnimator(0.94f, 0.85f, true, 250, value -> {
                    ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) SpeedCard.getLayoutParams();
                    params.verticalBias = (float) value.getAnimatedValue();
                    SpeedCard.setLayoutParams(params);
                });

                @SuppressLint("Recycle") ValueAnimator VCH = createValueAnimator(0.89f, 0.9f, true, 250, value -> {
                    ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) SpeedCard.getLayoutParams();
                    params.horizontalBias = (float) value.getAnimatedValue();
                    SpeedCard.setLayoutParams(params);
                });

                VW.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        super.onAnimationEnd(animator);
                        animateScaling();
                    }

                    @Override
                    public void onAnimationStart(Animator animator) {
                        SpeedCard.setVisibility(View.VISIBLE);
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
        if (!(VisibilityChecking() || SoundProgress.isDragging() || progress.isDragging() || Progress.isDragging())) {
            handler2.removeCallbacksAndMessages(null);
            ValueAnimator hide = bottomControls.getVisibility() == View.VISIBLE ? ValueAnimator.ofFloat(1f, 0f) : ValueAnimator.ofFloat(0f, 1f);
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
                    if (!hiddenViews) hideAllViews(true);
                    else handler2.postDelayed(() -> ControllerAnimation(), controllerShowTime);
                    hiddenViews = !hiddenViews;
                    handleClicksAndTouches(click, touch);
                }
            });
            hide.start();
        }
    }

    private void handleClicksAndTouches(View.OnClickListener click, View.OnTouchListener touch) {
        setOnTouchListener(touch);
        MoreSettings.setOnClickListener(click);
        Play.setOnClickListener(click);
        close.setOnClickListener(click);
        shrink.setOnClickListener(click);
    }

    private void changeAllViewsAlpha(float alpha) {
        volumes.setAlpha(alpha);
        if (alpha <= 0.74f) bottomControls.setAlpha(alpha);
        shrink.setAlpha(alpha);
        close.setAlpha(alpha);
        if (alpha <= 0.6f) Play.setAlpha(alpha);
        SpeedCard.setAlpha(alpha);
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
            bottomControls.getHitRect(rect);

            if (!rect.contains((int) event.getRawX(), (int) event.getRawY())) {
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN -> {
                        handler.removeCallbacksAndMessages(null);
                        handler2.removeCallbacksAndMessages(null);

                        isClickOnRight = x > (float) width / 2;

                        if (isClickOnRight) {
                            handler.postDelayed(() -> {
                                isLongClick = true;
                                FSpeed = player.getPlaybackParameters().speed;
                                if (adapter != null)
                                    updateRecyclerViewData(getSpeedItem(speeds, 2.0f));

                                player.setPlaybackSpeed(2.0f);
                                AnimateAlpha(Speed, Speed.getAlpha(), 1f);
                            }, ViewConfiguration.getLongPressTimeout());
                        }
                    }

                    case MotionEvent.ACTION_MOVE -> {
                        if (isLongClick) {
                            if (event.getX() < (float) width / 2) {
                                isClickOnRight = false;
                                if (adapter != null)
                                    updateRecyclerViewData(getSpeedItem(speeds, FSpeed));
                                player.setPlaybackSpeed(FSpeed);
                                AnimateAlpha(Speed, Speed.getAlpha(), 0f);
                            } else if (!isClickOnRight) {
                                isClickOnRight = true;
                                if (adapter != null)
                                    updateRecyclerViewData(getSpeedItem(speeds, 2.0f));
                                player.setPlaybackSpeed(2.0f);
                                AnimateAlpha(Speed, Speed.getAlpha(), 1f);
                            }
                        }
                    }

                    case MotionEvent.ACTION_UP -> {
                        isClickOnRight = false;
                        handler.removeCallbacksAndMessages(null);
                        oldLongClick = isLongClick;
                        if (isLongClick) {
                            isLongClick = false;
                            if (adapter != null)
                                updateRecyclerViewData(getSpeedItem(speeds, FSpeed));
                            player.setPlaybackSpeed(FSpeed);
                            AnimateAlpha(Speed, Speed.getAlpha(), 0f);
                            if (controllerShowTime != 0)
                                handler2.postDelayed(this::ControllerAnimation, controllerShowTime);
                        }
                    }
                }
                return false;
            }
        }

        return true;
    }

    private int getSeekPosition() {
        return isClickOnRight ? (int) player.getCurrentPosition() + 10000 : (int) player.getCurrentPosition() - 10000;
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
        player.seekTo(position);
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
        if (isPlaying) updateRemainTime(player.getCurrentPosition());
    }

    @Override
    public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
        Listener.super.onPlayWhenReadyChanged(playWhenReady, reason);
        updateProgress();
    }

    @Override
    public void onPlaybackStateChanged(int state) {
        Listener.super.onPlaybackStateChanged(state);
        boolean Playing = player.isPlaying();

        updateProgress();
        TimeLeft.setText(formatTime(player.getDuration()));

        if (state == Player.STATE_READY) {
            if (isPiP) {
                if (Playing) {
                    updatePictureInPictureActions(R.drawable.pause);
                } else {
                    updatePictureInPictureActions(R.drawable.play);
                }
            }
        } else if (state == Player.STATE_ENDED) {
            if (isPiP) {
                Play.setImageResource(R.drawable.play);
                updatePictureInPictureActions(R.drawable.play);
            }
        }
    }

    @Override
    public void onTimelineChanged(@NonNull Timeline timeline, int reason) {
        Listener.super.onTimelineChanged(timeline, reason);
        if (player.getDuration() != C.TIME_UNSET) {
            updateProgress();
            TimeLeft.setText(formatTime(player.getDuration()));
        }
    }

    private void updateProgress() {
        if (player == null) return;

        long duration = player.getDuration();
        long position = player.getCurrentPosition();
        long bufferedPosition = player.getBufferedPosition();

        Progress.setDuration(duration);
        Progress.setPosition(position);
        Progress.setBufferedPosition(bufferedPosition);
    }

    private void updateRemainTime(long position) {
        TimeLeft.setText(formatTime(player.getDuration() - position));
    }

    private ValueAnimator createValueAnimator(Object from, Object to, boolean Float, int duration, ValueAnimator.AnimatorUpdateListener updateListener) {
        ValueAnimator animator = Float ? ValueAnimator.ofFloat((float) from, (float) to) : ValueAnimator.ofInt((int) from, (int) to);
        animator.addUpdateListener(updateListener);
        animator.setDuration(duration);
        return animator;
    }

    private void adjustLayoutParams(Integer width, Integer height) {
        ViewGroup.LayoutParams params = SpeedCard.getLayoutParams();
        if (width != null) params.width = width;
        if (height != null) params.height = height;
        SpeedCard.setLayoutParams(params);
        Base.requestLayout();
    }

    private void animateScaling() {
        ValueAnimator scaling = ValueAnimator.ofFloat(1.0f, 1.1f);
        scaling.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            SpeedCard.setScaleX(scale);
            SpeedCard.setScaleY(scale);
            Base.requestLayout();
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
                actions.set(EnhancedPlayerView.REQUEST_PLAYBACK, new RemoteAction(icon, "PlayBack", "PlayBack", i));
                pictureParams.setActions(actions);
                ((Activity) getContext()).setPictureInPictureParams(pictureParams.build());
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
            actions.add(new RemoteAction(icon1, "Back", "Back", i1));

            final Icon icon2 = player.isPlaying() ? Icon.createWithResource(getContext(), R.drawable.pause) : Icon.createWithResource(getContext(), R.drawable.play);
            actions.add(new RemoteAction(icon2, "PlayBack", "PlayBack", i2));

            final Icon icon3 = Icon.createWithResource(getContext(), R.drawable.forward);
            actions.add(new RemoteAction(icon3, "Forward", "Forward", i3));

            pictureParams.setActions(actions);
            ((Activity) getContext()).setPictureInPictureParams(pictureParams.build());
        }
    }

    private void hideAllViews(boolean InVISIBLE) {
        if (InVISIBLE) {
            volumes.setVisibility(View.INVISIBLE);
            bottomControls.setVisibility(View.INVISIBLE);
            shrink.setVisibility(View.INVISIBLE);
            if (showClose) close.setVisibility(View.INVISIBLE);
            else close.setVisibility(View.GONE);
            Play.setVisibility(View.INVISIBLE);
            SpeedCard.setVisibility(View.INVISIBLE);
        } else {
            volumes.setVisibility(View.GONE);
            bottomControls.setVisibility(View.GONE);
            shrink.setVisibility(View.GONE);
            close.setVisibility(View.GONE);
            Play.setVisibility(View.GONE);
            SpeedCard.setVisibility(View.GONE);
        }
    }

    private void showAllViews() {
        volumes.setVisibility(View.VISIBLE);
        bottomControls.setVisibility(View.VISIBLE);
        shrink.setVisibility(View.VISIBLE);
        if (showClose) close.setVisibility(View.VISIBLE);
        Play.setVisibility(View.VISIBLE);
        SpeedCard.setVisibility(View.VISIBLE);
    }

    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        isPiP = isInPictureInPictureMode;

        if (isInPictureInPictureMode) {
            hideAllViews(false);
        } else {
            getContext().unregisterReceiver(playbackBroadCastReceiver);
            showAllViews();
        }
    }

    private void requestPIPPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (Settings.canDrawOverlays(getContext())) {
                Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getContext().getPackageName()));
                ActivityResult.launch(i);
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
                    getContext().registerReceiver(playbackBroadCastReceiver, new IntentFilter(ACTION_MEDIA_CONTROL), Context.RECEIVER_NOT_EXPORTED);
                } else {
                    getContext().registerReceiver(playbackBroadCastReceiver, new IntentFilter(ACTION_MEDIA_CONTROL));
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ((Activity) getContext()).enterPictureInPictureMode(pictureParams.setAutoEnterEnabled(true).setSeamlessResizeEnabled(true).build());
                } else {
                    ((Activity) getContext()).enterPictureInPictureMode(pictureParams.build());
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
        mediaRouter.addCallback(selector, Callback);
    }

    private void unregisterVolumeObserver() {
        mediaRouter.removeCallback(Callback);
    }

    private void updateRecyclerViewData(int index) {
        int ind = adapter.getIndexOfChosenSpeed();
        if (index != -1) {
            if (ind != -1) {
                if (ind != index) {
                    adapter.setChosen(ind, 0);
                    adapter.setChosen(index, 1);
                }
            } else {
                adapter.setChosen(index, 1);
            }
        } else {
            if (ind != -1) adapter.setChosen(ind, 0);
        }
    }

    private void animateTextPosition(int ContainerWidth, int ContainerHeight, TextView textView) {
        final ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);

        New = getRandomPosition(ContainerWidth, ContainerHeight, textView);

        animator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();

            if (oldXY == null) {
                textView.setTranslationX(New[0] * progress);
                textView.setTranslationY(New[1] * progress);
            } else {
                textView.setTranslationX(oldXY[0] + (New[0] - oldXY[0]) * progress);
                textView.setTranslationY(oldXY[1] + (New[1] - oldXY[1]) * progress);
            }
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                oldXY = New;
                New = getRandomPosition(ContainerWidth, ContainerHeight, textView);
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
            Optional<SpeedItems> foundItem = speeds.stream().filter(item -> item.getSpeed() == (float) value).findFirst();
            if (foundItem.isPresent()) position = items.indexOf(foundItem.get());
        } else {
            position = speeds.indexOf(new SpeedItems((float) value));
        }
        return position;
    }

    private boolean VisibilityChecking() {
        ViewGroup.LayoutParams speedCardParams = SpeedCard.getLayoutParams();
        return speedCardParams.width != -3 && speedCardParams.height != -3;
    }

    private void ReturnSpeedCardBack() {
        ViewGroup.LayoutParams speedCardParams = SpeedCard.getLayoutParams();
        speedCardParams.width = -3;
        speedCardParams.height = -3;
        SpeedCard.setLayoutParams(speedCardParams);
        Base.requestLayout();
    }

    @SuppressLint("Recycle")
    private void AnimateAlpha(CardView view, float start, float end) {
        ValueAnimator v = ValueAnimator.ofFloat(start, end).setDuration(300);
        if (v.isRunning()) v.cancel();
        v.addUpdateListener(animation -> view.setAlpha((float) animation.getAnimatedValue()));
        v.start();
    }

    public void initializeActivityResult(ActivityResultRegistry register) {
        ActivityResult = register.register("PIP_Result", new ActivityResultContracts.StartActivityForResult(), result -> {
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
            getContext().unregisterReceiver(playbackBroadCastReceiver);
        } catch (IllegalArgumentException ignored) {
        }
    }

    public void onUserLeaveHintActivity() {
        if (player.isPlaying()) requestPIP();
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
        UID.setText(text);
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
        showClose = show;
        close.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void showFloatingText(boolean show) {
        showFloatingText = show;
        UID.setVisibility(show ? View.VISIBLE : View.GONE);
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
            handler.removeCallbacksAndMessages(null);
            handler2.removeCallbacksAndMessages(null);

            int seekPosition = getSeekPosition();
            player.seekTo(seekPosition);
            Progress.setPosition(seekPosition);
            if (controllerShowTime != 0)
                handler2.postDelayed(EnhancedPlayerView.this::ControllerAnimation, controllerShowTime);

            return super.onDoubleTap(event);
        }

        @Override
        public boolean onSingleTapConfirmed(@NonNull MotionEvent event) {
            if (!oldLongClick && hideONTouch) ControllerAnimation();
            else if (controllerShowTime != 0)
                handler2.postDelayed(EnhancedPlayerView.this::ControllerAnimation, controllerShowTime);
            return super.onSingleTapConfirmed(event);
        }
    }
}
