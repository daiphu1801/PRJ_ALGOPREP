package com.algoprep.judge.infrastructure.gojudge;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/** Cancels oversized bodies before allocating or parsing an unbounded response. */
final class BoundedJudgeBody implements HttpResponse.BodySubscriber<byte[]> {
  private final HttpResponse.BodySubscriber<byte[]> delegate =
      HttpResponse.BodySubscribers.ofByteArray();
  private final long limit;
  private long received;
  private Flow.Subscription subscription;
  private boolean failed;

  BoundedJudgeBody(long limit) {
    this.limit = limit;
  }

  @Override
  public CompletionStage<byte[]> getBody() {
    return delegate.getBody();
  }

  @Override
  public void onSubscribe(Flow.Subscription incoming) {
    subscription = incoming;
    delegate.onSubscribe(incoming);
  }

  @Override
  public void onNext(List<ByteBuffer> buffers) {
    if (failed) {
      return;
    }
    long bytes = buffers.stream().mapToLong(ByteBuffer::remaining).sum();
    if (bytes > limit - received) {
      failed = true;
      subscription.cancel();
      delegate.onError(new IOException("Judge response exceeds its byte limit"));
      return;
    }
    received += bytes;
    delegate.onNext(buffers);
  }

  @Override
  public void onError(Throwable failure) {
    delegate.onError(failure);
  }

  @Override
  public void onComplete() {
    delegate.onComplete();
  }
}
