package gov.cms.bfd.pipeline.rda.grpc;

import static org.junit.Assert.*;

import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.pipeline.rda.grpc.source.FissClaimStreamCaller;
import gov.cms.bfd.pipeline.rda.grpc.source.FissClaimTransformer;
import gov.cms.bfd.pipeline.rda.grpc.source.GrpcResponseStream;
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
      final ManagedChannel channel = InProcessChannelBuilder.forName(serverName).build();
      final FissClaimStreamCaller caller =
          new FissClaimStreamCaller(new FissClaimTransformer(clock, hasher));
      final GrpcResponseStream<RdaChange<PreAdjFissClaim>> stream = caller.callService(channel, 2);
      assertEquals(true, stream.hasNext());
      assertEquals(2L, stream.next().getSequenceNumber());
      assertEquals(true, stream.hasNext());
      assertEquals(3L, stream.next().getSequenceNumber());
      assertEquals(false, stream.hasNext());
    } finally {
      exec.shutdownNow();
      exec.awaitTermination(10, TimeUnit.SECONDS);
      assertEquals(PipelineJobOutcome.WORK_DONE, outcome.get());
    }
  }
}
