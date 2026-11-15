package com.algoprep.identity.domain.model;

import java.util.List;

/** One page of the admin user list with the total after filtering. */
public record AdminUserPage(List<AdminUserItem> items, int page, int pageSize, long total) {}
