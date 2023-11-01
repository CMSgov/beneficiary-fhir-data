package gov.cms.bfd.pipeline.sharedutils;

import com.google.common.annotations.VisibleForTesting;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Utility class for coordinating the wait for a publisher ({@link Mono} or {@link Flux}) to finish
 * processing. Callers must provide an {@link AtomicBoolean} that can be used to signal to the
 * publisher that we have been interrupted while waiting so that it can cleanly shut itself down.
 * Callers are expected to use this flag in the appropriate {@code takeUntil} operator on the
 * publisher.
 */
@Slf4j
public class FluxWaiter {
  /** Maximum amount of time to wait for flux to complete normally. */
  private final Duration normalWaitTime;

  /** Maximum amount of time to wait for mono to complete following an interrupt. */
  private final Duration interruptedWaitTime;

  /** Provides current time values. */
  private final Clock clock;

  /** Creates latch instances on demand. */
  private final Function<Integer, CountDownLatch> latchFactory;

  /**
   * Initializes an instance to wait for any flux or mono. Uses the provided standard system clock
   * for time values.
   *
   * @param normalWaitTime maximum amount of time to wait for the mono to complete normally
   * @param interruptedWaitTime Maximum amount of time to wait for mono to complete following an
   *     interrupt
   */
  public FluxWaiter(Duration normalWaitTime, Duration interruptedWaitTime) {
    this(normalWaitTime, interruptedWaitTime, Clock.systemUTC(), CountDownLatch::new);
  }

  /**
   * Constructor intended for use in unit tests. Allows a non-standard clock and latch factory to be
   * provided for each test.
   *
   * @param normalWaitTime maximum amount of time to wait for the mono to complete normally
   * @param interruptedWaitTime Maximum amount of time to wait for mono to complete following an
   *     interrupt
   * @param clock used to get time values
   */
  @VisibleForTesting
  FluxWaiter(
      Duration normalWaitTime,
      Duration interruptedWaitTime,
      Clock clock,
      Function<Integer, CountDownLatch> latchFactory) {
    this.normalWaitTime = normalWaitTime;
    this.interruptedWaitTime = interruptedWaitTime;
    this.clock = clock;
    this.latchFactory = latchFactory;
  }

  /**
   * Waits for the provided {@link Flux} to terminate and returns the number of values that it
   * published while we waited. Any exception reported by the flux is unwrapped and rethrown from
   * this method. Any raw {@link Throwable} from the flux is wrapped and thrown.
   *
   * @param flux the flux to wait for
   * @param interruptedFlag the {@link AtomicBoolean} used to signal that an interrupt has been
   *     detected so that the flux can shut itself down cleanly before we unsubscribe interrupt
   * @return number of values published by the flux (possibly zero)
   * @throws Exception any error reported by the flux
   */
  public long waitForCompletion(Flux<?> flux, AtomicBoolean interruptedFlag) throws Exception {
    return waitForCompletion(flux.count(), interruptedFlag).orElse(0L);
  }

  /**
   * Waits for the provided {@link Mono} to terminate and returns the value that it published (if
   * any). Any exception reported by the mono is unwrapped and rethrown from this method. Any raw
   * {@link Throwable} from the mono is wrapped and thrown.
   *
   * @param mono the mono to wait for
   * @param interruptedFlag the {@link AtomicBoolean} used to signal that an interrupt has been
   *     detected so that the mono can shut itself down cleanly before we unsubscribe
   * @return value published by the mono (if any)
   * @throws Exception any error reported by the flux
   * @param <T> type of value published by the mono
   */
  public <T> Optional<T> waitForCompletion(Mono<T> mono, AtomicBoolean interruptedFlag)
      throws Exception {
    final var resultValue = new AtomicReference<T>();
    final var failureValue = new AtomicReference<Throwable>();
    final var shutdownSynchronizationLatch = latchFactory.apply(1);

    // Subscribe to the mono.  We capture any error or result and ensure our latch is updated when
    // the mono terminates.
    final var subscription =
        mono.doFinally(ignored -> shutdownSynchronizationLatch.countDown())
            .subscribe(resultValue::set, failureValue::set);

    // Loop until the mono completes or our timeout expires.
    long endMillis = clock.millis() + normalWaitTime.toMillis();
    boolean interrupted = false;
    boolean countdownInProgress = true;
    while (countdownInProgress) {
      // Update the maximum wait time on each iteration to stay within allowed time.
      // Stop the loop if the time has been exceeded.
      final long remainingMillis = endMillis - clock.millis();
      if (remainingMillis <= 0) {
        break;
      }

      // Check for countdown completion.  Remember if we encountered any interrupts along the way.
      try {
        log.info("waiting at most {} millis", remainingMillis);
        if (shutdownSynchronizationLatch.await(remainingMillis, TimeUnit.MILLISECONDS)) {
          countdownInProgress = false;
        }
      } catch (InterruptedException ex) {
        // Update max wait time on first interrupt.
        if (!interrupted) {
          interrupted = true;

          // Adjust our wait time to use the interrupted timeout instead of the normal one.
          // Using min handles the possibility we might have timed out sooner with the original
          // timeout than with the interrupt one.
          endMillis =
              Math.min(endMillis, System.currentTimeMillis() + interruptedWaitTime.toMillis());

          // Tell the upstream that we want it to stop processing data.
          interruptedFlag.set(true);
        }
      }
    }

    // Clean up if the timeout ended before the mono completed.
    if (countdownInProgress) {
      log.warn("wait time exceeded without reaching zero");

      // tell the upstream that we want it to stop processing data
      interruptedFlag.set(true);

      // cancel our subscription to the mono
      subscription.dispose();
    }

    // Let the caller know if we were interrupted.
    if (interrupted) {
      throw new InterruptedException();
    }

    // Let the caller know if the timeout expired.
    if (countdownInProgress) {
      throw new TimeoutExceededException();
    }

    // Throw any exception, unwrapping it if necessary.
    final Throwable error = failureValue.get();
    if (error != null) {
      if (error instanceof RuntimeException) {
        final Throwable unwrapped = Exceptions.unwrap(error);
        if (unwrapped instanceof RuntimeException re) {
          throw re;
        } else if (unwrapped instanceof Exception e) {
          throw e;
        }
      } else if (error instanceof Exception e) {
        // rethrow checked exceptions unchanged
        throw e;
      } else {
        // Must have been a Throwable and we don't want to throw a raw Throwable
        // so throw it as a wrapped exception.
        throw Exceptions.propagate(error);
      }
    }

    // Good news, nothing went wrong!
    return Optional.ofNullable(resultValue.get());
  }

  /** Exception thrown when wait time expires but a publisher has not terminated. */
  public static class TimeoutExceededException extends RuntimeException {}
}
