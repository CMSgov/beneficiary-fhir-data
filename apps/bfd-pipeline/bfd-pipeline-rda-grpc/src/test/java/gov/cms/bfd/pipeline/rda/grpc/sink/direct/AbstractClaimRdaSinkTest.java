package gov.cms.bfd.pipeline.rda.grpc.sink.direct;

import static gov.cms.bfd.pipeline.rda.grpc.RdaPipelineTestUtils.assertMeterReading;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
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
import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.model.rda.MessageError;
import gov.cms.bfd.model.rda.RdaApiProgress;
import gov.cms.bfd.model.rda.RdaClaimMessageMetaData;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.model.dsl.codegen.library.DataTransformer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Tests the {@link AbstractClaimRdaSink}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AbstractClaimRdaSinkTest {
  /** Test value for version. */
  private static final String VERSION = "version";

  /** The clock used for creating fixed timestamps. */
  private final Clock clock = Clock.fixed(Instant.ofEpochMilli(60_000L), ZoneOffset.UTC);

  /** The mock datasource. */
  @Mock private HikariDataSource dataSource;

  /** The mock entity manager factory. */
  @Mock private EntityManagerFactory entityManagerFactory;

  /** The mock entity manager. */
  @Mock private EntityManager entityManager;

  /** The mock entity transaction. */
  @Mock private EntityTransaction transaction;

  /** The sink under test. */
  private TestClaimRdaSink sink;

  /** Sets the test dependencies up before each test. */
  @BeforeEach
  public void setUp() {
    MeterRegistry meters = new SimpleMeterRegistry();
    MetricRegistry appMetrics = new MetricRegistry();
    doReturn(entityManager).when(entityManagerFactory).createEntityManager();
    doReturn(transaction).when(entityManager).getTransaction();
    doReturn(true).when(entityManager).isOpen();
    PipelineApplicationState appState =
        new PipelineApplicationState(meters, appMetrics, dataSource, entityManagerFactory, clock);
    sink = spy(new TestClaimRdaSink(appState, RdaApiProgress.ClaimType.FISS, true, 1));
    sink.getMetrics().setLatestSequenceNumber(0);
  }

  /**
   * Checks that claims are processed successfully when no exceptions were thrown by the {@link
   * AbstractClaimRdaSink#transformMessage(String, Object)} invocation, and that no {@link
   * AbstractClaimRdaSink#writeError(String, Object, DataTransformer.TransformationException)}
   * method was not invoked.
   */
  @Test
  void shouldTransformValidClaimsSuccessfully() throws IOException, ProcessingException {
    final List<String> messages = List.of("message1", "message2", "message3");

    for (String message : messages) {
      doReturn(Optional.of(createChangeClaimFromMessage(message)))
          .when(sink)
          .transformMessage(VERSION, message);
    }

    // Just to ensure default behavior isn't executed
    doNothing()
        .when(sink)
        .writeError(anyString(), anyString(), any(DataTransformer.TransformationException.class));

    List<RdaChange<String>> expected =
        messages.stream().map(this::createChangeClaimFromMessage).collect(Collectors.toList());

    AtomicReference<List<RdaChange<String>>> wrapper = new AtomicReference<>();

    assertDoesNotThrow(() -> wrapper.set(sink.transformMessages(VERSION, messages)));

    assertClaimChangesEquals(expected, wrapper.get());
    verify(sink, times(0))
        .writeError(anyString(), anyString(), any(DataTransformer.TransformationException.class));
  }

  /**
   * Checks that when {@link AbstractClaimRdaSink#transformMessage(String, Object)} throws an
   * exception, the {@link DataTransformer.TransformationException} is rethrown and the {@link
   * AbstractClaimRdaSink#writeError(String, Object, DataTransformer.TransformationException)}
   * method was correctly invoked.
   */
  @Test
  void shouldNotTransformInvalidClaimsSuccessfully() throws IOException, ProcessingException {
    final String badMessage = "message2";

    final List<String> messages = List.of("message1", badMessage, "message3");

    for (String message : messages) {
      if (message.equals(badMessage)) {
        doThrow(DataTransformer.TransformationException.class)
            .when(sink)
            .transformMessageImpl(VERSION, message);
      } else {
        // Just to ensure default behavior isn't executed
        lenient()
            .doReturn(createChangeClaimFromMessage(message))
            .when(sink)
            .transformMessageImpl(VERSION, message);
      }
    }

    doThrow(ProcessingException.class)
        .when(sink)
        .writeError(anyString(), anyString(), any(DataTransformer.TransformationException.class));

    assertThrows(ProcessingException.class, () -> sink.transformMessages(VERSION, messages));

    verify(sink, times(1))
        .writeError(
            eq(VERSION), eq(badMessage), any(DataTransformer.TransformationException.class));
  }

  /**
   * Tests that the {@link AbstractClaimRdaSink#writeError(String, Object,
   * DataTransformer.TransformationException)} method does NOT throw a {@link ProcessingException}
   * if the {@link MessageError} limit has NOT been exceeded.
   *
   * @throws IOException If there was a write error
   * @throws ProcessingException If the error count was exceeded
   */
  @Test
  void shouldCheckErrorCount() throws IOException, ProcessingException {
    final String API_VERSION = "1";
    final String MESSAGE = "message";
    final DataTransformer.TransformationException exception =
        new DataTransformer.TransformationException(
            "Exception Message", List.of(new DataTransformer.ErrorMessage("field 1", "bad field")));

    EntityTransaction mockTransaction = mock(EntityTransaction.class);
    MessageError mockMessageError = mock(MessageError.class);

    doNothing().when(mockTransaction).begin();

    doNothing().when(mockTransaction).commit();

    doReturn(mockTransaction).when(entityManager).getTransaction();

    doReturn(mockMessageError)
        .when(sink)
        .createMessageError(API_VERSION, MESSAGE, exception.getErrors());

    doNothing().when(sink).checkErrorCount();

    sink.writeError(API_VERSION, MESSAGE, exception);

    verify(mockTransaction, times(1)).begin();
    verify(entityManager, times(1)).merge(any(MessageError.class));
    verify(mockTransaction, times(1)).commit();
    verify(sink, times(1)).checkErrorCount();
  }

  /**
   * Tests that the {@link AbstractClaimRdaSink#writeError(String, Object,
   * DataTransformer.TransformationException)} method does NOT throw a {@link ProcessingException}
   * if the {@link MessageError} limit has NOT been exceeded.
   */
  @Test
  void shouldNotThrowProcessingExceptionIfMessageErrorLimitNotExceeded() {
    // unchecked - This is fine for a mock
    //noinspection unchecked
    TypedQuery<MessageError> mockQuery = mock(TypedQuery.class);

    doReturn(mockQuery).when(mockQuery).setParameter(anyString(), any());

    doReturn(1L).when(mockQuery).getSingleResult();

    doReturn(mockQuery)
        .when(entityManager)
        .createQuery(
            "select count(error) from MessageError error where status = :status and claimType = :claimType",
            Long.class);

    assertDoesNotThrow(() -> sink.checkErrorCount());
  }

  /**
   * Tests that the {@link AbstractClaimRdaSink#writeError(String, Object,
   * DataTransformer.TransformationException)} method DOES throw a {@link ProcessingException} if
   * the {@link MessageError} limit HAS been exceeded.
   */
  @Test
  void shouldThrowProcessingExceptionIfMessageErrorLimitExceeded() {
    // unchecked - This is fine for a mock
    //noinspection unchecked
    TypedQuery<MessageError> mockQuery = mock(TypedQuery.class);

    doReturn(mockQuery).when(mockQuery).setParameter(anyString(), any());

    doReturn(2L).when(mockQuery).getSingleResult();

    doReturn(mockQuery)
        .when(entityManager)
        .createQuery(
            "select count(error) from MessageError error where status = :status and claimType = :claimType",
            Long.class);

    assertThrows(ProcessingException.class, () -> sink.checkErrorCount());
  }

  /**
   * Verify that {@link AbstractClaimRdaSink#transformMessage(String, Object)} success updates
   * success metric.
   */
  @Test
  public void testSingleMessageTransformSuccessUpdatesMetric()
      throws IOException, ProcessingException {
    sink.transformMessage(VERSION, "message");

    final AbstractClaimRdaSink.Metrics metrics = sink.getMetrics();
    assertMeterReading(1, "transform successes", metrics.getTransformSuccesses());
    assertMeterReading(0, "transform failures", metrics.getTransformFailures());
  }

  /**
   * Verify that {@link AbstractClaimRdaSink#transformMessage(String, Object)} failure updates
   * failure metric.
   */
  @Test
  public void testSingleMessageTransformFailureUpdatesMetric()
      throws IOException, ProcessingException {
    final String MESSAGE = "message";
    DataTransformer.TransformationException transformException =
        new DataTransformer.TransformationException(
            "oops", List.of(new DataTransformer.ErrorMessage("field", "oops!")));

    doThrow(transformException).when(sink).transformMessageImpl(any(), any());

    doThrow(ProcessingException.class).when(sink).writeError(VERSION, MESSAGE, transformException);

    try {
      sink.transformMessage(VERSION, MESSAGE);
      fail("should have thrown");
    } catch (ProcessingException expectedException) {
      assertEquals(0, expectedException.getProcessedCount());
    } catch (Exception unexpectedException) {
      fail("unexpected exception thrown", unexpectedException);
    }

    final AbstractClaimRdaSink.Metrics metrics = sink.getMetrics();
    assertMeterReading(0, "transform successes", metrics.getTransformSuccesses());
    assertMeterReading(1, "transform failures", metrics.getTransformFailures());
  }

  /**
   * Verify that close method resets the latency metric histograms to zero.
   *
   * @throws Exception required because close can throw an exception
   */
  @Test
  public void testCloseResetsLatencyMetrics() throws Exception {
    sink.getMetrics().getChangeAgeMillis().record(100L);
    sink.getMetrics().getExtractAgeMillis().record(250L);
    assertEquals(1, sink.getMetrics().getChangeAgeMillis().count());
    assertEquals(1, sink.getMetrics().getExtractAgeMillis().count());
    assertEquals(100, sink.getMetrics().getChangeAgeMillis().mean());
    assertEquals(250, sink.getMetrics().getExtractAgeMillis().mean());
    sink.close();
    assertEquals(2, sink.getMetrics().getChangeAgeMillis().count());
    assertEquals(2, sink.getMetrics().getExtractAgeMillis().count());
    assertEquals(50, sink.getMetrics().getChangeAgeMillis().mean());
    assertEquals(125, sink.getMetrics().getExtractAgeMillis().mean());
  }

  /**
   * Helper method to create a {@link RdaChange} object with the given message.
   *
   * @param message The message to use to create the {@link RdaChange} object.
   * @return The created {@link RdaChange} object.
   */
  private RdaChange<String> createChangeClaimFromMessage(String message) {
    return new RdaChange<>(
        0,
        RdaChange.Type.UPDATE,
        message + "_claim",
        Instant.ofEpochMilli(1),
        new RdaChange.Source(
            (short) 1, (short) 0, LocalDate.of(1970, 1, 1), Instant.ofEpochSecond(0)));
  }

  /**
   * Helper method to check if the two {@link RdaChange} object lists are equivalent.
   *
   * @param expected The expected list of {@link RdaChange} values.
   * @param actual The actual list of {@link RdaChange} values.
   * @param <T> The type of the embedded data of the {@link RdaChange} objects.
   */
  private <T> void assertClaimChangesEquals(
      List<RdaChange<T>> expected, List<RdaChange<T>> actual) {
    assertEquals(expected.size(), actual.size());

    for (int i = 0; i < expected.size(); ++i) {
      assertClaimChangeEquals(expected.get(i), actual.get(i));
    }
  }

  /**
   * Helper method to check if the two {@link RdaChange} objects are equivalent.
   *
   * @param expected The expected {@link RdaChange} value.
   * @param actual The actual {@link RdaChange} value.
   * @param <T> The type of the embedded data of the {@link RdaChange} object.
   */
  private <T> void assertClaimChangeEquals(RdaChange<T> expected, RdaChange<T> actual) {
    assertEquals(expected.getSequenceNumber(), actual.getSequenceNumber());
    assertEquals(expected.getType(), actual.getType());
    assertEquals(expected.getClaim(), actual.getClaim());
    assertEquals(expected.getTimestamp(), actual.getTimestamp());
  }

  /** Simple implementation of {@link AbstractClaimRdaSink} for testing purposes. */
  static class TestClaimRdaSink extends AbstractClaimRdaSink<String, String> {
    /**
     * Instantiates a new Test claim rda sink.
     *
     * @param appState the app state
     * @param claimType the claim type
     * @param autoUpdateLastSeq if the sequence number should be updated automatically
     * @param errorLimit the error limit
     */
    protected TestClaimRdaSink(
        PipelineApplicationState appState,
        RdaApiProgress.ClaimType claimType,
        boolean autoUpdateLastSeq,
        int errorLimit) {
      super(appState, claimType, autoUpdateLastSeq, errorLimit);
    }

    /** {@inheritDoc} */
    @Override
    public String getClaimIdForMessage(String object) {
      return null;
    }

    /** {@inheritDoc} */
    @Override
    public long getSequenceNumberForObject(String object) {
      return 0;
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    RdaChange<String> transformMessageImpl(String apiVersion, String s) {
      // unchecked - This is not important for this test, it's never unwrapped
      //noinspection unchecked
      return mock(RdaChange.class);
    }

    /** {@inheritDoc} */
    @Override
    RdaClaimMessageMetaData createMetaData(RdaChange<String> change) {
      return null;
    }

    /** {@inheritDoc} */
    @Override
    int getInsertCount(String s) {
      return 1;
    }

    /** {@inheritDoc} */
    @Override
    MessageError createMessageError(
        String apiVersion, String change, List<DataTransformer.ErrorMessage> errors) {
      return null;
    }
  }
}
