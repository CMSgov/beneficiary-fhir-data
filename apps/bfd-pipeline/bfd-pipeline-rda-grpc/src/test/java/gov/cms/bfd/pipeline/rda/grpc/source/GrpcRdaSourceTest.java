package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.bfd.pipeline.rda.grpc.source.GrpcResponseStream.StreamInterruptedException;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GrpcRdaSourceTest {
  private static final Integer CLAIM_1 = 101;
  private static final Integer CLAIM_2 = 102;
  private static final Integer CLAIM_3 = 103;
  private static final Integer CLAIM_4 = 104;
  private static final Integer CLAIM_5 = 105;
  private MetricRegistry appMetrics;
  private GrpcStreamCaller<Integer> caller;
  private ManagedChannel channel;
  private RdaSink<Integer> sink;
  private GrpcRdaSource<Integer> source;
  private ClientCall<Integer, Integer> clientCall;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    appMetrics = new MetricRegistry();
    caller = mock(GrpcStreamCaller.class);
    channel = mock(ManagedChannel.class);
    sink = mock(RdaSink.class);
    clientCall = mock(ClientCall.class);
    source = new GrpcRdaSource<>(channel, caller, appMetrics);
  }

  @Test
  public void testSuccessfullyProcessThreeItems() throws Exception {
    doReturn(createResponse(CLAIM_1, CLAIM_2, CLAIM_3)).when(caller).callService(channel);
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
    doReturn(createResponse(CLAIM_1, CLAIM_2, CLAIM_3, CLAIM_4, CLAIM_5))
        .when(caller)
        .callService(channel);
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
      assertNotNull(ex.getCause());
      assertSame(error, ex.getCause().getCause());
    }
    assertMeterReading(1, GrpcRdaSource.CALLS_METER);
    assertMeterReading(4, GrpcRdaSource.RECORDS_RECEIVED_METER);
    assertMeterReading(2, GrpcRdaSource.RECORDS_STORED_METER);
    assertMeterReading(1, GrpcRdaSource.BATCHES_METER);
  }

  @Test
  public void testHandlesExceptionFromCaller() {
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
    doReturn(createResponse(CLAIM_1, CLAIM_2, CLAIM_3, CLAIM_4, CLAIM_5))
        .when(caller)
        .callService(channel);
    doReturn(2).when(sink).writeBatch(Arrays.asList(CLAIM_1, CLAIM_2));
    // second batch should throw our exception as though it failed after processing 1 record
    final Exception error = new RuntimeException("oops");
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
  public void testHandlesInterruptFromStream() throws Exception {
    // Creates a response with 3 valid values followed by an interrupt.
    final GrpcResponseStream<Integer> response = mock(GrpcResponseStream.class);
    when(response.next()).thenReturn(CLAIM_1, CLAIM_2, CLAIM_3);
    when(response.hasNext())
        .thenReturn(true)
        .thenReturn(true)
        .thenThrow(new StreamInterruptedException(new StatusRuntimeException(Status.INTERNAL)));
    doReturn(response).when(caller).callService(channel);

    // we expect to write a single batch with the first two records
    doReturn(2).when(sink).writeBatch(Arrays.asList(CLAIM_1, CLAIM_2));

    int processed = source.retrieveAndProcessObjects(2, sink);
    assertEquals(2, processed);
    assertMeterReading(1, GrpcRdaSource.CALLS_METER);
    assertMeterReading(2, GrpcRdaSource.RECORDS_RECEIVED_METER);
    assertMeterReading(2, GrpcRdaSource.RECORDS_STORED_METER);
    assertMeterReading(1, GrpcRdaSource.BATCHES_METER);
    verify(response).cancelStream(anyString());
  }

  @Test
  public void testClose() throws Exception {
    doReturn(channel).when(channel).shutdown();
    source.close();
    source.close(); // second call does nothing
    verify(channel, times(1)).shutdown();
    verify(channel, times(1)).awaitTermination(5, TimeUnit.SECONDS);
  }

  @Test
  public void configIsSerializable() throws Exception {
    final GrpcRdaSource.Config original =
        new GrpcRdaSource.Config("localhost", 5432, Duration.ofMinutes(59));
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
      out.writeObject(original);
    }
    GrpcRdaSource.Config loaded;
    try (ObjectInputStream inp =
        new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
      loaded = (GrpcRdaSource.Config) inp.readObject();
    }
    Assert.assertEquals(original, loaded);
  }

  private void assertMeterReading(long expected, String meterName) {
    long actual = appMetrics.meter(meterName).getCount();
    assertEquals("Meter " + meterName, expected, actual);
  }

  private GrpcResponseStream<Integer> createResponse(int... values) {
    return new GrpcResponseStream<>(clientCall, Arrays.stream(values).iterator());
  }
}
