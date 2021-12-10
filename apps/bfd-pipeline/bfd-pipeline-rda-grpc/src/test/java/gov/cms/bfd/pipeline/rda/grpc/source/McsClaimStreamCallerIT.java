package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.junit.Assert.assertEquals;

import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.pipeline.rda.grpc.server.RandomMcsClaimSource;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaServer;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import io.grpc.CallOptions;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.Test;

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
              final McsClaimStreamCaller caller = new McsClaimStreamCaller();
              final GrpcResponseStream<McsClaimChange> results =
                  caller.callService(channel, CallOptions.DEFAULT, 0L);
              assertEquals(true, results.hasNext());

              PreAdjMcsClaim claim = transform(results.next());
              assertEquals("75302", claim.getIdrClmHdIcn());
              assertEquals(Long.valueOf(0), claim.getSequenceNumber());
              assertEquals(Long.valueOf(0), claim.getSequenceNumber());
              assertEquals(true, results.hasNext());

              claim = transform(results.next());
              assertEquals("43644459", claim.getIdrClmHdIcn());
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
              final McsClaimStreamCaller caller = new McsClaimStreamCaller();
              final GrpcResponseStream<McsClaimChange> results =
                  caller.callService(channel, CallOptions.DEFAULT, 10L);
              assertEquals(Long.valueOf(10), transform(results.next()).getSequenceNumber());
              assertEquals(Long.valueOf(11), transform(results.next()).getSequenceNumber());
              assertEquals(Long.valueOf(12), transform(results.next()).getSequenceNumber());
              assertEquals(Long.valueOf(13), transform(results.next()).getSequenceNumber());
              assertEquals(Long.valueOf(14), transform(results.next()).getSequenceNumber());
              assertEquals(false, results.hasNext());
            });
  }

  private PreAdjMcsClaim transform(McsClaimChange change) {
    return transformer.transformClaim(change).getClaim();
  }
}
