package com.algoprep.identity.application.ports.in.account;

import com.algoprep.identity.application.dto.account.InterviewTranscriptExport;
import java.util.UUID;

/** Exports the caller's own interview conversations, once per five minutes (#17). */
public interface ExportMyInterviewTranscriptsUseCase {
  InterviewTranscriptExport export(UUID userId);
}
