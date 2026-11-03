package com.algoprep.judge.presentation.controller;

import com.algoprep.judge.application.ports.in.GetAdminLanguageConfigUseCase;
import com.algoprep.judge.application.ports.in.UpdateAdminLanguageConfigUseCase;
import com.algoprep.judge.domain.model.AdminLanguageConfig;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.JsonNodeFeature;
import tools.jackson.databind.json.JsonMapper;

/** Admin APIs #21/#22; current role and the endpoint-specific permission are both mandatory. */
@RestController
@RequestMapping("/api/v1/judge/admin/language-config")
class AdminLanguageConfigController {
  private final GetAdminLanguageConfigUseCase reader;
  private final UpdateAdminLanguageConfigUseCase writer;
  private final JsonMapper json =
      JsonMapper.builder()
          .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
          .disable(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES)
          .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .build();

  AdminLanguageConfigController(
      GetAdminLanguageConfigUseCase reader, UpdateAdminLanguageConfigUseCase writer) {
    this.reader = reader;
    this.writer = writer;
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN') and hasAuthority('LANGUAGE_CONFIG:READ')")
  AdminLanguageConfig read() {
    return reader.read();
  }

  @PutMapping
  @PreAuthorize("hasRole('ADMIN') and hasAuthority('LANGUAGE_CONFIG:UPDATE')")
  AdminLanguageConfig update(@AuthenticationPrincipal Jwt jwt, @RequestBody String request) {
    try {
      return writer.update(
          UUID.fromString(jwt.getSubject()),
          AdminLanguageConfigRequest.parse(json.readTree(request)));
    } catch (JacksonException failure) {
      throw new com.algoprep.judge.domain.exception.AdminConfigException(400, "JDG-V10");
    }
  }
}
