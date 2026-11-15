package com.algoprep.problembank.application.command;

import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.problembank.application.dto.BulkTestcaseUpload;
import com.algoprep.problembank.application.dto.BulkTestcaseUpload.Item;
import com.algoprep.problembank.application.dto.BulkTestcaseUpload.UrlItem;
import com.algoprep.problembank.application.dto.TestcaseUploadSession.Confirmation;
import com.algoprep.problembank.application.dto.TestcaseUploadSession.Session;
import com.algoprep.problembank.application.dto.TestcaseUploadSession.Slot;
import com.algoprep.problembank.application.ports.in.BulkTestcaseUploadUseCase;
import com.algoprep.problembank.application.ports.out.TestcaseUploadSessionPort;
import com.algoprep.problembank.domain.exception.TestcaseUploadInvalidException;
import com.algoprep.problembank.domain.model.StoredTestcase;
import com.algoprep.problembank.domain.ports.out.TestcaseWriteRepository;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * DD F2 sections 46 and 47, BR-14. The first call signs the PUT URLs and keeps a Redis session;
 * nothing is written to {@code testcases}. The second call checks every object, then inserts the
 * whole batch in one transaction or nothing at all. A confirm that is repeated with the same upload
 * id answers what the first one answered.
 *
 * <p>ponytail: the object of an upload that is never confirmed stays in the bucket; no sweeper (BD
 * storage 5, DD logic BR-14 "Dọn rác").
 */
@Service
class BulkTestcaseUploadCommandHandler implements BulkTestcaseUploadUseCase {
  static final int MAX_ITEMS = 100;
  static final long MAX_SIDE_BYTES = 64L * 1024 * 1024;
  static final long MAX_TOTAL_BYTES = 256L * 1024 * 1024;
  static final int MAX_CLIENT_KEY_CHARS = 100;
  static final Duration SESSION_TTL = Duration.ofMinutes(20);
  private static final Logger LOG = LoggerFactory.getLogger(BulkTestcaseUploadCommandHandler.class);

  private final TestcaseAuthoringSupport support;
  private final TestcaseWriteRepository testcases;
  private final BulkUploadObjects objects;
  private final TestcaseUploadSessionPort sessions;
  private final ClockPort clock;

  BulkTestcaseUploadCommandHandler(
      TestcaseAuthoringSupport support,
      TestcaseWriteRepository testcases,
      BulkUploadObjects objects,
      TestcaseUploadSessionPort sessions,
      ClockPort clock) {
    this.support = support;
    this.testcases = testcases;
    this.objects = objects;
    this.sessions = sessions;
    this.clock = clock;
  }

  @Override
  @Transactional
  public BulkTestcaseUpload.UrlPlan requestUrls(BulkTestcaseUpload.RequestUrls command) {
    var problem = support.lock(command.scope(), command.problemId());
    var items = checked(command.items());
    if (testcases.counts(problem.id()).total() + items.size()
        > AuthorTestcasesCommandHandler.MAX_TESTCASES) {
      throw new DomainRuleViolationException("PRB-046", "A problem holds at most 500 testcases");
    }
    var slots = new ArrayList<Slot>();
    var urls = new ArrayList<UrlItem>();
    for (Item item : items) {
      UUID id = objects.newId();
      var slot =
          new Slot(
              item.clientKey(),
              id,
              item.visibility() == null ? "HIDDEN" : item.visibility(),
              item.category(),
              objects.declare(problem.id(), id, "input", item.inputSizeBytes()),
              objects.declare(problem.id(), id, "expected_output", item.expectedOutputSizeBytes()));
      slots.add(slot);
      urls.add(
          new UrlItem(
              item.clientKey(),
              id,
              objects.sign(slot.input()),
              objects.sign(slot.expectedOutput())));
    }
    UUID uploadId = objects.newId();
    guard(
        () -> {
          sessions.save(
              new Session(uploadId, problem.id(), command.scope().actorId(), slots, null),
              SESSION_TTL);
          return null;
        });
    return new BulkTestcaseUpload.UrlPlan(
        uploadId, clock.now().plus(BulkUploadObjects.PUT_TTL), urls);
  }

