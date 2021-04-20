package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.pipeline.rda.grpc.PreAdjudicatedClaim;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RDASink;
import io.grpc.ManagedChannel;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;

public class GrpcRDASourceTest {
  private static final PreAdjudicatedClaim CLAIM_1 = new PreAdjudicatedClaim();
  private static final PreAdjudicatedClaim CLAIM_2 = new PreAdjudicatedClaim();
  private static final PreAdjudicatedClaim CLAIM_3 = new PreAdjudicatedClaim();
  private static final PreAdjudicatedClaim CLAIM_4 = new PreAdjudicatedClaim();
  private static final PreAdjudicatedClaim CLAIM_5 = new PreAdjudicatedClaim();
  private MetricRegistry appMetrics;
  private Clock clock;
  private GrpcStreamCaller<Integer> caller;
  private ManagedChannel channel;
  private RDASink<PreAdjudicatedClaim> sink;
  private GrpcRDASource<Integer> source;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    appMetrics = new MetricRegistry();
    clock = mock(Clock.class);
    caller = mock(GrpcStreamCaller.class);
    channel = mock(ManagedChannel.class);
    sink = mock(RDASink.class);
    doReturn(CLAIM_1).when(caller).convertResultToClaim(1);
    doReturn(CLAIM_2).when(caller).convertResultToClaim(2);
    doReturn(CLAIM_3).when(caller).convertResultToClaim(3);
    doReturn(CLAIM_4).when(caller).convertResultToClaim(4);
    doReturn(CLAIM_5).when(caller).convertResultToClaim(5);
    source = new GrpcRDASource<>(channel, this::callerFactory, clock, appMetrics);
  }

  @Test
  public void testSuccessfullyProcessThreeItems() throws Exception {
    doReturn(Instant.ofEpochMilli(1000)).when(clock).instant();
    doReturn(Arrays.asList(1, 2, 3).iterator()).when(caller).callService(any());
    doReturn(2).when(sink).writeBatch(Arrays.asList(CLAIM_1, CLAIM_2));
    doReturn(1).when(sink).writeBatch(Collections.singletonList(CLAIM_3));

    final int result = source.retrieveAndProcessObjects(3, 2, Duration.ofSeconds(10), sink);
    assertEquals(3, result);
    assertMeterReading(1, GrpcRDASource.CALLS_METER);
    assertMeterReading(3, GrpcRDASource.RECORDS_RECEIVED_METER);
    assertMeterReading(3, GrpcRDASource.RECORDS_STORED_METER);
    assertMeterReading(2, GrpcRDASource.BATCHES_METER);
  }

  @Test
  public void testPassesThroughProcessingExceptionFromSink() throws Exception {
    final Exception error = new IOException("oops");
    doReturn(Instant.ofEpochMilli(1000)).when(clock).instant();
    doReturn(Arrays.asList(1, 2, 3, 4, 5).iterator()).when(caller).callService(any());
    doReturn(2).when(sink).writeBatch(Arrays.asList(CLAIM_1, CLAIM_2));
    // second batch should throw our exception as though it failed after processing 1 record
    doThrow(new ProcessingException(error, 1))
        .when(sink)
        .writeBatch(Arrays.asList(CLAIM_3, CLAIM_4));

    try {
      source.retrieveAndProcessObjects(5, 2, Duration.ofSeconds(10), sink);
      fail("source should have thrown exception");
    } catch (ProcessingException ex) {
      assertEquals(3, ex.getProcessedCount());
      assertSame(error, ex.getCause());
    }
    assertMeterReading(1, GrpcRDASource.CALLS_METER);
    assertMeterReading(4, GrpcRDASource.RECORDS_RECEIVED_METER);
    assertMeterReading(2, GrpcRDASource.RECORDS_STORED_METER);
    assertMeterReading(1, GrpcRDASource.BATCHES_METER);
  }

  @Test
  public void testHandlesExceptionFromCallerFactory() {
    final Exception error = new IOException("oops");
    source =
        new GrpcRDASource<>(
            channel,
            c -> {
              throw error;
            },
            clock,
            appMetrics);

    try {
      source.retrieveAndProcessObjects(5, 2, Duration.ofSeconds(10), sink);
      fail("source should have thrown exception");
    } catch (ProcessingException ex) {
      assertEquals(0, ex.getProcessedCount());
      assertSame(error, ex.getCause());
    }
    assertMeterReading(1, GrpcRDASource.CALLS_METER);
    assertMeterReading(0, GrpcRDASource.RECORDS_RECEIVED_METER);
    assertMeterReading(0, GrpcRDASource.RECORDS_STORED_METER);
    assertMeterReading(0, GrpcRDASource.BATCHES_METER);
  }

  @Test
  public void testHandlesRuntimeExceptionFromSink() throws Exception {
    final Exception error = new RuntimeException("oops");
    doReturn(Instant.ofEpochMilli(1000)).when(clock).instant();
    doReturn(Arrays.asList(1, 2, 3, 4, 5).iterator()).when(caller).callService(any());
    doReturn(2).when(sink).writeBatch(Arrays.asList(CLAIM_1, CLAIM_2));
    // second batch should throw our exception as though it failed after processing 1 record
    doThrow(error).when(sink).writeBatch(Arrays.asList(CLAIM_3, CLAIM_4));

    try {
      source.retrieveAndProcessObjects(5, 2, Duration.ofSeconds(10), sink);
      fail("source should have thrown exception");
    } catch (ProcessingException ex) {
      assertEquals(2, ex.getProcessedCount());
      assertSame(error, ex.getCause());
    }
    assertMeterReading(1, GrpcRDASource.CALLS_METER);
    assertMeterReading(4, GrpcRDASource.RECORDS_RECEIVED_METER);
    assertMeterReading(2, GrpcRDASource.RECORDS_STORED_METER);
    assertMeterReading(1, GrpcRDASource.BATCHES_METER);
  }

  @Test
  public void testStopAfterFourItemsDueToMaxReceived() throws Exception {
    doReturn(Instant.ofEpochMilli(1000)).when(clock).instant();
    doReturn(Arrays.asList(1, 2, 3, 4, 5).iterator()).when(caller).callService(any());
    doReturn(2).when(sink).writeBatch(Arrays.asList(CLAIM_1, CLAIM_2));
    doReturn(2).when(sink).writeBatch(Arrays.asList(CLAIM_3, CLAIM_4));

    final int result = source.retrieveAndProcessObjects(4, 2, Duration.ofSeconds(10), sink);
    assertEquals(4, result);
    assertMeterReading(1, GrpcRDASource.CALLS_METER);
    assertMeterReading(4, GrpcRDASource.RECORDS_RECEIVED_METER);
    assertMeterReading(4, GrpcRDASource.RECORDS_STORED_METER);
    assertMeterReading(2, GrpcRDASource.BATCHES_METER);
  }

  @Test
  public void testStopAfterThreeItemsDueToMaxRuntime() throws Exception {
    doReturn(
            Instant.ofEpochMilli(1000), // service call
            Instant.ofEpochMilli(2000), // first item
            Instant.ofEpochMilli(3000), // second item
            Instant.ofEpochMilli(4000), // third item
            Instant.ofEpochMilli(5000), // fourth item
            Instant.ofEpochMilli(6000), // fifth item
            Instant.ofEpochMilli(7000)) // any others
        .when(clock)
        .instant();
    doReturn(Arrays.asList(1, 2, 3, 4, 5).iterator())
        .when(caller)
        .callService(Duration.ofSeconds(4));
    doReturn(2).when(sink).writeBatch(Arrays.asList(CLAIM_1, CLAIM_2));
    doReturn(1).when(sink).writeBatch(Collections.singletonList(CLAIM_3));

    final int result = source.retrieveAndProcessObjects(5, 2, Duration.ofSeconds(4), sink);
    assertEquals(3, result);
    assertMeterReading(1, GrpcRDASource.CALLS_METER);
    assertMeterReading(3, GrpcRDASource.RECORDS_RECEIVED_METER);
    assertMeterReading(3, GrpcRDASource.RECORDS_STORED_METER);
    assertMeterReading(2, GrpcRDASource.BATCHES_METER);
  }

  @Test
  public void testStopAfterFourItemsAndTwoCallsDueToMaxRuntime() throws Exception {
    doReturn(
            Instant.ofEpochMilli(1000), // first service call
            Instant.ofEpochMilli(2000), // first item
            Instant.ofEpochMilli(3000), // second item
            Instant.ofEpochMilli(4000), // second service call
            Instant.ofEpochMilli(5000), // third item
            Instant.ofEpochMilli(6000), // fourth item
            Instant.ofEpochMilli(7000), // fifth item
            Instant.ofEpochMilli(8000)) // any others
        .when(clock)
        .instant();
    doReturn(Arrays.asList(1, 2).iterator()).when(caller).callService(Duration.ofSeconds(6));
    doReturn(Arrays.asList(3, 4, 5).iterator()).when(caller).callService(Duration.ofSeconds(3));
    doReturn(2).when(sink).writeBatch(Arrays.asList(CLAIM_1, CLAIM_2));
    doReturn(2).when(sink).writeBatch(Arrays.asList(CLAIM_3, CLAIM_4));

    final int result = source.retrieveAndProcessObjects(5, 2, Duration.ofSeconds(6), sink);
    assertEquals(4, result);
    assertMeterReading(1, GrpcRDASource.CALLS_METER);
    assertMeterReading(4, GrpcRDASource.RECORDS_RECEIVED_METER);
    assertMeterReading(4, GrpcRDASource.RECORDS_STORED_METER);
    assertMeterReading(2, GrpcRDASource.BATCHES_METER);
  }

  @Test
  public void testClose() throws Exception {
    doReturn(channel).when(channel).shutdown();
    source.close();
    source.close(); // second call does nothing
    verify(channel, times(1)).shutdown();
    verify(channel, times(1)).awaitTermination(5, TimeUnit.SECONDS);
  }

  private GrpcStreamCaller<Integer> callerFactory(ManagedChannel channel) {
    assertSame(this.channel, channel);
    return caller;
  }

  private void assertMeterReading(long expected, String meterName) {
    long actual = appMetrics.meter(meterName).getCount();
    assertEquals("Meter " + meterName, expected, actual);
  }
}
