# EnhancedPlayerView
EnhancedPlayerView is a good idea for you to improve your design and make things more easy with Media3 PlayerView

[![StandWithPalestine](https://raw.githubusercontent.com/karim-eg/StandWithPalestine/main/assets/palestine_badge.svg)](https://github.com/karim-eg/StandWithPalestine)

<br>

[![StandWithPalestine](https://raw.githubusercontent.com/karim-eg/StandWithPalestine/main/assets/palestine_banner.svg)](https://github.com/karim-eg/StandWithPalestine/blob/main/Donate.md)

<br>



## Usage


### Step 1
> Add this line to root `build.gradle` at allprojects block code:
```gradle
allprojects {
  repositories {
   //...
   maven { url 'https://jitpack.io' }
  }
 }
 ```

> then add this line into your `build.gradle` app level.
```gradle
dependencies {
    implementation 'com.github.A0ks9:EnhancedPlayerView:1.0.0-Beta02'
}
```

<br>

### Step 2
> Add this file code without changing its name to your project Link:
[exo_control_view](https://github.com/A0ks9/EnhancedPlayerView/blob/main/app/src/main/res/layout/exo_control_view.xml)

<br>
  
### Step 3
> Add this widget to your `xml` activity file
```xml
<com.better.player.EnhancedPlayerView
  android:id="@+id/name_view"
  android:layout_width="match_parent"
  android:layout_height="match_parent"
  app:animation_enabled="false"
  app:controller_layout_id="@layout/exo_control_view"
  app:auto_show="true"
  app:hide_by_touching="true"
  app:hide_during_ads="true"
  app:hide_on_touch="false"
  app:show_timeout="0" />
```

**important**
- you should add without changing anything
```xml
app:animation_enabled="false"
app:hide_on_touch="false"
app:show_timeout="0"
app:controller_layout_id="@layout/exo_control_view"
```

<br>

### Step 4
> add ExoPlayer to your project and use it normally

<br>

### Step 5
> If you want to cache the video, so it doesn't download every time the user opens the app. Then add this to the EnhancedPlayerView view
```java
binding.playerView.initializeDatabaseProvider();
binding.playerView.initializeActivityResult(getActivityResultRegistry());
```

<br>

### Step 6
> **Important:** You should add this code at the beginning Of onCreate
```java
binding.playerView.initializeViews(getResources(), getPackageName());
```

<br>

### Step 7
> After adding ExoPlayer to the project and intializing it then use it with the EnhancedPlayerView like this
```java
binding.playerView.setPlayer(player);
player.setMediaSource(binding.playerView.EnableCaching(mediaUrl));
player.prepare();
```

<br>

### Step 8
> Add this Code in onResume of your Activity/Fragment
```java
binding.playerView.onResumeActivity();
```

> Add this code in onPause of your Activity/Fragment
```java
binding.playerView.onPauseActivity();
```

> Add this code in onDestory of your Activity/Fragment
```java
binding.playerView.onDestroyActivity();
if (player != null) {
    binding.playerView.setPlayer(null);
    player.release();
}
```

<br>

### OPTIONAL
> If you are looking for video orientation based on phone orientation then Add this code in your Activity/Fragment
```java
@Override
public void onConfigurationChanged(@NonNull Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    binding.playerView.onConfigurationChangedActivity(newConfig);
}
```

<br>

### OPTIONAL
> If tou are looking for PIP for the video then Add this code in your Activity/Fragment
```java
@Override
public void onUserLeaveHint() {
    binding.playerView.onUserLeaveHintActivity();
}

@Override
public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
    super.onPictureInPictureModeChanged(isInPictureInPictureMode);
    binding.playerView.onPictureInPictureModeChanged(isInPictureInPictureMode);
}
```


### IMPORTANT
**If you want to add new features to the library, you can change and add codes to the library. but when you add a new code then use a comment to explain the code for other developers to make the things easy to understand.**
