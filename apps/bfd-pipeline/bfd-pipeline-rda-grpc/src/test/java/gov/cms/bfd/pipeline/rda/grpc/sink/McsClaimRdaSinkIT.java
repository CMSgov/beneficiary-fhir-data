package gov.cms.bfd.pipeline.rda.grpc.sink;

import static gov.cms.bfd.pipeline.rda.grpc.RdaChange.MIN_SEQUENCE_NUM;
import static org.junit.Assert.assertEquals;

import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.model.rda.PreAdjMcsClaimJson;
import gov.cms.bfd.model.rda.PreAdjMcsDetail;
import gov.cms.bfd.model.rda.PreAdjMcsDiagnosisCode;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.RdaPipelineTestUtils;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public class McsClaimRdaSinkIT {
  @Test
  public void mcsClaim() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        McsClaimRdaSinkIT.class,
        Clock.systemUTC(),
        (appState, entityManager) -> {
          final McsClaimRdaSink sink = new McsClaimRdaSink(appState);

          assertEquals(Optional.empty(), sink.readMaxExistingSequenceNumber());

          final PreAdjMcsClaim claim =
              PreAdjMcsClaim.builder()
                  .sequenceNumber(7L)
                  .lastUpdated(Instant.now())
                  .idrClmHdIcn("3")
                  .idrContrId("c1")
                  .idrHic("hc")
                  .idrClaimType("c")
                  .build();

          final PreAdjMcsDetail detail = PreAdjMcsDetail.builder().idrDtlStatus("P").build();
          claim.getDetails().add(detail);

          final PreAdjMcsDiagnosisCode diagCode =
              PreAdjMcsDiagnosisCode.builder().idrDiagIcdType("T").build();
          claim.getDiagCodes().add(diagCode);

          int count =
              sink.writeObject(
                  new RdaChange<>(MIN_SEQUENCE_NUM, RdaChange.Type.INSERT, claim, Instant.now()));
          assertEquals(1, count);

          List<PreAdjMcsClaimJson> resultClaims =
              entityManager
                  .createQuery("select c from PreAdjMcsClaimJson c", PreAdjMcsClaimJson.class)
                  .getResultList();
          assertEquals(1, resultClaims.size());
          PreAdjMcsClaim resultClaim = resultClaims.get(0).getClaim();
          assertEquals(Long.valueOf(7), resultClaim.getSequenceNumber());
          assertEquals("hc", resultClaim.getIdrHic());
          assertEquals(1, resultClaim.getDetails().size());
          assertEquals(1, resultClaim.getDiagCodes().size());

          assertEquals(Optional.of(7L), sink.readMaxExistingSequenceNumber());
        });
  }
}
