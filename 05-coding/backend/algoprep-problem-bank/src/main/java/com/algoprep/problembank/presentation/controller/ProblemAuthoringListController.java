package com.algoprep.problembank.presentation.controller;

import com.algoprep.problembank.application.dto.AuthoringScope;
import com.algoprep.problembank.application.dto.ListProblemsQuery;
import com.algoprep.problembank.application.ports.in.GetAttentionListUseCase;
import com.algoprep.problembank.application.ports.in.GetTopicDistributionUseCase;
import com.algoprep.problembank.application.ports.in.ListAuthoringProblemsUseCase;
import com.algoprep.problembank.domain.model.AttentionCount;
import com.algoprep.problembank.domain.model.AuthoringListPage;
import com.algoprep.problembank.domain.model.TopicDistribution;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Management screen reads #24, #25 and #26 (DD F2 sections 25 to 27). An instructor sees only own
 * problems, an admin the whole bank. The literal paths win over {@code /{problemId}}.
 */
@RestController
@RequestMapping("/api/v1/problem-bank/authoring/problems")
@PreAuthorize("hasAuthority('PROBLEM_AUTHORING:READ')")
class ProblemAuthoringListController {
  private final ListAuthoringProblemsUseCase list;
  private final GetTopicDistributionUseCase distribution;
  private final GetAttentionListUseCase attention;

  ProblemAuthoringListController(
      ListAuthoringProblemsUseCase list,
      GetTopicDistributionUseCase distribution,
      GetAttentionListUseCase attention) {
    this.list = list;
    this.distribution = distribution;
    this.attention = attention;
  }

  record PageMeta(int number, int size, long totalItems, int totalPages) {}

  record Totals(long total, long published) {}

  record ListResponse(List<AuthoringListPage.Item> items, PageMeta page, Totals totals) {}

  record AttentionResponse(List<AttentionCount> items) {}

  /** Query parameters of #24; text is validated strictly by the handler, numbers by binding. */
  record ListParams(
      String q,
      String level,
      String status,
      String sort,
      String dir,
      Integer page,
      Integer pageSize) {}

  @GetMapping
  ListResponse list(JwtAuthenticationToken caller, @ModelAttribute ListParams p) {
    var result =
        list.list(
            new ListProblemsQuery(
                actor(caller),
                admin(caller),
                p.q(),
                p.level(),
                p.status(),
                p.sort(),
                p.dir(),
                p.page(),
                p.pageSize()));
    return new ListResponse(
        result.items(),
        new PageMeta(result.page(), result.size(), result.totalItems(), result.totalPages()),
        new Totals(result.total(), result.published()));
  }

  @GetMapping("/topic-distribution")
  TopicDistribution topicDistribution(JwtAuthenticationToken caller) {
    return distribution.get(new AuthoringScope(actor(caller), admin(caller)));
  }

  @GetMapping("/attention")
  AttentionResponse attention(JwtAuthenticationToken caller) {
    return new AttentionResponse(attention.get(new AuthoringScope(actor(caller), admin(caller))));
  }

  private static UUID actor(JwtAuthenticationToken caller) {
    return UUID.fromString(caller.getToken().getSubject());
  }

  private static boolean admin(JwtAuthenticationToken caller) {
    return caller.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
  }
}
