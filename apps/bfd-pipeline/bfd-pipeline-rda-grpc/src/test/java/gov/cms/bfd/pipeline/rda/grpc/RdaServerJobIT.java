package gov.cms.bfd.pipeline.rda.grpc;

import static org.junit.Assert.assertEquals;

import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.pipeline.rda.grpc.source.FissClaimStreamCaller;
import gov.cms.bfd.pipeline.rda.grpc.source.FissClaimTransformer;
import gov.cms.bfd.pipeline.rda.grpc.source.GrpcResponseStream;
import gov.cms.bfd.pipeline.rda.grpc.source.McsClaimStreamCaller;
import gov.cms.bfd.pipeline.rda.grpc.source.McsClaimTransformer;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class RdaServerJobIT {
  private final Clock clock = Clock.fixed(Instant.ofEpochMilli(60_000L), ZoneOffset.UTC);
  private final IdHasher hasher = new IdHasher(new IdHasher.Config(100, "whatever"));

  @Test
  public void testRandom() throws Exception {
    final String serverName = "test-server";
    final RdaServerJob.Config config =
        new RdaServerJob.Config(
            RdaServerJob.Config.ServerMode.Random,
            serverName,
            Optional.empty(),
            Optional.of(1L),
            Optional.of(4),
            Optional.empty(),
            Optional.empty());
    final RdaServerJob job = new RdaServerJob(config);
    final ExecutorService exec = Executors.newCachedThreadPool();
    final Future<PipelineJobOutcome> outcome = exec.submit(job);
    try {
      waitForServerToStart(job);
      final ManagedChannel fissChannel = InProcessChannelBuilder.forName(serverName).build();
      final FissClaimStreamCaller fissCaller =
          new FissClaimStreamCaller(new FissClaimTransformer(clock, hasher));
      final GrpcResponseStream<RdaChange<PreAdjFissClaim>> fissStream =
          fissCaller.callService(fissChannel, 2);
      assertEquals(true, fissStream.hasNext());
      assertEquals(2L, fissStream.next().getSequenceNumber());
      assertEquals(true, fissStream.hasNext());
      assertEquals(3L, fissStream.next().getSequenceNumber());
      assertEquals(false, fissStream.hasNext());
      final ManagedChannel mcsChannel = InProcessChannelBuilder.forName(serverName).build();
      final McsClaimStreamCaller mcsCaller =
          new McsClaimStreamCaller(new McsClaimTransformer(clock, hasher));
      final GrpcResponseStream<RdaChange<PreAdjMcsClaim>> mcsStream =
          mcsCaller.callService(mcsChannel, 3);
      assertEquals(true, mcsStream.hasNext());
      assertEquals(3L, mcsStream.next().getSequenceNumber());
      assertEquals(false, mcsStream.hasNext());
    } finally {
      exec.shutdownNow();
      exec.awaitTermination(10, TimeUnit.SECONDS);
      assertEquals(PipelineJobOutcome.WORK_DONE, outcome.get());
    }
  }

  /**
   * Waits at most 30 seconds for the server to get started. It's possible for the thread pool to
   * take longer to start than the test takes to create its StreamCallers.
   */
  private void waitForServerToStart(RdaServerJob job) throws InterruptedException {
    Thread.sleep(500);
    for (int i = 1; i <= 59 && !job.isRunning(); ++i) {
      Thread.sleep(500);
    }
  }
}
