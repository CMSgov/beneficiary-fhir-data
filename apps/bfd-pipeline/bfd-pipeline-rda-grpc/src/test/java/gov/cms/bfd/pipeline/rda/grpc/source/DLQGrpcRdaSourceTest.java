package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rda.MessageError;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import utils.TestUtils;

/** Tests for the {@link DLQGrpcRdaSource} class. */
@ExtendWith(MockitoExtension.class)
public class DLQGrpcRdaSourceTest {

  /** Arbitrary rda api version being used for tests. */
  private static final String TEST_RDA_VERSION = "0.0.1";

  /** Max age for message errors table. */
  private static final int MAX_DQL_AGE_DAYS = 100;

  /** Mock {@link RdaSink} to use in testing. */
  @Mock private RdaSink<Long, Long> mockSink;

  /** Mock {@link TransactionManager} to use in testing. */
  @Mock private TransactionManager mockManager;

  /** Mock {@link ManagedChannel} to use in testing. */
  @Mock private ManagedChannel mockChannel;

  /** Mock {@link RdaSourceConfig} to use in testing. */
  @Mock private RdaSourceConfig mockConfig;

  /** Mock {@link GrpcStreamCaller} to use in testing. */
  @Mock private GrpcStreamCaller<Long> mockCaller;

  /** Mock {@link DLQDao} to use in testing. */
  @Mock private DLQDao mockDao;

  /** Mock {@link RdaVersion} to use in testing. */
  @Mock private RdaVersion rdaVersion;

  /** {@link MetricRegistry} to use in testing. */
  private MeterRegistry meters;

  /** Set up the mocks prior to each test. */
  @BeforeEach
  public void setUp() {
    meters = new SimpleMeterRegistry();
    lenient().doReturn(false).when(rdaVersion).allows(anyString());
    lenient().doReturn(true).when(rdaVersion).allows(TEST_RDA_VERSION);
    lenient().doReturn(MAX_DQL_AGE_DAYS).when(mockConfig).getMessageErrorExpirationDays();
    lenient().doReturn(mockChannel).when(mockConfig).createChannel();
  }

  /** Testing value to use for the first FISS error sequence number. */
  private static final long FISS_ERROR_ONE_SEQ = 5L;

  /** Testing value to use for the second FISS error sequence number. */
  private static final long FISS_ERROR_TWO_SEQ = 15L;

  /** Testing value to use for the first MCS error sequence number. */
  private static final long MCS_ERROR_ONE_SEQ = 7L;

  /** Testing value to use for the second MCS error sequence number. */
  private static final long MCS_ERROR_TWO_SEQ = 9L;

  /** The FISS {@link MessageError} objects to use in testing. */
  private static final List<MessageError> FISS_MOCK_MESSAGE_ERRORS =
      List.of(
          MessageError.builder()
              .claimType(MessageError.ClaimType.FISS)
              .sequenceNumber(FISS_ERROR_ONE_SEQ)
              .build(),
          MessageError.builder()
              .claimType(MessageError.ClaimType.FISS)
              .sequenceNumber(FISS_ERROR_TWO_SEQ)
              .build());

  /** The MCS {@link MessageError} objects to use in testing. */
  private static final List<MessageError> MCS_MOCK_MESSAGE_ERRORS =
      List.of(
          MessageError.builder()
              .claimType(MessageError.ClaimType.MCS)
              .sequenceNumber(MCS_ERROR_ONE_SEQ)
              .build(),
          MessageError.builder()
              .claimType(MessageError.ClaimType.MCS)
              .sequenceNumber(MCS_ERROR_TWO_SEQ)
              .build());

  /** Checks that the logic lambda was successfully and correctly invoked for FISS claims. */
  @Test
  void shouldInvokeLogicLambdaFISS()
      throws NoSuchFieldException, IllegalAccessException, ProcessingException {
    final String claimType = "fiss";
    final MessageError.ClaimType type = MessageError.ClaimType.FISS;

    AbstractGrpcRdaSource.Processor mockLogic = mock(AbstractGrpcRdaSource.Processor.class);

    DLQGrpcRdaSource<Long, Long> sourceSpy =
        spy(
            new DLQGrpcRdaSource<>(
                mockManager,
                Objects::equals,
                mockConfig,
                mockCaller,
                meters,
                claimType,
                rdaVersion));

    doReturn(mockLogic)
        .when(sourceSpy)
        .dlqProcessingLogic(mockSink, type, Set.of(FISS_ERROR_ONE_SEQ, FISS_ERROR_TWO_SEQ));

    final int MOCK_PROCESS_COUNT = 2;

    doReturn(MOCK_PROCESS_COUNT).when(sourceSpy).tryRetrieveAndProcessObjects(mockLogic);

    doReturn(FISS_MOCK_MESSAGE_ERRORS)
        .when(mockDao)
        .findAllMessageErrorsByClaimTypeAndStatus(
            MessageError.ClaimType.FISS, MessageError.Status.UNRESOLVED);

    TestUtils.setField(sourceSpy, "dao", mockDao);

    int actualProcessed = sourceSpy.retrieveAndProcessObjects(5, mockSink);

    assertEquals(MOCK_PROCESS_COUNT, actualProcessed);

    // should have triggered DLQ cleanup method
    verify(mockDao).deleteExpiredMessageErrors(MAX_DQL_AGE_DAYS, MessageError.ClaimType.FISS);
  }

