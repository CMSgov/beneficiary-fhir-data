package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.pipeline.rda.grpc.server.JsonMessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.RandomFissClaimSource;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaServer;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaService;
import gov.cms.bfd.pipeline.rda.grpc.server.WrappedClaimSource;
import gov.cms.bfd.pipeline.rda.grpc.sink.direct.MbiCache;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import io.grpc.CallOptions;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

public class FissClaimStreamCallerIT {
  private static final String CLAIM_1 =
      "{"
          + "  \"dcn\": \"63843470\","
          + "  \"hicNo\": \"916689703543\","
          + "  \"currStatusEnum\": \"CLAIM_STATUS_PAID\","
          + "  \"currLoc1Enum\": \"PROCESSING_TYPE_MANUAL\","
          + "  \"currLoc2Unrecognized\": \"uma\","
          + "  \"totalChargeAmount\": \"3.75\","
          + "  \"currTranDtCymd\": \"2021-03-20\","
          + "  \"principleDiag\": \"uec\","
          + "  \"mbi\": \"c1ihk7q0g3i\","
          + "  \"fissProcCodes\": [],"
          + "  \"medaProvId\": \"oducjgzt67joc\""
          + "}";
  private static final String CLAIM_2 =
      "{"
          + "  \"dcn\": \"2643602\","
          + "  \"hicNo\": \"640930211775\","
          + "  \"currStatusEnum\": \"CLAIM_STATUS_REJECT\","
          + "  \"currLoc1Enum\": \"PROCESSING_TYPE_OFFLINE\","
          + "  \"currLoc2Unrecognized\": \"p6s\","
          + "  \"totalChargeAmount\": \"55.91\","
          + "  \"recdDtCymd\": \"2021-05-14\","
          + "  \"currTranDtCymd\": \"2020-12-21\","
          + "  \"principleDiag\": \"egnj\","
          + "  \"npiNumber\": \"5764657700\","
          + "  \"mbi\": \"0vtc7u321x0\","
          + "  \"fedTaxNb\": \"2845244764\","
          + "  \"fissProcCodes\": []"
          + "}";

  // hard coded time for consistent values in JSON (2021-06-03T18:02:37Z)
  private final Clock clock = Clock.fixed(Instant.ofEpochMilli(1622743357000L), ZoneOffset.UTC);
  private final IdHasher hasher = new IdHasher(new IdHasher.Config(10, "justsomestring"));
  private final FissClaimTransformer transformer =
      new FissClaimTransformer(clock, MbiCache.computedCache(hasher.getConfig()));

  @Test
  public void basicCall() throws Exception {
    RdaServer.InProcessConfig.builder()
        .serverName(getClass().getSimpleName())
        .fissSourceFactory(
            sequenceNumber ->
                WrappedClaimSource.wrapFissClaims(
                    new JsonMessageSource<>(
                        CLAIM_1 + System.lineSeparator() + CLAIM_2,
                        JsonMessageSource::parseFissClaim),
                    clock,
                    sequenceNumber))
        .build()
        .runWithChannelParam(
            channel -> {
              final FissClaimStreamCaller caller = new FissClaimStreamCaller();
              assertEquals(
                  RdaService.RDA_PROTO_VERSION,
                  caller.callVersionService(channel, CallOptions.DEFAULT));

              final GrpcResponseStream<FissClaimChange> results =
                  caller.callService(channel, CallOptions.DEFAULT, 0L);
              assertTrue(results.hasNext());

              PreAdjFissClaim claim = transform(results.next());
              assertEquals("63843470", claim.getDcn());
              assertEquals(Long.valueOf(0), claim.getSequenceNumber());
              assertTrue(results.hasNext());

              claim = transform(results.next());
              assertEquals("2643602", claim.getDcn());
              assertEquals(Long.valueOf(1), claim.getSequenceNumber());
              assertFalse(results.hasNext());
            });
  }

  @Test
  public void sequenceNumbers() throws Exception {
    RdaServer.InProcessConfig.builder()
        .serverName(getClass().getSimpleName())
        .fissSourceFactory(
            sequenceNumber ->
                new RandomFissClaimSource(1000L, 15).toClaimChanges().skip(sequenceNumber))
        .build()
        .runWithChannelParam(
            channel -> {
              final FissClaimStreamCaller caller = new FissClaimStreamCaller();
              final GrpcResponseStream<FissClaimChange> results =
                  caller.callService(channel, CallOptions.DEFAULT, 10L);
              assertEquals(Long.valueOf(10), transform(results.next()).getSequenceNumber());
              assertEquals(Long.valueOf(11), transform(results.next()).getSequenceNumber());
              assertEquals(Long.valueOf(12), transform(results.next()).getSequenceNumber());
              assertEquals(Long.valueOf(13), transform(results.next()).getSequenceNumber());
              assertEquals(Long.valueOf(14), transform(results.next()).getSequenceNumber());
              assertFalse(results.hasNext());
            });
  }

  private PreAdjFissClaim transform(FissClaimChange change) {
    return transformer.transformClaim(change).getClaim();
  }
}
