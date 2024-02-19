package gov.cms.bfd.pipeline.rda.grpc;

import static gov.cms.bfd.pipeline.rda.grpc.AbstractRdaLoadJob.Config;
import static gov.cms.bfd.pipeline.rda.grpc.RdaPipelineTestUtils.assertMeterReading;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import gov.cms.bfd.sharedutils.interfaces.ThrowingFunction;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

/** Tests the {@link AbstractRdaLoadJob} class. */
public class AbstractRdaLoadJobTest {

  /** Mock {@link Callable} task used in testing for the preJob logic. */
  @Mock private Callable<RdaSource<Integer, Integer>> preJobTask;

  /** Mock {@link Callable} task used in testing for the RDA source logic. */
  @Mock private Callable<RdaSource<Integer, Integer>> sourceFactory;

  /** Mock factory function used in testing for creating {@link RdaSink} objects. */
  @Mock
  private ThrowingFunction<
          RdaSink<Integer, Integer>, AbstractRdaLoadJob.SinkTypePreference, Exception>
      sinkFactory;

  /** Mock {@link RdaSource} to use in testing. */
  @Mock private RdaSource<Integer, Integer> source;

  /** Mock {@link RdaSink} to use in testing. */
  @Mock private RdaSink<Integer, Integer> sink;

  /** The {@link TestingLoadJob} used in the testing. */
  private TestingLoadJob job;

  /** Mock {@link AbstractCleanupJob} to use in testing. */
  @Mock private AbstractCleanupJob cleanupJob;

  /** The {@link MeterRegistry} used in the testing. */
  private MeterRegistry appMetrics;

  /** The {@link Config} used in the testing. */
  private Config config;

  /**
   * The {@link AutoCloseable} to store the object returned by {@link
   * MockitoAnnotations#openMocks(Object)} so it can be closed at the end of testing.
   */
  private AutoCloseable mocksClosable;

  /** Set up the mocks before each test. */
  @BeforeEach
  public void setUp() {
    mocksClosable = MockitoAnnotations.openMocks(this);
    config =
        AbstractRdaLoadJob.Config.builder()
            .runInterval(Duration.ofSeconds(10))
            .batchSize(3)
            .sinkTypePreference(AbstractRdaLoadJob.SinkTypePreference.NONE)
            .build();
    appMetrics = new SimpleMeterRegistry();
    job =
        new TestingLoadJob(config, preJobTask, sourceFactory, sinkFactory, cleanupJob, appMetrics);
  }

  /**
   * Close the mocks that were created after each test.
   *
   * @throws Exception If there was an issue closing a mock
   */
  @AfterEach
  public void tearDown() throws Exception {
    mocksClosable.close();
  }

  /** Tests that the {@link #appMetrics} meter names are the expected values. */
  @Test
  public void meterNames() {
    assertEquals(
        Arrays.asList(
            "TestingLoadJob.calls",
            "TestingLoadJob.failures",
            "TestingLoadJob.processed",
            "TestingLoadJob.successes"),
        appMetrics.getMeters().stream()
            .map(meter -> meter.getId().getName())
            .sorted()
            .collect(Collectors.toList()));
  }

  /**
   * Tests that the meters are appropriately updated when the {@link #sourceFactory} fails to create
   * a {@link RdaSource} object.
   *
   * @throws Exception If an error occurred during logic execution
   */
  @Test
  public void openSourceFails() throws Exception {
    // resource - This is a mock, not an invocation
    //noinspection resource
    doThrow(new IOException("oops")).when(sourceFactory).call();
    try {
      job.callRdaServiceAndStoreRecords();
      fail("job should have thrown exception");
    } catch (Exception ex) {
      assertEquals("oops", ex.getCause().getMessage());
      MatcherAssert.assertThat(ex.getCause(), Matchers.instanceOf(IOException.class));
    }
    verifyNoInteractions(sinkFactory);
    assertMeterReading(1, "calls", job.getMetrics().getCalls());
    assertMeterReading(0, "successes", job.getMetrics().getSuccesses());
    assertMeterReading(1, "failures", job.getMetrics().getFailures());
    assertMeterReading(0, "processed", job.getMetrics().getProcessed());
  }

