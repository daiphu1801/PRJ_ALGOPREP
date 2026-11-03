package com.algoprep.judge.application.ports.out;

import com.algoprep.judge.application.dto.SubmissionReads.Membership;
import java.util.List;
import java.util.UUID;

/** Historical memberships including left members and empty classes. Fail closed on errors. */
public interface ClassScopeReadPort {
  List<Membership> read(UUID instructorId);
}
