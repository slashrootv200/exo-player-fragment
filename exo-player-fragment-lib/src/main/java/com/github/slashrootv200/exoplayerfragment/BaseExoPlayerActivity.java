package com.github.slashrootv200.exoplayerfragment;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public abstract class BaseExoPlayerActivity extends AppCompatActivity {
  private final String TAG = BaseExoPlayerActivity.class.getCanonicalName();
  private final int PLAY_STATE = 1;
  private final int PAUSE_STATE = 2;
  protected ExoPlayerFragment mExoPlayerFragment;
  private int mVideoPlaybackStateAtPause;
  private int mLayoutResId;
  private Object mExoErrorListener;
  private Object mExoVideoEndedListener;

  public BaseExoPlayerActivity(int layoutResId) {
    this.mLayoutResId = layoutResId;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(mLayoutResId);
    if (savedInstanceState != null) {
      mExoPlayerFragment =
          (ExoPlayerFragment) getSupportFragmentManager().findFragmentByTag(ExoPlayerFragment.TAG);
    }
  }

  private final void dLog(String message) {
    Log.d(TAG, message);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    dLog("entering onConfigurationChanged with newConfig=" + newConfig);
    super.onConfigurationChanged(newConfig);
    dLog("new orientation=" + newConfig.orientation);
    View decorView = getWindow().getDecorView();
    if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
      showHideActionBarIfPresent(true);
      showSystemUI(decorView);
      onPortrait();
    } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
      showHideActionBarIfPresent(false);
      hideSystemUI(decorView);
      onLandscape();
    }
  }

  private void showHideActionBarIfPresent(boolean show) {
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      if (show) {
        actionBar.show();
      } else {
        actionBar.hide();
      }
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    dLog("entering onResume");
    if (mVideoPlaybackStateAtPause == PLAY_STATE && mExoPlayerFragment != null) {
      mExoPlayerFragment.resumeVideo();
    }
    mExoErrorListener = new Object() {
      @SuppressWarnings("unused")
      @Subscribe(threadMode = ThreadMode.MAIN)
      public void onExoErrorReceived(ExoErrorEvent e) {
        BaseExoPlayerActivity.this.onExoErrorReceived(e);
      }
    };
    mExoVideoEndedListener = new Object() {
      @SuppressWarnings("unused")
      @Subscribe(threadMode = ThreadMode.MAIN)
      public void onExoVideoEnded(ExoVideoEndedEvent e) {
        BaseExoPlayerActivity.this.onExoVideoEndedEventReceived(e);
      }
    };
    EventBus.getDefault().register(mExoErrorListener);
    EventBus.getDefault().register(mExoVideoEndedListener);
  }

  @Override
  protected void onPause() {
    dLog("entering onPause");
    super.onPause();
    if (mExoPlayerFragment != null) {
      mVideoPlaybackStateAtPause =
          mExoPlayerFragment.getCurrentPlaybackState() ? PLAY_STATE : PAUSE_STATE;
      mExoPlayerFragment.pauseVideo();
    }
    EventBus.getDefault().unregister(mExoErrorListener);
    EventBus.getDefault().unregister(mExoVideoEndedListener);
    mExoErrorListener = null;
    dLog("exiting onPause");
  }

  void hideSystemUI(View decorView) {
    // Set the IMMERSIVE flag.
    // Set the content to appear under the system bars so that the content
    // doesn't resize when the system bars hide and show.
    decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        // hide nav bar
        | View.SYSTEM_UI_FLAG_FULLSCREEN
        // hide status bar
        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
  }

  // This snippet shows the system bars. It does this by removing all the flags
  // except for the ones that make the content appear under the system bars.
  void showSystemUI(View decorView) {
    decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
  }

  protected abstract void onPortrait();

  protected abstract void onLandscape();

  abstract public void onExoVideoEndedEventReceived(ExoVideoEndedEvent e);

  abstract public void onExoErrorReceived(ExoErrorEvent e);
}
