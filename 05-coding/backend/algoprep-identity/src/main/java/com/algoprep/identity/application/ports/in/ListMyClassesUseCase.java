package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.domain.model.MyClass;
import java.util.List;
import java.util.UUID;

/** Lists the classes the caller currently belongs to (#25A). */
public interface ListMyClassesUseCase {
  List<MyClass> list(UUID userId);
}
