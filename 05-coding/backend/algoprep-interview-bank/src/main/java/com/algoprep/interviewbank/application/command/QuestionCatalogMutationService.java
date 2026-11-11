package com.algoprep.interviewbank.application.command;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.exception.PermissionDeniedException;
import com.algoprep.common.exception.ResourceConflictException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.interviewbank.application.dto.CatalogCommand;
import com.algoprep.interviewbank.domain.exception.CatalogInUseException;
import com.algoprep.interviewbank.domain.exception.LastQuestionLevelException;
import com.algoprep.interviewbank.domain.model.CatalogItem;
import com.algoprep.interviewbank.domain.model.CatalogKind;
import com.algoprep.interviewbank.domain.model.InterviewAuditEntry;
import com.algoprep.interviewbank.domain.ports.out.CatalogRepository;
import com.algoprep.interviewbank.domain.ports.out.InterviewBankAuditPort;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/** Shared catalogue rules (BR-IB-02); every caller is a transactional command handler. */
@Service
class QuestionCatalogMutationService {
  private static final int MAX_NAME = 60;

  private record AuditChange(Object before, Object after) {}

  private final CatalogRepository repository;
  private final InterviewBankAuditPort audit;
  private final JsonMapper json = JsonMapper.builder().build();

  QuestionCatalogMutationService(CatalogRepository repository, InterviewBankAuditPort audit) {
    this.repository = repository;
    this.audit = audit;
  }

  CatalogItem create(CatalogKind kind, CatalogCommand command) {
    requireAdmin(command);
    String name = validateName(command.displayName());
    repository.lock(kind);
    requireUnique(kind, name, null);
    var created = repository.create(kind, name);
    record(
        kind,
        command.actorId(),
        "CREATED",
        created.id(),
        new AuditChange(null, snapshot(kind, created)));
    return created;
  }

  CatalogItem update(CatalogKind kind, CatalogCommand command) {
    requireAdmin(command);
    String name = command.displayName() == null ? null : validateName(command.displayName());
    if (kind == CatalogKind.LEVEL && name == null) {
      throw invalidName();
    }
    repository.lock(kind);
    var existing = find(kind, command.id());
    String nextName = name == null ? existing.displayName() : name;
    boolean nextStar =
        kind == CatalogKind.TOPIC && command.usesStarFramework() != null
            ? command.usesStarFramework()
            : existing.usesStarFramework();
    if (nextName.equals(existing.displayName()) && nextStar == existing.usesStarFramework()) {
      return existing;
    }
    if (!nextName.equals(existing.displayName())) {
      requireUnique(kind, nextName, existing.id());
    }
    repository.update(kind, existing.id(), nextName, kind == CatalogKind.TOPIC ? nextStar : null);
    var updated =
        new CatalogItem(
            existing.id(), existing.code(), nextName, existing.sortOrder(), nextStar, null);
    record(
        kind,
        command.actorId(),
        kind == CatalogKind.LEVEL ? "RENAMED" : "UPDATED",
        existing.id(),
        new AuditChange(snapshot(kind, existing), snapshot(kind, updated)));
    return updated;
  }

  List<CatalogItem> reorder(CatalogKind kind, CatalogCommand command) {
    requireAdmin(command);
    repository.lock(kind);
    var previous = repository.list(kind, false).stream().map(CatalogItem::id).toList();
    var ordered = command.orderedIds();
    if (ordered == null
        || ordered.stream().anyMatch(Objects::isNull)
        || ordered.size() != previous.size()
        || new HashSet<>(ordered).size() != ordered.size()
        || !new HashSet<>(ordered).equals(new HashSet<>(previous))) {
      throw new DomainRuleViolationException(
          "INB-034", "Order must contain every catalogue id exactly once");
    }
    repository.reorder(kind, ordered);
    record(kind, command.actorId(), "REORDERED", null, new AuditChange(previous, ordered));
    return repository.list(kind, false);
  }

  void delete(CatalogKind kind, CatalogCommand command) {
    requireAdmin(command);
    repository.lock(kind);
    var existing = find(kind, command.id());
    long count = repository.references(kind, existing.id());
    if (count > 0) {
      throw new CatalogInUseException(
          kind == CatalogKind.LEVEL ? "INB-031" : "INB-030", count, kind == CatalogKind.LEVEL);
    }
    if (kind == CatalogKind.LEVEL && repository.list(kind, false).size() == 1) {
      throw new LastQuestionLevelException();
    }
    repository.delete(kind, existing.id());
    record(
        kind,
        command.actorId(),
        "DELETED",
        existing.id(),
        new AuditChange(snapshot(kind, existing), null));
  }

  private void requireAdmin(CatalogCommand command) {
    if (!command.admin()) {
      throw new PermissionDeniedException("INB-302", "Admin role required");
    }
  }

  private InvalidRequestException invalidName() {
    return new InvalidRequestException("INB-V03", "Display name must have 1 to 60 characters");
  }

  private String validateName(String value) {
    String name = value == null ? "" : value.strip();
    if (name.isEmpty() || name.codePointCount(0, name.length()) > MAX_NAME) {
      throw invalidName();
    }
    return name;
  }

  private CatalogItem find(CatalogKind kind, UUID id) {
    return repository
        .find(kind, id)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    kind == CatalogKind.LEVEL ? "INB-012" : "INB-011", "Catalogue item not found"));
  }

  private void requireUnique(CatalogKind kind, String name, UUID excluded) {
    if (repository.nameTaken(kind, name, excluded)) {
      throw new ResourceConflictException(
          kind == CatalogKind.LEVEL ? "INB-021" : "INB-020", "Catalogue name already exists");
    }
  }

  private Map<String, Object> snapshot(CatalogKind kind, CatalogItem item) {
    var map = new LinkedHashMap<String, Object>();
    map.put("id", item.id());
    map.put("code", item.code());
    map.put("displayName", item.displayName());
    map.put("sortOrder", item.sortOrder());
    if (kind == CatalogKind.TOPIC) {
      map.put("usesStarFramework", item.usesStarFramework());
    }
    return map;
  }

  private void record(
      CatalogKind kind, UUID actor, String action, UUID target, AuditChange change) {
    try {
      audit.record(
          new InterviewAuditEntry(
              actor,
              "INTERVIEW_" + kind + "_" + action,
              "INTERVIEW_" + kind,
              target == null ? null : target.toString(),
              change.before() == null ? null : json.writeValueAsString(change.before()),
              change.after() == null ? null : json.writeValueAsString(change.after())));
    } catch (RuntimeException failure) {
      throw new ServiceUnavailableException(
          "INB-503", "Audit write unavailable; change rolled back", failure);
    }
  }
}
