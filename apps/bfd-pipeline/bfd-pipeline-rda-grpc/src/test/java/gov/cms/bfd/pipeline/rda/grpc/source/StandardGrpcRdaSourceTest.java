package gov.cms.bfd.pipeline.rda.grpc.source;

import static gov.cms.bfd.pipeline.rda.grpc.RdaPipelineTestUtils.assertMeterReading;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.intThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.common.base.Throwables;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.bfd.pipeline.rda.grpc.source.GrpcResponseStream.StreamInterruptedException;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for the {@link StandardGrpcRdaSource} class. */
@ExtendWith(MockitoExtension.class)
public class StandardGrpcRdaSourceTest {
  /** Value used as result from {@link RdaSink#readMaxExistingSequenceNumber()}. */
  public static final long DATABASE_SEQUENCE_NUMBER = 42;

  /** Value used when passing a non-empty configured sequence number to the constructor. */
  public static final long CONFIGURED_SEQUENCE_NUMBER = 18;

  /**
   * We need a starting time for the {@link Clock} used to compute idle time. The time and date are
   * arbitrary since only relative calculations are used.
   */
  private static final Instant BASE_TIME_FOR_TEST =
      ZonedDateTime.of(LocalDateTime.of(2022, 4, 19, 1, 2, 3), ZoneId.systemDefault()).toInstant();

  /** Configuration setting for {@link StandardGrpcRdaSource#minIdleMillisBeforeConnectionDrop}. */
  private static final long MIN_IDLE_MILLIS_BEFORE_CONNECTION_DROP =
      Duration.ofMinutes(2).toMillis();

  private static final long SEQUENCE_RANGE_UPDATE_INTERVAL_SECONDS =
      Duration.ofMinutes(5).toSeconds();

  /** Integer used as a "claim" in the unit tests. */
  private static final Integer CLAIM_1 = 101;

  /** Integer used as a "claim" in the unit tests. */
  private static final Integer CLAIM_2 = 102;

  /** Integer used as a "claim" in the unit tests. */
  private static final Integer CLAIM_3 = 103;

  /** Integer used as a "claim" in the unit tests. */
  private static final Integer CLAIM_4 = 104;

  /** Integer used as a "claim" in the unit tests. */
  private static final Integer CLAIM_5 = 105;

  /** Integer used as a "claim" in the unit tests. */
  private static final Integer INVALID_CLAIM = 106;

  /** String used as a RDA API "version" in the unit tests. */
  public static final String VERSION = "0.0.1";

  /** A MeterRegistry used to verify metrics. */
  private MeterRegistry appMetrics;

  /** A mock clock used to when testing the idle time for dropped connection exceptions. */
  @Mock private Clock clock;

  /** A mock stream caller used to simulate data returned from the RDA API server. */
  @Mock private GrpcStreamCaller<Integer> caller;

  /** A mock channel used to simulate a connection to the RDA API server. */
  @Mock private ManagedChannel channel;

  /** A mock sink used to simulate writing claims to a database. */
  @Mock private RdaSink<Integer, Integer> sink;

  /** A mock gRPC call used to simulate call parameters for a gRPC call. */
  @Mock private ClientCall<Integer, Integer> clientCall;

  /** A mock response stream used to simulate claims arriving from the RDA API server. */
  @Mock private GrpcResponseStream<Integer> mockResponseStream;

  /** A mock {@link RdaVersion} to use for testing. */
  @Mock private RdaVersion rdaVersion;

  /** The object we are testing. */
  private StandardGrpcRdaSource<Integer, Integer> source;

  /** Shortcut for accessing the {@link StandardGrpcRdaSource.Metrics} object. */
  private StandardGrpcRdaSource.Metrics metrics;

