package com.algoprep.problembank.domain.model.catalog;

import java.util.UUID;

public record CatalogItem(UUID id, String code, String name, int sortOrder, Long problemCount) {}
