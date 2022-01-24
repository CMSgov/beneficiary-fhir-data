package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.server.RandomMcsClaimSource;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaServer;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import io.grpc.CallOptions;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

public class McsClaimStreamCallerIT {
  // hard coded time for consistent values in JSON (2021-06-03T18:02:37Z)
  private final Clock clock = Clock.fixed(Instant.ofEpochMilli(1622743357000L), ZoneOffset.UTC);
  private final IdHasher hasher = new IdHasher(new IdHasher.Config(10, "justsomestring"));
  private final McsClaimTransformer transformer = new McsClaimTransformer(clock, hasher);

  @Test
  public void basicCall() throws Exception {
    RdaServer.InProcessConfig.builder()
        .serverName(getClass().getSimpleName())
        .mcsSourceFactory(
            sequenceNumber ->
                new RandomMcsClaimSource(1000L, 2).toClaimChanges().skip(sequenceNumber))
        .build()
        .runWithChannelParam(
            channel -> {
              final McsClaimStreamCaller caller = new McsClaimStreamCaller(transformer);
              final GrpcResponseStream<RdaChange<PreAdjMcsClaim>> results =
                  caller.callService(channel, CallOptions.DEFAULT, 0L);
              assertEquals(true, results.hasNext());

              PreAdjMcsClaim claim = results.next().getClaim();
              assertEquals("75302", claim.getIdrClmHdIcn());
              assertEquals(Long.valueOf(0), claim.getSequenceNumber());
              assertEquals(Long.valueOf(0), claim.getSequenceNumber());
              assertEquals(true, results.hasNext());

              claim = results.next().getClaim();
              assertEquals("972078", claim.getIdrClmHdIcn());
              assertEquals(Long.valueOf(1), claim.getSequenceNumber());
              assertEquals(Long.valueOf(1), claim.getSequenceNumber());
              assertEquals(false, results.hasNext());
            });
  }

  @Test
  public void sequenceNumbers() throws Exception {
    RdaServer.InProcessConfig.builder()
        .serverName(getClass().getSimpleName())
        .mcsSourceFactory(
            sequenceNumber ->
                new RandomMcsClaimSource(1000L, 15).toClaimChanges().skip(sequenceNumber))
        .build()
        .runWithChannelParam(
            channel -> {
              final McsClaimStreamCaller caller = new McsClaimStreamCaller(transformer);
              final GrpcResponseStream<RdaChange<PreAdjMcsClaim>> results =
                  caller.callService(channel, CallOptions.DEFAULT, 10L);
              assertEquals(10L, results.next().getSequenceNumber());
              assertEquals(11L, results.next().getSequenceNumber());
              assertEquals(12L, results.next().getSequenceNumber());
              assertEquals(13L, results.next().getSequenceNumber());
              assertEquals(14L, results.next().getSequenceNumber());
              assertEquals(false, results.hasNext());
            });
  }
}
