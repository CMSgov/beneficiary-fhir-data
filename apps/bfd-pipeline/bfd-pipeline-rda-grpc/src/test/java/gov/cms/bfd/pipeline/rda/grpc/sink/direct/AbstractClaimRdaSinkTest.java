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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.model.rda.MessageError;
import gov.cms.bfd.model.rda.RdaApiProgress;
import gov.cms.bfd.model.rda.RdaClaimMessageMetaData;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.model.dsl.codegen.library.DataTransformer;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.distribution.CountAtBucket;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AbstractClaimRdaSinkTest {
  private static final String VERSION = "version";

  private final Clock clock = Clock.fixed(Instant.ofEpochMilli(60_000L), ZoneOffset.UTC);

  @Mock private HikariDataSource dataSource;
  @Mock private EntityManagerFactory entityManagerFactory;
  @Mock private EntityManager entityManager;
  @Mock private EntityTransaction transaction;
  private TestClaimRdaSink sink;

  @BeforeEach
  public void setUp() {
    MeterRegistry meters = new SimpleMeterRegistry();
    MetricRegistry appMetrics = new MetricRegistry();
    doReturn(entityManager).when(entityManagerFactory).createEntityManager();
    doReturn(transaction).when(entityManager).getTransaction();
    doReturn(true).when(entityManager).isOpen();
    PipelineApplicationState appState =
        new PipelineApplicationState(meters, appMetrics, dataSource, entityManagerFactory, clock);
    sink = spy(new TestClaimRdaSink(appState, RdaApiProgress.ClaimType.FISS, true));
    sink.getMetrics().setLatestSequenceNumber(0);
  }

  /**
   * Checks that claims are processed successfully when no exceptions were thrown by the {@link
   * AbstractClaimRdaSink#transformMessage(String, Object)} invocation, and that no {@link
   * AbstractClaimRdaSink#writeError(String, Object, DataTransformer.TransformationException)}
   * method was not invoked.
   */
  @Test
  void shouldTransformValidClaimsSuccessfully() throws IOException {
    final List<String> messages = List.of("message1", "message2", "message3");

    for (String message : messages) {
      doReturn(createChangeClaimFromMessage(message)).when(sink).transformMessage(VERSION, message);
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
  void shouldNotTransformInvalidClaimsSuccessfully() throws IOException {
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

    doNothing()
        .when(sink)
        .writeError(anyString(), anyString(), any(DataTransformer.TransformationException.class));

    assertThrows(
        DataTransformer.TransformationException.class,
        () -> sink.transformMessages(VERSION, messages));

    verify(sink, times(1))
        .writeError(
            eq(VERSION), eq(badMessage), any(DataTransformer.TransformationException.class));
  }

  /**
   * Verify that {@link AbstractClaimRdaSink#transformMessage(String, Object)} success updates
   * success metric.
   */
  @Test
  public void testSingleMessageTransformSuccessUpdatesMetric() {
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
  public void testSingleMessageTransformFailureUpdatesMetric() {
    doThrow(
            new DataTransformer.TransformationException(
                "oops", List.of(new DataTransformer.ErrorMessage("field", "oops!"))))
        .when(sink)
        .transformMessageImpl(any(), any());

    try {
      sink.transformMessage(VERSION, "message");
      fail("should have thrown");
    } catch (DataTransformer.TransformationException error) {
      assertEquals("oops", error.getMessage());
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
    //    assertEquals(List.of(100L), getHistogramValues(sink.getMetrics().getChangeAgeMillis()));
    //    assertEquals(List.of(250L), getHistogramValues(sink.getMetrics().getExtractAgeMillis()));
    sink.close();
    assertEquals(2, sink.getMetrics().getChangeAgeMillis().count());
    assertEquals(2, sink.getMetrics().getExtractAgeMillis().count());
    assertEquals(50, sink.getMetrics().getChangeAgeMillis().mean());
    assertEquals(125, sink.getMetrics().getExtractAgeMillis().mean());
    //    assertEquals(List.of(0L, 100L),
    // getHistogramValues(sink.getMetrics().getChangeAgeMillis()));
    //    assertEquals(List.of(0L, 250L),
    // getHistogramValues(sink.getMetrics().getExtractAgeMillis()));
  }

  /**
   * Extracts the array of long values from the {@link Histogram} and converts them into a list of
   * {link Long}s.
   *
   * @param histogram {@link Histogram} containing the values we want to extract
   * @return a {@link List} or {@link Long} containing all of the histogram values
   */
  private List<Long> getHistogramValues(DistributionSummary histogram) {
    return Arrays.stream(histogram.takeSnapshot().histogramCounts())
        .map(CountAtBucket::count)
        .map(Double::longValue)
        .collect(Collectors.toList());
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
    protected TestClaimRdaSink(
        PipelineApplicationState appState,
        RdaApiProgress.ClaimType claimType,
        boolean autoUpdateLastSeq) {
      super(appState, claimType, autoUpdateLastSeq);
    }

    @Override
    public String getClaimIdForMessage(String object) {
      return null;
    }

    @Override
    public long getSequenceNumberForObject(String object) {
      return 0;
    }

    @Nonnull
    @Override
    RdaChange<String> transformMessageImpl(String apiVersion, String s) {
      // ConstantConditions - This method is never actually executed in testing
      //noinspection ConstantConditions
      return null;
    }

    @Override
    RdaClaimMessageMetaData createMetaData(RdaChange<String> change) {
      return null;
    }

    @Override
    int getInsertCount(String s) {
      return 1;
    }

    @Override
    MessageError createMessageError(
        String apiVersion, String change, List<DataTransformer.ErrorMessage> errors)
        throws IOException {
      return null;
    }
  }
}
