package gov.cms.bfd.pipeline.sharedutils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Unit tests for {@link FluxWaiter}. */
@ExtendWith(MockitoExtension.class)
public class FluxWaiterTest {
  /**
   * Maximum time to wait for normal termination. The tests are not expected to ever reach this time
   * since we use a simulated clock but we need a value consistent with the values returned by
   * {@link #clock} to pass to the {@link FluxWaiter} constructor.
   */
  private static final Duration NORMAL_WAIT_TIME = Duration.ofMinutes(2);

  /**
   * Maximum time to wait following an interrupt. The tests are not expected to ever reach this time
   * since we use a simulated clock but we need a value consistent with {@link #NORMAL_WAIT_TIME} to
   * pass to the {@link FluxWaiter} constructor.
   */
  private static final Duration INTERRUPTED_WAIT_TIME = Duration.ofMinutes(1);

  /** A time greater than {@link #NORMAL_WAIT_TIME} that can be used for timeout tests. */
  private static final Duration SLOW_TIME = Duration.ofMinutes(5);

  /** A mock clock to provide control over the passage of simulated time. */
  @Mock private Clock clock;

  /** A mock latch to provide control over wait behavior when needed. */
  @Mock private CountDownLatch latch;

  /** A {@link FluxWaiter} to test when we need to use the simulated clock and latch. */
  private FluxWaiter fluxWaiter;

  /** The interrupted flag passed to the {@link FluxWaiter#waitForCompletion} method. */
  private AtomicBoolean interrupted;

  /** Sets up mocks and real objects before each test run. */
  @BeforeEach
  void setUp() {
    // Limits time to 2:30 and repeats first time once because first loop iteration calls for the
    // time twice without any intervening wait. After that it increments time by 15 seconds on each
    // method call.  These values would need to change if NORMAL_WAIT_TIME is increased.
    lenient()
        .doReturn(
            15_000L, 15_000L, 30_000L, 45_000L, 60_000L, 75_000L, 90_000L, 105_000L, 120_000L,
            135_000L, 150_000L)
        .when(clock)
        .millis();
    // The 2 minute max wait is coordinated with our mock clock's return values.
    fluxWaiter = new FluxWaiter(NORMAL_WAIT_TIME, INTERRUPTED_WAIT_TIME, clock, i -> latch);
    interrupted = new AtomicBoolean();
  }

  @Test
  void verifyMonoCompletesImmediately() throws Exception {
    // We can use a normal waiter here because we know the mono will terminate quickly.
    final var fluxWaiter = new FluxWaiter(NORMAL_WAIT_TIME, INTERRUPTED_WAIT_TIME);
    final var fastMono = Mono.just(1);
    assertEquals(Optional.of(1), fluxWaiter.waitForCompletion(fastMono, interrupted));
    assertEquals(false, interrupted.get());
  }

  @Test
  void verifyMonoCompletesAfterInterrupt() throws Exception {
    final var slowMono = Mono.delay(SLOW_TIME);
    doThrow(new InterruptedException()).doReturn(true).when(latch).await(anyLong(), any());
    assertThatThrownBy(() -> fluxWaiter.waitForCompletion(slowMono, interrupted))
        .isInstanceOf(InterruptedException.class);
    assertEquals(true, interrupted.get());
  }

  @Test
  void verifyExceptionThrownForTimeOut() throws Exception {
    final var slowFlux = Flux.interval(SLOW_TIME);
    doReturn(false).when(latch).await(anyLong(), any());
    assertThatThrownBy(() -> fluxWaiter.waitForCompletion(slowFlux, interrupted))
        .isInstanceOf(FluxWaiter.TimeoutExceededException.class);
    assertEquals(true, interrupted.get());
  }

  @Test
  void verifyCheckedExceptionIsRethrown() throws Exception {
    // We can use a normal waiter here because we know the mono will terminate quickly.
    final var fluxWaiter = new FluxWaiter(NORMAL_WAIT_TIME, INTERRUPTED_WAIT_TIME);
    final var error = new IOException();
    final var errorMono = Mono.error(error);
    assertThatThrownBy(() -> fluxWaiter.waitForCompletion(errorMono, interrupted)).isSameAs(error);
    assertEquals(false, interrupted.get());
  }

  @Test
  void verifyUncheckedExceptionIsPassedThrough() throws Exception {
    // We can use a normal waiter here because we know the mono will terminate quickly.
    final var fluxWaiter = new FluxWaiter(NORMAL_WAIT_TIME, INTERRUPTED_WAIT_TIME);
    final var error = new IllegalArgumentException();
    final var errorFlux = Flux.error(error);
    assertThatThrownBy(() -> fluxWaiter.waitForCompletion(errorFlux, interrupted)).isSameAs(error);
    assertEquals(false, interrupted.get());
  }

  @Test
  void verifyWrappedUncheckedExceptionIsUnwrapped() throws Exception {
    // We can use a normal waiter here because we know the mono will terminate quickly.
    final var fluxWaiter = new FluxWaiter(NORMAL_WAIT_TIME, INTERRUPTED_WAIT_TIME);
    final var error = new IOException();
    final var errorMono = Mono.error(Exceptions.propagate(error));
    assertThatThrownBy(() -> fluxWaiter.waitForCompletion(errorMono, interrupted)).isSameAs(error);
    assertEquals(false, interrupted.get());
  }

  @Test
  void verifyRawThrowableIsWrapped() throws Exception {
    // We can use a normal waiter here because we know the mono will terminate quickly.
    final var fluxWaiter = new FluxWaiter(NORMAL_WAIT_TIME, INTERRUPTED_WAIT_TIME);
    final var error = new AssertionError();
    final var errorFlux = Flux.error(error);
    // Unwrapping the exception thrown should yield exactly our error instance.
    assertThatThrownBy(() -> fluxWaiter.waitForCompletion(errorFlux, interrupted))
        .matches(ex -> Exceptions.unwrap(ex) == error);
    assertEquals(false, interrupted.get());
  }

  @Test
  void verifyFluxResultsCountedCorrectly() throws Exception {
    // We can use a normal waiter here because we know the flux will terminate quickly.
    final var fluxWaiter = new FluxWaiter(NORMAL_WAIT_TIME, INTERRUPTED_WAIT_TIME);
    final var flux = Flux.just(1, 2, 3, 4);
    assertEquals(4, fluxWaiter.waitForCompletion(flux, interrupted));
    assertEquals(false, interrupted.get());
  }
}
