package gov.cms.bfd.pipeline.rda.grpc.sink.direct;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rda.MessageError;
import gov.cms.bfd.model.rda.RdaApiClaimMessageMetaData;
import gov.cms.bfd.model.rda.RdaApiProgress;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.source.DataTransformer;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;

public class AbstractClaimRdaSinkTest {

  /**
   * Checks that claims are processed successfully when no exceptions were thrown by the {@link
   * AbstractClaimRdaSink#transformMessage(String, Object)} invocation, and that no {@link
   * AbstractClaimRdaSink#writeError(String, Object, DataTransformer.TransformationException)}
   * method was not invoked.
   */
  @Test
  void shouldTransformValidClaimsSuccessfully() throws IOException {
    MetricRegistry mockMetricRegistry = mock(MetricRegistry.class);
    Meter mockMeter = mock(Meter.class);
    EntityManagerFactory mockEntityManagerFactory = mock(EntityManagerFactory.class);
    EntityManager mockEntityManager = mock(EntityManager.class);
    Clock mockClock = mock(Clock.class);
    PipelineApplicationState mockPipelineApplicationState = mock(PipelineApplicationState.class);

    doReturn(mockEntityManager).when(mockEntityManagerFactory).createEntityManager();

    doReturn(mockEntityManagerFactory).when(mockPipelineApplicationState).getEntityManagerFactory();

    doReturn(mockMeter).when(mockMetricRegistry).meter(anyString());

    doReturn(mockMetricRegistry).when(mockPipelineApplicationState).getMetrics();

    doReturn(mockClock).when(mockPipelineApplicationState).getClock();

    AbstractClaimRdaSink<String, String> sinkSpy =
        spy(
            new TestClaimRdaSink(
                mockPipelineApplicationState, RdaApiProgress.ClaimType.FISS, true));

    final String apiVersion = "version";
    final List<String> messages = List.of("message1", "message2", "message3");

    for (String message : messages) {
      doReturn(createChangeClaimFromMessage(message))
          .when(sinkSpy)
          .transformMessage(apiVersion, message);
    }

    doNothing()
        .when(sinkSpy)
        .writeError(anyString(), anyString(), any(DataTransformer.TransformationException.class));

    List<RdaChange<String>> expected =
        messages.stream().map(this::createChangeClaimFromMessage).collect(Collectors.toList());

    Wrapper<List<RdaChange<String>>> wrapper = new Wrapper<>();

    assertDoesNotThrow(() -> wrapper.wrap(sinkSpy.transformMessages(apiVersion, messages)));

    assertClaimChangesEquals(expected, wrapper.unwrap());
    verify(sinkSpy, times(0))
        .writeError(anyString(), anyString(), any(DataTransformer.TransformationException.class));
  }

  /**
   * Checks that when {@link AbstractClaimRdaSink#transformMessage(String, Object)} throws an
   * exception, the {@link
   * gov.cms.bfd.pipeline.rda.grpc.source.DataTransformer.TransformationException} is rethrown and
   * the {@link AbstractClaimRdaSink#writeError(String, Object,
   * DataTransformer.TransformationException)} method was correctly invoked.
   */
  @Test
  void shouldNotTransformInvalidClaimsSuccessfully() throws IOException {
    final String badMessage = "message2";

    MetricRegistry mockMetricRegistry = mock(MetricRegistry.class);
    Meter mockMeter = mock(Meter.class);
    EntityManagerFactory mockEntityManagerFactory = mock(EntityManagerFactory.class);
    EntityManager mockEntityManager = mock(EntityManager.class);
    Clock mockClock = mock(Clock.class);
    PipelineApplicationState mockPipelineApplicationState = mock(PipelineApplicationState.class);

    doReturn(mockEntityManager).when(mockEntityManagerFactory).createEntityManager();

    doReturn(mockEntityManagerFactory).when(mockPipelineApplicationState).getEntityManagerFactory();

    doReturn(mockMeter).when(mockMetricRegistry).meter(anyString());

    doReturn(mockMetricRegistry).when(mockPipelineApplicationState).getMetrics();

    doReturn(mockClock).when(mockPipelineApplicationState).getClock();

    AbstractClaimRdaSink<String, String> sinkSpy =
        spy(
            new TestClaimRdaSink(
                mockPipelineApplicationState, RdaApiProgress.ClaimType.FISS, true));

    final String apiVersion = "version";
    final List<String> messages = List.of("message1", badMessage, "message3");

    for (String message : messages) {
      if (message.equals(badMessage)) {
        doThrow(DataTransformer.TransformationException.class)
            .when(sinkSpy)
            .transformMessage(apiVersion, message);
      } else {
        doReturn(createChangeClaimFromMessage(message))
            .when(sinkSpy)
            .transformMessage(apiVersion, message);
      }
    }

    doNothing()
        .when(sinkSpy)
        .writeError(anyString(), anyString(), any(DataTransformer.TransformationException.class));

    List<RdaChange<String>> expected =
        messages.stream().map(this::createChangeClaimFromMessage).collect(Collectors.toList());

    Wrapper<List<RdaChange<String>>> wrapper = new Wrapper<>();

    assertThrows(
        DataTransformer.TransformationException.class,
        () -> wrapper.wrap(sinkSpy.transformMessages(apiVersion, messages)));

    verify(sinkSpy, times(1))
        .writeError(
            eq(apiVersion), eq(badMessage), any(DataTransformer.TransformationException.class));
  }

  /**
   * Helper method to create a {@link RdaChange} object with the given message.
   *
   * @param message The message to use to create the {@link RdaChange} object.
   * @return The created {@link RdaChange} object.
   */
  private RdaChange<String> createChangeClaimFromMessage(String message) {
    return new RdaChange<>(0, RdaChange.Type.UPDATE, message + "_claim", Instant.ofEpochMilli(1));
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

  /**
   * Helper class for retrieving values from inside lambda expressions.
   *
   * @param <T> The type of data being wrapped.
   */
  static class Wrapper<T> {

    private T wrapped;

    void wrap(T wrapped) {
      this.wrapped = wrapped;
    }

    T unwrap() {
      return wrapped;
    }
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
    public String getDedupKeyForMessage(String object) {
      return null;
    }

    @Override
    public long getSequenceNumberForObject(String object) {
      return 0;
    }

    @Nonnull
    @Override
    public RdaChange<String> transformMessage(String apiVersion, String s) {
      return null;
    }

    @Override
    RdaApiClaimMessageMetaData createMetaData(RdaChange<String> change) {
      return null;
    }

    @Override
    MessageError createMessageError(
        String apiVersion, String change, List<DataTransformer.ErrorMessage> errors)
        throws IOException {
      return null;
    }
  }
}
