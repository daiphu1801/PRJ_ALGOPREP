package com.algoprep.identity.application.ports.in.audit;

import com.algoprep.identity.domain.model.audit.ActiveAdmin;
import java.util.List;

/** The five busiest administrators over the last 24 hours (#64). */
public interface ListActiveAdminsUseCase {
  List<ActiveAdmin> list();
}
