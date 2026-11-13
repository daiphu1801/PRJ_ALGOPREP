package com.algoprep.identity.presentation.controller;

import com.algoprep.identity.application.dto.JoinClassByCodeCommand;
import com.algoprep.identity.application.ports.in.GetMyClassUseCase;
import com.algoprep.identity.application.ports.in.JoinClassByCodeUseCase;
import com.algoprep.identity.application.ports.in.ListMyClassesUseCase;
import com.algoprep.identity.domain.model.ClassRef;
import com.algoprep.identity.domain.model.MyClass;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Student class endpoints #25, #25A, #25B. The caller comes from the token only. */
@RestController
@RequestMapping("/api/v1/identity")
@PreAuthorize("hasRole('STUDENT')")
class StudentClassController {
  private final JoinClassByCodeUseCase join;
  private final ListMyClassesUseCase list;
  private final GetMyClassUseCase get;

  StudentClassController(
      JoinClassByCodeUseCase join, ListMyClassesUseCase list, GetMyClassUseCase get) {
    this.join = join;
    this.list = list;
    this.get = get;
  }

  record JoinRequest(String code) {}

  record JoinResponse(UUID classId, String className) {}

  record MyClassesResponse(List<MyClass> items) {}

  @PostMapping("/classes/join")
  JoinResponse join(@AuthenticationPrincipal Jwt jwt, @RequestBody JoinRequest body) {
    ClassRef ref =
        join.join(new JoinClassByCodeCommand(UUID.fromString(jwt.getSubject()), body.code()));
    return new JoinResponse(ref.id(), ref.name());
  }

  @GetMapping("/me/classes")
  MyClassesResponse list(@AuthenticationPrincipal Jwt jwt) {
    return new MyClassesResponse(list.list(UUID.fromString(jwt.getSubject())));
  }

  @GetMapping("/me/classes/{classId}")
  MyClass get(@AuthenticationPrincipal Jwt jwt, @PathVariable String classId) {
    return get.get(UUID.fromString(jwt.getSubject()), classId);
  }
}
