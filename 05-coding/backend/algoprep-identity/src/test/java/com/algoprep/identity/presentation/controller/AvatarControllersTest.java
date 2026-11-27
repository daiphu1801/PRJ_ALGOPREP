package com.algoprep.identity.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.identity.application.dto.avatar.AvatarContent;
import com.algoprep.identity.application.ports.in.avatar.GetUserAvatarUseCase;
import com.algoprep.identity.application.ports.in.avatar.RemoveMyAvatarUseCase;
import com.algoprep.identity.application.ports.in.avatar.UploadMyAvatarUseCase;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;

/** Request shape of #74 to #76: one file part, caller from the token, cache headers. */
class AvatarControllersTest {
  private final UUID me = UUID.randomUUID();
  private final Jwt jwt =
      Jwt.withTokenValue("t").header("alg", "none").subject(me.toString()).build();
  private final UploadMyAvatarUseCase upload = mock(UploadMyAvatarUseCase.class);
  private final RemoveMyAvatarUseCase remove = mock(RemoveMyAvatarUseCase.class);
  private final MyAvatarController mine = new MyAvatarController(upload, remove);
  private final GetUserAvatarUseCase read = mock(GetUserAvatarUseCase.class);
  private final UserAvatarController users = new UserAvatarController(read);

  private static void assertCode(Throwable error, String code) {
    assertThat(error)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo(code));
  }

  @Test
  void theSingleFilePartIsUploadedForTheTokenSubject() {
    var request = new MockMultipartHttpServletRequest();
    request.addFile(new MockMultipartFile("file", "x.png", "image/png", new byte[] {1, 2}));
    when(upload.upload(eq(me), any())).thenReturn(77L);

    assertThat(mine.put(jwt, request).avatarVersion()).isEqualTo(77L);

    verify(upload).upload(me, new byte[] {1, 2});
  }

  @Test
  void anyOtherRequestShapeIs400IdtV32AndNothingIsUploaded() {
    var none = new MockMultipartHttpServletRequest();
    var twoParts = new MockMultipartHttpServletRequest();
    twoParts.addFile(new MockMultipartFile("file", new byte[] {1}));
    twoParts.addFile(new MockMultipartFile("file", new byte[] {2}));
    var otherName = new MockMultipartHttpServletRequest();
    otherName.addFile(new MockMultipartFile("avatar", new byte[] {1}));
    var extra = new MockMultipartHttpServletRequest();
    extra.addFile(new MockMultipartFile("file", new byte[] {1}));
    extra.addFile(new MockMultipartFile("note", new byte[] {2}));

    for (var request : new MockHttpServletRequest[] {none, twoParts, otherName, extra}) {
      assertThatThrownBy(() -> mine.put(jwt, request)).satisfies(e -> assertCode(e, "IDT-V32"));
    }
    assertThatThrownBy(() -> mine.put(jwt, new MockHttpServletRequest()))
        .satisfies(e -> assertCode(e, "IDT-V32"));

    verify(upload, never()).upload(any(), any());
  }

  @Test
  void deleteRemovesTheTokenSubjectsAvatar() {
    mine.delete(jwt);

    verify(remove).remove(me);
  }

  @Test
  void theBytesCarryTypeNosniffEtagAndPrivateCache() {
    UUID target = UUID.randomUUID();
    when(read.get(target, null))
        .thenReturn(new AvatarContent("image/webp", "\"a1\"", false, () -> new byte[] {9}));

    ResponseEntity<byte[]> response = users.get(target.toString(), null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsExactly(9);
    var h = response.getHeaders();
    assertThat(h.getContentType().toString()).isEqualTo("image/webp");
    assertThat(h.getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
    assertThat(h.getETag()).isEqualTo("\"a1\"");
    assertThat(h.getCacheControl()).isEqualTo("private, max-age=86400");
  }

  @Test
  void aMatchingEtagIs304WithoutBodyAndTheHeaderIsForwarded() {
    UUID target = UUID.randomUUID();
    when(read.get(target, "\"a1\""))
        .thenReturn(new AvatarContent("image/png", "\"a1\"", true, null));

    ResponseEntity<byte[]> response = users.get(target.toString(), "\"a1\"");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
    assertThat(response.getBody()).isNull();
    assertThat(response.getHeaders().getETag()).isEqualTo("\"a1\"");
  }

  @Test
  void aMalformedUserIdIsTheSame404AsNoAvatar() {
    assertThatThrownBy(() -> users.get("not-a-uuid", null))
        .satisfies(e -> assertCode(e, "IDT-451"));

    verify(read, never()).get(any(), any());
  }
}
