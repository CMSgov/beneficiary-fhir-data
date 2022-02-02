package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.junit.jupiter.api.Assertions.*;

import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.pipeline.rda.grpc.server.RandomMcsClaimSource;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaServer;
import gov.cms.bfd.pipeline.rda.grpc.sink.direct.MbiCache;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import io.grpc.CallOptions;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

public class McsClaimStreamCallerIT {
  // hard coded time for consistent values in JSON (2021-06-03T18:02:37Z)
  private final Clock clock = Clock.fixed(Instant.ofEpochMilli(1622743357000L), ZoneOffset.UTC);
  private final McsClaimTransformer transformer =
      new McsClaimTransformer(
          clock, MbiCache.computedCache(new IdHasher.Config(10, "justsomestring")));

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
              assertTrue(results.hasNext());

              PreAdjMcsClaim claim = transform(results.next());
              assertTrue(claim.getIdrClmHdIcn().length() > 0);
              assertEquals(Long.valueOf(0), claim.getSequenceNumber());
              assertTrue(results.hasNext());

              claim = transform(results.next());
              assertTrue(claim.getIdrClmHdIcn().length() > 0);
              assertEquals(Long.valueOf(1), claim.getSequenceNumber());
              assertFalse(results.hasNext());
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
              assertFalse(results.hasNext());
            });
  }

  private PreAdjMcsClaim transform(McsClaimChange change) {
    return transformer.transformClaim(change).getClaim();
  }
}
