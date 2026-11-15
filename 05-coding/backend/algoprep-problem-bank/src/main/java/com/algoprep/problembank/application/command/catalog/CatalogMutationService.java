package com.algoprep.problembank.application.command;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.exception.PermissionDeniedException;
import com.algoprep.common.exception.ResourceConflictException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.problembank.application.dto.CatalogCommand;
import com.algoprep.problembank.application.ports.out.ProblemListVersionPort;
import com.algoprep.problembank.domain.exception.CatalogInUseException;
import com.algoprep.problembank.domain.exception.LastProblemLevelException;
import com.algoprep.problembank.domain.model.CatalogAuditEntry;
import com.algoprep.problembank.domain.model.CatalogItem;
import com.algoprep.problembank.domain.model.CatalogKind;
import com.algoprep.problembank.domain.ports.out.CatalogRepository;
import com.algoprep.problembank.domain.ports.out.ProblemAuditPort;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/** Shared catalogue rules; every caller is a transactional command handler. */
@Service
class CatalogMutationService {
  private record AuditChange(Object before, Object after) {}

  private final CatalogRepository repository;
  private final ProblemAuditPort audit;
  private final ProblemListVersionPort cache;
  private final JsonMapper json = JsonMapper.builder().build();

  CatalogMutationService(
      CatalogRepository repository, ProblemAuditPort audit, ProblemListVersionPort cache) {
    this.repository = repository;
    this.audit = audit;
    this.cache = cache;
  }

  List<CatalogItem> create(CatalogKind kind, CatalogCommand command) {
    requireAdmin(command);
    String name = validateName(command.name());
    repository.lock(kind);
    requireUnique(kind, name, null);
    var created = repository.create(kind, name);
    record(
        kind,
        command.actorId(),
        "CREATED",
        created.id().toString(),
        new AuditChange(null, snapshot(kind, created)));
    return List.of(created);
  }

  List<CatalogItem> update(CatalogKind kind, CatalogCommand command) {
    requireAdmin(command);
    repository.lock(kind);
    var existing = find(kind, command.id());
    String name = validateName(command.name());
    if (name.equals(existing.name())) {
      return List.of(existing);
    }
    requireUnique(kind, name, command.id());
    repository.rename(kind, command.id(), name);
    var updated = new CatalogItem(existing.id(), existing.code(), name, existing.sortOrder(), null);
    record(
        kind,
        command.actorId(),
        "RENAMED",
        existing.id().toString(),
        new AuditChange(snapshot(kind, existing), snapshot(kind, updated)));
    return List.of(updated);
  }

  List<CatalogItem> reorder(CatalogKind kind, CatalogCommand command) {
    requireAdmin(command);
    repository.lock(kind);
    var previous = repository.list(kind, false).stream().map(CatalogItem::id).toList();
    var ordered = command.orderedIds();
    if (ordered == null
        || ordered.stream().anyMatch(java.util.Objects::isNull)
        || ordered.size() != previous.size()
        || new HashSet<>(ordered).size() != ordered.size()
        || !new HashSet<>(ordered).equals(new HashSet<>(previous))) {
      throw new DomainRuleViolationException(
          "PRB-049", "Order must contain every catalogue id exactly once");
    }
    repository.reorder(kind, ordered);
    record(kind, command.actorId(), "REORDERED", null, new AuditChange(previous, ordered));
    return repository.list(kind, false);
  }

  List<CatalogItem> delete(CatalogKind kind, CatalogCommand command) {
    requireAdmin(command);
    repository.lock(kind);
    var existing = find(kind, command.id());
    long count = repository.references(kind, command.id());
    if (count > 0) {
      throw new CatalogInUseException(
          kind == CatalogKind.LEVEL ? "PRB-032" : "PRB-031", count, kind == CatalogKind.LEVEL);
    }
    if (kind == CatalogKind.LEVEL && repository.list(kind, false).size() == 1) {
      throw new LastProblemLevelException();
    }
    repository.delete(kind, command.id());
    record(
        kind,
        command.actorId(),
        "DELETED",
        existing.id().toString(),
        new AuditChange(snapshot(kind, existing), null));
    return List.of();
  }

  private void requireAdmin(CatalogCommand command) {
    if (!command.admin()) {
      throw new PermissionDeniedException("PRB-004", "Admin role required");
    }
  }

  private String validateName(String value) {
    String name = value == null ? "" : value.strip();
    if (name.isEmpty() || name.codePointCount(0, name.length()) > 60) {
      throw new InvalidRequestException("PRB-001", "Catalogue name must have 1 to 60 characters");
    }
    return name;
  }

  private CatalogItem find(CatalogKind kind, UUID id) {
    return repository.list(kind, false).stream()
        .filter(item -> item.id().equals(id))
        .findFirst()
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    kind == CatalogKind.LEVEL ? "PRB-013" : "PRB-012", "Catalogue item not found"));
  }

  private void requireUnique(CatalogKind kind, String name, UUID excluded) {
    if (repository.list(kind, false).stream()
        .anyMatch(item -> !item.id().equals(excluded) && item.name().equalsIgnoreCase(name))) {
      throw new ResourceConflictException(
          kind == CatalogKind.LEVEL ? "PRB-022" : "PRB-021", "Catalogue name already exists");
    }
  }

  private Map<String, Object> snapshot(CatalogKind kind, CatalogItem item) {
    return Map.of(
        "id",
        item.id(),
        "code",
        item.code(),
        kind == CatalogKind.LEVEL ? "displayName" : "name",
        item.name(),
        "sortOrder",
        item.sortOrder());
  }

  private void record(CatalogKind kind, UUID actor, String action, String id, AuditChange change) {
    try {
      audit.record(
          new CatalogAuditEntry(
              actor,
              "PROBLEM_" + kind + "_" + action,
              "PROBLEM_" + kind,
              id,
              change.before() == null ? null : json.writeValueAsString(change.before()),
              change.after() == null ? null : json.writeValueAsString(change.after())));
    } catch (RuntimeException failure) {
      throw new ServiceUnavailableException(
          "PRB-050", "Audit write unavailable; change rolled back", failure);
    }
    cache.incrementAfterCommit();
  }
}
