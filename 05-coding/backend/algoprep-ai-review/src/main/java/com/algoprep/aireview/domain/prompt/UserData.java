package com.algoprep.aireview.domain.prompt;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Layer 3: named data blocks. Has no constructor that accepts a {@link SystemInstruction}; every
 * block is wrapped in a tag carrying the per-call nonce.
 */
public record UserData(String nonce, List<DataBlock> blocks) {
  public UserData {
    blocks = List.copyOf(blocks);
  }

  public String render() {
    return blocks.stream()
        .map(b -> PromptComposer.wrap(b.name().tag(), nonce, b.content()))
        .collect(Collectors.joining("\n"));
  }
}
