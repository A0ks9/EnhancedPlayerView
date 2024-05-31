# EnhancedPlayerView
EnhancedPlayerView is a good idea for you to improve your design and make things more easy with Media3 PlayerView

[![StandWithPalestine](https://raw.githubusercontent.com/karim-eg/StandWithPalestine/main/assets/palestine_badge.svg)](https://github.com/karim-eg/StandWithPalestine)

<br>
[![StandWithPalestine](https://raw.githubusercontent.com/karim-eg/StandWithPalestine/main/assets/palestine_banner.svg)](https://github.com/karim-eg/StandWithPalestine/blob/main/Donate.md)
<br>

##Usage

##Step 1
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
    implementation 'implementation 'com.github.A0ks9:EnhancedPlayerView:1.0.0-Beta'
}
```

##Step 2

##Step 3
> Add this widget to your `xml` activity file
```  <com.better.player.EnhancedPlayerView
        android:id="@+id/name_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:animation_enabled="false"
        app:controller_layout_id="@layout/exo_control_view"
        app:auto_show="true"
        app:hide_by_touching="true"
        app:hide_during_ads="true"
        app:hide_on_touch="false"
        app:hide_time_out="5000"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:show_close="true"
        app:show_floating_text="true"
        app:show_timeout="0" />
```
**important**
- you should add without changing anything
```
app:animation_enabled="false"
app:hide_on_touch="false"
app:show_timeout="0"
app:controller_layout_id="@layout/exo_control_view"
```