  /**
   * Establishes a baseline configuration consisting of mocks and real objects in the unit test
   * methods.
   *
   * @throws Exception required because methods being used and simulated have checked exceptions
   */
  @BeforeEach
  public void setUp() throws Exception {
    appMetrics = new SimpleMeterRegistry();
    source =
        spy(
            new StandardGrpcRdaSource<>(
                clock,
                channel,
                caller,
                () -> CallOptions.DEFAULT,
                appMetrics,
                "ints",
                Optional.empty(),
                MIN_IDLE_MILLIS_BEFORE_CONNECTION_DROP,
                SEQUENCE_RANGE_UPDATE_INTERVAL_SECONDS,
                RdaSourceConfig.ServerType.Remote,
                rdaVersion));
    lenient().doReturn(false).when(rdaVersion).allows(anyString());
    lenient().doReturn(true).when(rdaVersion).allows(VERSION);
    lenient().doReturn(VERSION).when(caller).callVersionService(channel, CallOptions.DEFAULT);
    lenient().doAnswer(i -> i.getArgument(0).toString()).when(sink).getClaimIdForMessage(any());
    metrics = source.getMetrics();
    lenient().doReturn(BASE_TIME_FOR_TEST.toEpochMilli()).when(clock).millis();
    lenient().doReturn(true).when(sink).isValidMessage(CLAIM_1);
    lenient().doReturn(true).when(sink).isValidMessage(CLAIM_2);
    lenient().doReturn(true).when(sink).isValidMessage(CLAIM_3);
    lenient().doReturn(true).when(sink).isValidMessage(CLAIM_4);
    lenient().doReturn(true).when(sink).isValidMessage(CLAIM_5);
    lenient().doReturn(false).when(sink).isValidMessage(INVALID_CLAIM);
  }

  /** Verify that all expected metrics are defined and have expected names. */
  @Test
  public void metricNames() {
    assertEquals(
        Arrays.asList(
            "StandardGrpcRdaSource.ints.batches",
            "StandardGrpcRdaSource.ints.calls",
            "StandardGrpcRdaSource.ints.failures",
            "StandardGrpcRdaSource.ints.objects.received",
            "StandardGrpcRdaSource.ints.objects.stored",
            "StandardGrpcRdaSource.ints.skipped.delete",
            "StandardGrpcRdaSource.ints.skipped.invalid",
            "StandardGrpcRdaSource.ints.successes",
            "StandardGrpcRdaSource.ints.uptime"),
        appMetrics.getMeters().stream()
            .map(meter -> meter.getId().getName())
            .sorted()
            .collect(Collectors.toList()));
  }

  /**
   * Verifies that {@link StandardGrpcRdaSource#performSmokeTest} performs all of the expected tests
   * and returns success.
   *
   * @throws Exception required in signature because tested method has checked exceptions
   */
  @Test
  public void testSmokeTestPerformsExpectedActions() throws Exception {
    GrpcResponseStream<Integer> responseStream =
        spy(createResponse(CLAIM_1, CLAIM_2, CLAIM_3, CLAIM_4, CLAIM_5));
    doReturn(Optional.of(DATABASE_SEQUENCE_NUMBER)).when(sink).readMaxExistingSequenceNumber();
    doReturn(responseStream)
        .when(caller)
        .callService(channel, CallOptions.DEFAULT, RdaChange.MIN_SEQUENCE_NUM);

    assertTrue(source.performSmokeTest(sink));

    verify(caller).callVersionService(channel, CallOptions.DEFAULT);
    verify(sink).readMaxExistingSequenceNumber();
    verify(caller).callService(channel, CallOptions.DEFAULT, RdaChange.MIN_SEQUENCE_NUM);
    verify(responseStream).cancelStream(anyString());
    verify(responseStream).close();
  }

  /**
   * Verifies that {@link StandardGrpcRdaSource#performSmokeTest} skips the RDA API tests when
   * configured to use an {@link RdaSourceConfig.ServerType#InProcess} server.
   *
   * @throws Exception required in signature because tested method has checked exceptions
   */
  @Test
  public void testSmokeTestSkipsCallsToInProcessServer() throws Exception {
    source =
        spy(
            new StandardGrpcRdaSource<>(
                clock,
                channel,
                caller,
                () -> CallOptions.DEFAULT,
                appMetrics,
                "ints",
                Optional.empty(),
                MIN_IDLE_MILLIS_BEFORE_CONNECTION_DROP,
                SEQUENCE_RANGE_UPDATE_INTERVAL_SECONDS,
                RdaSourceConfig.ServerType.InProcess,
                rdaVersion));

    doReturn(Optional.of(DATABASE_SEQUENCE_NUMBER)).when(sink).readMaxExistingSequenceNumber();

    assertTrue(source.performSmokeTest(sink));

    verifyNoInteractions(caller);
    verify(sink).readMaxExistingSequenceNumber();
  }

