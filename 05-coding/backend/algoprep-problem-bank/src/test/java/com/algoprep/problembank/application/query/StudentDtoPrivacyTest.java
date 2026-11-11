package com.algoprep.problembank.application.query;
import static org.assertj.core.api.Assertions.assertThat;
import com.algoprep.problembank.application.dto.StudentProblemViews;
import com.algoprep.problembank.application.dto.StudentCatalogViews;
import com.algoprep.problembank.domain.model.PublishedProblemModels;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
class StudentDtoPrivacyTest {
  @Test void learnerHttpShapesContainNoAuthorSourceOrStorageKeys() {
    for(Class<?> outer:List.of(StudentProblemViews.class,StudentCatalogViews.class)) {
      for(Class<?> dto:outer.getDeclaredClasses()) {
        if(dto.isRecord()) assertThat(Arrays.stream(dto.getRecordComponents()).map(component->component.getName()).toList())
          .as(dto.getSimpleName()).doesNotContain("referenceSolution","sourceCode","aiBrief","inputObjectKey","expectedOutputObjectKey","testcases");
      }
    }
    for(Class<?> dto:List.of(PublishedProblemModels.Workspace.class,PublishedProblemModels.Overview.class))
      assertThat(Arrays.stream(dto.getRecordComponents()).map(component->component.getName()).toList())
        .doesNotContain("referenceSolution","sourceCode","aiBrief","inputObjectKey","expectedOutputObjectKey","testcases");
  }
}
