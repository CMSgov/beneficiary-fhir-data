package gov.cms.bfd.pipeline.sharedutils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

/** Unit tests for {@link FluxUtils}. */
public class FluxUtilsTest {
  /**
   * Verifies that creating a flux using {@link FluxUtils#fromFluxFunction} handles both success and
   * failure cases appropriately.
   */
  @Test
  void verifyFromFluxFunction() {
    Flux<Integer> flux = FluxUtils.fromFluxFunction(() -> Flux.just(1, 2));
    StepVerifier.create(flux).expectNext(1).expectNext(2).expectComplete().verify();

    final IOException error = new IOException();
    flux =
        FluxUtils.fromFluxFunction(
            () -> {
              throw error;
            });
    StepVerifier.create(flux).expectErrorMatches(e -> e == error).verify();
  }

  /**
   * Verifies that creating a flux using {@link FluxUtils#fromIterableFunction} handles both success
   * and failure cases appropriately.
   */
  @Test
  void verifyFromIterableFunction() {
    Flux<Integer> flux = FluxUtils.fromIterableFunction(() -> List.of(1, 2));
    StepVerifier.create(flux).expectNext(1).expectNext(2).expectComplete().verify();

    final IOException error = new IOException();
    flux =
        FluxUtils.fromIterableFunction(
            () -> {
              throw error;
            });
    StepVerifier.create(flux).expectErrorMatches(e -> e == error).verify();
  }

  /**
   * Verifies that creating a flux from an arbitrary resource using {@link FluxUtils#fromResource}
   * produces a flux that passes along any exception thrown while creating the resource.
   */
  @Test
  void verifyFromResourceOpenFailure() throws IOException {
    final IOException error = new IOException();
    final Flux<Integer> flux =
        FluxUtils.fromResource(
            () -> {
              throw error;
            },
            r -> Flux.just(1, 2),
            Closeable::close,
            "close failed");
    StepVerifier.create(flux).expectErrorMatches(e -> e == error).verify();
  }

  /**
   * Verifies that creating a flux from an arbitrary resource using {@link FluxUtils#fromResource}
   * produces a flux that can create, consume from, and close the resource.
   */
  @Test
  void verifyFromResourceAllSuccessful() throws IOException {
    final Closeable mockResource = mock(Closeable.class);

    final Flux<Integer> flux =
        FluxUtils.fromResource(
            () -> mockResource, r -> Flux.just(1, 2), Closeable::close, "close failed");
    StepVerifier.create(flux).expectNext(1).expectNext(2).expectComplete().verify();
    verify(mockResource).close();
  }

  /**
   * Verifies that creating a flux from an arbitrary resource using {@link FluxUtils#fromResource}
   * produces a flux that can create, consume from, and close the resource and passes along any
   * exception thrown by the read method.
   */
  @Test
  void verifyFromResourceReadFailure() throws IOException {
    final Closeable mockResource = mock(Closeable.class);
    final IOException error = new IOException();

    final Flux<Integer> flux =
        FluxUtils.fromResource(
            () -> mockResource, r -> Flux.error(error), Closeable::close, "close failed");
    StepVerifier.create(flux).expectErrorMatches(e -> e == error).verify();
    verify(mockResource).close();
  }

  /**
   * Verifies that creating a flux from a {@link Closeable} resource using {@link
   * FluxUtils#fromAutoCloseable} produces a flux that can create, consume from, and close the
   * resource and discards any exception thrown by the close method.
   */
  @Test
  void verifyFromResourceReadSuccessfulCloseFailure() throws IOException {
    final Closeable mockResource = mock(Closeable.class);
    final IOException error = new IOException();
    doThrow(error).when(mockResource).close();

    final Flux<Integer> flux =
        FluxUtils.fromResource(
            () -> mockResource, r -> Flux.just(1, 2), Closeable::close, "close failed");
    StepVerifier.create(flux).expectNext(1).expectNext(2).expectComplete().verify();
    verify(mockResource).close();
  }