  /**
   * Verifies that {@link AbstractGrpcRdaSource#checkApiVersion(String)} is called, and that any
   * thrown exception is thrown up the stack.
   */
  @Test
  public void testThrowsExceptionWhenRdaCheckThrows() {
    IllegalStateException expectedException = new IllegalStateException("Bad");

    lenient().doNothing().when(source).checkApiVersion(anyString());

    doThrow(expectedException).when(source).checkApiVersion(VERSION);

    try {
      source.retrieveAndProcessObjects(2, sink);
      fail("Expected exception not thrown");
    } catch (Exception e) {
      Throwable actualException = e.getCause();
      assertSame(expectedException, actualException, "Expected exception not thrown");
    }
  }

  /**
   * Verify that normal (happy path) processing saves objects, updates all expected metrics, and
   * shuts down cleanly.
   *
   * @throws Exception required in signature because tested method has checked exceptions
   */
  @Test
  public void testSuccessfullyProcessThreeItems() throws Exception {
    doReturn(Optional.of(DATABASE_SEQUENCE_NUMBER)).when(sink).readMaxExistingSequenceNumber();
    doReturn(createResponse(CLAIM_1, CLAIM_2, INVALID_CLAIM, CLAIM_3))
        .when(caller)
        .callService(channel, CallOptions.DEFAULT, DATABASE_SEQUENCE_NUMBER);
    doReturn(2).when(sink).writeMessages(VERSION, List.of(CLAIM_1, CLAIM_2));
    doReturn(1).when(sink).writeMessages(VERSION, List.of(CLAIM_3));

    final int result = source.retrieveAndProcessObjects(2, sink);
    assertEquals(3, result);
    assertMeterReading(1, "calls", metrics.getCalls());
    assertMeterReading(4, "received", metrics.getObjectsReceived());
    assertMeterReading(3, "stored", metrics.getObjectsStored());
    assertMeterReading(2, "batches", metrics.getBatches());
    assertMeterReading(1, "successes", metrics.getSuccesses());
    assertMeterReading(0, "failures", metrics.getFailures());
    assertMeterReading(1, "invalid", metrics.getInvalidObjectsSkipped());
    // once at start, twice after a batch
    verify(source, times(3)).setUptimeToRunning();
    // once per object received
    verify(source, times(4)).setUptimeToReceiving();
    verify(source).setUptimeToStopped();
    verify(caller).callService(channel, CallOptions.DEFAULT, DATABASE_SEQUENCE_NUMBER);
  }

  /**
   * Verify that a starting sequence number defined in the config is used instead of using the value
   * from the database.
   *
   * @throws Exception required in signature because tested method has checked exceptions
   */
  @Test
  public void testUsesHardCodedSequenceNumberWhenProvided() throws Exception {
    source =
        spy(
            new StandardGrpcRdaSource<>(
                clock,
                channel,
                caller,
                () -> CallOptions.DEFAULT,
                appMetrics,
                "ints",
                Optional.of(CONFIGURED_SEQUENCE_NUMBER),
                MIN_IDLE_MILLIS_BEFORE_CONNECTION_DROP,
                SEQUENCE_RANGE_UPDATE_INTERVAL_SECONDS,
                RdaSourceConfig.ServerType.Remote,
                rdaVersion));
    doReturn(createResponse(CLAIM_1))
        .when(caller)
        .callService(channel, CallOptions.DEFAULT, CONFIGURED_SEQUENCE_NUMBER - 1);
    doReturn(1).when(sink).writeMessages(VERSION, List.of(CLAIM_1));

    final int result = source.retrieveAndProcessObjects(2, sink);
    assertEquals(1, result);
    assertMeterReading(1, "calls", metrics.getCalls());
    assertMeterReading(1, "received", metrics.getObjectsReceived());
    assertMeterReading(1, "stored", metrics.getObjectsStored());
    assertMeterReading(1, "batches", metrics.getBatches());
    assertMeterReading(1, "successes", metrics.getSuccesses());
    assertMeterReading(0, "failures", metrics.getFailures());
    // once at start, once after a batch
    verify(source, times(2)).setUptimeToRunning();
    // once per object received
    verify(source, times(1)).setUptimeToReceiving();
    verify(source).setUptimeToStopped();
    verify(caller).callService(channel, CallOptions.DEFAULT, CONFIGURED_SEQUENCE_NUMBER - 1);
    verify(sink, times(0)).readMaxExistingSequenceNumber();
  }

