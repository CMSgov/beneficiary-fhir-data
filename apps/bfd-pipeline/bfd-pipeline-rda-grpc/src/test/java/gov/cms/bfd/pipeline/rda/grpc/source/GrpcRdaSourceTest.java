package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.pipeline.rda.grpc.PreAdjudicatedClaim;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import io.grpc.ManagedChannel;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;

public class GrpcRdaSourceTest {
  private static final PreAdjudicatedClaim CLAIM_1 = new PreAdjudicatedClaim();
  private static final PreAdjudicatedClaim CLAIM_2 = new PreAdjudicatedClaim();
  private static final PreAdjudicatedClaim CLAIM_3 = new PreAdjudicatedClaim();
  private static final PreAdjudicatedClaim CLAIM_4 = new PreAdjudicatedClaim();
  private static final PreAdjudicatedClaim CLAIM_5 = new PreAdjudicatedClaim();
  private MetricRegistry appMetrics;
  private GrpcStreamCaller<Integer> caller;
  private ManagedChannel channel;
  private RdaSink<PreAdjudicatedClaim> sink;
  private GrpcRdaSource<Integer> source;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    appMetrics = new MetricRegistry();
    caller = mock(GrpcStreamCaller.class);
    channel = mock(ManagedChannel.class);
    sink = mock(RdaSink.class);
    doReturn(CLAIM_1).when(caller).convertResultToClaim(1);
    doReturn(CLAIM_2).when(caller).convertResultToClaim(2);
    doReturn(CLAIM_3).when(caller).convertResultToClaim(3);
    doReturn(CLAIM_4).when(caller).convertResultToClaim(4);
    doReturn(CLAIM_5).when(caller).convertResultToClaim(5);
    source = new GrpcRdaSource<>(channel, this::callerFactory, appMetrics);
  }

  @Test
  public void testSuccessfullyProcessThreeItems() throws Exception {
    doReturn(Arrays.asList(1, 2, 3).iterator()).when(caller).callService();
    doReturn(2).when(sink).writeBatch(Arrays.asList(CLAIM_1, CLAIM_2));
    doReturn(1).when(sink).writeBatch(Collections.singletonList(CLAIM_3));

    final int result = source.retrieveAndProcessObjects(2, sink);
    assertEquals(3, result);
    assertMeterReading(1, GrpcRdaSource.CALLS_METER);
    assertMeterReading(3, GrpcRdaSource.RECORDS_RECEIVED_METER);
    assertMeterReading(3, GrpcRdaSource.RECORDS_STORED_METER);
    assertMeterReading(2, GrpcRdaSource.BATCHES_METER);
  }

  @Test
  public void testPassesThroughProcessingExceptionFromSink() throws Exception {
    final Exception error = new IOException("oops");
    doReturn(Arrays.asList(1, 2, 3, 4, 5).iterator()).when(caller).callService();
    doReturn(2).when(sink).writeBatch(Arrays.asList(CLAIM_1, CLAIM_2));
    // second batch should throw our exception as though it failed after processing 1 record
    doThrow(new ProcessingException(error, 1))
        .when(sink)
        .writeBatch(Arrays.asList(CLAIM_3, CLAIM_4));

    try {
      source.retrieveAndProcessObjects(2, sink);
      fail("source should have thrown exception");
    } catch (ProcessingException ex) {
      assertEquals(3, ex.getProcessedCount());
      assertSame(error, ex.getCause());
    }
    assertMeterReading(1, GrpcRdaSource.CALLS_METER);
    assertMeterReading(4, GrpcRdaSource.RECORDS_RECEIVED_METER);
    assertMeterReading(2, GrpcRdaSource.RECORDS_STORED_METER);
    assertMeterReading(1, GrpcRdaSource.BATCHES_METER);
  }

  @Test
  public void testHandlesExceptionFromCallerFactory() {
    final Exception error = new IOException("oops");
    source =
        new GrpcRdaSource<>(
            channel,
            c -> {
              throw error;
            },
            appMetrics);

    try {
      source.retrieveAndProcessObjects(2, sink);
      fail("source should have thrown exception");
    } catch (ProcessingException ex) {
      assertEquals(0, ex.getProcessedCount());
      assertSame(error, ex.getCause());
    }
    assertMeterReading(1, GrpcRdaSource.CALLS_METER);
    assertMeterReading(0, GrpcRdaSource.RECORDS_RECEIVED_METER);
    assertMeterReading(0, GrpcRdaSource.RECORDS_STORED_METER);
    assertMeterReading(0, GrpcRdaSource.BATCHES_METER);
  }

  @Test
  public void testHandlesRuntimeExceptionFromSink() throws Exception {
    final Exception error = new RuntimeException("oops");
    doReturn(Arrays.asList(1, 2, 3, 4, 5).iterator()).when(caller).callService();
    doReturn(2).when(sink).writeBatch(Arrays.asList(CLAIM_1, CLAIM_2));
    // second batch should throw our exception as though it failed after processing 1 record
    doThrow(error).when(sink).writeBatch(Arrays.asList(CLAIM_3, CLAIM_4));

    try {
      source.retrieveAndProcessObjects(2, sink);
      fail("source should have thrown exception");
    } catch (ProcessingException ex) {
      assertEquals(2, ex.getProcessedCount());
      assertSame(error, ex.getCause());
    }
    assertMeterReading(1, GrpcRdaSource.CALLS_METER);
    assertMeterReading(4, GrpcRdaSource.RECORDS_RECEIVED_METER);
    assertMeterReading(2, GrpcRdaSource.RECORDS_STORED_METER);
    assertMeterReading(1, GrpcRdaSource.BATCHES_METER);
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
