package gov.cms.bfd.pipeline.rda.grpc;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Callable;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DcGeoRdaLoadJobTest {
  private Callable<RdaSource<PreAdjudicatedClaim>> sourceFactory;
  private Callable<RdaSink<PreAdjudicatedClaim>> sinkFactory;
  private RdaSource<PreAdjudicatedClaim> source;
  private RdaSink<PreAdjudicatedClaim> sink;
  private DcGeoRdaLoadJob job;
  private MetricRegistry appMetrics;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    sourceFactory = mock(Callable.class);
    sinkFactory = mock(Callable.class);
    source = mock(RdaSource.class);
    sink = mock(RdaSink.class);
    DcGeoRdaLoadJob.Config config =
        new DcGeoRdaLoadJob.Config(Duration.ofSeconds(10), Duration.ofSeconds(25), 5, 3);
    appMetrics = new MetricRegistry();
    job = new DcGeoRdaLoadJob(config, sourceFactory, sinkFactory, appMetrics);
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
    Assert.assertEquals(1, appMetrics.meter(DcGeoRdaLoadJob.CALLS_METER_NAME).getCount());
    Assert.assertEquals(1, appMetrics.meter(DcGeoRdaLoadJob.FAILURES_METER_NAME).getCount());
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
    Assert.assertEquals(1, appMetrics.meter(DcGeoRdaLoadJob.CALLS_METER_NAME).getCount());
    Assert.assertEquals(1, appMetrics.meter(DcGeoRdaLoadJob.FAILURES_METER_NAME).getCount());
  }

  @Test
  public void sourceFails() throws Exception {
    doReturn(source).when(sourceFactory).call();
    doReturn(sink).when(sinkFactory).call();
    doThrow(new ProcessingException(new IOException("oops"), 7))
        .when(source)
        .retrieveAndProcessObjects(anyInt(), anyInt(), any(), same(sink));
    try {
      job.call();
      Assert.fail("job should have thrown exception");
    } catch (Exception ex) {
      Assert.assertEquals("oops", ex.getCause().getMessage());
      MatcherAssert.assertThat(ex.getCause(), Matchers.instanceOf(IOException.class));
    }
    verify(source).close();
    verify(sink).close();
    Assert.assertEquals(1, appMetrics.meter(DcGeoRdaLoadJob.CALLS_METER_NAME).getCount());
    Assert.assertEquals(1, appMetrics.meter(DcGeoRdaLoadJob.FAILURES_METER_NAME).getCount());
  }

  @Test
  public void nothingToDo() throws Exception {
    doReturn(source).when(sourceFactory).call();
    doReturn(sink).when(sinkFactory).call();
    doReturn(0).when(source).retrieveAndProcessObjects(anyInt(), anyInt(), any(), same(sink));
    try {
      PipelineJobOutcome outcome = job.call();
      Assert.assertEquals(PipelineJobOutcome.NOTHING_TO_DO, outcome);
    } catch (Exception ex) {
      Assert.fail("job should NOT have thrown exception");
    }
    verify(source).close();
    verify(sink).close();
    Assert.assertEquals(1, appMetrics.meter(DcGeoRdaLoadJob.CALLS_METER_NAME).getCount());
    Assert.assertEquals(1, appMetrics.meter(DcGeoRdaLoadJob.SUCCESSES_METER_NAME).getCount());
  }

  @Test
  public void workDone() throws Exception {
    doReturn(source).when(sourceFactory).call();
    doReturn(sink).when(sinkFactory).call();
    doReturn(25000).when(source).retrieveAndProcessObjects(anyInt(), anyInt(), any(), same(sink));
    try {
      PipelineJobOutcome outcome = job.call();
      Assert.assertEquals(PipelineJobOutcome.WORK_DONE, outcome);
    } catch (Exception ex) {
      Assert.fail("job should NOT have thrown exception");
    }
    verify(source).close();
    verify(sink).close();
    Assert.assertEquals(1, appMetrics.meter(DcGeoRdaLoadJob.CALLS_METER_NAME).getCount());
    Assert.assertEquals(1, appMetrics.meter(DcGeoRdaLoadJob.SUCCESSES_METER_NAME).getCount());
  }
}
