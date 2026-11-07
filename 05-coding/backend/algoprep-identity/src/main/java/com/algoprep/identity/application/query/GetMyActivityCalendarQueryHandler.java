package com.algoprep.identity.application.query;

import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.identity.application.ports.in.GetMyActivityCalendarUseCase;
import com.algoprep.identity.domain.model.ActivityCalendar;
import com.algoprep.identity.domain.ports.out.MyProgressReadRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Always 364 days ending today (UTC+7); the screen never patches gaps itself. */
@Service
class GetMyActivityCalendarQueryHandler implements GetMyActivityCalendarUseCase {
  static final int DAYS = 364;
  private static final ZoneOffset LOCAL = ZoneOffset.ofHours(7);

  private final MyProgressReadRepository progress;
  private final ClockPort clock;

  GetMyActivityCalendarQueryHandler(MyProgressReadRepository progress, ClockPort clock) {
    this.progress = progress;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public ActivityCalendar get(UUID userId) {
    LocalDate to = clock.now().atOffset(LOCAL).toLocalDate();
    LocalDate from = to.minusDays(DAYS - 1);
    Map<LocalDate, Integer> counts = progress.dailyCounts(userId, from, to);
    List<ActivityCalendar.Day> days = new ArrayList<>(DAYS);
    for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
      days.add(new ActivityCalendar.Day(d, counts.getOrDefault(d, 0)));
    }
    return new ActivityCalendar(from, to, days);
  }
}