  /**
   * Verify that a {@link ProcessingException} thrown by {@link GrpcStreamCaller} triggers a
   * shutdown and is passed through in a new ProcessingException with updated count.
   *
   * @throws Exception required in signature because tested method has checked exceptions
   */
  @Test
  public void testPassesThroughProcessingExceptionFromSink() throws Exception {
    doReturn(Optional.of(DATABASE_SEQUENCE_NUMBER)).when(sink).readMaxExistingSequenceNumber();
    final Exception error = new IOException("oops");
    doReturn(createResponse(CLAIM_1, CLAIM_2, CLAIM_3, CLAIM_4, CLAIM_5))
        .when(caller)
        .callService(same(channel), any(), anyLong());
    doReturn(2).when(sink).writeMessages(VERSION, Arrays.asList(CLAIM_1, CLAIM_2));
    // second batch should throw our exception as though it failed after processing 1 record
    doThrow(new ProcessingException(error, 1))
        .when(sink)
        .writeMessages(VERSION, Arrays.asList(CLAIM_3, CLAIM_4));

    try {
      source.retrieveAndProcessObjects(2, sink);
      fail("source should have thrown exception");
    } catch (ProcessingException ex) {
      assertEquals(3, ex.getProcessedCount());
      assertNotNull(ex.getCause());
      assertSame(error, Throwables.getRootCause(ex));
    }
    assertMeterReading(1, "calls", metrics.getCalls());
    assertMeterReading(4, "received", metrics.getObjectsReceived());
    assertMeterReading(2, "stored", metrics.getObjectsStored());
    assertMeterReading(1, "batches", metrics.getBatches());
    assertMeterReading(0, "successes", metrics.getSuccesses());
    assertMeterReading(1, "failures", metrics.getFailures());
    // once at start, once after a batch
    verify(source, times(2)).setUptimeToRunning();
    // once per object received
    verify(source, times(4)).setUptimeToReceiving();
    verify(source).setUptimeToStopped();
    verify(caller).callService(channel, CallOptions.DEFAULT, DATABASE_SEQUENCE_NUMBER);
  }

  /**
   * Verify that an exception thrown by {@link GrpcStreamCaller} triggers a shutdown and is passed
   * through in a ProcessingException.
   *
   * @throws Exception required in signature because tested method has checked exceptions
   */
  @Test
  public void testHandlesExceptionFromCaller() throws Exception {
    doReturn(Optional.empty()).when(sink).readMaxExistingSequenceNumber();
    final Exception error = new IOException("oops");
    doThrow(error).when(caller).callService(any(), any(), anyLong());

    try {
      source.retrieveAndProcessObjects(2, sink);
      fail("source should have thrown exception");
    } catch (ProcessingException ex) {
      assertEquals(0, ex.getProcessedCount());
      assertSame(error, ex.getCause());
    }
    assertMeterReading(1, "calls", metrics.getCalls());
    assertMeterReading(0, "received", metrics.getObjectsReceived());
    assertMeterReading(0, "stored", metrics.getObjectsStored());
    assertMeterReading(0, "batches", metrics.getBatches());
    assertMeterReading(0, "successes", metrics.getSuccesses());
    assertMeterReading(1, "failures", metrics.getFailures());
    verify(source).setUptimeToRunning();
    verify(source, times(0)).setUptimeToReceiving();
    verify(source).setUptimeToStopped();
  }

