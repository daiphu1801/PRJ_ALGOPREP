package com.algoprep.identity.presentation.controller;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.identity.application.dto.PersonalDataExport;
import com.algoprep.identity.application.ports.in.ExportMyProfileDataUseCase;
import com.algoprep.identity.domain.model.MySettings;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Personal data export #16. Only {@code scope=profile} exists: {@code submissions} needs F4 and the
 * interview transcripts of #17 need F5, so those answer 501 until those modules exist.
 */
@RestController
@RequestMapping("/api/v1/identity/me/export")
class MyExportController {
  private final ExportMyProfileDataUseCase exportProfile;

  MyExportController(ExportMyProfileDataUseCase exportProfile) {
    this.exportProfile = exportProfile;
  }

  record PersonalDataResponse(MyProfileController.ProfileResponse profile, MySettings settings) {}

  @GetMapping
  ResponseEntity<PersonalDataResponse> export(
      @AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String scope) {
    if ("submissions".equals(scope)) {
      throw new ResponseStatusException(
          HttpStatus.NOT_IMPLEMENTED, "Submission export arrives with the judge module");
    }
    if (!"profile".equals(scope)) {
      throw new InvalidRequestException("IDT-400", "scope must be profile or submissions");
    }
    PersonalDataExport data = exportProfile.export(UUID.fromString(jwt.getSubject()));
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"algoprep-profile.json\"")
        .body(
            new PersonalDataResponse(
                MyProfileController.ProfileResponse.of(data.profile()), data.settings()));
  }
}
