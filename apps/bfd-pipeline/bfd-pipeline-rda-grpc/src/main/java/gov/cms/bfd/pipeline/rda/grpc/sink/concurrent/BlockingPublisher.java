package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import java.util.concurrent.Semaphore;
import javax.annotation.Nonnull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;

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
   * @param maxAvailable number of unconsumed messages allowed at any given time
   */
  public BlockingPublisher(int maxAvailable, Scheduler scheduler) {
    available = new Semaphore(maxAvailable, true);
    reactiveSink = Sinks.many().unicast().onBackpressureBuffer();
    flux = reactiveSink.asFlux().publishOn(scheduler).doOnNext(o -> available.release());
  }

  /**
   * Emits a message to the {@link Flux}. If the maximum allowed number of unconsumed messages have
   * already been emitted this call will block until one of them has been consumed.
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
   * Provides access to the {@link Flux} so that it can be subscribed to.
   *
   * @return
   */
  @Nonnull
  public Flux<T> flux() {
    return flux;
  }
}