  /**
   * Verify that {@link RuntimeException} thrown by the {@link RdaSink} triggers a shutdown and is
   * passed through in a ProcessingException.
   *
   * @throws Exception required in signature because tested method has checked exceptions
   */
  @Test
  public void testHandlesRuntimeExceptionFromSink() throws Exception {
    doReturn(Optional.empty()).when(sink).readMaxExistingSequenceNumber();
    doReturn(createResponse(CLAIM_1, CLAIM_2, CLAIM_3, CLAIM_4, CLAIM_5))
        .when(caller)
        .callService(same(channel), eq(CallOptions.DEFAULT), anyLong());
    doReturn(2).when(sink).writeMessages(VERSION, Arrays.asList(CLAIM_1, CLAIM_2));
    // second batch should throw our exception as though it failed after processing 1 record
    final Exception error = new RuntimeException("oops");
    doThrow(error)
        // Second doThrow() prevents "self-suppression" from testing logic
        .doThrow(new RuntimeException("oops 2"))
        .when(sink)
        .writeMessages(VERSION, Arrays.asList(CLAIM_3, CLAIM_4));

    try {
      source.retrieveAndProcessObjects(2, sink);
      fail("source should have thrown exception");
    } catch (ProcessingException ex) {
      assertEquals(2, ex.getProcessedCount());
      assertSame(error, ex.getCause());
    }
    assertMeterReading(1, "calls", metrics.getCalls());
    assertMeterReading(4, "received", metrics.getObjectsReceived());
    assertMeterReading(2, "stored", metrics.getObjectsStored());
    assertMeterReading(1, "batches", metrics.getBatches());
    assertMeterReading(0, "successes", metrics.getSuccesses());
    assertMeterReading(1, "failures", metrics.getFailures());
    // once at start, once after a batch
    verify(source, times(2)).setUptimeToRunning();
    // once per object received
    verify(source, times(4)).setUptimeToReceiving();
    verify(source).setUptimeToStopped();
    verify(caller).callService(channel, CallOptions.DEFAULT, 0L);
  }

  /**
   * Verify that {@link InterruptedException} from the stream causes a clean shutdown.
   *
   * @throws Exception required in signature because tested method has checked exceptions
   */
  @Test
  public void testHandlesInterruptFromStream() throws Exception {
    doReturn(Optional.of(DATABASE_SEQUENCE_NUMBER)).when(sink).readMaxExistingSequenceNumber();
    // Creates a response with 3 valid values followed by an interrupt.
    // unchecked - This is fine for making a mock.
    //noinspection unchecked
    final GrpcResponseStream<Integer> response = mock(GrpcResponseStream.class);
    when(response.next()).thenReturn(CLAIM_1, CLAIM_2, CLAIM_3);
    when(response.hasNext())
        .thenReturn(true)
        .thenReturn(true)
        .thenThrow(new StreamInterruptedException(new StatusRuntimeException(Status.INTERNAL)));
    doReturn(response).when(caller).callService(same(channel), any(), anyLong());

    // we expect to write a single batch with the first two records
    doReturn(2).when(sink).writeMessages(VERSION, Arrays.asList(CLAIM_1, CLAIM_2));

    int processed = source.retrieveAndProcessObjects(2, sink);
    assertEquals(2, processed);
    assertMeterReading(1, "calls", metrics.getCalls());
    assertMeterReading(2, "received", metrics.getObjectsReceived());
    assertMeterReading(2, "stored", metrics.getObjectsStored());
    assertMeterReading(1, "batches", metrics.getBatches());
    assertMeterReading(1, "successes", metrics.getSuccesses());
    assertMeterReading(0, "failures", metrics.getFailures());
    verify(response).cancelStream(anyString());
    // once at start, once after a batch
    verify(source, times(2)).setUptimeToRunning();
    // once per object received
    verify(source, times(2)).setUptimeToReceiving();
    verify(source).setUptimeToStopped();
    verify(caller).callService(channel, CallOptions.DEFAULT, DATABASE_SEQUENCE_NUMBER);
  }

  /**
   * Verify that calling close triggers a channel shutdown and that calling it multiple times is
   * safe.
   *
   * @throws Exception required in signature because tested method has checked exceptions
   */
  @Test
  public void testClose() throws Exception {
    doReturn(channel).when(channel).shutdown();
    source.close();
    source.close(); // second call does nothing
    verify(channel, times(1)).shutdown();
    verify(channel, times(1)).awaitTermination(anyLong(), any(TimeUnit.class));
  }

