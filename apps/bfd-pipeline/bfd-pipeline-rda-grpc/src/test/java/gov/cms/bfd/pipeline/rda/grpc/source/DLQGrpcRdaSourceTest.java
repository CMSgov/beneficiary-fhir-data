package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rda.MessageError;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import utils.TestUtils;

@ExtendWith(MockitoExtension.class)
public class DLQGrpcRdaSourceTest {

  @Mock private RdaSink<Long, Long> mockSink;

  @Mock private EntityManager mockManager;

  @Mock private ManagedChannel mockChannel;

  @Mock private RdaSourceConfig mockConfig;

  @Mock private GrpcStreamCaller<Long> mockCaller;

  @Mock private MetricRegistry mockMetrics;

  @Mock private DLQGrpcRdaSource.DLQDao mockDao;

  @BeforeEach
  public void setUp() {
    doReturn(mockChannel).when(mockConfig).createChannel();
  }

  private static final List<MessageError> FISS_MOCK_MESSAGE_ERRORS =
      List.of(
          MessageError.builder().claimType(MessageError.ClaimType.FISS).sequenceNumber(5L).build(),
          MessageError.builder()
              .claimType(MessageError.ClaimType.FISS)
              .sequenceNumber(15L)
              .build());

  private static final List<MessageError> MCS_MOCK_MESSAGE_ERRORS =
      List.of(
          MessageError.builder().claimType(MessageError.ClaimType.MCS).sequenceNumber(7L).build(),
          MessageError.builder().claimType(MessageError.ClaimType.MCS).sequenceNumber(9L).build());

  /** Checks that the logic lambda was successfully and correctly invoked for FISS claims */
  @Test
  void shouldInvokeLogicLambdaFISS()
      throws NoSuchFieldException, IllegalAccessException, ProcessingException {
    final String claimType = "fiss";
    final MessageError.ClaimType type = MessageError.ClaimType.FISS;

    AbstractGrpcRdaSource.Processor mockLogic = mock(AbstractGrpcRdaSource.Processor.class);

    DLQGrpcRdaSource<Long, Long> sourceSpy =
        spy(
            new DLQGrpcRdaSource<>(
                mockManager, Objects::equals, mockConfig, mockCaller, mockMetrics, claimType));

    doReturn(mockLogic).when(sourceSpy).dlqProcessingLogic(mockSink, type, Set.of(5L, 15L));

    doReturn(2).when(sourceSpy).tryRetrieveAndProcessObjects(mockLogic);

    doReturn(FISS_MOCK_MESSAGE_ERRORS)
        .when(mockDao)
        .findAllMessageErrorsByClaimType(MessageError.ClaimType.FISS);

    TestUtils.setField(sourceSpy, "dao", mockDao);

    int actualProcessed = sourceSpy.retrieveAndProcessObjects(5, mockSink);

    assertEquals(2, actualProcessed);
  }

  /** Checks that the logic lambda was successfully and correctly invoked for MCS claims */
  @Test
  void shouldInvokeLogicLambdaForMCS()
      throws NoSuchFieldException, IllegalAccessException, ProcessingException {
    final String claimType = "mcs";
    final MessageError.ClaimType type = MessageError.ClaimType.MCS;

    AbstractGrpcRdaSource.Processor mockLogic = mock(AbstractGrpcRdaSource.Processor.class);

    DLQGrpcRdaSource<Long, Long> sourceSpy =
        spy(
            new DLQGrpcRdaSource<>(
                mockManager, Objects::equals, mockConfig, mockCaller, mockMetrics, claimType));

    doReturn(mockLogic).when(sourceSpy).dlqProcessingLogic(mockSink, type, Set.of(7L, 9L));

    doReturn(2).when(sourceSpy).tryRetrieveAndProcessObjects(mockLogic);

    doReturn(MCS_MOCK_MESSAGE_ERRORS)
        .when(mockDao)
        .findAllMessageErrorsByClaimType(MessageError.ClaimType.MCS);

    TestUtils.setField(sourceSpy, "dao", mockDao);

    int actualProcessed = sourceSpy.retrieveAndProcessObjects(5, mockSink);

    assertEquals(2, actualProcessed);
  }

