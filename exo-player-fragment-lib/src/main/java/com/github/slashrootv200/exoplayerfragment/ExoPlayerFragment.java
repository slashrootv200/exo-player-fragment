package com.github.slashrootv200.exoplayerfragment;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.source.BehindLiveWindowException;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.UUID;
import org.greenrobot.eventbus.EventBus;

import static com.google.android.exoplayer2.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER;

public class ExoPlayerFragment extends Fragment
    implements View.OnClickListener, ExoPlayer.EventListener,
    PlaybackControlView.VisibilityListener {
  public static final String TAG = ExoPlayerFragment.class.getCanonicalName();
  public static final String DRM_SCHEME_UUID_EXTRA = "drm_scheme_uuid";
  public static final String DRM_LICENSE_URL = "drm_license_url";
  public static final String DRM_KEY_REQUEST_PROPERTIES = "drm_key_request_properties";
  public static final String PREFER_EXTENSION_DECODERS = "prefer_extension_decoders";
  public static final String ACTION_VIEW = "com.google.android.exoplayer.demo.action.VIEW";
  public static final String ACTION = "ACTION";
  public static final String EXTENSION_EXTRA = "extension";
  public static final String ACTION_VIEW_LIST =
      "com.google.android.exoplayer.demo.action.VIEW_LIST";
  public static final String URI_EXTRA = "URI_EXTRA";
  public static final String URI_LIST_EXTRA = "uri_list";
  public static final String EXTENSION_LIST_EXTRA = "extension_list";
  public static final String EXTRA_VIDEO_TITLE = "EXTRA_VIDEO_TITLE";
  private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
  private static final CookieManager DEFAULT_COOKIE_MANAGER;

  static {
    DEFAULT_COOKIE_MANAGER = new CookieManager();
    DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
  }

  private SimpleExoPlayerView simpleExoPlayerView;
  private LinearLayout debugRootView;
  private Button retryButton;
  private View rootView;
  private TextView mErrorTv;
  private ProgressBar mProgressBar;
  private ImageButton mFullscreenIb;
  private TextView mVideoTitleTv;
  private Handler mainHandler;
  private EventLogger eventLogger;
  private DataSource.Factory mediaDataSourceFactory;
  private SimpleExoPlayer player;
  private DefaultTrackSelector trackSelector;
  private TrackSelectionHelper trackSelectionHelper;
  private boolean needRetrySource;
  private TrackGroupArray lastSeenTrackGroupArray;
  private boolean shouldAutoPlay;
  private int resumeWindow;
  private long resumePosition;
  private boolean showAudioVideoAndTextButtons = true;
  private boolean mIsReturnedFromSharing;
  private boolean isFullscreen;
  private Uri mDelayedVideoUri = null;
  private String mDelayedVideoTitle = null;
  private boolean notifyErrorOnResume = false;
  private boolean mIsRestored = false;
  private ExoPlayerFragmentModel m;

  // fragment lifecycle
  private static boolean isBehindLiveWindow(ExoPlaybackException e) {
    if (e.type != ExoPlaybackException.TYPE_SOURCE) {
      return false;
    }
    Throwable cause = e.getSourceException();
    while (cause != null) {
      if (cause instanceof BehindLiveWindowException) {
        return true;
      }
      cause = cause.getCause();
    }
    return false;
  }

  public static ExoPlayerFragment newInstance(Uri uri) {
    Log.d(TAG, "entering newInstance with uri=" + uri);
    Bundle args = new Bundle();
    args.putBoolean(PREFER_EXTENSION_DECODERS, true);
    args.putString(ACTION, ACTION_VIEW);
    args.putParcelable(URI_EXTRA, uri);
    ExoPlayerFragment f = new ExoPlayerFragment();
    f.setArguments(args);
    return f;
  }

  public static ExoPlayerFragment newInstance(Uri uri, String videoTitle) {
    Log.d(TAG, "entering newInstance with uri=" + uri + ",videoTitle=" + videoTitle);
    Bundle args = new Bundle();
    args.putBoolean(PREFER_EXTENSION_DECODERS, true);
    args.putString(ACTION, ACTION_VIEW);
    args.putParcelable(URI_EXTRA, uri);
    args.putString(EXTRA_VIDEO_TITLE, videoTitle);
    ExoPlayerFragment f = new ExoPlayerFragment();
    f.setArguments(args);
    return f;
  }

  public void setVideoUriAndTitle(Uri videoUri, String videoTitle) {
    dLog("entering setVideoUri with videoTitle=" + videoTitle + ",videoUri=" + videoUri);
    if (mVideoTitleTv == null) {
      mDelayedVideoUri = videoUri;
      mDelayedVideoTitle = videoTitle;
    } else {
      getFragmentModel().setVideoTitle(videoTitle);
      if (!TextUtils.isEmpty(getFragmentModel().getVideoTitle())) {
        mVideoTitleTv.setText(getFragmentModel().getVideoTitle());
      } else {
        mVideoTitleTv.setText("");
      }
      setVideoUri(videoUri);
    }
  }

  public void setVideoUri(Uri videoUri) {
    dLog("entering setVideoUri with videoUri=" + videoUri);
    Bundle args = getArguments();
    String[] extensions = new String[] { args.getString(EXTENSION_EXTRA) };
    getFragmentModel().setUris(new Uri[] { videoUri });
    if (Util.maybeRequestReadExternalStoragePermission(getActivity(),
        getFragmentModel().getUris())) {
      // The player will be reinitialized if the permission is granted.
      return;
    }
    MediaSource[] mediaSources = new MediaSource[getFragmentModel().getUris().length];
    for (int i = 0; i < getFragmentModel().getUris().length; i++) {
      mediaSources[i] = buildMediaSource(getFragmentModel().getUris()[i], extensions[i]);
    }
    MediaSource mediaSource =
        mediaSources.length == 1 ? mediaSources[0] : new ConcatenatingMediaSource(mediaSources);
    boolean haveResumePosition = resumeWindow != C.INDEX_UNSET;
    if (haveResumePosition) {
      player.seekTo(resumeWindow, resumePosition);
    }
    player.prepare(mediaSource, !haveResumePosition, false);
    needRetrySource = false;
    showHideView(mErrorTv, false);
    updateButtonVisibilities();
  }

  protected Class<?> getFragmentModelClass() {
    return ExoPlayerFragmentModel.class;
  }

  protected ExoPlayerFragmentModel instantiateFragmentModel() {
    return new ExoPlayerFragmentModel();
  }

  private void initView(View v) {
    simpleExoPlayerView = (SimpleExoPlayerView) v.findViewById(R.id.player_view);
    debugRootView = (LinearLayout) v.findViewById(R.id.controls_root);
    retryButton = (Button) v.findViewById(R.id.retry_button);
    rootView = v.findViewById(R.id.root);
    mErrorTv = (TextView) v.findViewById(R.id.f_exo_error_tv);
    mProgressBar = (ProgressBar) v.findViewById(R.id.f_exo_progress_bar);
    mFullscreenIb = (ImageButton) v.findViewById(R.id.exo_full_screen);
    mVideoTitleTv = (TextView) v.findViewById(R.id.video_title);
    v.findViewById(R.id.exo_full_screen).setOnClickListener(view -> onFullScreenClicked());
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getFragmentModelClass() != null) {
      if (savedInstanceState == null) {
        if (m == null) { // just in case where the instantiater set the model
          m = instantiateFragmentModel();
        }
      } else {
        m = savedInstanceState.getParcelable(getFragmentModelClass().getCanonicalName());
      }
    }
    shouldAutoPlay = false;
    clearResumePosition();
    mediaDataSourceFactory = buildDataSourceFactory(true);
    mainHandler = new Handler();
    if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
      CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
    }
    if (savedInstanceState == null) {
      getDataFromArguments();
    }
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater,
      @Nullable
          ViewGroup container,
      @Nullable
          Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.fragment_exo_player, container, false);
    initView(v);
    return v;
  }

  @Override
  public void onViewCreated(View view,
      @Nullable
          Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    rootView.setOnClickListener(this);
    retryButton.setOnClickListener(this);
    rootView.setOnTouchListener((view1, motionEvent) -> {
      onPlayerViewTouch(view1, motionEvent);
      return false;
    });
    simpleExoPlayerView.setOnTouchListener((view12, motionEvent) -> {
      onPlayerViewTouch(view12, motionEvent);
      return false;
    });
    simpleExoPlayerView.setControllerVisibilityListener(this);
    simpleExoPlayerView.requestFocus();
    mIsRestored = savedInstanceState != null;
  }

  @Override
  public void onStart() {
    super.onStart();
    if (Util.SDK_INT > 23) {
      initializePlayer();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    if ((Util.SDK_INT <= 23 || player == null)) {
      initializePlayer();
    }
    if (mDelayedVideoUri != null && mDelayedVideoTitle != null) {
      setVideoUriAndTitle(mDelayedVideoUri, mDelayedVideoTitle);
      mDelayedVideoTitle = null;
      mDelayedVideoUri = null;
    }
    if (mIsReturnedFromSharing) {
      mIsReturnedFromSharing = false;
      stopSpinner();
    }
    if (notifyErrorOnResume) {
      EventBus.getDefault()
          .post(new ExoErrorEvent(getFragmentModel().getUris()[0].toString(), null));
      notifyErrorOnResume = false;
    }
  }

  public void startSpinner(boolean isCancellable) {
    dLog("entering startSpinner with isCancellable = " + isCancellable);
    showHideView(mProgressBar, true);
  }

  public void stopSpinner() {
    dLog("entering stopSpinner");
    showHideView(mProgressBar, false);
  }

  @Override
  public void onPause() {
    super.onPause();
    if (Util.SDK_INT <= 23) {
      releasePlayer();
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    if (Util.SDK_INT > 23) {
      releasePlayer();
    }
  }

  // Fragment input
  @Override
  public void onRequestPermissionsResult(int requestCode,
      @NonNull
          String[] permissions,
      @NonNull
          int[] grantResults) {
    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      initializePlayer();
    } else {
      toastShort(getString(R.string.storage_permission_denied));
      showHideView(mErrorTv, true);
      mErrorTv.setText(getString(R.string.storage_permission_denied));
    }
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
  }

  private void onFullScreenClicked() {
    isFullscreen = !isFullscreen;
    if (isFullscreen) {
      mFullscreenIb.setImageResource(R.drawable.ic_fullscreen_exit);
      getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
      debugRootView.setVisibility(View.GONE);
    } else {
      mFullscreenIb.setImageResource(R.drawable.ic_fullscreen);
      getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
      debugRootView.setVisibility(View.VISIBLE);
    }
  }

  @Override
  public void onClick(View view) {
    if (view == retryButton) {
      initializePlayer();
    } else if (view.getParent() == debugRootView) {
      MappingTrackSelector.MappedTrackInfo mappedTrackInfo =
          trackSelector.getCurrentMappedTrackInfo();
      if (mappedTrackInfo != null) {
        trackSelectionHelper.showSelectionDialog(getActivity(), ((Button) view).getText(),
            trackSelector.getCurrentMappedTrackInfo(), (int) view.getTag());
      }
    }
  }

  @Override
  public void onVisibilityChange(int visibility) {
    if (!isFullscreen) debugRootView.setVisibility(visibility);
  }

  private void initializePlayer() {
    boolean needNewPlayer = player == null;
    if (needNewPlayer) {
      DrmSessionManager<FrameworkMediaCrypto> drmSessionManager = null;
      if (getFragmentModel().getDrmSchemeUUID() != null) {
        try {
          drmSessionManager = buildDrmSessionManager(getFragmentModel().getDrmSchemeUUID(),
              getFragmentModel().getDrmLicenseUrl(),
              getFragmentModel().getKeyRequestPropertiesArray());
        } catch (UnsupportedDrmException e) {
          int errorStringId = Util.SDK_INT < 18 ? R.string.error_drm_not_supported
              : (e.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
                  ? R.string.error_drm_unsupported_scheme : R.string.error_drm_unknown);
          toastShort(getString(errorStringId));
          return;
        }
      }
      DefaultRenderersFactory renderersFactory =
          new DefaultRenderersFactory(getActivity(), drmSessionManager,
              EXTENSION_RENDERER_MODE_PREFER);

      TrackSelection.Factory videoTrackSelectionFactory =
          new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
      trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
      trackSelectionHelper = new TrackSelectionHelper(trackSelector, videoTrackSelectionFactory);
      lastSeenTrackGroupArray = null;

      player = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector);
      player.addListener(this);

      eventLogger = new EventLogger(trackSelector);
      player.addListener(eventLogger);
      player.setAudioDebugListener(eventLogger);
      player.setVideoDebugListener(eventLogger);
      player.setMetadataOutput(eventLogger);

      simpleExoPlayerView.setPlayer(player);
      player.setPlayWhenReady(shouldAutoPlay);
    }
    if (needNewPlayer || needRetrySource) {
      String action = getFragmentModel().getAction();
      if (ACTION_VIEW.equals(action)) {
        if (TextUtils.isEmpty(getFragmentModel().getVideoTitle())) {
          mVideoTitleTv.setText("");
        } else {
          mVideoTitleTv.setText(getFragmentModel().getVideoTitle());
        }
      } else if (ACTION_VIEW_LIST.equals(action)) {
      } else {
        showHideView(mErrorTv, true);
        mErrorTv.setText(getString(R.string.unexpected_intent_action, action));
        toastShort(getString(R.string.unexpected_intent_action, action));
        return;
      }
      if (Util.maybeRequestReadExternalStoragePermission(getActivity(),
          getFragmentModel().getUris())) {
        // The player will be reinitialized if the permission is granted.
        return;
      }
      MediaSource[] mediaSources = new MediaSource[getFragmentModel().getUris().length];
      for (int i = 0; i < getFragmentModel().getUris().length; i++) {
        mediaSources[i] = buildMediaSource(getFragmentModel().getUris()[i],
            getFragmentModel().getExtensions()[i]);
      }
      MediaSource mediaSource =
          mediaSources.length == 1 ? mediaSources[0] : new ConcatenatingMediaSource(mediaSources);
      boolean haveResumePosition = resumeWindow != C.INDEX_UNSET;
      if (haveResumePosition) {
        player.seekTo(resumeWindow, resumePosition);
      }
      if (getFragmentModel().getResumePosition() > 0L) {
        player.seekTo(getFragmentModel().getResumePosition());
      }
      player.prepare(mediaSource, !haveResumePosition, false);
      needRetrySource = false;
      showHideView(mErrorTv, false);
      updateButtonVisibilities();
    }
  }

  public boolean getCurrentPlaybackState() {
    dLog("entering getCurrentPlaybackState");
    if (simpleExoPlayerView != null && simpleExoPlayerView.getPlayer() != null) {
      dLog("simpleExoPlayerView != null && simpleExoPlayerView.getPlayer() != null is true");
      return simpleExoPlayerView.getPlayer().getPlayWhenReady();
    } else {
      dLog("simpleExoPlayerView != null && simpleExoPlayerView.getPlayer() != null is false");
      return false;
    }
  }

  public void pauseVideo() {
    dLog("entering pauseVideo");
    if (simpleExoPlayerView != null && simpleExoPlayerView.getPlayer() != null) {
      dLog("simpleExoPlayerView != null && simpleExoPlayerView.getPlayer() != null is true");
      simpleExoPlayerView.getPlayer().setPlayWhenReady(false);
    } else {
      dLog("simpleExoPlayerView != null && simpleExoPlayerView.getPlayer() != null is false");
    }
  }

  public void resumeVideo() {
    dLog("entering resumeVideo");
    if (simpleExoPlayerView != null && simpleExoPlayerView.getPlayer() != null) {
      dLog("simpleExoPlayerView != null && simpleExoPlayerView.getPlayer() != null is true");
      simpleExoPlayerView.getPlayer().setPlayWhenReady(true);
    } else {
      dLog("simpleExoPlayerView != null && simpleExoPlayerView.getPlayer() != null is false");
    }
  }

  private MediaSource buildMediaSource(Uri uri, String overrideExtension) {
    int type = TextUtils.isEmpty(overrideExtension) ? Util.inferContentType(uri)
        : Util.inferContentType("." + overrideExtension);
    switch (type) {
      case C.TYPE_SS:
        return new SsMediaSource(uri, buildDataSourceFactory(false),
            new DefaultSsChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger);
      case C.TYPE_DASH:
        return new DashMediaSource(uri, buildDataSourceFactory(false),
            new DefaultDashChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger);
      case C.TYPE_HLS:
        return new HlsMediaSource(uri, mediaDataSourceFactory, mainHandler, eventLogger);
      case C.TYPE_OTHER:
        return new ExtractorMediaSource(uri, mediaDataSourceFactory, new DefaultExtractorsFactory(),
            mainHandler, eventLogger);
      default: {
        throw new IllegalStateException("Unsupported type: " + type);
      }
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    if (simpleExoPlayerView != null && simpleExoPlayerView.getPlayer() != null) {
      getFragmentModel().setResumePosition(simpleExoPlayerView.getPlayer().getCurrentPosition());
    }
    if (getFragmentModelClass() != null) {
      outState.putParcelable(getFragmentModelClass().getCanonicalName(), m);
    }
    super.onSaveInstanceState(outState);
  }

  private DrmSessionManager<FrameworkMediaCrypto> buildDrmSessionManager(UUID uuid,
      String licenseUrl, String[] keyRequestPropertiesArray) throws
      UnsupportedDrmException {
    if (Util.SDK_INT < 18) {
      return null;
    }
    HttpMediaDrmCallback drmCallback =
        new HttpMediaDrmCallback(licenseUrl, buildHttpDataSourceFactory(false));
    if (keyRequestPropertiesArray != null) {
      for (int i = 0; i < keyRequestPropertiesArray.length - 1; i += 2) {
        drmCallback.setKeyRequestProperty(keyRequestPropertiesArray[i],
            keyRequestPropertiesArray[i + 1]);
      }
    }
    return new DefaultDrmSessionManager<>(uuid, FrameworkMediaDrm.newInstance(uuid), drmCallback,
        null, mainHandler, eventLogger);
  }

  private void releasePlayer() {
    if (player != null) {
      shouldAutoPlay = player.getPlayWhenReady();
      updateResumePosition();
      player.release();
      player = null;
      trackSelector = null;
      trackSelectionHelper = null;
      eventLogger = null;
    }
  }

  private void updateResumePosition() {
    resumeWindow = player.getCurrentWindowIndex();
    resumePosition =
        player.isCurrentWindowSeekable() ? Math.max(0, player.getCurrentPosition()) : C.TIME_UNSET;
  }

  private void clearResumePosition() {
    resumeWindow = C.INDEX_UNSET;
    resumePosition = C.TIME_UNSET;
  }

  /**
   * Returns a new DataSource factory.
   *
   * DataSource factory.
   *
   * @return A new DataSource factory.
   */
  private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
    return buildDataSourceFactory(useBandwidthMeter ? BANDWIDTH_METER : null);
  }

  public DataSource.Factory buildDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
    return new DefaultDataSourceFactory(getContext(), bandwidthMeter,
        buildHttpDataSourceFactory(bandwidthMeter));
  }

  public HttpDataSource.Factory buildHttpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
    return new DefaultHttpDataSourceFactory("exo-player", bandwidthMeter);
  }

  /**
   * Returns a new HttpDataSource factory.
   *
   * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
   * DataSource factory.
   * @return A new HttpDataSource factory.
   */

  private HttpDataSource.Factory buildHttpDataSourceFactory(boolean useBandwidthMeter) {
    return buildHttpDataSourceFactory(useBandwidthMeter ? BANDWIDTH_METER : null);
  }

  @Override
  public void onLoadingChanged(boolean isLoading) {
    // Do nothing.
  }

  private void dLog(String message) {
    Log.d(TAG, message);
  }

  @Override
  public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
    switch (playbackState) {
      case ExoPlayer.STATE_IDLE: {
        dLog("onPlayerStateChanged with playbackState = ExoPlayer.STATE_IDLE");
        showHideView(mProgressBar, false);
      }
      break;
      case ExoPlayer.STATE_BUFFERING: {
        dLog("onPlayerStateChanged with playbackState = ExoPlayer.STATE_BUFFERING");
        showHideView(mProgressBar, true);
      }
      break;
      case ExoPlayer.STATE_READY: {
        dLog("onPlayerStateChanged with playbackState = ExoPlayer.STATE_READY");
        showHideView(mProgressBar, false);
      }
      break;
      case ExoPlayer.STATE_ENDED: {
        dLog("onPlayerStateChanged with playbackState = ExoPlayer.STATE_ENDED");
        showHideView(mProgressBar, false);
        showControls();
        EventBus.getDefault().post(new ExoVideoEndedEvent());
      }
      break;
      default: {
        dLog("onPlayerStateChanged with playbackState = " + playbackState);
      }
      break;
    }
    updateButtonVisibilities();
  }

  @Override
  public void onPositionDiscontinuity() {
    if (needRetrySource) {
      // This will only occur if the user has performed a seek whilst in the error state. Update the
      // resume position so that if the user then retries, playback will resume from the position to
      // which they seeked.
      updateResumePosition();
    }
  }

  @Override
  public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
    // Do nothing.
  }

  public void showHideView(View v, boolean show) {
    if (show) {
      v.setVisibility(View.VISIBLE);
    } else {
      v.setVisibility(View.GONE);
    }
  }

  @Override
  public void onTimelineChanged(Timeline timeline, Object manifest) {
    // Do nothing.
  }

  @Override
  public void onPlayerError(ExoPlaybackException e) {
    String errorString = null;
    if (isResumed()) {
      EventBus.getDefault()
          .post(new ExoErrorEvent(getFragmentModel().getUris()[0].toString(), e.getMessage()));
    } else {
      notifyErrorOnResume = true;
    }
    if (e.type == ExoPlaybackException.TYPE_RENDERER) {
      Exception cause = e.getRendererException();
      if (cause instanceof MediaCodecRenderer.DecoderInitializationException) {
        // Special case for decoder initialization failures.
        MediaCodecRenderer.DecoderInitializationException decoderInitializationException =
            (MediaCodecRenderer.DecoderInitializationException) cause;
        if (decoderInitializationException.decoderName == null) {
          if (decoderInitializationException.getCause() instanceof MediaCodecUtil.DecoderQueryException) {
            errorString = getString(R.string.error_querying_decoders);
          } else if (decoderInitializationException.secureDecoderRequired) {
            errorString = getString(R.string.error_no_secure_decoder,
                decoderInitializationException.mimeType);
          } else {
            errorString =
                getString(R.string.error_no_decoder, decoderInitializationException.mimeType);
          }
        } else {
          errorString = getString(R.string.error_instantiating_decoder,
              decoderInitializationException.decoderName);
        }
      }
    }
    if (errorString != null) {
      showHideView(mErrorTv, true);
      toastShort(errorString);
    }
    needRetrySource = true;
    if (isBehindLiveWindow(e)) {
      clearResumePosition();
      initializePlayer();
    } else {
      updateResumePosition();
      updateButtonVisibilities();
      showControls();
    }
  }

  public void toastShort(String message) {
    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
  }

  // User controls
  @Override
  @SuppressWarnings("ReferenceEquality")
  public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
    updateButtonVisibilities();
    if (trackGroups != lastSeenTrackGroupArray) {
      MappingTrackSelector.MappedTrackInfo mappedTrackInfo =
          trackSelector.getCurrentMappedTrackInfo();
      if (mappedTrackInfo != null) {
        if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_VIDEO)
            == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
          toastShort(getString(R.string.error_unsupported_video));
        }
        if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_AUDIO)
            == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
          toastShort(getString(R.string.error_unsupported_audio));
        }
      }
      lastSeenTrackGroupArray = trackGroups;
    }
  }

  public ExoPlayerFragmentModel getFragmentModel() {
    return this.m;
  }

  public void setFragmentModel(ExoPlayerFragmentModel m) {
    this.m = m;
  }

  private void updateButtonVisibilities() {
    debugRootView.removeAllViews();
    retryButton.setVisibility(needRetrySource ? View.VISIBLE : View.GONE);
    debugRootView.addView(retryButton);
    if (player == null) {
      return;
    }
    if (showAudioVideoAndTextButtons) {
      MappingTrackSelector.MappedTrackInfo mappedTrackInfo =
          trackSelector.getCurrentMappedTrackInfo();
      if (mappedTrackInfo == null) {
        return;
      }
      for (int i = 0; i < mappedTrackInfo.length; i++) {
        TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(i);
        if (trackGroups.length != 0) {
          Button button = new Button(getActivity());
          int label;
          switch (player.getRendererType(i)) {
            case C.TRACK_TYPE_AUDIO:
              label = R.string.audio;
              break;
            case C.TRACK_TYPE_VIDEO:
              label = R.string.video;
              break;
            case C.TRACK_TYPE_TEXT:
              label = R.string.text;
              break;
            default:
              continue;
          }
          button.setText(label);
          button.setTag(i);
          button.setOnClickListener(this);
          debugRootView.addView(button, debugRootView.getChildCount() - 1);
        }
      }
    }
  }

  private void showControls() {
    debugRootView.setVisibility(View.VISIBLE);
  }

  public boolean onPlayerViewTouch(View v, MotionEvent event) {
    // Show the controls on any key event.
    simpleExoPlayerView.showController();
    // If the event was not handled then see if the
    // player view can handle it as a media key event.
    //TODO
    //simpleExoPlayerView.dispatchMediaKeyEvent(event);
    return false;
  }

  private void getDataFromArguments() {
    Bundle args = getArguments();
    if (args.containsKey(PREFER_EXTENSION_DECODERS)) {
      getFragmentModel().setPreferExtensionDecoders(
          args.getBoolean(PREFER_EXTENSION_DECODERS, false));
    }
    if (args.containsKey(DRM_LICENSE_URL)) {
      getFragmentModel().setDrmLicenseUrl(args.getString(DRM_LICENSE_URL));
    }
    if (args.containsKey(DRM_KEY_REQUEST_PROPERTIES)) {
      getFragmentModel().setKeyRequestPropertiesArray(
          args.getStringArray(DRM_KEY_REQUEST_PROPERTIES));
    }
    if (args.containsKey(EXTENSION_LIST_EXTRA)) {
      getFragmentModel().setExtensions(args.getStringArray(EXTENSION_LIST_EXTRA));
    }
    if (args.containsKey(ACTION)) {
      getFragmentModel().setAction(args.getString(ACTION));
      if (ACTION_VIEW.equals(getFragmentModel().getAction())) {
        getFragmentModel().setUris(new Uri[] { args.getParcelable(URI_EXTRA) });
        if (getFragmentModel().getExtensions() == null) {
          getFragmentModel().setExtensions(new String[1]);
        }
      } else if (ACTION_VIEW_LIST.equals(getFragmentModel().getAction())) {
        String[] arr = args.getStringArray(URI_LIST_EXTRA);
        getFragmentModel().setUris(new Uri[arr.length]);
        for (int i = 0; i < arr.length; i++) {
          getFragmentModel().getUris()[i] = Uri.parse(arr[i]);
        }
        if (getFragmentModel().getExtensions() == null) {
          getFragmentModel().setExtensions(new String[arr.length]);
        }
      }
    }
    UUID drmSchemeUuid = args.containsKey(DRM_SCHEME_UUID_EXTRA) ? UUID.fromString(
        args.getString(DRM_SCHEME_UUID_EXTRA)) : null;
    getFragmentModel().setDrmSchemeUUID(drmSchemeUuid);
  }
}
