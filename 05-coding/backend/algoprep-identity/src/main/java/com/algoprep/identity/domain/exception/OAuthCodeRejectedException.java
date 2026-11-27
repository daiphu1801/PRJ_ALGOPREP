package com.algoprep.identity.domain.exception;

/** The provider refused the authorization code (expired, used or forged). */
public final class OAuthCodeRejectedException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public OAuthCodeRejectedException() {
    super("OAuth authorization code rejected");
  }
}
