package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.Nonnull;
import java.util.concurrent.Semaphore;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Publishes messages to a {@link Flux}. Uses a semaphore to limit the number of unconsumed messages
 * that will be written to the flux at any given moment. Calls to {@link #emit} will block until
 * space is available. Each instance only allows a single subscriber to its {@link Flux}.
 *
 * @param <T> type of objects being published
 */
public class BlockingPublisher<T> {
  /** Limits the number of unconsumed messages that can be emitted. */
  private final Semaphore available;

  /** Used to actually publish messages to subscribers. */
  private final Sinks.Many<T> reactiveSink;

  /** {@link Flux} that subscribers use to receive messages. */
  private final Flux<T> flux;

  /**
   * Creates an instance with the specified limit on number of unconsumed messages.
   *
   * @param startingAllowance number of unconsumed messages initally allowed
   */
  public BlockingPublisher(int startingAllowance) {
    available = new Semaphore(startingAllowance, true);
    reactiveSink = Sinks.many().unicast().onBackpressureBuffer();
    flux = reactiveSink.asFlux();
  }

  /**
   * Emits a message to the {@link Flux}. If the maximum allowed number of unconsumed messages have
   * already been emitted this call will block until one of them has been consumed and allowance
   * increased by a call to {@link #allow}.
   *
   * @param message the message to emit
   * @throws InterruptedException if waiting is interrupted
   */
  public void emit(T message) throws InterruptedException {
    available.acquire();
    reactiveSink.emitNext(message, Sinks.EmitFailureHandler.FAIL_FAST);
  }

  /** Sends a completed signal to subscribers. */
  public void complete() {
    reactiveSink.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
  }

  /**
   * Increases the allowance by the stated amount. Used to permit more calls to the {@link #emit}
   * method.
   *
   * @param count number messages by which to increase the allowance
   */
  public void allow(int count) {
    available.release(count);
  }

  /**
   * Provides access to the {@link Flux} so that it can be subscribed to.
   *
   * @return the {@link Flux}
   */
  @Nonnull
  public Flux<T> flux() {
    return flux;
  }

  /**
   * Used to allow tests to verify permits change as expected.
   *
   * @return current number of available permits
   */
  @VisibleForTesting
  int getAvailablePermits() {
    return available.availablePermits();
  }
}
