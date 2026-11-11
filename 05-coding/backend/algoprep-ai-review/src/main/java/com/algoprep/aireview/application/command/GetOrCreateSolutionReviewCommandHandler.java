package com.algoprep.aireview.application.command;

import com.algoprep.aireview.application.ports.in.GetOrCreateSolutionReviewUseCase;
import com.algoprep.aireview.application.ports.out.SubmissionSnapshotPort;
import com.algoprep.aireview.domain.exception.AiNotOwnerException;
import com.algoprep.aireview.domain.exception.AiProviderUnavailableException;
import com.algoprep.aireview.domain.exception.AiReviewInProgressException;
import com.algoprep.aireview.domain.model.CallerCategory;
import com.algoprep.aireview.domain.model.CostReservation;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.PromptSnapshot;
import com.algoprep.aireview.domain.model.StoredSolutionReview;
import com.algoprep.aireview.domain.model.SubmissionSnapshot;
import com.algoprep.aireview.domain.ports.out.AiPromptSnapshotRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * BR-AI-02 and BR-AI-03. Deliberately not transactional: the LLM call must stay outside any
 * transaction, so each write goes through {@link SolutionReviewStore}. Nothing in F1-F4 calls this.
 */
@Service
class GetOrCreateSolutionReviewCommandHandler implements GetOrCreateSolutionReviewUseCase {
  private final SubmissionSnapshotPort submissions;
  private final AiPromptSnapshotRepository templates;
  private final SolutionReviewStore store;
  private final ReviewCostGate costGate;
  private final ReviewLlmCall llmCall;

  GetOrCreateSolutionReviewCommandHandler(
      SubmissionSnapshotPort submissions,
      AiPromptSnapshotRepository templates,
      SolutionReviewStore store,
      ReviewCostGate costGate,
      ReviewLlmCall llmCall) {
    this.submissions = submissions;
    this.templates = templates;
    this.store = store;
    this.costGate = costGate;
    this.llmCall = llmCall;
  }

  @Override
  public StoredSolutionReview getOrCreate(
      UUID callerId, CallerCategory category, UUID submissionId) {
    SubmissionSnapshot source = submissions.getAccepted(submissionId);
    // The serving side checks no ownership (DD F4 P4), so the check lives here.
    if (!callerId.equals(source.userId())) {
      throw new AiNotOwnerException();
    }
    Optional<StoredSolutionReview> existing = store.find(submissionId);
    if (existing.isPresent()) {
      return existing.get();
    }
    return store
        .withLock(
            submissionId, () -> store.find(submissionId).orElseGet(() -> create(source, category)))
        .orElseThrow(AiReviewInProgressException::new);
  }

  private StoredSolutionReview create(SubmissionSnapshot source, CallerCategory category) {
    String hash = sha256(source.sourceCode());
    PromptSnapshot template =
        templates
            .readActive(FeatureCode.SOLUTION_REVIEW)
            .orElseThrow(
                () ->
                    new AiProviderUnavailableException(
                        AiProviderUnavailableException.Reason.UPSTREAM_PORT, null));
    var hit = store.findCache(source.problemId(), source.language(), hash, template.id());
    if (hit.isPresent()) {
      Optional<StoredSolutionReview> copy =
          store.saveFromCache(hit.get(), source, hash, template.id());
      return copy.orElseGet(() -> store.find(source.submissionId()).orElseThrow());
    }
    CostReservation reservation = costGate.reserve(source.userId(), category);
    try {
      return generate(source, hash, template);
    } catch (RuntimeException failure) {
      costGate.refund(reservation);
      throw failure;
    }
  }

  private StoredSolutionReview generate(
      SubmissionSnapshot source, String hash, PromptSnapshot template) {
    UUID reviewId = store.newId();
    var normalized = llmCall.generate(source, template, reviewId);
    var review =
        new StoredSolutionReview(
            reviewId,
            source.submissionId(),
            source.userId(),
            source.problemId(),
            source.language(),
            hash,
            template.id(),
            normalized.json(),
            normalized.readabilityScore(),
            false,
            store.now());
    if (store.saveGenerated(review)) {
      return review;
    }
    return store.find(source.submissionId()).orElseThrow();
  }

  /** The exact UTF-8 bytes of the stored source, no normalisation (BR-AI-02 step 5). */
  static String sha256(String source) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }
}
