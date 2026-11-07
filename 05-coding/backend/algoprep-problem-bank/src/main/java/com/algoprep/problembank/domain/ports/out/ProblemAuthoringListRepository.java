package com.algoprep.problembank.domain.ports.out;

import com.algoprep.problembank.domain.model.AttentionCount;
import com.algoprep.problembank.domain.model.AuthoringListFilter;
import com.algoprep.problembank.domain.model.AuthoringListPage;
import com.algoprep.problembank.domain.model.TopicDistribution;
import java.util.List;
import java.util.UUID;

/**
 * Read side of the management screen (DD F2 sections 25 to 27); soft-deleted problems never show.
 */
public interface ProblemAuthoringListRepository {

  /** The filtered page; a page past the end answers the last page. */
  AuthoringListPage list(AuthoringListFilter filter);

  /**
   * Every row for the CSV export, newest edit first: the given ids inside the scope, or, when
   * {@code ids} is null, everything the filter matches. Ids outside the scope are silently absent.
   */
  List<AuthoringListPage.Item> export(
      UUID authorScope, List<UUID> ids, String q, UUID levelId, String status);

  /** Topic usage; {@code authorScope} null means the whole bank. */
  TopicDistribution topicDistribution(UUID authorScope);

  /** Always the four rules in the DD order, a count of 0 where nothing matches. */
  List<AttentionCount> attention(UUID authorScope, int lowAcPercent, int staleDraftDays);
}
