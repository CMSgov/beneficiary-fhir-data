package gov.cms.bfd.pipeline.rda.grpc.sink;

import static gov.cms.bfd.pipeline.rda.grpc.RdaChange.MIN_SEQUENCE_NUM;
import static org.junit.Assert.assertEquals;

import gov.cms.bfd.model.rda.PartAdjMcsClaim;
import gov.cms.bfd.model.rda.PartAdjMcsDetail;
import gov.cms.bfd.model.rda.PartAdjMcsDiagnosisCode;
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

          final PartAdjMcsClaim claim = new PartAdjMcsClaim();
          claim.setSequenceNumber(7L);
          claim.setIdrClmHdIcn("3");
          claim.setIdrContrId("c1");
          claim.setIdrHic("hc");
          claim.setIdrClaimType("c");

          final PartAdjMcsDetail detail = new PartAdjMcsDetail();
          detail.setIdrClmHdIcn(claim.getIdrClmHdIcn());
          detail.setPriority((short) 0);
          detail.setIdrDtlStatus("P");
          claim.getDetails().add(detail);

          final PartAdjMcsDiagnosisCode diagCode = new PartAdjMcsDiagnosisCode();
          diagCode.setIdrClmHdIcn(claim.getIdrClmHdIcn());
          diagCode.setPriority((short) 0);
          diagCode.setIdrDiagIcdType("T");
          diagCode.setIdrDiagCode("D");
          claim.getDiagCodes().add(diagCode);

          int count =
              sink.writeObject(
                  new RdaChange<>(MIN_SEQUENCE_NUM, RdaChange.Type.INSERT, claim, Instant.now()));
          assertEquals(1, count);

          List<PartAdjMcsClaim> resultClaims =
              entityManager
                  .createQuery("select c from PartAdjMcsClaim c", PartAdjMcsClaim.class)
                  .getResultList();
          assertEquals(1, resultClaims.size());
          PartAdjMcsClaim resultClaim = resultClaims.get(0);
          assertEquals(Long.valueOf(7), resultClaim.getSequenceNumber());
          assertEquals("hc", resultClaim.getIdrHic());
          assertEquals(1, resultClaim.getDetails().size());
          assertEquals(1, resultClaim.getDiagCodes().size());

          assertEquals(Optional.of(7L), sink.readMaxExistingSequenceNumber());
        });
  }
}
