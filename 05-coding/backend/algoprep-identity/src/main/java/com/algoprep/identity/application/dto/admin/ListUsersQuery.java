package com.algoprep.identity.application.dto.admin;

/** Filters of #35 as sent; blank means "any". */
public record ListUsersQuery(
    String status, String role, String keyword, Integer page, Integer pageSize) {}
