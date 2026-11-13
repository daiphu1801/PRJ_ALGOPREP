package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.domain.model.ActiveAdmin;
import java.util.List;

/** The five busiest administrators over the last 24 hours (#64). */
public interface ListActiveAdminsUseCase {
  List<ActiveAdmin> list();
}
