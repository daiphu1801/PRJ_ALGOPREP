package com.algoprep.problembank.infrastructure.persistence;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.problembank.domain.model.ProblemContentData;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Relation writes belong to the parent transaction; reference source is never logged. */
@Component
class JdbcProblemContentRelations {
  private final ObjectProvider<JdbcClient> jdbc;
  private final IdGeneratorPort ids;

  JdbcProblemContentRelations(ObjectProvider<JdbcClient> jdbc, IdGeneratorPort ids) {
    this.jdbc = jdbc;
    this.ids = ids;
  }

  List<UUID> topics(UUID problem) {
    return jdbc.getObject()
        .sql("SELECT topic_id FROM problem.problem_topics WHERE problem_id=:id ORDER BY topic_id")
        .param("id", problem)
        .query(UUID.class)
        .list();
  }

  List<String> tags(UUID problem) {
    return jdbc.getObject()
        .sql(
            "SELECT t.name FROM problem.tags t JOIN problem.problem_tags pt ON pt.tag_id=t.id WHERE pt.problem_id=:id ORDER BY lower(t.name),t.id")
        .param("id", problem)
        .query(String.class)
        .list();
  }

  List<ProblemContentData.Example> examples(UUID problem) {
    return jdbc.getObject()
        .sql(
            "SELECT id,position,input,output,explanation_md FROM problem.worked_examples WHERE problem_id=:id ORDER BY position,id")
        .param("id", problem)
        .query(
            (rs, row) ->
                new ProblemContentData.Example(
                    rs.getObject("id", UUID.class),
                    rs.getInt("position"),
                    rs.getString("input"),
                    rs.getString("output"),
                    rs.getString("explanation_md")))
        .list();
  }

  ProblemContentData.Solution solution(UUID problem) {
    return jdbc.getObject()
        .sql(
            "SELECT language,source_code FROM problem.problem_reference_solutions WHERE problem_id=:id")
        .param("id", problem)
        .query(
            (rs, row) ->
                new ProblemContentData.Solution(
                    rs.getString("language"), rs.getString("source_code")))
        .optional()
        .orElse(null);
  }

  void replace(UUID problem, ProblemContentData content) {
    jdbc.getObject()
        .sql("DELETE FROM problem.problem_topics WHERE problem_id=:id")
        .param("id", problem)
        .update();
    for (var topic : content.topicIds()) {
      jdbc.getObject()
          .sql("INSERT INTO problem.problem_topics(problem_id,topic_id) VALUES(:problem,:topic)")
          .param("problem", problem)
          .param("topic", topic)
          .update();
    }
    replaceTags(problem, content.tags());
    replaceExamples(problem, content.workedExamples());
    replaceSolution(problem, content.referenceSolution());
  }

  private void replaceTags(UUID problem, List<String> tags) {
    jdbc.getObject()
        .sql("DELETE FROM problem.problem_tags WHERE problem_id=:id")
        .param("id", problem)
        .update();
    var ordered = new ArrayList<>(tags);
    ordered.sort(
        Comparator.comparing((String name) -> name.toLowerCase(Locale.ROOT))
            .thenComparing(Comparator.naturalOrder()));
    for (String name : ordered) {
      UUID tag =
          jdbc.getObject()
              .sql(
                  "INSERT INTO problem.tags(id,name) VALUES(:id,:name) ON CONFLICT (lower(name)) DO UPDATE SET name=tags.name RETURNING id")
              .param("id", ids.newId())
              .param("name", name)
              .query(UUID.class)
              .single();
      jdbc.getObject()
          .sql(
              "INSERT INTO problem.problem_tags(problem_id,tag_id) VALUES(:problem,:tag) ON CONFLICT DO NOTHING")
          .param("problem", problem)
          .param("tag", tag)
          .update();
    }
  }

  private void replaceExamples(UUID problem, List<ProblemContentData.Example> examples) {
    jdbc.getObject()
        .sql("DELETE FROM problem.worked_examples WHERE problem_id=:id")
        .param("id", problem)
        .update();
    int position = 0;
    for (var example : examples) {
      jdbc.getObject()
          .sql(
              "INSERT INTO problem.worked_examples(id,problem_id,position,input,output,explanation_md) VALUES(:id,:problem,:position,:input,:output,:explanation)")
          .param("id", example.id() == null ? ids.newId() : example.id())
          .param("problem", problem)
          .param("position", ++position)
          .param("input", example.input())
          .param("output", example.output())
          .param("explanation", example.explanationMd(), Types.VARCHAR)
          .update();
    }
  }

  private void replaceSolution(UUID problem, ProblemContentData.Solution solution) {
    if (solution == null) {
      jdbc.getObject()
          .sql("DELETE FROM problem.problem_reference_solutions WHERE problem_id=:id")
          .param("id", problem)
          .update();
      return;
    }
    jdbc.getObject()
        .sql(
            """
        INSERT INTO problem.problem_reference_solutions(id,problem_id,language,source_code,updated_at)
        VALUES(:id,:problem,:language,:source,clock_timestamp())
        ON CONFLICT(problem_id) DO UPDATE SET language=EXCLUDED.language,source_code=EXCLUDED.source_code,updated_at=EXCLUDED.updated_at
        """)
        .param("id", ids.newId())
        .param("problem", problem)
        .param("language", solution.language())
        .param("source", solution.sourceCode())
        .update();
  }
}
