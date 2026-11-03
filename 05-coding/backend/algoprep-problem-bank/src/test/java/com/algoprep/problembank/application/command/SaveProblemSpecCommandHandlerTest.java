package com.algoprep.problembank.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.exception.ClientErrorDetails;
import com.algoprep.problembank.application.dto.SaveProblemSpecCommand;
import com.algoprep.problembank.application.ports.out.ProblemSpecCachePort;
import com.algoprep.problembank.domain.model.ProblemWriteState;
import com.algoprep.problembank.domain.model.StoredProblemSpec;
import com.algoprep.problembank.domain.ports.out.ProblemSpecRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

class SaveProblemSpecCommandHandlerTest {
  static final JsonMapper JSON =
      JsonMapper.builder().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).build();
  static final String VALID =
      "{\"functionName\":\"solve\",\"returnType\":{\"kind\":\"INT\"},\"parameters\":[]}";
  private final UUID id = UUID.randomUUID();
  private final UUID actor = UUID.randomUUID();
  private final Instant time = Instant.parse("2026-11-02T00:00:00Z");
  private final ProblemSpecRepository repository = mock(ProblemSpecRepository.class);
  private final ProblemSpecCachePort cache = mock(ProblemSpecCachePort.class);
  private final IdGeneratorPort ids = mock(IdGeneratorPort.class);
  private final SaveProblemSpecCommandHandler handler =
      new SaveProblemSpecCommandHandler(repository, cache, ids);

  @BeforeEach
  void setup() {
    when(repository.lock(id))
        .thenReturn(Optional.of(new ProblemWriteState(actor, "UNPUBLISHED", time)));
    when(repository.find(id)).thenReturn(Optional.empty());
    when(repository.touch(id, actor, true)).thenReturn(time.plusSeconds(1));
    when(repository.touch(id, actor, false)).thenReturn(time.plusSeconds(1));
    when(ids.newId()).thenReturn(UUID.randomUUID());
  }

  SaveProblemSpecCommand command(String body) {
    return new SaveProblemSpecCommand(id, actor, false, time, JSON.readTree(body));
  }

  @Test
  void createsWithExactDefaultAndDurableEnvelopeAndSchedulesCache() {
    var result = handler.save(command(VALID));
    assertThat(result.functionWrapperSupported()).isTrue();
    assertThat(result.spec().path("matchingStrategy").asString()).isEqualTo("EXACT");
    assertThat(result.spec().path("specVersion").asInt()).isEqualTo(1);
    assertThat(result.problemUpdatedAt()).isEqualTo(time.plusSeconds(1));
    var envelope = ArgumentCaptor.forClass(String.class);
    verify(repository).appendEvent(eq(id), envelope.capture());
    var event = JSON.readTree(envelope.getValue());
    assertThat(event.path("eventType").asString()).isEqualTo("ProblemSpecChanged");
    assertThat(event.path("payload").path("signatureChanged").asBoolean()).isTrue();
    assertThat(event.path("schemaVersion").asInt()).isEqualTo(1);
    verify(cache).invalidateAfterCommit(id);
  }

  @Test
  void unchangedJsonObjectOrderingAndEpsilonScaleDoNotWrite() {
    String body = VALID.replace("INT\"}", "ARRAY\",\"of\":{\"kind\":\"INT\"},\"dimensions\":1}");
    var data = ProblemSpecJson.data(JSON.readTree(body), null);
    when(repository.find(id)).thenReturn(Optional.of(new StoredProblemSpec(data, 7)));
    var result =
        handler.save(
            command(
                body.replace("\"kind\":\"ARRAY\",", "\"dimensions\":1,\"kind\":\"ARRAY\",")
                    .replace(",\"dimensions\":1}", "}")));
    assertThat(result.spec().path("specVersion").asInt()).isEqualTo(7);
    assertThat(result.problemUpdatedAt()).isEqualTo(time);
    verify(repository, never()).save(any(), any());
    verify(repository, never()).appendEvent(any(), any());
    verify(cache, never()).invalidateAfterCommit(any());
  }

  @Test
  void formatOnlyChangeIncrementsVersionAndRetainsMatchingDefaultFromExistingRow() {
    var data =
        ProblemSpecJson.data(
            JSON.readTree(
                VALID.replace(
                    "{\"functionName", "{\"matchingStrategy\":\"TRIMMED\",\"functionName")),
            null);
    when(repository.find(id)).thenReturn(Optional.of(new StoredProblemSpec(data, 4)));
    var result =
        handler.save(
            command(
                VALID.replace("{\"functionName", "{\"stdinFormatMd\":\"Read n\",\"functionName")));
    assertThat(result.spec().path("specVersion").asInt()).isEqualTo(5);
    assertThat(result.spec().path("matchingStrategy").asString()).isEqualTo("TRIMMED");
    var payload = ArgumentCaptor.forClass(String.class);
    verify(repository).appendEvent(eq(id), payload.capture());
    assertThat(JSON.readTree(payload.getValue()).path("payload").path("changedColumns").toString())
        .isEqualTo("[\"stdinFormatMd\"]");
    assertThat(
            JSON.readTree(payload.getValue()).path("payload").path("signatureChanged").asBoolean())
        .isFalse();
  }

  @ParameterizedTest
  @CsvSource({"UNPUBLISHED,PRB-010", "PUBLISHED,PRB-003"})
  void anotherAuthorsDraftIsHiddenAndPublishedProblemIsForbidden(String status, String code) {
    when(repository.lock(id))
        .thenReturn(Optional.of(new ProblemWriteState(UUID.randomUUID(), status, time)));
    rejected(command(VALID), code);
  }

  @Test
  void adminCanWriteAnotherAuthorsDraft() {
    when(repository.lock(id))
        .thenReturn(Optional.of(new ProblemWriteState(UUID.randomUUID(), "UNPUBLISHED", time)));
    var input = command(VALID);
    assertThat(
            handler
                .save(new SaveProblemSpecCommand(id, actor, true, time, input.body()))
                .functionWrapperSupported())
        .isTrue();
  }

  @Test
  void missingProblemAndStaleOrMissingTimestampNeverWrite() {
    when(repository.lock(id)).thenReturn(Optional.empty());
    rejected(command(VALID), "PRB-010");
    when(repository.lock(id))
        .thenReturn(Optional.of(new ProblemWriteState(actor, "UNPUBLISHED", time.plusSeconds(1))));
    rejected(command(VALID), "PRB-020");
    rejected(new SaveProblemSpecCommand(id, actor, false, null, JSON.readTree(VALID)), "PRB-001");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{}",
        "{\"functionName\":null,\"returnType\":null,\"parameters\":null}",
        "{\"functionName\":\"solve\",\"returnType\":{\"kind\":\"GRAPH\"},\"parameters\":[]}"
      })
  void absentSignatureAndUnknownKindsSaveWithWarnings(String body) {
    var result = handler.save(command(body));
    assertThat(result.functionWrapperSupported()).isFalse();
    assertThat(result.warnings()).isNotEmpty().allMatch(w -> w.code().equals("PRB-W01"));
    if (body.contains("GRAPH")) {
      assertThat(result.spec().path("returnType").path("kind").asString()).isEqualTo("GRAPH");
    }
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "{\"functionName\":\"solve\"}|HRN-V17",
        "{\"functionName\":\"1bad\",\"returnType\":{\"kind\":\"INT\"},\"parameters\":[]}|HRN-V11",
        "{\"matchingStrategy\":\"EPSILON\"}|HRN-V20",
        "{\"matchingStrategy\":\"UNORDERED_SET\",\"functionName\":\"solve\",\"returnType\":{\"kind\":\"BINARY_TREE\",\"of\":{\"kind\":\"INT\"}},\"parameters\":[]}|HRN-V22",
        "{\"functionName\":\"solve\",\"returnType\":5,\"parameters\":[]}|HRN-V10",
        "{\"functionName\":\"solve\",\"returnType\":{\"kind\":7},\"parameters\":[]}|HRN-V10",
        "{\"functionName\":\"solve\",\"returnType\":{\"kind\":\"INT\",\"other\":1},\"parameters\":[]}|HRN-V10",
        "{\"functionName\":\"solve\",\"returnType\":{\"kind\":\"ARRAY\",\"dimensions\":1.5},\"parameters\":[]}|HRN-V10",
        "{\"functionName\":\"solve\",\"returnType\":{\"kind\":\"LIST\",\"of\":null},\"parameters\":[]}|HRN-V10",
        "{\"nameOverrides\":{\"JAVA\":\"ok\",\"java\":\"alsoOk\"}}|HRN-V18",
        "{\"nameOverrides\":{\"PYTHON\":3}}|HRN-V19",
        "{\"functionName\":\"solve\",\"returnType\":{\"kind\":\"INT\"},\"parameters\":[null]}|HRN-V10",
        "{\"functionName\":\"solve\",\"returnType\":{\"kind\":\"INT\"},\"parameters\":[{\"name\":\"n\",\"type\":{\"kind\":\"INT\"}},{\"name\":\"n\",\"type\":{\"kind\":\"INT\"}}]}|HRN-V14"
      })
  void hardViolationsHaveStableReasonAndDoNotWrite(String body, String reason) {
    assertThatThrownBy(() -> handler.save(command(body)))
        .isInstanceOfSatisfying(
            ClientErrorDetails.class,
            e -> assertThat(e.clientErrorDetails().get("violations").toString()).contains(reason));
    verify(repository, never()).save(any(), any());
    verify(repository, never()).appendEvent(any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "-1", "0.0000000000001", "1000000", "999999.9999999999991"})
  void rejectsNonrepresentableEpsilonWithoutDatabaseRounding(String epsilon) {
    rejected(command("{\"epsilonValue\":" + epsilon + "}"), "PRB-036");
  }

  @ParameterizedTest
  @ValueSource(strings = {"0.000000000001", "999999.999999999999", "1.0000000000000"})
  void acceptsExactEpsilonBoundaryEvenOutsideEpsilonStrategy(String epsilon) {
    assertThat(
            handler
                .save(command("{\"epsilonValue\":" + epsilon + "}"))
                .spec()
                .path("epsilonValue")
                .decimalValue())
        .isEqualByComparingTo(epsilon);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "[]",
        "null",
        "{\"matchingStrategy\":\"WRONG\"}",
        "{\"epsilonValue\":\"1\"}",
        "{\"parameters\":{}}",
        "{\"nameOverrides\":[]}",
        "{\"functionName\":1}"
      })
  void outerShapeErrorsAre400(String body) {
    rejected(command(body), "PRB-001");
  }

  @Test
  void formatBoundaryAndUppercaseOverridesAndNestedParameters() {
    String body =
        "{\"stdinFormatMd\":\""
            + "x".repeat(20000)
            + "\",\"functionName\":\"solve\",\"returnType\":{\"kind\":\"LIST\",\"of\":{\"kind\":\"INT\"}},\"parameters\":[{\"name\":\"n\",\"type\":{\"kind\":\"INT\"}}],\"nameOverrides\":{\"python\":\"solve_it\"}}";
    assertThat(handler.save(command(body)).spec().path("nameOverrides").path("PYTHON").asString())
        .isEqualTo("solve_it");
    assertThatThrownBy(() -> handler.save(command(body.replace("x\"", "xx\""))))
        .isInstanceOf(AlgoPrepException.class);
  }

  @Test
  void outboxFailureIsSanitized503AndDoesNotScheduleInvalidation() {
    doThrow(new org.springframework.dao.DataAccessResourceFailureException("secret SQL fixture"))
        .when(repository)
        .appendEvent(any(), any());
    assertThatThrownBy(() -> handler.save(command(VALID)))
        .isInstanceOfSatisfying(
            com.algoprep.common.exception.ServiceUnavailableException.class,
            error -> {
              assertThat(error.errorCode()).isEqualTo("PRB-056");
              assertThat(error.getMessage()).isEqualTo("Problem event persistence unavailable");
              assertThat(error.getCause()).isNull();
            });
    verify(cache, never()).invalidateAfterCommit(any());
  }

  private void rejected(SaveProblemSpecCommand input, String code) {
    assertThatThrownBy(() -> handler.save(input))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo(code));
    verify(repository, never()).save(any(), any());
    verify(cache, never()).invalidateAfterCommit(any());
  }
}
