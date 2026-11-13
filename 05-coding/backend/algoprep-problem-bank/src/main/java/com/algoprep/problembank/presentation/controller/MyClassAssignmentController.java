package com.algoprep.problembank.presentation.controller;

import com.algoprep.problembank.application.dto.StudentClassViews.MyAssignmentGroups;
import com.algoprep.problembank.application.dto.StudentClassViews.MyClassProblems;
import com.algoprep.problembank.application.ports.in.MyClassAssignmentsUseCase;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * #4 and #13A (DD F2 sections 5 and 5A). The student is the token subject; neither route takes a
 * user id, and the classes always come from the current enrolment.
 */
@RestController
@RequestMapping("/api/v1/problem-bank/me")
class MyClassAssignmentController {
  private final MyClassAssignmentsUseCase assignments;

  MyClassAssignmentController(MyClassAssignmentsUseCase assignments) {
    this.assignments = assignments;
  }

  @GetMapping("/class-assignments")
  @PreAuthorize("isAuthenticated()")
  MyAssignmentGroups groups(JwtAuthenticationToken caller) {
    return assignments.groups(user(caller));
  }

  @GetMapping("/class-problems")
  @PreAuthorize("hasRole('STUDENT')")
  MyClassProblems problems(
      JwtAuthenticationToken caller,
      @RequestParam(required = false) String classId,
      @RequestParam(required = false) String page,
      @RequestParam(required = false) String pageSize) {
    return assignments.problems(user(caller), classId, page, pageSize);
  }

  private static UUID user(JwtAuthenticationToken caller) {
    return UUID.fromString(caller.getToken().getSubject());
  }
}