  /**
   * Simulate an abrupt RDA API server disconnect before the minimum acceptable idle time has
   * elapsed. In this scenario the exception should be passed through in a ProcessingException.
   *
   * @throws Exception required in signature because tested method has checked exceptions
   */
  @Test
  public void testDisconnectBeforeExpectedIdleLimitThrowsException() throws Exception {
    doReturn(Optional.of(DATABASE_SEQUENCE_NUMBER)).when(sink).readMaxExistingSequenceNumber();

    // set up clock times to ensure idle time is not exceeded
    doReturn(BASE_TIME_FOR_TEST.toEpochMilli())
        .doReturn(BASE_TIME_FOR_TEST.toEpochMilli() + 1)
        .doReturn(BASE_TIME_FOR_TEST.toEpochMilli() + 2)
        .doReturn(BASE_TIME_FOR_TEST.toEpochMilli() + MIN_IDLE_MILLIS_BEFORE_CONNECTION_DROP)
        .when(clock)
        .millis();

    // set up the response to throw a dropped exception on the second call to next()
    doReturn(true).when(mockResponseStream).hasNext();
    doReturn(CLAIM_1)
        .doReturn(CLAIM_2)
        .doThrow(
            new GrpcResponseStream.DroppedConnectionException(
                new StatusRuntimeException(Status.INTERNAL)))
        .when(mockResponseStream)
        .next();
    doReturn(mockResponseStream)
        .when(caller)
        .callService(channel, CallOptions.DEFAULT, DATABASE_SEQUENCE_NUMBER);

    doReturn(2).when(sink).writeMessages(VERSION, List.of(CLAIM_1, CLAIM_2));

    try {
      source.retrieveAndProcessObjects(3, sink);
      fail("should have thrown an exception");
    } catch (ProcessingException error) {
      assertEquals(2, error.getProcessedCount());
      assertTrue(error.getCause() instanceof GrpcResponseStream.DroppedConnectionException);
    }
    assertMeterReading(1, "calls", metrics.getCalls());
    assertMeterReading(2, "received", metrics.getObjectsReceived());
    assertMeterReading(2, "stored", metrics.getObjectsStored());
    assertMeterReading(1, "batches", metrics.getBatches());
    assertMeterReading(0, "successes", metrics.getSuccesses());
    assertMeterReading(1, "failures", metrics.getFailures());
    // once at start, once after a batch
    verify(source, times(2)).setUptimeToRunning();
    // once per object received
    verify(source, times(3)).setUptimeToReceiving();
    verify(source).setUptimeToStopped();
    verify(caller).callService(channel, CallOptions.DEFAULT, DATABASE_SEQUENCE_NUMBER);
  }

  /**
   * Simulate an abrupt RDA API server disconnect after the minimum acceptable idle time has
   * elapsed. In this scenario the exception will stop processing but will not pass through the
   * exception since it's an acceptable error.
   *
   * @throws Exception not really thrown
   */
  @Test
  public void testDisconnectAfterExpectedIdleLimitStopsButDoesNotThrow() throws Exception {
    doReturn(Optional.of(DATABASE_SEQUENCE_NUMBER)).when(sink).readMaxExistingSequenceNumber();

    // set up clock times to ensure idle time is exceeded
    doReturn(BASE_TIME_FOR_TEST.toEpochMilli())
        .doReturn(BASE_TIME_FOR_TEST.toEpochMilli() + 1)
        .doReturn(BASE_TIME_FOR_TEST.toEpochMilli() + 2)
        .doReturn(BASE_TIME_FOR_TEST.toEpochMilli() + 3 + MIN_IDLE_MILLIS_BEFORE_CONNECTION_DROP)
        .when(clock)
        .millis();

    // set up the response to throw a dropped exception on the second call to next()
    doReturn(true).when(mockResponseStream).hasNext();
    doReturn(CLAIM_1)
        .doReturn(CLAIM_2)
        .doThrow(
            new GrpcResponseStream.DroppedConnectionException(
                new StatusRuntimeException(Status.INTERNAL)))
        .when(mockResponseStream)
        .next();
    doReturn(mockResponseStream)
        .when(caller)
        .callService(channel, CallOptions.DEFAULT, DATABASE_SEQUENCE_NUMBER);

    doReturn(2).when(sink).writeMessages(VERSION, List.of(CLAIM_1, CLAIM_2));

    int result = source.retrieveAndProcessObjects(2, sink);
    assertEquals(2, result);

    assertMeterReading(1, "calls", metrics.getCalls());
    assertMeterReading(2, "received", metrics.getObjectsReceived());
    assertMeterReading(2, "stored", metrics.getObjectsStored());
    assertMeterReading(1, "batches", metrics.getBatches());
    assertMeterReading(1, "successes", metrics.getSuccesses());
    assertMeterReading(0, "failures", metrics.getFailures());
    // once at start, once after a batch
    verify(source, times(2)).setUptimeToRunning();
    // once per object received
    verify(source, times(3)).setUptimeToReceiving();
    verify(source).setUptimeToStopped();
    verify(caller).callService(channel, CallOptions.DEFAULT, DATABASE_SEQUENCE_NUMBER);
  }