  /**
   * Verifies that creating a flux from an {@link AutoCloseable} resource using {@link
   * FluxUtils#fromAutoCloseable} produces a flux that can create, consume from, and close the
   * resource.
   */
  @Test
  void verifyFromAutoCloseable() throws IOException {
    final String sourceString = "hello\nworld!\n";
    final var theResource = spy(new BufferedReader(new StringReader(sourceString)));

    final Flux<String> flux =
        FluxUtils.fromAutoCloseable(
            () -> theResource,
            reader -> Flux.fromIterable(reader.lines().toList()),
            "close failed");
    StepVerifier.create(flux).expectNext("hello").expectNext("world!").expectComplete().verify();
    verify(theResource).close();
  }

  /**
   * Verifies that {@link FluxUtils#waitForCompletion} returns the number of values published by the
   * flux while it was waiting for the flux to terminate.
   *
   * @throws Exception potential pass through
   */
  @Test
  void verifyWaitForCompletionCountsResults() throws Exception {
    final Flux<Integer> quickFlux =
        Flux.just(1, 2, 3, 4, 5).subscribeOn(Schedulers.boundedElastic());
    assertEquals(5, FluxUtils.waitForCompletion(quickFlux, Duration.ofMinutes(5)));

    final Flux<Integer> emptyFlux = Flux.<Integer>empty().subscribeOn(Schedulers.boundedElastic());
    assertEquals(0, FluxUtils.waitForCompletion(emptyFlux, Duration.ofMinutes(5)));
  }

  /**
   * Verifies that {@link FluxUtils#waitForCompletion} unwraps and rethrows any checked exception
   * reported by the flux while waiting for it to terminate.
   */
  @Test
  void verifyWaitForCompletionUnwrapsCheckedExceptionFromFlux() {
    final IOException error = new IOException();
    final Flux<Integer> badFlux =
        Flux.<Integer>error(error).subscribeOn(Schedulers.boundedElastic());
    assertThatThrownBy(() -> FluxUtils.waitForCompletion(badFlux, Duration.ofMinutes(5)))
        .isSameAs(error);
  }

  /**
   * Verifies that {@link FluxUtils#waitForCompletion} rethrows any unchecked exception reported by
   * the flux while waiting for it to terminate.
   */
  @Test
  void verifyWaitForCompletionRethrowsRuntimeExceptionFromFlux() {
    final RuntimeException error = new RuntimeException();
    final Flux<Integer> badFlux =
        Flux.<Integer>error(error).subscribeOn(Schedulers.boundedElastic());
    assertThatThrownBy(() -> FluxUtils.waitForCompletion(badFlux, Duration.ofMinutes(5)))
        .isSameAs(error);
  }

  /**
   * Verifies that {@link FluxUtils#waitForCompletion} throws an {@link IllegalStateException} if
   * the flux fails to terminate before the wait time expires.
   */
  @Test
  void verifyWaitForCompletionThrowsIfTimeoutExpires() {
    // the flux won't emit anything until 5 minutes have elapsed.
    final Flux<Long> slowFlux =
        Flux.interval(Duration.ofMinutes(5)).subscribeOn(Schedulers.boundedElastic());
    // Time out after waiting 10 ms and ensure the expected exception was thrown.
    assertThatThrownBy(() -> FluxUtils.waitForCompletion(slowFlux, Duration.ofMillis(10)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Timeout");
  }

  /** Verifies that function wrappers returned by {@link FluxUtils#wrapFunction} work correctly. */
  @Test
  void verifyWrapFunction() {
    // Wrapping a function that throws an IOException yields a function that
    // throws a wrapped, unchecked, version of that exception.
    final IOException error = new IOException();
    final RuntimeException wrappedError = Exceptions.propagate(error);
    final Function<Integer, Integer> wrappedThrows =
        FluxUtils.wrapFunction(
            x -> {
              throw error;
            });
    assertThrows(wrappedError.getClass(), () -> wrappedThrows.apply(1));

    // Wrapping a function that doesn't throw yields a function that returns the result of calling
    // the original function.
    final Function<Integer, Integer> wrappedSuccessful = FluxUtils.wrapFunction(x -> x + 1);
    assertEquals(3, wrappedSuccessful.apply(2));
  }
}
