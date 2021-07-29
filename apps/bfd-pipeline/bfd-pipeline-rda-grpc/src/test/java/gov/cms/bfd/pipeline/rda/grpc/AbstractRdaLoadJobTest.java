package gov.cms.bfd.pipeline.rda.grpc;

import static gov.cms.bfd.pipeline.rda.grpc.AbstractRdaLoadJob.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class AbstractRdaLoadJobTest {
  @Mock private Callable<RdaSource<Integer>> sourceFactory;
  @Mock private Callable<RdaSink<Integer>> sinkFactory;
  @Mock private RdaSource<Integer> source;
  @Mock private RdaSink<Integer> sink;
  private TestingLoadJob job;
  private MetricRegistry appMetrics;
  private Config config;

  @Before
  public void setUp() {
    config = new Config(Duration.ofSeconds(10), 3);
    appMetrics = new MetricRegistry();
    job = new TestingLoadJob(config, sourceFactory, sinkFactory, appMetrics);
  }

  @Test
  public void meterNames() {
    assertEquals("TestingLoadJob.calls", job.metricName(CALLS_METER_NAME));
    assertEquals("TestingLoadJob.failures", job.metricName(FAILURES_METER_NAME));
  }

  @Test
  public void openSourceFails() throws Exception {
    doThrow(new IOException("oops")).when(sourceFactory).call();
    try {
      job.callRdaServiceAndStoreRecords();
      Assert.fail("job should have thrown exception");
    } catch (Exception ex) {
      assertEquals("oops", ex.getCause().getMessage());
      MatcherAssert.assertThat(ex.getCause(), Matchers.instanceOf(IOException.class));
    }
    verifyNoInteractions(sinkFactory);
    assertEquals(1, appMetrics.meter(job.metricName(CALLS_METER_NAME)).getCount());
    assertEquals(1, appMetrics.meter(job.metricName(FAILURES_METER_NAME)).getCount());
  }

  @Test
  public void openSinkFails() throws Exception {
    doReturn(source).when(sourceFactory).call();
    doThrow(new IOException("oops")).when(sinkFactory).call();
    try {
      job.callRdaServiceAndStoreRecords();
      Assert.fail("job should have thrown exception");
    } catch (Exception ex) {
      assertEquals("oops", ex.getCause().getMessage());
      MatcherAssert.assertThat(ex.getCause(), Matchers.instanceOf(IOException.class));
    }
    verify(source).close();
    assertEquals(1, appMetrics.meter(job.metricName(CALLS_METER_NAME)).getCount());
    assertEquals(1, appMetrics.meter(job.metricName(FAILURES_METER_NAME)).getCount());
  }

  @Test
  public void sourceFails() throws Exception {
    doReturn(source).when(sourceFactory).call();
    doReturn(sink).when(sinkFactory).call();
    doThrow(new ProcessingException(new IOException("oops"), 7))
        .when(source)
        .retrieveAndProcessObjects(anyInt(), same(sink));
    try {
      job.callRdaServiceAndStoreRecords();
      Assert.fail("job should have thrown exception");
    } catch (Exception ex) {
      Assert.assertNotNull(ex.getCause());
      MatcherAssert.assertThat(ex.getCause(), Matchers.instanceOf(ProcessingException.class));
      assertEquals(7, ((ProcessingException) ex.getCause()).getProcessedCount());
      final Throwable actualCause = ex.getCause().getCause();
      MatcherAssert.assertThat(actualCause, Matchers.instanceOf(IOException.class));
      assertEquals("oops", actualCause.getMessage());
    }
    verify(source).close();
    verify(sink).close();
    assertEquals(1, appMetrics.meter(job.metricName(CALLS_METER_NAME)).getCount());
    assertEquals(1, appMetrics.meter(job.metricName(FAILURES_METER_NAME)).getCount());
  }

  @Test
  public void nothingToDo() throws Exception {
    doReturn(source).when(sourceFactory).call();
    doReturn(sink).when(sinkFactory).call();
    doReturn(0).when(source).retrieveAndProcessObjects(anyInt(), same(sink));
    try {
      PipelineJobOutcome outcome = job.call();
      assertEquals(PipelineJobOutcome.NOTHING_TO_DO, outcome);
    } catch (Exception ex) {
      Assert.fail("job should NOT have thrown exception");
    }
    verify(source).close();
    verify(sink).close();
    assertEquals(1, appMetrics.meter(job.metricName(CALLS_METER_NAME)).getCount());
    assertEquals(1, appMetrics.meter(job.metricName(SUCCESSES_METER_NAME)).getCount());
  }

  @Test
  public void workDone() throws Exception {
    doReturn(source).when(sourceFactory).call();
    doReturn(sink).when(sinkFactory).call();
    doReturn(25000).when(source).retrieveAndProcessObjects(anyInt(), same(sink));
    try {
      PipelineJobOutcome outcome = job.call();
      assertEquals(PipelineJobOutcome.WORK_DONE, outcome);
    } catch (Exception ex) {
      Assert.fail("job should NOT have thrown exception");
    }
    verify(source).close();
    verify(sink).close();
    assertEquals(1, appMetrics.meter(job.metricName(CALLS_METER_NAME)).getCount());
    assertEquals(1, appMetrics.meter(job.metricName(SUCCESSES_METER_NAME)).getCount());
  }

  @Test
  public void enforcesOneCallAtATime() throws Exception {
    // let the source indicate that it did some work to set the first call apart from the second one
    doReturn(100).when(source).retrieveAndProcessObjects(anyInt(), same(sink));

    // Used to allow the second call to happen after the first call has acquired its semaphore
    final CountDownLatch waitForStartup = new CountDownLatch(1);

    // Used to allow the first call to wait until the second call has completed before it proceeds.
    final CountDownLatch waitForCompletion = new CountDownLatch(1);

    // A test job that waits for the second job to complete before doing any work itself.
    job =
        new TestingLoadJob(
            config,
            () -> {
              // lets the main thread know we've acquired the semaphore
              waitForStartup.countDown();
              // waits until the second call is done before returning a source
              waitForCompletion.await();
              return source;
            },
            () -> sink,
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

      // now allow the first call to proceed and it should reflect that it has done some work
      waitForCompletion.countDown();
      assertEquals(PipelineJobOutcome.WORK_DONE, firstCall.get());
    } finally {
      pool.shutdown();
      pool.awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Test
  public void configIsSerializable() throws Exception {
    final AbstractRdaLoadJob.Config original = new Config(Duration.ofMillis(1000), 45);
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
      out.writeObject(original);
    }
    AbstractRdaLoadJob.Config loaded;
    try (ObjectInputStream inp =
        new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
      loaded = (Config) inp.readObject();
    }
    assertEquals(original, loaded);
  }

  private static class TestingLoadJob extends AbstractRdaLoadJob<Integer> {
    public TestingLoadJob(
        Config config,
        Callable<RdaSource<Integer>> sourceFactory,
        Callable<RdaSink<Integer>> sinkFactory,
        MetricRegistry appMetrics) {
      super(
          config,
          sourceFactory,
          sinkFactory,
          appMetrics,
          LoggerFactory.getLogger(TestingLoadJob.class));
    }
  }
}
