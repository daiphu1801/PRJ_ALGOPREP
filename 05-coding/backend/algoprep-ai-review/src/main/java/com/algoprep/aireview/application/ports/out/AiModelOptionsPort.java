package com.algoprep.aireview.application.ports.out;

import java.util.List;

/** The dev-declared {@code ai.models.allowed} list the admin may pick from per prompt. */
public interface AiModelOptionsPort {
  List<String> allowed();
}
