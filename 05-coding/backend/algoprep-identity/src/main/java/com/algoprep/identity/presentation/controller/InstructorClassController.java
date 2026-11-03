package com.algoprep.identity.presentation.controller;

import com.algoprep.identity.application.dto.CreateClassCommand;
import com.algoprep.identity.application.dto.CreatedClass;
import com.algoprep.identity.application.dto.UpdateClassCommand;
import com.algoprep.identity.application.ports.in.CreateClassUseCase;
import com.algoprep.identity.application.ports.in.DeleteClassUseCase;
import com.algoprep.identity.application.ports.in.ListInstructorClassesUseCase;
import com.algoprep.identity.application.ports.in.UpdateClassUseCase;
import com.algoprep.identity.domain.model.ClassInviteCode;
import com.algoprep.identity.domain.model.InstructorClass;
import com.algoprep.identity.domain.model.ScheduleSpec;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Instructor class management #19 to #22. The caller is the token subject; a class id that is
 * malformed, missing or someone else's is 404 IDT-414.
 */
@RestController
@RequestMapping("/api/v1/identity/classes")
class InstructorClassController {
  private final ListInstructorClassesUseCase list;
  private final CreateClassUseCase create;
  private final UpdateClassUseCase update;
  private final DeleteClassUseCase delete;

  InstructorClassController(
      ListInstructorClassesUseCase list,
      CreateClassUseCase create,
      UpdateClassUseCase update,
      DeleteClassUseCase delete) {
    this.list = list;
    this.create = create;
    this.update = update;
    this.delete = delete;
  }

  /** Absent or null fields keep their value on PATCH; an empty schedule object clears it. */
  record ClassRequest(String name, String description, ScheduleSpec schedule) {}

  record ClassResponse(
      UUID id,
      String name,
      String description,
      ScheduleSpec schedule,
      int studentCount,
      String createdAt) {

    static ClassResponse of(InstructorClass c) {
      return new ClassResponse(
          c.id(),
          c.name(),
          c.description(),
          c.schedule(),
          c.studentCount(),
          c.createdAt().toString());
    }
  }

  record ClassListResponse(List<ClassResponse> items) {}

  record InviteCodeResponse(UUID id, String code, String expiresAt) {

    static InviteCodeResponse of(ClassInviteCode c) {
      return new InviteCodeResponse(c.id(), c.code(), c.expiresAt().toString());
    }
  }

  /** The new class plus its first invite code; {@code classId} drives the redirect. */
  record CreatedClassResponse(
      UUID classId,
      String name,
      String description,
      ScheduleSpec schedule,
      int studentCount,
      String createdAt,
      InviteCodeResponse inviteCode) {

    static CreatedClassResponse of(CreatedClass c) {
      InstructorClass k = c.created();
      return new CreatedClassResponse(
          k.id(),
          k.name(),
          k.description(),
          k.schedule(),
          k.studentCount(),
          k.createdAt().toString(),
          InviteCodeResponse.of(c.inviteCode()));
    }
  }

  @GetMapping("/my")
  @PreAuthorize("hasAuthority('CLASS_MANAGEMENT:READ')")
  ClassListResponse mine(@AuthenticationPrincipal Jwt jwt) {
    return new ClassListResponse(
        list.list(UUID.fromString(jwt.getSubject())).stream().map(ClassResponse::of).toList());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('CLASS_MANAGEMENT:CREATE')")
  CreatedClassResponse create(@AuthenticationPrincipal Jwt jwt, @RequestBody ClassRequest body) {
    return CreatedClassResponse.of(
        create.create(
            new CreateClassCommand(
                UUID.fromString(jwt.getSubject()),
                body.name(),
                body.description(),
                body.schedule())));
  }

  @PatchMapping("/{classId}")
  @PreAuthorize("hasAuthority('CLASS_MANAGEMENT:UPDATE')")
  ClassResponse patch(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable String classId,
      @RequestBody ClassRequest body) {
    return ClassResponse.of(
        update.update(
            new UpdateClassCommand(
                UUID.fromString(jwt.getSubject()),
                ClassPath.id(classId),
                body.name(),
                body.description(),
                body.schedule())));
  }

  @DeleteMapping("/{classId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAuthority('CLASS_MANAGEMENT:DELETE')")
  void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable String classId) {
    delete.delete(UUID.fromString(jwt.getSubject()), ClassPath.id(classId));
  }
}
