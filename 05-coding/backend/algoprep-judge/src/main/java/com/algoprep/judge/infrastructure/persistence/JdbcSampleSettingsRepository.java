package com.algoprep.judge.infrastructure.persistence;

import com.algoprep.judge.domain.model.SampleSettings;
import com.algoprep.judge.domain.model.WorkerSettings;
import com.algoprep.judge.domain.ports.out.SampleSettingsRepository;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcSampleSettingsRepository implements SampleSettingsRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcSampleSettingsRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<SampleSettings> read() {
    return jdbc.getObject()
        .sql(
            """
        SELECT l.language,l.enabled,l.time_limit_multiplier,l.memory_limit_multiplier,
          d.time_limit_ms,d.memory_limit_mb,d.output_limit_kb,d.compile_timeout_sec,
          s.network_access_enabled,s.child_process_limit_enabled,s.return_stderr_to_student
        FROM judge.language_configs l CROSS JOIN judge.grading_defaults d
          CROSS JOIN judge.sandbox_settings s WHERE d.id=1 AND s.id=1 ORDER BY l.language
        """)
        .query(
            (r, n) ->
                new SampleSettings(
                    r.getString(1),
                    r.getBoolean(2),
                    new WorkerSettings(
                        r.getBigDecimal(3),
                        r.getBigDecimal(4),
                        r.getInt(5),
                        r.getInt(6),
                        r.getInt(7),
                        r.getInt(8),
                        r.getBoolean(9),
                        r.getBoolean(10),
                        r.getBoolean(11))))
        .list();
  }
}
