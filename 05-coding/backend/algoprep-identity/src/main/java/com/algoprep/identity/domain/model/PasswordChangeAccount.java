package com.algoprep.identity.domain.model;

/**
 * A locked account for BR-06.
 *
 * @param user the account as authentication sees it
 * @param email login email, needed by the "password must differ from email" rule
 */
public record PasswordChangeAccount(UserAccount user, String email) {}
