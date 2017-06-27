package com.github.slashrootv200.exoplayerfragment;

public class ExoErrorEvent {
  public final String url;
  public final String error;

  public ExoErrorEvent(String url, String error) {
    this.url = url;
    this.error = error;
  }
}
