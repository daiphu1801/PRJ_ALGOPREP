package com.algoprep.problembank.application.command;

import com.algoprep.problembank.application.dto.BulkTestcaseUpload.Target;
import com.algoprep.problembank.application.dto.TestcaseUploadSession.Side;
import com.algoprep.problembank.application.ports.out.TestcaseObjectStorePort;
import com.algoprep.problembank.domain.exception.TestcaseStorageUnavailableException;
import com.algoprep.problembank.domain.exception.TestcaseUploadInvalidException.Fault;
import com.algoprep.problembank.domain.model.StoredGradingTestcase.Content;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * The MinIO side of BR-14: signs the PUT for a key the server made, and at confirm checks every
 * object with a HEAD (size equals the declared size) and reads the small sides as UTF-8.
 */
@Component
class BulkUploadObjects {
  static final Duration PUT_TTL = Duration.ofMinutes(15);

  private final TestcaseObjectStorePort store;
  private final TestcaseContentStore contents;

  BulkUploadObjects(TestcaseObjectStorePort store, TestcaseContentStore contents) {
    this.store = store;
    this.contents = contents;
  }

  UUID newId() {
    return contents.newTestcaseId();
  }

  Side declare(UUID problemId, UUID testcaseId, String side, long sizeBytes) {
    return new Side(sizeBytes, contents.newKey(problemId, testcaseId, side));
  }

  Target sign(Side side) {
    return new Target(store.presignPut(side.objectKey(), PUT_TTL));
  }

  /** What the HEAD and read pass found: every fault, and the text of each small side by key. */
  static final class Verified {
    final List<Fault> faults = new ArrayList<>();
    final Map<String, String> smallTexts = new HashMap<>();
  }

  /** A storage failure other than a missing object propagates as 503 PRB-052. */
  void verify(String clientKey, String name, Side side, Verified into) {
    long size;
    try {
      size = store.head(side.objectKey());
    } catch (TestcaseStorageUnavailableException failure) {
      if (!failure.isObjectMissing()) {
        throw failure;
      }
      into.faults.add(new Fault(clientKey, name, "MISSING"));
      return;
    }
    if (size != side.sizeBytes()) {
      into.faults.add(new Fault(clientKey, name, "SIZE_MISMATCH"));
      return;
    }
    if (size > TestcaseContentStore.INLINE_LIMIT_BYTES) {
      return;
    }
    String text = decode(store.get(side.objectKey()));
    if (text == null) {
      into.faults.add(new Fault(clientKey, name, "INVALID_TEXT"));
      return;
    }
    into.smallTexts.put(side.objectKey(), text);
  }

  /**
   * A small side becomes an INLINE value and its object is removed after commit; a large side keeps
   * the object the browser uploaded.
   */
  Content content(Side side, Verified verified) {
    if (side.sizeBytes() > TestcaseContentStore.INLINE_LIMIT_BYTES) {
      return new Content("MINIO", null, side.objectKey());
    }
    var uploaded = new Content("MINIO", null, side.objectKey());
    contents.discardAfterCommit(uploaded);
    return new Content("INLINE", verified.smallTexts.get(side.objectKey()), null);
  }

  private static String decode(byte[] bytes) {
    try {
      return StandardCharsets.UTF_8
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(bytes))
          .toString();
    } catch (CharacterCodingException invalid) {
      return null;
    }
  }
}
