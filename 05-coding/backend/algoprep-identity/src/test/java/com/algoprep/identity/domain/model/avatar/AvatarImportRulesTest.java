package com.algoprep.identity.domain.model.avatar;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.identity.domain.model.auth.OAuthProvider;
import org.junit.jupiter.api.Test;

class AvatarImportRulesTest {
  private static String google(String url) {
    return AvatarImportRules.normalise(OAuthProvider.GOOGLE, url).orElseThrow();
  }

  private static String github(String url) {
    return AvatarImportRules.normalise(OAuthProvider.GITHUB, url).orElseThrow();
  }

  @Test
  void googleGetsOrReplacesTheSizeSuffix() {
    assertThat(google("https://lh3.googleusercontent.com/a/abc")).endsWith("/a/abc=s256-c");
    assertThat(google("https://lh3.googleusercontent.com/a/abc=s96-c")).endsWith("/a/abc=s256-c");
    assertThat(google("https://lh3.googleusercontent.com/a/abc=s96")).endsWith("/a/abc=s256-c");
  }

  @Test
  void githubGetsOrReplacesTheSizeQuery() {
    assertThat(github("https://avatars.githubusercontent.com/u/1"))
        .isEqualTo("https://avatars.githubusercontent.com/u/1?s=256");
    assertThat(github("https://avatars.githubusercontent.com/u/1?v=4"))
        .isEqualTo("https://avatars.githubusercontent.com/u/1?v=4&s=256");
    assertThat(github("https://avatars.githubusercontent.com/u/1?s=40&v=4"))
        .isEqualTo("https://avatars.githubusercontent.com/u/1?v=4&s=256");
  }

  @Test
  void aBlankUrlGivesNothing() {
    assertThat(AvatarImportRules.normalise(OAuthProvider.GOOGLE, " ")).isEmpty();
    assertThat(AvatarImportRules.normalise(OAuthProvider.GITHUB, null)).isEmpty();
  }
}
