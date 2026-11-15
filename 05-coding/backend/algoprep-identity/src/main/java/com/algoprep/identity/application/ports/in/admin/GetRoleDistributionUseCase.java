package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.domain.model.UserGroupShare;
import java.util.List;

/** The four exclusive account groups with counts and shares (#63). */
public interface GetRoleDistributionUseCase {
  List<UserGroupShare> distribution();
}
