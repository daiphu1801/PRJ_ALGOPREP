package com.algoprep.identity.application.dto;

/**
 * Input of the login use case.
 *
 * @param email address typed by the user
 * @param password password typed by the user
 * @param instructorPortal true for {@code /auth/instructor/login}, which only lets instructors in
 * @param sourceIp source address observed by the server, never a value from the JSON request
 */
public record LoginCommand(
    String email, String password, boolean instructorPortal, String sourceIp) {}