  /**
   * Ensure that {@link DLQGrpcRdaSource#deleteExpiredDlqRecords} calls the dao method and returns
   * its result.
   */
  @Test
  void deleteExpiredDlqRecordsShouldCallDaoAndReturnResult() {
    final var claimType = "fiss";
    final var source =
        new DLQGrpcRdaSource<>(
            mockManager,
            Objects::equals,
            mockChannel,
            mockCaller,
            mockConfig::createCallOptions,
            meters,
            claimType,
            rdaVersion,
            MAX_DQL_AGE_DAYS,
            mockDao);
    doReturn(18)
        .when(mockDao)
        .deleteExpiredMessageErrors(MAX_DQL_AGE_DAYS, MessageError.ClaimType.FISS);
    assertEquals(18, source.deleteExpiredDlqRecords(MessageError.ClaimType.FISS));
  }

  /**
   * Ensure that {@link DLQGrpcRdaSource#deleteExpiredDlqRecords} calls the dao method and returns
   * its result and captures any exception it throws.
   */
  @Test
  void deleteExpiredDlqRecordsShouldCallDaoAndCaptureException() {
    final var claimType = "fiss";
    final var source =
        new DLQGrpcRdaSource<>(
            mockManager,
            Objects::equals,
            mockChannel,
            mockCaller,
            mockConfig::createCallOptions,
            meters,
            claimType,
            rdaVersion,
            MAX_DQL_AGE_DAYS,
            mockDao);
    doThrow(new RuntimeException("can't stop me!"))
        .when(mockDao)
        .deleteExpiredMessageErrors(MAX_DQL_AGE_DAYS, MessageError.ClaimType.FISS);
    assertEquals(0, source.deleteExpiredDlqRecords(MessageError.ClaimType.FISS));
  }

  /** Checks that the logic lambda was successfully and correctly invoked for MCS claims. */
  @Test
  void shouldInvokeLogicLambdaForMCS()
      throws NoSuchFieldException, IllegalAccessException, ProcessingException {
    final String claimType = "mcs";
    final MessageError.ClaimType type = MessageError.ClaimType.MCS;

    AbstractGrpcRdaSource.Processor mockLogic = mock(AbstractGrpcRdaSource.Processor.class);

    DLQGrpcRdaSource<Long, Long> sourceSpy =
        spy(
            new DLQGrpcRdaSource<>(
                mockManager,
                Objects::equals,
                mockConfig,
                mockCaller,
                meters,
                claimType,
                rdaVersion));

    doReturn(mockLogic)
        .when(sourceSpy)
        .dlqProcessingLogic(mockSink, type, Set.of(MCS_ERROR_ONE_SEQ, MCS_ERROR_TWO_SEQ));

    final int MOCK_PROCESS_COUNT = 2;

    doReturn(MOCK_PROCESS_COUNT).when(sourceSpy).tryRetrieveAndProcessObjects(mockLogic);

    doReturn(MCS_MOCK_MESSAGE_ERRORS)
        .when(mockDao)
        .findAllMessageErrorsByClaimTypeAndStatus(
            MessageError.ClaimType.MCS, MessageError.Status.UNRESOLVED);

    TestUtils.setField(sourceSpy, "dao", mockDao);

    int actualProcessed = sourceSpy.retrieveAndProcessObjects(5, mockSink);

    assertEquals(MOCK_PROCESS_COUNT, actualProcessed);
  }

