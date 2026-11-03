package com.algoprep.identity.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.identity.domain.ports.out.RefreshTokenRepository;
import org.junit.jupiter.api.Test;

class PurgeRefreshTokensCommandHandlerTest {
  private final RefreshTokenRepository tokens = mock(RefreshTokenRepository.class);
  private final PurgeRefreshTokensCommandHandler handler =
      new PurgeRefreshTokensCommandHandler(tokens);

  @Test
  void keepsDeletingFullBatchesAndStopsAtTheFirstShortOne() {
    int full = PurgeRefreshTokensCommandHandler.BATCH_SIZE;
    when(tokens.deleteExpiredBatch(full)).thenReturn(full, full, 3);
    assertThat(handler.purge()).isEqualTo(2 * full + 3);
    verify(tokens, times(3)).deleteExpiredBatch(full);
  }

  @Test
  void nothingToDeleteIsOneCallAndZero() {
    when(tokens.deleteExpiredBatch(PurgeRefreshTokensCommandHandler.BATCH_SIZE)).thenReturn(0);
    assertThat(handler.purge()).isZero();
  }
}
