package com.algoprep.judge.application.dto;

import com.algoprep.judge.domain.model.InfraEvents.Event;
import com.algoprep.judge.domain.model.InfraEvents.Row;
import java.util.Map;

/** Application-owned templates also parameterize SQL keyword matching of the displayed text. */
public final class InfraEventMessages {
  private InfraEventMessages() {}

  public static Map<String, String> templates() {
    return Map.of(
        "OUTBOX",
            "Bản ghi Outbox {kind} của lượt nộp {id} đã thử publish {attempts} lần, chưa được broker xác nhận",
        "WORKER_TIMEOUT",
            "Lượt nộp {id} bị đưa về lỗi hệ thống vì worker không cập nhật quá 5 phút",
        "SANDBOX_FAULT", "Lượt nộp {id}: go-judge báo lỗi hạ tầng",
        "QUEUE_UNAVAILABLE", "Lượt nộp {id}: hàng đợi không xác nhận",
        "SPEC_UNAVAILABLE", "Lượt nộp {id}: đặc tả hoặc bộ testcase không dùng được");
  }

  public static Event event(Row row) {
    String template = templates().get(row.service().equals("outbox") ? "OUTBOX" : row.kind());
    if (template == null) {
      throw new IllegalStateException("Unknown infrastructure event kind");
    }
    String message =
        template
            .replace("{id}", row.submissionId().toString())
            .replace("{kind}", row.kind())
            .replace("{attempts}", Integer.toString(row.attempts()));
    return new Event(row.position().occurredAt(), row.level(), message, row.service());
  }
}
