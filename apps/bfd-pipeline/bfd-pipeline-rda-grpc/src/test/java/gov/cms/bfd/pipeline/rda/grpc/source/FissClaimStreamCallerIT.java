package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.junit.Assert.assertEquals;

import gov.cms.bfd.model.rda.PartAdjFissClaim;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.server.JsonMessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.RandomFissClaimSource;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaServer;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaService;
import gov.cms.bfd.pipeline.rda.grpc.server.WrappedClaimSource;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import io.grpc.CallOptions;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.Test;

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
          + "  \"mbi\": \"c1ihk7q0g3i57\","
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
          + "  \"mbi\": \"0vtc7u321x0se\","
          + "  \"fedTaxNb\": \"2845244764\","
          + "  \"fissProcCodes\": []"
          + "}";

  // hard coded time for consistent values in JSON (2021-06-03T18:02:37Z)
  private final Clock clock = Clock.fixed(Instant.ofEpochMilli(1622743357000L), ZoneOffset.UTC);
  private final IdHasher hasher = new IdHasher(new IdHasher.Config(10, "justsomestring"));
  private final FissClaimTransformer transformer = new FissClaimTransformer(clock, hasher);

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
              final FissClaimStreamCaller caller = new FissClaimStreamCaller(transformer);
              final GrpcResponseStream<RdaChange<PartAdjFissClaim>> results =
                  caller.callService(channel, CallOptions.DEFAULT, 0L);
              assertEquals(true, results.hasNext());

              PartAdjFissClaim claim = results.next().getClaim();
              assertEquals("63843470", claim.getDcn());
              assertEquals(Long.valueOf(0), claim.getSequenceNumber());
              assertEquals(RdaService.RDA_PROTO_VERSION, claim.getApiSource());
              assertEquals(true, results.hasNext());

              claim = results.next().getClaim();
              assertEquals("2643602", claim.getDcn());
              assertEquals(Long.valueOf(1), claim.getSequenceNumber());
              assertEquals(RdaService.RDA_PROTO_VERSION, claim.getApiSource());
              assertEquals(false, results.hasNext());
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
              final FissClaimStreamCaller caller = new FissClaimStreamCaller(transformer);
              final GrpcResponseStream<RdaChange<PartAdjFissClaim>> results =
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
