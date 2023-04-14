package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.junit.jupiter.api.Assertions.*;

import gov.cms.bfd.model.rda.RdaMcsClaim;
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

/** Integration tests for the {@link McsClaimStreamCaller}. */
public class McsClaimStreamCallerIT {
  /** Clock for creating for consistent values in JSON (2021-06-03T18:02:37Z). */
  private final Clock clock = Clock.fixed(Instant.ofEpochMilli(1622743357000L), ZoneOffset.UTC);
  /** The transformer to create results for correctness verification. */
  private final McsClaimTransformer transformer =
      new McsClaimTransformer(
          clock, MbiCache.computedCache(new IdHasher.Config(10, "justsomestring")));

  /**
   * Verifies the caller can respond to a basic request and the results contain the expected values.
   *
   * @throws Exception indicates a test failure / setup issue
   */
  @Test
  public void basicCall() throws Exception {
    RdaServer.InProcessConfig.builder()
        .serverName(getClass().getSimpleName())
        .mcsSourceFactory(
            sequenceNumber ->
                new RandomMcsClaimSource(1000L, 2).toClaimChanges().skipTo(sequenceNumber))
        .build()
        .runWithChannelParam(
            channel -> {
              final McsClaimStreamCaller caller = new McsClaimStreamCaller();
              final GrpcResponseStream<McsClaimChange> results =
                  caller.callService(channel, CallOptions.DEFAULT, 0L);
              assertTrue(results.hasNext());

              RdaMcsClaim claim = transform(results.next());
              assertTrue(claim.getIdrClmHdIcn().length() > 0);
              assertEquals(Long.valueOf(0), claim.getSequenceNumber());
              assertTrue(results.hasNext());

              claim = transform(results.next());
              assertTrue(claim.getIdrClmHdIcn().length() > 0);
              assertEquals(Long.valueOf(1), claim.getSequenceNumber());
              assertFalse(results.hasNext());
            });
  }

  /**
   * Verifies the caller's results have sequential sequence numbers.
   *
   * @throws Exception indicates a test failure / setup issue
   */
  @Test
  public void sequenceNumbers() throws Exception {
    RdaServer.InProcessConfig.builder()
        .serverName(getClass().getSimpleName())
        .mcsSourceFactory(
            sequenceNumber ->
                new RandomMcsClaimSource(1000L, 15).toClaimChanges().skipTo(sequenceNumber))
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

  /**
   * Transforms a {@link McsClaimChange} to a {@link RdaMcsClaim}.
   *
   * @param change the change to transform
   * @return the resulting RDA MCS claim
   */
  private RdaMcsClaim transform(McsClaimChange change) {
    return transformer.transformClaim(change).getClaim();
  }
}
