package gov.cms.bfd.pipeline.sharedutils;

import gov.cms.bfd.sharedutils.interfaces.ThrowingConsumer;
import gov.cms.bfd.sharedutils.interfaces.ThrowingFunction;
import java.util.concurrent.Callable;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;

/** Utility class containing static methods to create and wait for fluxes. */
@Slf4j
public class FluxUtils {
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