  @Override
  @Transactional
  public BulkTestcaseUpload.Added confirm(BulkTestcaseUpload.Confirm command) {
    var problem = support.lock(command.scope(), command.problemId());
    var session = ownSession(command);
    if (session.confirmed()) {
      return replay(session);
    }
    var verified = new BulkUploadObjects.Verified();
    for (Slot slot : session.slots()) {
      objects.verify(slot.clientKey(), "input", slot.input(), verified);
      objects.verify(slot.clientKey(), "expectedOutput", slot.expectedOutput(), verified);
    }
    if (!verified.faults.isEmpty()) {
      throw new TestcaseUploadInvalidException(verified.faults);
    }
    if (testcases.counts(problem.id()).total() + session.slots().size()
        > AuthorTestcasesCommandHandler.MAX_TESTCASES) {
      throw new DomainRuleViolationException("PRB-046", "A problem holds at most 500 testcases");
    }
    int position = testcases.nextPosition(problem.id());
    var rows = new ArrayList<StoredTestcase>();
    for (Slot slot : session.slots()) {
      var row =
          new StoredTestcase(
              slot.testcaseId(),
              slot.visibility(),
              "MANUAL",
              true,
              slot.category(),
              position++,
              objects.content(slot.input(), verified),
              objects.content(slot.expectedOutput(), verified));
      testcases.insert(problem.id(), row);
      rows.add(row);
    }
    var touch = support.settle(problem, command.scope().actorId(), true);
    markConfirmed(session, new Confirmation(touch.testcaseSetVersion(), touch.updatedAt()));
    return new BulkTestcaseUpload.Added(
        rows.stream().map(support::view).toList(), touch.testcaseSetVersion(), touch.updatedAt());
  }

  /** Same answer as the first confirm: the stored version and stamp, the rows as they are now. */
  private BulkTestcaseUpload.Added replay(Session session) {
    var rows = new ArrayList<StoredTestcase>();
    for (Slot slot : session.slots()) {
      testcases.lock(session.problemId(), slot.testcaseId()).ifPresent(rows::add);
    }
    var done = session.confirmation();
    return new BulkTestcaseUpload.Added(
        rows.stream().map(support::view).toList(),
        done.testcaseSetVersion(),
        done.problemUpdatedAt());
  }

  /**
   * The session is marked inside the transaction, while the problem lock is held, so a second
   * confirm cannot slip in between commit and the mark; a rollback puts it back to pending.
   */
  private void markConfirmed(Session session, Confirmation result) {
    guard(
        () -> {
          sessions.update(session.confirmedWith(result));
          return null;
        });
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status == STATUS_COMMITTED) {
              return;
            }
            try {
              sessions.update(session.pending());
            } catch (RuntimeException unavailable) {
              LOG.warn("Upload session could not be reopened after a rollback");
            }
          }
        });
  }

  private Session ownSession(BulkTestcaseUpload.Confirm command) {
    Optional<Session> found = guard(() -> sessions.find(command.uploadId()));
    return found
        .filter(
            s ->
                s.problemId().equals(command.problemId())
                    && s.userId().equals(command.scope().actorId()))
        .orElseThrow(
            () ->
                new DomainRuleViolationException(
                    "PRB-044", "Upload session not found, expired or not yours"));
  }

  /** Redis failure is the same 503 as a storage failure: the upload cannot be tracked. */
  private static <T> T guard(Supplier<T> action) {
    try {
      return action.get();
    } catch (ServiceUnavailableException unavailable) {
      throw unavailable;
    } catch (RuntimeException unavailable) {
      throw new ServiceUnavailableException("PRB-052", "Upload session unavailable", null);
    }
  }

  private static List<Item> checked(List<Item> items) {
    if (items == null || items.isEmpty()) {
      throw new InvalidRequestException("PRB-001", "items must not be empty");
    }
    if (items.size() > MAX_ITEMS) {
      throw tooMuch();
    }
    Set<String> keys = new HashSet<>();
    long total = 0;
    for (Item item : items) {
      validate(item, keys);
      if (item.inputSizeBytes() > MAX_SIDE_BYTES
          || item.expectedOutputSizeBytes() > MAX_SIDE_BYTES) {
        throw tooMuch();
      }
      total += item.inputSizeBytes() + item.expectedOutputSizeBytes();
    }
    if (total > MAX_TOTAL_BYTES) {
      throw tooMuch();
    }
    return items;
  }

  private static void validate(Item item, Set<String> keys) {
    if (item == null
        || item.clientKey() == null
        || item.clientKey().isBlank()
        || item.clientKey().length() > MAX_CLIENT_KEY_CHARS
        || !keys.add(item.clientKey())
        || item.inputSizeBytes() <= 0
        || item.expectedOutputSizeBytes() <= 0) {
      throw new InvalidRequestException("PRB-001", "Invalid bulk upload item");
    }
    if (item.visibility() != null) {
      AuthorTestcasesCommandHandler.visibility(item.visibility());
    }
    AuthorTestcasesCommandHandler.category(item.category());
  }

  private static DomainRuleViolationException tooMuch() {
    return new DomainRuleViolationException(
        "PRB-047", "A bulk upload holds at most 100 pairs, 64 MB a side and 256 MB in total");
  }
}
