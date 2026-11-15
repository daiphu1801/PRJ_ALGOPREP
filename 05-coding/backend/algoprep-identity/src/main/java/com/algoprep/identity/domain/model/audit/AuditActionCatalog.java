package com.algoprep.identity.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Maps an {@code action_type} string to its display group and service (DD BR-19). The table is by
 * prefix, so an action another module starts writing later lands in a sensible group without a code
 * change here; an unknown prefix falls back to {@code CONFIG} / {@code system}, the group the BD
 * describes as "every operational write by an admin". No prefix is a prefix of another, so the
 * order of the rules does not matter and a rule set can be turned into an OR predicate.
 */
public final class AuditActionCatalog {

  private record Rule(String prefix, AuditCategory category, String service) {}

  private static final AuditActionInfo FALLBACK =
      new AuditActionInfo(AuditCategory.CONFIG, "system");

  private static final List<Rule> RULES =
      List.of(
          // Authentication events written by this module and by session handling.
          new Rule("PASSWORD_", AuditCategory.AUTH, "auth"),
          new Rule("EMAIL_", AuditCategory.AUTH, "auth"),
          new Rule("SELF_ACCOUNT_", AuditCategory.AUTH, "auth"),
          new Rule("SECURITY_", AuditCategory.AUTH, "auth"),
          new Rule("SESSION_", AuditCategory.AUTH, "auth"),
          new Rule("LOGIN_", AuditCategory.AUTH, "auth"),
          new Rule("LOGOUT_", AuditCategory.AUTH, "auth"),
          new Rule("ACCOUNT_", AuditCategory.AUTH, "auth"),
          // Permission matrix and user administration (owner 2026-10-25: no fifth USER group).
          new Rule("ROLE_", AuditCategory.PERMISSION, "permission"),
          new Rule("PERMISSION_", AuditCategory.PERMISSION, "permission"),
          new Rule("USER_", AuditCategory.PERMISSION, "permission"),
          // Configuration, including the retention settings and the export action itself.
          new Rule("AUDIT_LOG_", AuditCategory.CONFIG, "config"),
          new Rule("LOG_RETENTION_", AuditCategory.CONFIG, "config"),
          new Rule("AI_", AuditCategory.CONFIG, "ai-config"),
          new Rule("LANGUAGE_", AuditCategory.CONFIG, "config"),
          new Rule("QUEUE_", AuditCategory.CONFIG, "config"),
          new Rule("SANDBOX_", AuditCategory.CONFIG, "config"),
          new Rule("GRADING_", AuditCategory.CONFIG, "config"),
          new Rule("GENERATOR_", AuditCategory.CONFIG, "config"),
          new Rule("CLUSTER_", AuditCategory.CONFIG, "config"),
          // Content administration of the problem bank and the interview bank.
          new Rule("PROBLEM_", AuditCategory.CONTENT, "content"),
          new Rule("INTERVIEW_", AuditCategory.CONTENT, "content"));

  private AuditActionCatalog() {}

  /** Display group and service of an action; an unknown action gets the fallback. */
  public static AuditActionInfo resolve(String actionType) {
    for (Rule rule : RULES) {
      if (actionType.startsWith(rule.prefix())) {
        return new AuditActionInfo(rule.category(), rule.service());
      }
    }
    return FALLBACK;
  }

  /** The actions of one display group; {@code CONFIG} also takes every unmapped action. */
  public static ActionMatch ofCategory(AuditCategory category) {
    List<String> own = new ArrayList<>();
    for (Rule rule : RULES) {
      if (rule.category() == category) {
        own.add(rule.prefix());
      }
    }
    return new ActionMatch(own, allPrefixes(), FALLBACK.category() == category);
  }

  /**
   * The actions whose service name contains the keyword (case-insensitive). The match is empty when
   * no service name contains it, so the predicate built from it selects nothing.
   */
  public static ActionMatch ofServiceContaining(String keyword) {
    String needle = keyword.toLowerCase(Locale.ROOT);
    List<String> own = new ArrayList<>();
    for (Rule rule : RULES) {
      if (rule.service().contains(needle)) {
        own.add(rule.prefix());
      }
    }
    return new ActionMatch(own, allPrefixes(), FALLBACK.service().contains(needle));
  }

  private static List<String> allPrefixes() {
    return RULES.stream().map(Rule::prefix).toList();
  }
}
