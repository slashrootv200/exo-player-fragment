package com.github.slashrootv200.exoplayerfragment;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.UUID;

public class ExoPlayerFragmentModel implements Parcelable {
  public static final Creator<ExoPlayerFragmentModel> CREATOR =
      new Creator<ExoPlayerFragmentModel>() {
        @Override
        public ExoPlayerFragmentModel createFromParcel(Parcel source) {
          return new ExoPlayerFragmentModel(source);
        }

        @Override
        public ExoPlayerFragmentModel[] newArray(int size) {
          return new ExoPlayerFragmentModel[size];
        }
      };
  private Uri[] uris;
  private String videoTitle;
  private long resumePosition;
  private boolean preferExtensionDecoders;
  private String drmLicenseUrl;
  private String[] keyRequestPropertiesArray;
  private String action;
  private String[] extensions;
  private UUID drmSchemeUUID;

  public ExoPlayerFragmentModel() {
  }

  protected ExoPlayerFragmentModel(Parcel in) {
    this.uris = in.createTypedArray(Uri.CREATOR);
    this.videoTitle = in.readString();
    this.resumePosition = in.readLong();
    this.preferExtensionDecoders = in.readByte() != 0;
    this.drmLicenseUrl = in.readString();
    this.keyRequestPropertiesArray = in.createStringArray();
    this.action = in.readString();
    this.extensions = in.createStringArray();
    this.drmSchemeUUID = (UUID) in.readSerializable();
  }

  public boolean isPreferExtensionDecoders() {
    return preferExtensionDecoders;
  }

  public void setPreferExtensionDecoders(boolean preferExtensionDecoders) {
    this.preferExtensionDecoders = preferExtensionDecoders;
  }

  public String getDrmLicenseUrl() {
    return drmLicenseUrl;
  }

  public void setDrmLicenseUrl(String drmLicenseUrl) {
    this.drmLicenseUrl = drmLicenseUrl;
  }

  public String[] getKeyRequestPropertiesArray() {
    return keyRequestPropertiesArray;
  }

  public void setKeyRequestPropertiesArray(String[] keyRequestPropertiesArray) {
    this.keyRequestPropertiesArray = keyRequestPropertiesArray;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String[] getExtensions() {
    return extensions;
  }

  public void setExtensions(String[] extensions) {
    this.extensions = extensions;
  }

  public long getResumePosition() {
    return resumePosition;
  }

  public void setResumePosition(long resumePosition) {
    this.resumePosition = resumePosition;
  }

  public Uri[] getUris() {
    return uris;
  }

  public void setUris(Uri[] uris) {
    this.uris = uris;
  }

  public String getVideoTitle() {
    return videoTitle;
  }

  public void setVideoTitle(String videoTitle) {
    this.videoTitle = videoTitle;
  }

  public UUID getDrmSchemeUUID() {
    return drmSchemeUUID;
  }

  public void setDrmSchemeUUID(UUID drmSchemeUUID) {
    this.drmSchemeUUID = drmSchemeUUID;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeTypedArray(this.uris, flags);
    dest.writeString(this.videoTitle);
    dest.writeLong(this.resumePosition);
    dest.writeByte(this.preferExtensionDecoders ? (byte) 1 : (byte) 0);
    dest.writeString(this.drmLicenseUrl);
    dest.writeStringArray(this.keyRequestPropertiesArray);
    dest.writeString(this.action);
    dest.writeStringArray(this.extensions);
    dest.writeSerializable(this.drmSchemeUUID);
  }
}
