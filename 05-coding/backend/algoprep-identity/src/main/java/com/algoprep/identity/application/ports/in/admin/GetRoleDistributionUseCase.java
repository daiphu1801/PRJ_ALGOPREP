package com.algoprep.identity.application.ports.in.admin;

import com.algoprep.identity.domain.model.admin.UserGroupShare;
import java.util.List;

/** The four exclusive account groups with counts and shares (#63). */
public interface GetRoleDistributionUseCase {
  List<UserGroupShare> distribution();
}