  /**
   * Tests that the meters are appropriately updated when the {@link #sinkFactory} fails to create
   * an {@link RdaSink} object.
   *
   * @throws Exception If a resource fails to close or some other issue occurred.
   */
  @Test
  public void openSinkFails() throws Exception {
    // resource - This is a mock, not an invocation
    //noinspection resource
    doReturn(source).when(sourceFactory).call();
    // resource - This is a mock, not an invocation
    //noinspection resource
    doThrow(new IOException("oops"))
        .when(sinkFactory)
        .apply(any(AbstractRdaLoadJob.SinkTypePreference.class));
    try {
      job.callRdaServiceAndStoreRecords();
      fail("job should have thrown exception");
    } catch (Exception ex) {
      assertEquals("oops", ex.getCause().getMessage());
      MatcherAssert.assertThat(ex.getCause(), Matchers.instanceOf(IOException.class));
    }
    verify(source).close();
    assertMeterReading(1, "calls", job.getMetrics().getCalls());
    assertMeterReading(0, "successes", job.getMetrics().getSuccesses());
    assertMeterReading(1, "failures", job.getMetrics().getFailures());
    assertMeterReading(0, "processed", job.getMetrics().getProcessed());
  }

  /**
   * Tests that the process count and meters are correctly set when {@link
   * RdaSource#retrieveAndProcessObjects(int, RdaSink)} fails to invoke on the {@link #source}.
   *
   * @throws Exception If the resource fails to close or some other issue occurred.
   */
  @Test
  public void sourceFails() throws Exception {
    // resource - This is a mock, not an invocation
    //noinspection resource
    doReturn(source).when(sourceFactory).call();
    // resource - This is a mock, not an invocation
    //noinspection resource
    doReturn(sink).when(sinkFactory).apply(AbstractRdaLoadJob.SinkTypePreference.NONE);
    doThrow(new ProcessingException(new IOException("oops"), 7))
        .when(source)
        .retrieveAndProcessObjects(anyInt(), same(sink));
    try {
      job.callRdaServiceAndStoreRecords();
      fail("job should have thrown exception");
    } catch (Exception ex) {
      assertNotNull(ex.getCause());
      MatcherAssert.assertThat(ex.getCause(), Matchers.instanceOf(ProcessingException.class));
      assertEquals(7, ((ProcessingException) ex.getCause()).getProcessedCount());
      final Throwable actualCause = ex.getCause().getCause();
      MatcherAssert.assertThat(actualCause, Matchers.instanceOf(IOException.class));
      assertEquals("oops", actualCause.getMessage());
    }
    verify(source).close();
    verify(sink).close();
    assertMeterReading(1, "calls", job.getMetrics().getCalls());
    assertMeterReading(0, "successes", job.getMetrics().getSuccesses());
    assertMeterReading(1, "failures", job.getMetrics().getFailures());
    assertMeterReading(7, "processed", job.getMetrics().getProcessed());
  }

  /**
   * Checks that a job properly updates the metrics, as well as returning {@link
   * PipelineJobOutcome#NOTHING_TO_DO} when it has executed successfully to completion and no work
   * was available to be done.
   *
   * @throws Exception If a resource fails to close or some other issue has occurred.
   */
  @Test
  public void nothingToDo() throws Exception {
    // resource - This is a mock, not an invocation
    //noinspection resource
    doReturn(mock(RdaSource.class)).when(preJobTask).call();
    // resource - This is a mock, not an invocation
    //noinspection resource
    doReturn(source).when(sourceFactory).call();
    // resource - This is a mock, not an invocation
    //noinspection resource
    doReturn(sink).when(sinkFactory).apply(AbstractRdaLoadJob.SinkTypePreference.NONE);
    doReturn(0).when(source).retrieveAndProcessObjects(anyInt(), same(sink));
    try {
      PipelineJobOutcome outcome = job.call();
      assertEquals(PipelineJobOutcome.NOTHING_TO_DO, outcome);
    } catch (Exception ex) {
      fail("job should NOT have thrown exception");
    }
    verify(source).close();
    verify(sink, times(1)).close();
    assertMeterReading(1, "calls", job.getMetrics().getCalls());
    assertMeterReading(1, "successes", job.getMetrics().getSuccesses());
    assertMeterReading(0, "failures", job.getMetrics().getFailures());
    assertMeterReading(0, "processed", job.getMetrics().getProcessed());
  }

