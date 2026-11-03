package com.algoprep.identity.presentation.controller;

import com.algoprep.identity.application.ports.in.GenerateClassInviteCodeUseCase;
import com.algoprep.identity.application.ports.in.ListClassInviteCodesUseCase;
import com.algoprep.identity.application.ports.in.ListClassRosterUseCase;
import com.algoprep.identity.application.ports.in.RemoveStudentFromClassUseCase;
import com.algoprep.identity.domain.model.ClassInviteCode;
import com.algoprep.identity.domain.model.ClassRosterEntry;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Invite codes #23 and #24, roster #26 and leave #27 of a class the caller owns. */
@RestController
@RequestMapping("/api/v1/identity/classes/{classId}")
class ClassMembershipController {
  private final GenerateClassInviteCodeUseCase generate;
  private final ListClassInviteCodesUseCase codes;
  private final ListClassRosterUseCase roster;
  private final RemoveStudentFromClassUseCase remove;

  ClassMembershipController(
      GenerateClassInviteCodeUseCase generate,
      ListClassInviteCodesUseCase codes,
      ListClassRosterUseCase roster,
      RemoveStudentFromClassUseCase remove) {
    this.generate = generate;
    this.codes = codes;
    this.roster = roster;
    this.remove = remove;
  }

  record InviteCodeResponse(UUID id, String code, String expiresAt) {

    static InviteCodeResponse of(ClassInviteCode c) {
      return new InviteCodeResponse(c.id(), c.code(), c.expiresAt().toString());
    }
  }

  record InviteCodeListResponse(List<InviteCodeResponse> items) {}

  /** {@code status} is always null for now: the five labels of BR-10 need F4 metrics (deferred). */
  record StudentResponse(
      UUID studentId,
      String displayName,
      String email,
      String phone,
      String joinedAt,
      String status) {

    static StudentResponse of(ClassRosterEntry e) {
      return new StudentResponse(
          e.studentId(), e.displayName(), e.email(), e.phone(), e.joinedAt().toString(), null);
    }
  }

  record RosterResponse(List<StudentResponse> items) {}

  record LeaveResponse(UUID classId, UUID studentId, String leftAt) {}

  @PostMapping("/invite-codes")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('CLASS_MANAGEMENT:CREATE')")
  InviteCodeResponse generate(@AuthenticationPrincipal Jwt jwt, @PathVariable String classId) {
    return InviteCodeResponse.of(
        generate.generate(UUID.fromString(jwt.getSubject()), ClassPath.id(classId)));
  }

  @GetMapping("/invite-codes")
  @PreAuthorize("hasAuthority('CLASS_MANAGEMENT:READ')")
  InviteCodeListResponse codes(@AuthenticationPrincipal Jwt jwt, @PathVariable String classId) {
    return new InviteCodeListResponse(
        codes.list(UUID.fromString(jwt.getSubject()), ClassPath.id(classId)).stream()
            .map(InviteCodeResponse::of)
            .toList());
  }

  @GetMapping("/students")
  @PreAuthorize("hasAuthority('CLASS_MANAGEMENT:READ')")
  RosterResponse students(@AuthenticationPrincipal Jwt jwt, @PathVariable String classId) {
    return new RosterResponse(
        roster.list(UUID.fromString(jwt.getSubject()), ClassPath.id(classId)).stream()
            .map(StudentResponse::of)
            .toList());
  }

  @PostMapping("/students/{studentId}/leave")
  @PreAuthorize("hasAuthority('CLASS_MANAGEMENT:UPDATE')")
  LeaveResponse leave(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable String classId,
      @PathVariable String studentId) {
    UUID cls = ClassPath.id(classId);
    UUID student = ClassPath.id(studentId);
    return new LeaveResponse(
        cls, student, remove.remove(UUID.fromString(jwt.getSubject()), cls, student).toString());
  }
}