  /**
   * Verifies that {@link AbstractGrpcRdaSource#checkApiVersion(String)} is called, and that any
   * thrown exception is thrown up the stack.
   */
  @Test
  public void shouldThrowExceptionIfRdaVersionCheckThrows() throws Exception {
    final String claimType = "fiss";
    IllegalStateException expectedException = new IllegalStateException("Bad");

    DLQGrpcRdaSource<Long, Long> sourceSpy =
        spy(
            new DLQGrpcRdaSource<>(
                mockManager,
                Objects::equals,
                mockConfig,
                mockCaller,
                meters,
                claimType,
                rdaVersion));

    final CallOptions mockCallOptions = mock(CallOptions.class);

    // Set up config mocks, needs to return fake call options and channel
    doReturn(mockCallOptions).when(mockConfig).createCallOptions();

    // Always return fake version for caller's version call
    doReturn(TEST_RDA_VERSION).when(mockCaller).callVersionService(mockChannel, mockCallOptions);

    lenient().doNothing().when(sourceSpy).checkApiVersion(anyString());

    doThrow(expectedException).when(sourceSpy).checkApiVersion(TEST_RDA_VERSION);

    try {
      // We're testing the lambda logic in this test, so have to grab it first from the method that
      // returns it
      AbstractGrpcRdaSource.Processor logic =
          sourceSpy.dlqProcessingLogic(
              mockSink,
              MessageError.ClaimType.FISS,
              Set.of(FISS_ERROR_ONE_SEQ, FISS_ERROR_TWO_SEQ));

      AbstractGrpcRdaSource.ProcessResult actualResult = logic.process();

      fail("Expected exception not thrown");
    } catch (Exception actualException) {
      assertSame(expectedException, actualException, "Unexpected exception: " + actualException);
    }
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

    doReturn(0).when(mockSink).getProcessedCount();

    // Set up config mocks, needs to return fake call options and channel
    doReturn(mockCallOptions).when(mockConfig).createCallOptions();

    // Create our spy for the class we're testing, so we can mock sibling methods
    // Using Object::equals for sequence predicate, effectively making the "message"
    // be treated as the sequence number, for testing simplicity
    DLQGrpcRdaSource<Long, Long> sourceSpy =
        spy(
            new DLQGrpcRdaSource<>(
                mockManager,
                Objects::equals,
                mockConfig,
                mockCaller,
                meters,
                claimType,
                rdaVersion));

    doNothing().when(sourceSpy).setUptimeToReceiving();

    // unchecked - This is fine for a mock.
    //noinspection unchecked
    doReturn(1)
        .when(sourceSpy)
        .submitBatchToSink(eq(TEST_RDA_VERSION), eq(mockSink), any(Map.class));

    // Always return fake version for caller's version call
    doReturn(TEST_RDA_VERSION).when(mockCaller).callVersionService(mockChannel, mockCallOptions);

    // Create a stream that returns a sequence in the DLQ (5)
    // unchecked - This is fine for a mock.
    //noinspection unchecked
    GrpcResponseStream<Long> mockStreamA = mock(GrpcResponseStream.class);

    doReturn(true).doReturn(false).when(mockStreamA).hasNext();

    final long MOCK_STREAM_A_MESSAGE = 5L;

    doReturn(MOCK_STREAM_A_MESSAGE).doReturn(null).when(mockStreamA).next();

    doReturn("A").when(mockSink).getClaimIdForMessage(MOCK_STREAM_A_MESSAGE);

    doReturn(mockStreamA)
        .when(mockCaller)
        .callService(mockChannel, mockCallOptions, FISS_ERROR_ONE_SEQ - 1);

    // Create a stream that returns no sequence in the DLQ
    // unchecked - This is fine for a mock.
    //noinspection unchecked
    GrpcResponseStream<Long> mockStreamB = mock(GrpcResponseStream.class);

    doReturn(true).when(mockStreamB).hasNext();

    final long MOCK_STREAM_B_MESSAGE = 16L;

    doReturn(MOCK_STREAM_B_MESSAGE).when(mockStreamB).next();

    doReturn(mockStreamB)
        .when(mockCaller)
        .callService(mockChannel, mockCallOptions, FISS_ERROR_TWO_SEQ - 1);

    // Mock for deleting the only sequence that should have been found and processed
    doReturn(1L)
        .when(mockDao)
        .updateState(FISS_ERROR_ONE_SEQ, MessageError.ClaimType.FISS, MessageError.Status.RESOLVED);

    // Force our mock dao into the source object using reflection hackery
    TestUtils.setField(sourceSpy, "dao", mockDao);

    AbstractGrpcRdaSource.ProcessResult expectedResult = new AbstractGrpcRdaSource.ProcessResult();
    // Only a claim that was could be successfully written to the DB is considered processed, thus
    // the expected value is 1, since only one DLQ message could be reprocessed.
    expectedResult.setCount(1);

    // We're testing the lambda logic in this test, so have to grab it first from the method that
    // returns it
    AbstractGrpcRdaSource.Processor logic =
        sourceSpy.dlqProcessingLogic(
            mockSink, type, Set.of(FISS_ERROR_ONE_SEQ, FISS_ERROR_TWO_SEQ));
    AbstractGrpcRdaSource.ProcessResult actualResult = logic.process();

    // In the end, 1 sequence should have been deleted (5), and the other marked obsolete (15)
    assertEquals(expectedResult, actualResult);

    verify(mockDao, times(2))
        .updateState(anyLong(), any(MessageError.ClaimType.class), any(MessageError.Status.class));
    verify(mockDao)
        .updateState(FISS_ERROR_ONE_SEQ, MessageError.ClaimType.FISS, MessageError.Status.RESOLVED);
    verify(mockDao)
        .updateState(FISS_ERROR_TWO_SEQ, MessageError.ClaimType.FISS, MessageError.Status.OBSOLETE);

    verify(mockSink, times(1)).shutdown(any(Duration.class));
  }
}