  /**
   * Checks that a job properly updates the metrics, as well as returning {@link
   * PipelineJobOutcome#NOTHING_TO_DO} when it has executed successfully to completion and work was
   * available to process.
   *
   * @throws Exception If a resource fails to close or some other issue has occurred.
   */
  @Test
  public void workDone() throws Exception {
    // resource - This is a mock, not an invocation
    //noinspection resource
    doReturn(mock(RdaSource.class)).when(preJobTask).call();
    // resource - This is a mock, not an invocation
    //noinspection resource
    doReturn(source).when(sourceFactory).call();
    // resource - This is a mock, not an invocation
    //noinspection resource
    doReturn(sink).when(sinkFactory).apply(AbstractRdaLoadJob.SinkTypePreference.NONE);
    doReturn(25_000).when(source).retrieveAndProcessObjects(anyInt(), same(sink));
    try {
      PipelineJobOutcome outcome = job.call();
      assertEquals(PipelineJobOutcome.WORK_DONE, outcome);
    } catch (Exception ex) {
      fail("job should NOT have thrown exception");
    }
    verify(source).close();
    verify(sink, times(1)).close();
    assertMeterReading(1, "calls", job.getMetrics().getCalls());
    assertMeterReading(1, "successes", job.getMetrics().getSuccesses());
    assertMeterReading(0, "failures", job.getMetrics().getFailures());
    assertMeterReading(25_000, "processed", job.getMetrics().getProcessed());
  }

  /**
   * Tests that if multiple jobs try to execute at the same time, only one will perform the work.
   * The second job will complete immediately and return {@link PipelineJobOutcome#NOTHING_TO_DO}.
   *
   * @throws Exception If there was an issue processing the work.
   */
  @Test
  public void enforcesOneCallAtATime() throws Exception {
    // let the source indicate that it did some work to set the first call apart from the second one
    doReturn(100).when(source).retrieveAndProcessObjects(anyInt(), same(sink));

    // Used to allow the second call to happen after the first call has acquired its semaphore
    final CountDownLatch waitForStartup = new CountDownLatch(1);

    // Used to allow the first call to wait until the second call has completed before it proceeds.
    final CountDownLatch waitForCompletion = new CountDownLatch(1);

    // A test job that waits for the second job to complete before doing any work itself.
    //noinspection unchecked
    job =
        new TestingLoadJob(
            config,
            () -> mock(RdaSource.class),
            () -> {
              // lets the main thread know we've acquired the semaphore
              waitForStartup.countDown();
              // waits until the second call is done before returning a source
              waitForCompletion.await();
              return source;
            },
            (preference) -> sink,
            cleanupJob,
            appMetrics);
    final ExecutorService pool = Executors.newCachedThreadPool();
    try {
      // this call will grab the semaphore and hold it until we count down the waitForCompletion
      Future<PipelineJobOutcome> firstCall = pool.submit(() -> job.call());

      // wait for the first call to have grabbed the semaphore before we make the second call
      waitForStartup.await();

      // this job should exit immediately without doing any work
      Future<PipelineJobOutcome> secondCall = pool.submit(() -> job.call());
      assertEquals(PipelineJobOutcome.NOTHING_TO_DO, secondCall.get());

      // now allow the first call to proceed, and it should reflect that it has done some work
      waitForCompletion.countDown();
      assertEquals(PipelineJobOutcome.WORK_DONE, firstCall.get());
    } finally {
      pool.shutdown();
      // ResultOfMethodCallIgnored - We don't care if it was graceful
      //noinspection ResultOfMethodCallIgnored
      pool.awaitTermination(5, TimeUnit.SECONDS);
    }

    assertMeterReading(1, "calls", job.getMetrics().getCalls());
    assertMeterReading(1, "successes", job.getMetrics().getSuccesses());
    assertMeterReading(0, "failures", job.getMetrics().getFailures());
    assertMeterReading(100, "processed", job.getMetrics().getProcessed());
  }

  /** Test class used to perform the associated {@link AbstractRdaLoadJob} testing. */
  private static class TestingLoadJob extends AbstractRdaLoadJob<Integer, Integer> {
    /**
     * Instantiates a new Testing load job.
     *
     * @param config the config
     * @param preJobTask the pre job task
     * @param sourceFactory the source factory
     * @param sinkFactory the sink factory
     * @param cleanupJob the cleanup job
     * @param appMetrics the app metrics
     */
    public TestingLoadJob(
        Config config,
        Callable<RdaSource<Integer, Integer>> preJobTask,
        Callable<RdaSource<Integer, Integer>> sourceFactory,
        ThrowingFunction<RdaSink<Integer, Integer>, SinkTypePreference, Exception> sinkFactory,
        CleanupJob cleanupJob,
        MeterRegistry appMetrics) {
      super(
          config,
          preJobTask,
          sourceFactory,
          sinkFactory,
          cleanupJob,
          appMetrics,
          LoggerFactory.getLogger(TestingLoadJob.class));
    }
  }
}
