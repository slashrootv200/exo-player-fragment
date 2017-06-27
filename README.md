# exo-player-fragment
[ ![Download](https://api.bintray.com/packages/slashroot-v200/exo-player-fragment/exo-player-fragment/images/download.svg) ](https://bintray.com/slashroot-v200/exo-player-fragment/exo-player-fragment/_latestVersion)

Library which contains ExoPlayer(https://github.com/google/ExoPlayer) inside a Fragment, so that it is easier to use on an Activity

The Fragment use the code from the demo player provided in https://github.com/google/ExoPlayer repository.

Note: The `ExoPlayerFragmanet` extends support [Fragment.java](https://developer.android.com/reference/android/support/v4/app/Fragment.html) and therefore requires support [FragmentManager.java](https://developer.android.com/reference/android/support/v4/app/FragmentManager.html)

## Download
### maven
```xml
<dependency>
  <groupId>com.github.slashrootv200</groupId>
  <artifactId>exo-player-fragment-lib</artifactId>
  <version>0.0.3</version>
  <type>pom</type>
</dependency>
```

### gradle
```groovy
compile 'com.github.slashrootv200:exo-player-fragment-lib:0.0.3'
```
## Configuration
If you don't want the track selection for video, audio, text to be shown on the ExoPlayerFragment then override these boolean resources 

`res/values/{some_file_name_of_your_choice}.xml`

```xml
<bool name="exo_fragment_lib_show_video_selector">true</bool>
<bool name="exo_fragment_lib_show_audio_selector">false</bool>
<bool name="exo_fragment_lib_show_text_selector">false</bool>
```

By default all the values are true so you will see all the three choices if they are available.

## Usage
Example: In onCreate of an Activity.

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
  super.onCreate(savedInstanceState);
  setContentView(R.layout.activity_main);
  if (savedInstanceState == null) {
    Uri videoUri = Uri.parse("http://playertest.longtailvideo.com/adaptive/oceans_aes/oceans_aes.m3u8");
    String videoTitle = "Sample Video";
    mExoPlayerFragment = ExoPlayerFragment.newInstance(videoUri, videoTitle);
    getSupportFragmentManager().beginTransaction()
        .add(R.id.main_container, mExoPlayerFragment, ExoPlayerFragment.TAG)
        .commit();
  }
}
```

If you do not have any Base Activity class then you can also use `BaseExoPlayerActivity.java` as base class for your Activity which contains the Exo player. Please refer the demo app in the git repository for more information.

`ExoPlayerFragment` can be instantiated in using one of the four static methods:
1. `ExoPlayerFragment.newInstance(Uri uri)`-> uri = video uri
1. `ExoPlayerFragment.newInstance(Uri uri, String videoTitle)`-> videoTitle = video title
1. `ExoPlayerFragment.newInstance(Uri uri, int dialogTheme)`-> dialogTheme = theme resource Id for the dialogs created for track selection 
1. `ExoPlayerFragment.newInstance(Uri uri, String videoTitle, int dialogTheme)`-> videoTitle = video title and dialogTheme = theme resource Id for the dialogs created for track selection 

Instead of passing `int dialogTheme` you can also override theme by name `ExoPlayerFragmentTrackSelectionDialogTheme` in your application's `styles.xml` to override the dialog theme
