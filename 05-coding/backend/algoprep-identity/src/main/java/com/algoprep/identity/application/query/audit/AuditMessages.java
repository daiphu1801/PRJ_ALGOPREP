package com.algoprep.identity.application.query;

import com.algoprep.identity.application.dto.AuditLogItem;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The sentence of the CSV {@code content} column (DD BR-19): a template chosen by {@code
 * actionType} in Vietnamese or English, with the masked {@code safeParams} appended. An action with
 * no template gets a generic sentence, so a new action type never breaks the export. Same action
 * and params give the same sentence in one language.
 */
final class AuditMessages {
  private static final int MAX_VALUE = 200;

  private record Template(String vi, String en) {}

  private static final Map<String, Template> TEMPLATES =
      Map.of(
          "PASSWORD_CHANGED",
          new Template("Người dùng đổi mật khẩu", "User changed the password"),
          "EMAIL_CHANGED",
          new Template("Người dùng đổi email", "User changed the e-mail"),
          "PASSWORD_RESET_REQUESTED",
          new Template("Yêu cầu đặt lại mật khẩu", "Password reset requested"),
          "PASSWORD_RESET_COMPLETED",
          new Template("Hoàn tất đặt lại mật khẩu", "Password reset completed"),
          "SELF_ACCOUNT_DELETE_REQUESTED",
          new Template("Người dùng yêu cầu xoá tài khoản", "User requested account deletion"),
          "SECURITY_TOKEN_REUSE_DETECTED",
          new Template("Phát hiện dùng lại refresh token", "Refresh token reuse detected"),
          "AUDIT_LOG_EXPORTED",
          new Template("Xuất nhật ký hệ thống", "System log exported"));

  private AuditMessages() {}

  static String sentence(AuditLogItem item, boolean english) {
    Template template = TEMPLATES.get(item.actionType());
    StringBuilder text = new StringBuilder();
    if (template != null) {
      text.append(english ? template.en() : template.vi());
    } else {
      text.append(english ? "Action " : "Hành động ").append(item.actionType());
      text.append(english ? " on " : " trên ").append(item.targetType());
      if (item.targetId() != null) {
        text.append(' ').append(item.targetId());
      }
    }
    if (!item.safeParams().isEmpty()) {
      text.append(" (").append(params(item.safeParams())).append(')');
    }
    return text.toString().replaceAll("[\\r\\n]+", " ");
  }

  private static String params(Map<String, Object> params) {
    return params.entrySet().stream()
        .map(e -> e.getKey() + "=" + shorten(String.valueOf(e.getValue())))
        .collect(Collectors.joining("; "));
  }

  private static String shorten(String value) {
    return value.length() <= MAX_VALUE ? value : value.substring(0, MAX_VALUE) + "...";
  }
}
