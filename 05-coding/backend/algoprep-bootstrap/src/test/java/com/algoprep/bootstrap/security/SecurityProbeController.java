package com.algoprep.bootstrap.security;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only endpoints that exercise the security foundation the way a controller of any module
 * will: a layer-2 matrix cell and a layer-1 base category.
 */
@RestController
class SecurityProbeController {

  @GetMapping("/probe/class-read")
  @PreAuthorize("hasAuthority('CLASS_MANAGEMENT:READ')")
  String classRead() {
    return "ok";
  }

  @GetMapping("/probe/admin-only")
  @PreAuthorize("hasRole('ADMIN')")
  String adminOnly() {
    return "ok";
  }
}
