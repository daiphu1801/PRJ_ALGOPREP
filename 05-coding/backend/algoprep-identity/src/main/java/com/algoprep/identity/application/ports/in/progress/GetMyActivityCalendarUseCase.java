package com.algoprep.identity.application.ports.in.progress;

import com.algoprep.identity.domain.model.progress.ActivityCalendar;
import java.util.UUID;

/** Personal progress read #55. */
public interface GetMyActivityCalendarUseCase {
  ActivityCalendar get(UUID userId);
}