  /**
   * Tests if the {@link MessageError}s in the database are correctly reprocessed. A message should
   * successfully reprocess if it was returned by the {@link GrpcResponseStream} and it was
   * subsequently successfully deleted from the database.
   */
  @Test
  void shouldReprocessDLQ() throws Exception {
    final String claimType = "fiss";
    final MessageError.ClaimType type = MessageError.ClaimType.FISS;
    final CallOptions mockCallOptions = mock(CallOptions.class);
    final Meter mockMeter = mock(Meter.class);

    // Set up sink mocks, needs to return fake DCNs and process count
    doReturn("A").when(mockSink).getDedupKeyForMessage(5L);

    doReturn(0).when(mockSink).getProcessedCount();

    // Set up metrics mock for meters
    doReturn(mockMeter).when(mockMetrics).meter(anyString());

    // Set up config mocks, needs to return fake call options and channel
    doReturn(mockCallOptions).when(mockConfig).createCallOptions();

    // Create our spy for the class we're testing, so we can mock sibling methods
    DLQGrpcRdaSource<Long, Long> sourceSpy =
        spy(
            new DLQGrpcRdaSource<>(
                mockManager, Objects::equals, mockConfig, mockCaller, mockMetrics, claimType));

    doNothing().when(sourceSpy).setUptimeToReceiving();

    // unchecked - This is fine for a mock.
    //noinspection unchecked
    doReturn(1).when(sourceSpy).submitBatchToSink(eq("v1"), eq(mockSink), any(Map.class));

    // Always return fake version for caller's version call
    doReturn("v1").when(mockCaller).callVersionService(mockChannel, mockCallOptions);

    // Create a stream that returns a sequence in the DLQ (5)
    // unchecked - This is fine for a mock.
    //noinspection unchecked
    GrpcResponseStream<Long> mockStreamA = mock(GrpcResponseStream.class);

    doReturn(true).doReturn(false).when(mockStreamA).hasNext();

    doReturn(5L).doReturn(null).when(mockStreamA).next();

    doReturn(mockStreamA).when(mockCaller).callService(mockChannel, mockCallOptions, 5);

    // Create a stream that returns no sequence in the DLQ
    // unchecked - This is fine for a mock.
    //noinspection unchecked
    GrpcResponseStream<Long> mockStreamB = mock(GrpcResponseStream.class);

    doReturn(true).when(mockStreamB).hasNext();

    doReturn(16L).when(mockStreamB).next();

    doReturn(mockStreamB).when(mockCaller).callService(mockChannel, mockCallOptions, 15);

    // Mock for deleting the only sequence that should have been found and processed
    doReturn(1L).when(mockDao).delete(5L, MessageError.ClaimType.FISS);

    // Force our mock dao into the source object using reflection hackery
    TestUtils.setField(sourceSpy, "dao", mockDao);

    AbstractGrpcRdaSource.ProcessResult expectedResult = new AbstractGrpcRdaSource.ProcessResult();
    expectedResult.setCount(1);

    // We're testing the lambda logic in this test, so have to grab it first from the method that
    // returns it
    AbstractGrpcRdaSource.Processor logic =
        sourceSpy.dlqProcessingLogic(mockSink, type, Set.of(5L, 15L));
    AbstractGrpcRdaSource.ProcessResult actualResult = logic.process();

    // In the end, only 1 sequence should have been reprocessed and deleted (5)
    assertEquals(expectedResult, actualResult);
    verify(mockDao).delete(5L, MessageError.ClaimType.FISS);
    verify(mockDao, times(1)).delete(anyLong(), any(MessageError.ClaimType.class));
    verify(mockSink, times(2)).shutdown(any(Duration.class));
  }
}
