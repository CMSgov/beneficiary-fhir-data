package gov.cms.bfd.pipeline.sharedutils;

import gov.cms.bfd.sharedutils.interfaces.ThrowingConsumer;
import gov.cms.bfd.sharedutils.interfaces.ThrowingFunction;
import gov.cms.bfd.sharedutils.interfaces.ThrowingRunnable;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
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
   * Creates a {@link Flux} that, when subscribed to, opens a resource object using the given lambda
   * and creates a flux to read it by calling another given lambda. When the flux terminates for any
   * reason, another given lambda will be called on the resource to release it.
   *
   * <p>Note that once a flux has terminated there is no way to pass an exception on to the
   * subscriber so we cannot propagate the exception. Instead we simply log the exception and
   * include the provided message.
   *
   * @param open lambda used to open the resource
   * @param read lambda used to create a flux from the resource
   * @param close lambda used to close the resource
   * @param failedCloseMessage message to include when logging a close failure
   * @return the flux
   * @param <T> type of values published from the resource
   * @param <R> type of the resource
   */
  public static <T, R> Flux<T> fromResource(
      Callable<R> open,
      ThrowingFunction<Flux<T>, R, Exception> read,
      ThrowingConsumer<R, Exception> close,
      String failedCloseMessage) {
    return Flux.using(
        open,
        wrapFunction(read),
        resource -> {
          try {
            close.accept(resource);
          } catch (Exception ex) {
            log.warn("unable to close resource {}", failedCloseMessage);
          }
        });
  }

  /**
   * Creates a {@link Flux} that, when subscribed to, opens an {@link AutoCloseable} object using
   * the given lambda and creates a flux to read it by calling another given lambda. When the flux
   * terminates for any reason, the {@link AutoCloseable#close} method will be called on the
   * resource.
   *
   * <p>Note that once a flux has terminated there is no way to pass an exception on to the
   * subscriber so we cannot propagate the exception. Instead we simply log the exception and
   * include the provided message.
   *
   * @param open lambda used to open the resource
   * @param read lambda used to create a flux from the resource
   * @param failedCloseMessage message to include when logging a close failure
   * @return the flux
   * @param <T> type of values published from the resource
   * @param <R> type of the resource
   */
  public static <T, R extends AutoCloseable> Flux<T> fromAutoCloseable(
      Callable<R> open, ThrowingFunction<Flux<T>, R, Exception> read, String failedCloseMessage) {
    return fromResource(open, read, AutoCloseable::close, failedCloseMessage);
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

  /**
   * Creates a {@link Function} that calls a {@link ThrowingFunction} and converts any checked
   * exceptions thrown into unchecked exceptions using {@link Exceptions#propagate} so that they can
   * be unwrapped later if necessary.
   *
   * @param func lambda that can throw checked exceptions
   * @return lambda that wraps checked exceptions into unchecked exceptions
   * @param <T> type of input argument to the function
   * @param <R> type of result of the function
   */
  public static <T, R> Function<T, R> wrapFunction(ThrowingFunction<R, T, Exception> func) {
    return x -> {
      try {
        return func.apply(x);
      } catch (Exception ex) {
        throw Exceptions.propagate(ex);
      }
    };
  }
}
