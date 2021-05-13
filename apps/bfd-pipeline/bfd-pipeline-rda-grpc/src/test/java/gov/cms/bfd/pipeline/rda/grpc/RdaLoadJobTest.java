package gov.cms.bfd.pipeline.rda.grpc;

import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.pipeline.rda.grpc.RdaLoadJob.Config;
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

public class RdaLoadJobTest {
  private Callable<RdaSource<Integer>> sourceFactory;
  private Callable<RdaSink<Integer>> sinkFactory;
  private RdaSource<Integer> source;
  private RdaSink<Integer> sink;
  private RdaLoadJob job;
  private MetricRegistry appMetrics;
  private Config config;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    sourceFactory = mock(Callable.class);
    sinkFactory = mock(Callable.class);
    source = mock(RdaSource.class);
    sink = mock(RdaSink.class);
    config = new Config(Duration.ofSeconds(10), 3);
    appMetrics = new MetricRegistry();
    job = new RdaLoadJob(config, sourceFactory, sinkFactory, appMetrics);
  }

  @Test
  public void openSourceFails() throws Exception {
    doThrow(new IOException("oops")).when(sourceFactory).call();
    try {
      job.call();
      Assert.fail("job should have thrown exception");
    } catch (Exception ex) {
      Assert.assertEquals("oops", ex.getCause().getMessage());
      MatcherAssert.assertThat(ex.getCause(), Matchers.instanceOf(IOException.class));
    }
    verifyNoInteractions(sinkFactory);
    Assert.assertEquals(1, appMetrics.meter(RdaLoadJob.CALLS_METER_NAME).getCount());
    Assert.assertEquals(1, appMetrics.meter(RdaLoadJob.FAILURES_METER_NAME).getCount());
  }

  @Test
  public void openSinkFails() throws Exception {
    doReturn(source).when(sourceFactory).call();
    doThrow(new IOException("oops")).when(sinkFactory).call();
    try {
      job.call();
      Assert.fail("job should have thrown exception");
    } catch (Exception ex) {
      Assert.assertEquals("oops", ex.getCause().getMessage());
      MatcherAssert.assertThat(ex.getCause(), Matchers.instanceOf(IOException.class));
    }
    verify(source).close();
    Assert.assertEquals(1, appMetrics.meter(RdaLoadJob.CALLS_METER_NAME).getCount());
    Assert.assertEquals(1, appMetrics.meter(RdaLoadJob.FAILURES_METER_NAME).getCount());
  }

  @Test
  public void sourceFails() throws Exception {
    doReturn(source).when(sourceFactory).call();
    doReturn(sink).when(sinkFactory).call();
    doThrow(new ProcessingException(new IOException("oops"), 7))
        .when(source)
        .retrieveAndProcessObjects(anyInt(), same(sink));
    try {
      job.call();
      Assert.fail("job should have thrown exception");
    } catch (Exception ex) {
      Assert.assertNotNull(ex.getCause());
      MatcherAssert.assertThat(ex.getCause(), Matchers.instanceOf(ProcessingException.class));
      Assert.assertEquals(7, ((ProcessingException) ex.getCause()).getProcessedCount());
      final Throwable actualCause = ex.getCause().getCause();
      MatcherAssert.assertThat(actualCause, Matchers.instanceOf(IOException.class));
      Assert.assertEquals("oops", actualCause.getMessage());
    }
    verify(source).close();
    verify(sink).close();
    Assert.assertEquals(1, appMetrics.meter(RdaLoadJob.CALLS_METER_NAME).getCount());
    Assert.assertEquals(1, appMetrics.meter(RdaLoadJob.FAILURES_METER_NAME).getCount());
  }

  @Test
  public void nothingToDo() throws Exception {
    doReturn(source).when(sourceFactory).call();
    doReturn(sink).when(sinkFactory).call();
    doReturn(0).when(source).retrieveAndProcessObjects(anyInt(), same(sink));
    try {
      PipelineJobOutcome outcome = job.call();
      Assert.assertEquals(PipelineJobOutcome.NOTHING_TO_DO, outcome);
    } catch (Exception ex) {
      Assert.fail("job should NOT have thrown exception");
    }
    verify(source).close();
    verify(sink).close();
    Assert.assertEquals(1, appMetrics.meter(RdaLoadJob.CALLS_METER_NAME).getCount());
    Assert.assertEquals(1, appMetrics.meter(RdaLoadJob.SUCCESSES_METER_NAME).getCount());
  }

  @Test
  public void workDone() throws Exception {
    doReturn(source).when(sourceFactory).call();
    doReturn(sink).when(sinkFactory).call();
    doReturn(25000).when(source).retrieveAndProcessObjects(anyInt(), same(sink));
    try {
      PipelineJobOutcome outcome = job.call();
      Assert.assertEquals(PipelineJobOutcome.WORK_DONE, outcome);
    } catch (Exception ex) {
      Assert.fail("job should NOT have thrown exception");
    }
    verify(source).close();
    verify(sink).close();
    Assert.assertEquals(1, appMetrics.meter(RdaLoadJob.CALLS_METER_NAME).getCount());
    Assert.assertEquals(1, appMetrics.meter(RdaLoadJob.SUCCESSES_METER_NAME).getCount());
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
        new RdaLoadJob<>(
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
      Assert.assertEquals(PipelineJobOutcome.NOTHING_TO_DO, secondCall.get());

      // now allow the first call to proceed and it should reflect that it has done some work
      waitForCompletion.countDown();
      Assert.assertEquals(PipelineJobOutcome.WORK_DONE, firstCall.get());
    } finally {
      pool.shutdown();
      pool.awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Test
  public void configIsSerializable() throws Exception {
    final RdaLoadJob.Config original = new Config(Duration.ofMillis(1000), 45);
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
      out.writeObject(original);
    }
    RdaLoadJob.Config loaded;
    try (ObjectInputStream inp =
        new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
      loaded = (Config) inp.readObject();
    }
    Assert.assertEquals(original, loaded);
  }
}
