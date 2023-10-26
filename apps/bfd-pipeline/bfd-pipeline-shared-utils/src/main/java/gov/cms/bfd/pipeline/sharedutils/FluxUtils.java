package gov.cms.bfd.pipeline.sharedutils;

import gov.cms.bfd.sharedutils.interfaces.ThrowingRunnable;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Callable;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class FluxUtils {
  /**
   * Creates a {@link Mono} that, when subscribed to, calls the specified function and publishes its
   * result. Function is allowed to throw checked exceptions and they are reported by the {@link
   * Mono} unwrapped.
   *
   * @param function the function to call
   * @return mono that calls the function
   * @param <T> type returned by the function
   */
  public static <T> Mono<T> fromValueFunction(Callable<T> function) {
    return Mono.defer(
        () -> {
          try {
            return Mono.just(function.call());
          } catch (Exception ex) {
            return Mono.error(ex);
          }
        });
  }

  /**
   * Creates a {@link Mono} that, when subscribed to, calls the specified procedure. Function is
   * allowed to throw checked exceptions and they are reported by the {@link Mono} unwrapped. The
   * returned mono will always either be empty or have an error.
   *
   * @param procedure the procedure to call
   * @return mono that calls the procedure
   */
  public static Mono<Void> fromProcedure(ThrowingRunnable<? extends Exception> procedure) {
    return Mono.defer(
        () -> {
          try {
            procedure.run();
            return Mono.empty();
          } catch (Exception ex) {
            return Mono.error(ex);
          }
        });
  }

  /**
   * Creates a {@link Flux} that, when subscribed to, calls the specified function and processes the
   * flux it returns. Function is allowed to throw checked exceptions and they are reported by the
   * {@link Flux} unwrapped.
   *
   * @param function the function to call
   * @return flux that calls the function and processes to its result flux
   * @param <T> type returned by the function
   */
  public static <T> Flux<T> fromFluxFunction(Callable<Flux<T>> function) {
    return Flux.defer(
        () -> {
          try {
            return function.call();
          } catch (Exception ex) {
            return Flux.error(ex);
          }
        });
  }

  /**
   * Creates a {@link Flux} that, when subscribed to, calls the specified function and publishes the
   * contents of the {@link Iterable} that it returns. Function is allowed to throw checked
   * exceptions and they are reported by the {@link Flux} unwrapped.
   *
   * @param function the function to call
   * @return mono that calls the function
   * @param <T> type returned by the function
   */
  public static <T> Flux<T> fromIterableFunction(Callable<Iterable<T>> function) {
    return Flux.defer(
        () -> {
          try {
            return Flux.fromIterable(function.call());
          } catch (Exception ex) {
            return Flux.error(ex);
          }
        });
  }

  /**
   * Waits for the provided {@link Flux} to terminate and returns the number of values that it
   * published. Any error reported by the flux is unwrapped and rethrown from this method.
   *
   * <p>Wrapped exceptions are unwrapped if possible. This involves undoing the wrapping that might
   * have been done internally by project reactor to expose original exceptions. The unwrapping
   * makes error handling in our caller more meaningful.
   *
   * @param flux the flux to wait for
   * @param maxWaitTime the maximum amount of time to wait
   * @return number of values published by the flux while we waited
   * @throws Exception any error reported by the flux
   */
  public static long waitForCompletion(Flux<?> flux, Duration maxWaitTime) throws Exception {
    return waitForCompletion(flux.count(), maxWaitTime).orElse(0L);
  }

  /**
   * Waits for the provided {@link Mono} to terminate and returns the value that it published (if
   * any). Any error reported by the mono is unwrapped and rethrown from this method.
   *
   * <p>Wrapped exceptions are unwrapped if possible. This involves undoing the wrapping that might
   * have been done internally by project reactor to expose original exceptions. The unwrapping
   * makes error handling in our caller more meaningful.
   *
   * @param mono the mono to wait for
   * @param maxWaitTime the maximum amount of time to wait
   * @return any value published by the mono
   * @throws Exception any error reported by the flux
   * @param <T> type published by the mono
   */
  public static <T> Optional<T> waitForCompletion(Mono<T> mono, Duration maxWaitTime)
      throws Exception {
    try {
      return mono.blockOptional(maxWaitTime);
    } catch (RuntimeException wrapped) {
      final Throwable unwrapped = Exceptions.unwrap(wrapped);
      if (unwrapped instanceof RuntimeException re) {
        // might be the original or another unchecked exception that had been wrapped
        throw re;
      } else if (unwrapped instanceof Exception e) {
        // extracted an original checked exception, throw that since it's more meaningful than the
        // wrapper
        throw e;
      } else {
        // must have been a wrapped Throwable and we don't want to throw a raw Throwable
        throw wrapped;
      }
    }
  }
}
