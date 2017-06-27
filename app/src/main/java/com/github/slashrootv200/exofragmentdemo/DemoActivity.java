package com.github.slashrootv200.exofragmentdemo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import com.github.slashrootv200.exoplayerfragment.BaseExoPlayerActivity;
import com.github.slashrootv200.exoplayerfragment.ExoErrorEvent;
import com.github.slashrootv200.exoplayerfragment.ExoPlayerFragment;
import com.github.slashrootv200.exoplayerfragment.ExoVideoEndedEvent;

public class DemoActivity extends BaseExoPlayerActivity {
  private static final String EXTRA_VIDEO_URI = "EXTRA_VIDEO_URI";
  private static final String EXTRA_VIDEO_TITLE = "EXTRA_VIDEO_TITLE";

  public DemoActivity() {
    super(R.layout.activity_demo);
  }

  public static Intent getDemoActivityIntent(
      @NonNull
          Activity activity,
      @NonNull
          Uri videoUri) {
    Intent intent = new Intent(activity, DemoActivity.class);
    intent.putExtra(EXTRA_VIDEO_URI, videoUri);
    return intent;
  }

  public static Intent getDemoActivityIntent(
      @NonNull
          Activity activity,
      @NonNull
          Uri videoUri,
      @NonNull
          String videoTitle) {
    Intent intent = getDemoActivityIntent(activity, videoUri);
    intent.putExtra(EXTRA_VIDEO_TITLE, videoTitle);
    return intent;
  }

  private Data processIntent() {
    Intent intent = getIntent();
    Data data = new Data();
    data.videoUri = intent.getParcelableExtra(EXTRA_VIDEO_URI);
    data.videoTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE);
    return data;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_demo);
    if (savedInstanceState == null) {
      Data data = processIntent();
      Uri videoUri = data.isDataAvailable() ? data.videoUri : Uri.parse(
          "https://ndtvstream-lh.akamaihd" + "" + ".net/i/ndtv_india_1@300634/master.m3u8");
      String videoTitle = TextUtils.isEmpty(data.videoTitle) ? "NDTV इंडिया" : data.videoTitle;
      mExoPlayerFragment = ExoPlayerFragment.newInstance(videoUri, videoTitle);
      getSupportFragmentManager().beginTransaction()
          .add(R.id.main_container, mExoPlayerFragment, ExoPlayerFragment.TAG)
          .commit();
    }
  }

  @Override
  public void onExoVideoEndedEventReceived(ExoVideoEndedEvent e) {
    // TODO
  }

  @Override
  public void onExoErrorReceived(ExoErrorEvent e) {
    // TODO
  }

  @Override
  protected void onPortrait() {
    // TODO
  }

  @Override
  protected void onLandscape() {
    // TODO
  }

  static class Data {
    Uri videoUri;
    String videoTitle;

    boolean isDataAvailable() {
      return videoUri != null;
    }
  }
}
