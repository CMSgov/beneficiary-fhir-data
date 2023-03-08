package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/** Tests functionality of the {@link AbstractGrpcRdaSource} class. */
class AbstractGrpcRdaSourceTest {

  /**
   * Tests that an exception is thrown when {@link AbstractGrpcRdaSource#checkApiVersion(String)} is
   * called on an incompatible RDA version.
   *
   * @throws Exception If something went wrong in the test
   */
  @Test
  void shouldThrowExceptionIfRdaVersionDoesNotMatch() throws Exception {
    final String TEST_RDA_VERSION = "0.0.1";

    ManagedChannel mockChannel = mock(ManagedChannel.class);
    // unchecked - This is fine for a mock.
    //noinspection unchecked
    GrpcStreamCaller<String> mockCaller = mock(GrpcStreamCaller.class);
    String claimType = "fiss";
    Supplier<CallOptions> callOptionsSupplier = () -> mock(CallOptions.class);
    MeterRegistry mockRegistery = mock(MeterRegistry.class);
    RdaVersion mockVersion = mock(RdaVersion.class);

    doReturn(false).when(mockVersion).allows(TEST_RDA_VERSION);

    try (TestGrpcRdaSource source =
        new TestGrpcRdaSource(
            mockChannel, mockCaller, claimType, callOptionsSupplier, mockRegistery, mockVersion)) {
      assertThrows(IllegalStateException.class, () -> source.checkApiVersion(TEST_RDA_VERSION));
    }
  }

  /** Test class used for testing {@link AbstractGrpcRdaSource}. */
  private static class TestGrpcRdaSource extends AbstractGrpcRdaSource<String, String> {

    /**
     * Instantiates a new test grpc rda source.
     *
     * @param channel the channel
     * @param caller the caller
     * @param claimType the claim type
     * @param callOptionsFactory the call options factory
     * @param appMetrics the app metrics
     * @param rdaVersion The required {@link RdaVersion} in order to ingest data
     */
    protected TestGrpcRdaSource(
        ManagedChannel channel,
        GrpcStreamCaller<String> caller,
        String claimType,
        Supplier<CallOptions> callOptionsFactory,
        MeterRegistry appMetrics,
        RdaVersion rdaVersion) {
      super(channel, caller, claimType, callOptionsFactory, appMetrics, rdaVersion);
    }

    /** {@inheritDoc} */
    @Override
    public int retrieveAndProcessObjects(int maxPerBatch, RdaSink<String, String> sink)
        throws ProcessingException {
      return 0;
    }
  }
}
