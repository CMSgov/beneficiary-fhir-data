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
  private Clock clock;
  private GrpcStreamCaller<Integer> caller;
  private ManagedChannel channel;
  private RDASink<PreAdjudicatedClaim> sink;
  private GrpcRDASource<Integer> source;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    clock = mock(Clock.class);
    caller = mock(GrpcStreamCaller.class);
    channel = mock(ManagedChannel.class);
    source = new GrpcRDASource<>(channel, caller, clock);
    sink = mock(RDASink.class);
    doReturn(CLAIM_1).when(caller).convertResultToClaim(1);
    doReturn(CLAIM_2).when(caller).convertResultToClaim(2);
    doReturn(CLAIM_3).when(caller).convertResultToClaim(3);
    doReturn(CLAIM_4).when(caller).convertResultToClaim(4);
    doReturn(CLAIM_5).when(caller).convertResultToClaim(5);
  }

  @Test
  public void testSuccessfullyProcessThreeItems() throws Exception {
    doReturn(Instant.ofEpochMilli(1000)).when(clock).instant();
    doReturn(Arrays.asList(1, 2, 3).iterator()).when(caller).callService(any());
    doReturn(2).when(sink).writeBatch(Arrays.asList(CLAIM_1, CLAIM_2));
    doReturn(1).when(sink).writeBatch(Collections.singletonList(CLAIM_3));

    final int result = source.retrieveAndProcessObjects(3, 2, Duration.ofSeconds(10), sink);
    assertEquals(3, result);
    verify(caller).createStub(channel);
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
    verify(caller).createStub(channel);
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
    verify(caller).createStub(channel);
  }

  @Test
  public void testStopAfterFourItemsDueToMaxReceived() throws Exception {
    doReturn(Instant.ofEpochMilli(1000)).when(clock).instant();
    doReturn(Arrays.asList(1, 2, 3, 4, 5).iterator()).when(caller).callService(any());
    doReturn(2).when(sink).writeBatch(Arrays.asList(CLAIM_1, CLAIM_2));
    doReturn(2).when(sink).writeBatch(Arrays.asList(CLAIM_3, CLAIM_4));

    final int result = source.retrieveAndProcessObjects(4, 2, Duration.ofSeconds(10), sink);
    assertEquals(4, result);
    verify(caller).createStub(channel);
  }

  @Test
  public void testStopAfterThreeItemsDueToMaxRuntime() throws Exception {
    doReturn(
            Instant.ofEpochMilli(1000), // computing stopTime
            Instant.ofEpochMilli(1000), // outer loop
            Instant.ofEpochMilli(1000), // first item
            Instant.ofEpochMilli(1000), // second item
            Instant.ofEpochMilli(1000), // third item
            Instant.ofEpochMilli(2000), // fourth item
            Instant.ofEpochMilli(3000), // fifth item
            Instant.ofEpochMilli(4000)) // outer look and any others
        .when(clock)
        .instant();
    doReturn(Arrays.asList(1, 2, 3, 4, 5).iterator()).when(caller).callService(any());
    doReturn(2).when(sink).writeBatch(Arrays.asList(CLAIM_1, CLAIM_2));
    doReturn(1).when(sink).writeBatch(Arrays.asList(CLAIM_3));

    final int result = source.retrieveAndProcessObjects(5, 2, Duration.ofMillis(10), sink);
    assertEquals(3, result);
    verify(caller).createStub(channel);
  }

  @Test
  public void testClose() throws Exception {
    doReturn(channel).when(channel).shutdownNow();
    source.close();
    source.close(); // second call does nothing
    verify(channel, times(1)).shutdownNow();
    verify(channel, times(1)).awaitTermination(5, TimeUnit.SECONDS);
  }
}
