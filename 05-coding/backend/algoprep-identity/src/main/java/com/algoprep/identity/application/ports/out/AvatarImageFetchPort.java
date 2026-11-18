package com.algoprep.identity.application.ports.out;

import java.util.Optional;

/**
 * Downloads a provider avatar (BD security F1 section 6.5). The implementation owns every network
 * safety rule (scheme, host list, resolved address, redirects, timeouts, size cap) and answers
 * empty for any refusal or failure; it never throws and never logs the full URL.
 */
public interface AvatarImageFetchPort {

  Optional<byte[]> fetch(String url);
}
