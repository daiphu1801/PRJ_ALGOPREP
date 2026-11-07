package com.algoprep.identity.application.dto;

/** Response of #5: where to send the browser, and the {@code state} that must come back. */
public record OAuthStart(String authorizationUrl, String state) {}
