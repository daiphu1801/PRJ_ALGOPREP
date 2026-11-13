package com.algoprep.interviewbank.domain.ports.out;

import com.algoprep.interviewbank.domain.model.CatalogItem;
import com.algoprep.interviewbank.domain.model.CatalogKind;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** All writes use the caller transaction; {@link #lock} serializes changes per catalogue. */
public interface CatalogRepository {
  /** Rows by sort order; {@code counts} adds the number of referencing questions, any status. */
  List<CatalogItem> list(CatalogKind kind, boolean counts);

  /** Advisory lock plus row locks, so create, reorder and delete cannot interleave. */
  void lock(CatalogKind kind);

  Optional<CatalogItem> find(CatalogKind kind, UUID id);

  /** Case-insensitive name check, ignoring {@code excluded} (may be null). */
  boolean nameTaken(CatalogKind kind, String displayName, UUID excluded);

  /** Inserts with a generated slug code and {@code sort_order = max + 1}. */
  CatalogItem create(CatalogKind kind, String displayName);

  /** Null arguments leave the column untouched; the star flag applies to topics only. */
  void update(CatalogKind kind, UUID id, String displayName, Boolean usesStarFramework);

  void reorder(CatalogKind kind, List<UUID> orderedIds);

  long references(CatalogKind kind, UUID id);

  void delete(CatalogKind kind, UUID id);
}
