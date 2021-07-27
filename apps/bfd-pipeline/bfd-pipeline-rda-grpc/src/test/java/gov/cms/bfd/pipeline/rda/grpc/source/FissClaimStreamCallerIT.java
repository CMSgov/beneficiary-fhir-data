package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.junit.Assert.assertEquals;

import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.server.EmptyMessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.JsonMessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaServer;
import gov.cms.bfd.pipeline.rda.grpc.server.WrappedClaimSource;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import java.time.Clock;
import java.util.concurrent.TimeUnit;
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

  @Test
  public void test() throws Exception {
    final Server server =
        RdaServer.startInProcess(
            "test",
            () ->
                WrappedClaimSource.wrapFissClaims(
                    new JsonMessageSource<>(
                        CLAIM_1 + System.lineSeparator() + CLAIM_2,
                        JsonMessageSource::parseFissClaim)),
            EmptyMessageSource::new);
    try {
      final ManagedChannel channel = InProcessChannelBuilder.forName("test").build();
      try {
        final IdHasher hasher = new IdHasher(new IdHasher.Config(10, "justsomestring"));
        final FissClaimTransformer transformer =
            new FissClaimTransformer(Clock.systemUTC(), hasher);
        final FissClaimStreamCaller caller = new FissClaimStreamCaller(transformer);
        final GrpcResponseStream<RdaChange<PreAdjFissClaim>> results = caller.callService(channel);
        assertEquals(true, results.hasNext());
        assertEquals("63843470", results.next().getClaim().getDcn());
        assertEquals(true, results.hasNext());
        assertEquals("2643602", results.next().getClaim().getDcn());
        assertEquals(false, results.hasNext());
      } finally {
        channel.shutdown();
        channel.awaitTermination(5, TimeUnit.SECONDS);
      }
    } finally {
      server.shutdown();
      server.awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