  /**
   * Verifies that if an InterruptedException is thrown while waiting for the {@link ManagedChannel}
   * to close we retry the call internally and return successfully.
   *
   * @throws Exception required since method being tested has checked exceptions
   */
  @Test
  public void testSingleInterruptedExceptionDuringChannelShutdown() throws Exception {
    doReturn(false).when(channel).isShutdown();
    doReturn(false).when(channel).isTerminated();
    doThrow(new InterruptedException())
        .doReturn(false)
        .when(channel)
        .awaitTermination(anyLong(), any());

    source.close();

    verify(channel).isShutdown();
    verify(channel).isTerminated();
    verify(channel).shutdown();
    verify(channel, times(2)).awaitTermination(anyLong(), any(TimeUnit.class));
  }

  /**
   * Verifies that DELETE messages are skipped and metric is updated.
   *
   * @throws Exception required since method being tested has checked exceptions
   */
  @Test
  public void testSkipsDeleteMessage() throws Exception {
    doReturn(Optional.of(DATABASE_SEQUENCE_NUMBER)).when(sink).readMaxExistingSequenceNumber();
    doReturn(createResponse(CLAIM_1, CLAIM_2, CLAIM_3))
        .when(caller)
        .callService(same(channel), any(), anyLong());
    // treat CLAIM_2 as a DELETE message
    doReturn(true).when(sink).isDeleteMessage(eq(CLAIM_2));
    doReturn(false).when(sink).isDeleteMessage(intThat(i -> !i.equals(CLAIM_2)));
    doReturn(2).when(sink).writeMessages(VERSION, Arrays.asList(CLAIM_1, CLAIM_3));

    int processedCount = source.retrieveAndProcessObjects(2, sink);
    assertEquals(2, processedCount);

    assertMeterReading(1, "calls", metrics.getCalls());
    assertMeterReading(3, "received", metrics.getObjectsReceived());
    assertMeterReading(2, "stored", metrics.getObjectsStored());
    assertMeterReading(1, "batches", metrics.getBatches());
    assertMeterReading(1, "successes", metrics.getSuccesses());
    assertMeterReading(0, "failures", metrics.getFailures());
    assertMeterReading(1, "failures", metrics.getDeleteMessagesSkipped());
    // once at start, once after a batch
    verify(source, times(2)).setUptimeToRunning();
    // once per object received
    verify(source, times(3)).setUptimeToReceiving();
    verify(source).setUptimeToStopped();
    verify(caller).callService(channel, CallOptions.DEFAULT, DATABASE_SEQUENCE_NUMBER);
  }

  /**
   * Creates an {@link Integer} {@link GrpcResponseStream} from the given values.
   *
   * @param values the values to create a stream from
   * @return the grpc response stream
   */
  private GrpcResponseStream<Integer> createResponse(int... values) {
    return new GrpcResponseStream<>(clientCall, Arrays.stream(values).iterator());
  }
}
