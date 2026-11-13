package com.algoprep.problembank.domain.ports.out;

import com.algoprep.problembank.domain.model.CatalogItem;
import com.algoprep.problembank.domain.model.CatalogKind;
import java.util.List;
import java.util.UUID;

/**
 * All writes use the caller transaction; lock serializes membership/order changes per catalogue.
 */
public interface CatalogRepository {
  List<CatalogItem> list(CatalogKind kind, boolean counts);

  void lock(CatalogKind kind);

  CatalogItem create(CatalogKind kind, String name);

  void rename(CatalogKind kind, UUID id, String name);

  void reorder(CatalogKind kind, List<UUID> ids);

  long references(CatalogKind kind, UUID id);

  void delete(CatalogKind kind, UUID id);
}
