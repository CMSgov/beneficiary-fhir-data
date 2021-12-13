package gov.cms.bfd.pipeline.rda.grpc.source;

import static gov.cms.bfd.pipeline.rda.grpc.RdaPipelineTestUtils.assertMeterReading;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.bfd.pipeline.rda.grpc.source.GrpcResponseStream.StreamInterruptedException;
import io.grpc.CallOptions;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GrpcRdaSourceTest {
  private static final Integer CLAIM_1 = 101;
  private static final Integer CLAIM_2 = 102;
  private static final Integer CLAIM_3 = 103;
  private static final Integer CLAIM_4 = 104;
  private static final Integer CLAIM_5 = 105;
  public static final String VERSION = "version";
  private MetricRegistry appMetrics;
  @Mock private GrpcStreamCaller<Integer> caller;
  @Mock private ManagedChannel channel;
  @Mock private RdaSink<Integer, Integer> sink;
  @Mock private ClientCall<Integer, Integer> clientCall;
  private GrpcRdaSource<Integer, Integer> source;
  private GrpcRdaSource.Metrics metrics;

  @Before
  public void setUp() throws Exception {
    appMetrics = new MetricRegistry();
    source =
        spy(
            new GrpcRdaSource<>(
                channel, caller, () -> CallOptions.DEFAULT, appMetrics, "ints", Optional.empty()));
    doReturn(VERSION).when(caller).callVersionService(channel, CallOptions.DEFAULT);
    doAnswer(i -> i.getArgument(0).toString()).when(sink).getDedupKeyForMessage(any());
    metrics = source.getMetrics();
  }

  @Test
  public void metricNames() {
    assertEquals(
        Arrays.asList(
            "GrpcRdaSource.ints.batches",
            "GrpcRdaSource.ints.calls",
            "GrpcRdaSource.ints.failures",
            "GrpcRdaSource.ints.objects.received",
            "GrpcRdaSource.ints.objects.stored",
            "GrpcRdaSource.ints.successes",
            "GrpcRdaSource.ints.uptime"),
        new ArrayList<>(appMetrics.getNames()));
  }

  @Test
  public void testSuccessfullyProcessThreeItems() throws Exception {
    doReturn(Optional.of(41L)).when(sink).readMaxExistingSequenceNumber();
    doReturn(createResponse(CLAIM_1, CLAIM_2, CLAIM_3))
        .when(caller)
        .callService(channel, CallOptions.DEFAULT, 42L);
    doReturn(2).when(sink).writeMessages(VERSION, List.of(CLAIM_1, CLAIM_2));
    doReturn(1).when(sink).writeMessages(VERSION, List.of(CLAIM_3));

    final int result = source.retrieveAndProcessObjects(2, sink);
    assertEquals(3, result);
    assertMeterReading(1, "calls", metrics.getCalls());
    assertMeterReading(3, "received", metrics.getObjectsReceived());
    assertMeterReading(3, "stored", metrics.getObjectsStored());
    assertMeterReading(2, "batches", metrics.getBatches());
    assertMeterReading(1, "successes", metrics.getSuccesses());
    assertMeterReading(0, "failures", metrics.getFailures());
    // once at start, twice after a batch
    verify(source, times(3)).setUptimeToRunning();
    // once per object received
    verify(source, times(3)).setUptimeToReceiving();
    verify(source).setUptimeToStopped();
    verify(caller).callService(channel, CallOptions.DEFAULT, 42L);
  }

  @Test
  public void testUsesHardCodedSequenceNumberWhenProvided() throws Exception {
    source =
        spy(
            new GrpcRdaSource<>(
                channel, caller, () -> CallOptions.DEFAULT, appMetrics, "ints", Optional.of(18L)));
    doReturn(createResponse(CLAIM_1)).when(caller).callService(channel, CallOptions.DEFAULT, 18L);
    doReturn(1).when(sink).writeMessages(VERSION, Arrays.asList(CLAIM_1));

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
    verify(caller).callService(channel, CallOptions.DEFAULT, 18L);
    verify(sink, times(0)).readMaxExistingSequenceNumber();
  }

  @Test
  public void testPassesThroughProcessingExceptionFromSink() throws Exception {
    doReturn(Optional.of(41L)).when(sink).readMaxExistingSequenceNumber();
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
      assertSame(error, ex.getCause().getCause());
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
    verify(caller).callService(channel, CallOptions.DEFAULT, 42L);
  }

  @Test
  public void testHandlesExceptionFromCaller() throws Exception {
    doReturn(Optional.empty()).when(sink).readMaxExistingSequenceNumber();
    final Exception error = new IOException("oops");
    final GrpcStreamCaller<Integer> caller = mock(GrpcStreamCaller.class);
    doThrow(error).when(caller).callService(any(), any(), anyLong());
    source =
        spy(
            new GrpcRdaSource<>(
                channel, caller, () -> CallOptions.DEFAULT, appMetrics, "ints", Optional.empty()));

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

  @Test
  public void testHandlesRuntimeExceptionFromSink() throws Exception {
    doReturn(Optional.empty()).when(sink).readMaxExistingSequenceNumber();
    doReturn(createResponse(CLAIM_1, CLAIM_2, CLAIM_3, CLAIM_4, CLAIM_5))
        .when(caller)
        .callService(same(channel), eq(CallOptions.DEFAULT), anyLong());
    doReturn(2).when(sink).writeMessages(VERSION, Arrays.asList(CLAIM_1, CLAIM_2));
    // second batch should throw our exception as though it failed after processing 1 record
    final Exception error = new RuntimeException("oops");
    doThrow(error).when(sink).writeMessages(VERSION, Arrays.asList(CLAIM_3, CLAIM_4));

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

  @Test
  public void testHandlesInterruptFromStream() throws Exception {
    doReturn(Optional.of(41L)).when(sink).readMaxExistingSequenceNumber();
    // Creates a response with 3 valid values followed by an interrupt.
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
    verify(caller).callService(channel, CallOptions.DEFAULT, 42L);
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
        GrpcRdaSource.Config.builder()
            .serverType(GrpcRdaSource.Config.ServerType.Remote)
            .host("localhost")
            .port(5432)
            .maxIdle(Duration.ofMinutes(59))
            .authenticationToken("secret")
            .build();
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

  private GrpcResponseStream<Integer> createResponse(int... values) {
    return new GrpcResponseStream<>(clientCall, Arrays.stream(values).iterator());
  }
}
