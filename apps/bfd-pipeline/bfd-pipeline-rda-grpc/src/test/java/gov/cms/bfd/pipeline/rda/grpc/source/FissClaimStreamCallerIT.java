package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.junit.Assert.assertEquals;

import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.pipeline.rda.grpc.server.JsonFissClaimSource;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaServer;
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
          + "  \"currStatus\": \"CLAIM_STATUS_PAID\","
          + "  \"currLoc1\": \"PROCESSING_TYPE_MANUAL\","
          + "  \"currLoc2\": \"uma\","
          + "  \"totalChargeAmount\": \"3.75\","
          + "  \"currTranDate\": \"2021-03-20\","
          + "  \"principleDiag\": \"uec\","
          + "  \"mbi\": \"c1ihk7q0g3i57\","
          + "  \"fissProcCodes\": [],"
          + "  \"medaProvId\": \"oducjgzt67joc\""
          + "}";
  private static final String CLAIM_2 =
      "{"
          + "  \"dcn\": \"2643602\","
          + "  \"hicNo\": \"640930211775\","
          + "  \"currStatus\": \"CLAIM_STATUS_REJECT\","
          + "  \"currLoc1\": \"PROCESSING_TYPE_OFFLINE\","
          + "  \"currLoc2\": \"p6s\","
          + "  \"totalChargeAmount\": \"55.91\","
          + "  \"recdDt\": \"2021-05-14\","
          + "  \"currTranDate\": \"2020-12-21\","
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
            "test", () -> new JsonFissClaimSource(CLAIM_1 + System.lineSeparator() + CLAIM_2));
    try {
      final ManagedChannel channel = InProcessChannelBuilder.forName("test").build();
      try {
        final IdHasher hasher = new IdHasher(new IdHasher.Config(10, "justsomestring"));
        final FissClaimTransformer transformer =
            new FissClaimTransformer(Clock.systemUTC(), hasher);
        final FissClaimStreamCaller caller = new FissClaimStreamCaller(transformer);
        final GrpcResponseStream<PreAdjFissClaim> results = caller.callService(channel);
        assertEquals(true, results.hasNext());
        assertEquals("63843470", results.next().getDcn());
        assertEquals(true, results.hasNext());
        assertEquals("2643602", results.next().getDcn());
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
