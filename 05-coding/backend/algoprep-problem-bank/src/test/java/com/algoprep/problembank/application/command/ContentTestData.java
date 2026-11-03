package com.algoprep.problembank.application.command;

import com.algoprep.problembank.domain.model.ProblemContentData;
import java.util.List;
import java.util.UUID;

final class ContentTestData {
  static final UUID LEVEL = UUID.fromString("20000000-0000-4000-8000-000000000001");

  private ContentTestData() {}

  static ProblemContentData valid() {
    return new ProblemContentData(
        " Bài cộng ",
        "# Raw <b>Markdown</b>",
        null,
        LEVEL,
        new ProblemContentData.Limits(1000, 256, 1024, null),
        List.of(),
        List.of(),
        List.of(),
        null);
  }

  static ProblemContentData replace(String field, Object value) {
    return replace(valid(), field, value);
  }

  static ProblemContentData replace(ProblemContentData base, String field, Object value) {
    try {
      var fields = ProblemContentData.class.getRecordComponents();
      Object[] values = new Object[fields.length];
      Class<?>[] types = new Class<?>[fields.length];
      for (int i = 0; i < fields.length; i++) {
        values[i] =
            fields[i].getName().equals(field) ? value : fields[i].getAccessor().invoke(base);
        types[i] = fields[i].getType();
      }
      return ProblemContentData.class.getDeclaredConstructor(types).newInstance(values);
    } catch (ReflectiveOperationException failure) {
      throw new IllegalStateException(failure);
    }
  }
}
