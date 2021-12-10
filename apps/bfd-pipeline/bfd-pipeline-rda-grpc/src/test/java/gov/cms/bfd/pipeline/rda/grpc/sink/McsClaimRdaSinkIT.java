package gov.cms.bfd.pipeline.rda.grpc.sink;

import static gov.cms.bfd.pipeline.rda.grpc.RdaChange.MIN_SEQUENCE_NUM;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.model.rda.PreAdjMcsDetail;
import gov.cms.bfd.model.rda.PreAdjMcsDiagnosisCode;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.RdaPipelineTestUtils;
import gov.cms.bfd.pipeline.rda.grpc.source.McsClaimTransformer;
import gov.cms.mpsm.rda.v1.McsClaimChange;
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
          final PreAdjMcsClaim claim = new PreAdjMcsClaim();
          claim.setSequenceNumber(7L);
          claim.setIdrClmHdIcn("3");
          claim.setIdrContrId("c1");
          claim.setIdrHic("hc");
          claim.setIdrClaimType("c");

          final PreAdjMcsDetail detail = new PreAdjMcsDetail();
          detail.setIdrClmHdIcn(claim.getIdrClmHdIcn());
          detail.setPriority((short) 0);
          detail.setIdrDtlStatus("P");
          claim.getDetails().add(detail);

          final PreAdjMcsDiagnosisCode diagCode = new PreAdjMcsDiagnosisCode();
          diagCode.setIdrClmHdIcn(claim.getIdrClmHdIcn());
          diagCode.setPriority((short) 0);
          diagCode.setIdrDiagIcdType("T");
          claim.getDiagCodes().add(diagCode);

          final RdaChange<PreAdjMcsClaim> change =
              new RdaChange<>(MIN_SEQUENCE_NUM, RdaChange.Type.INSERT, claim, Instant.now());
          final McsClaimChange message = mock(McsClaimChange.class);
          final McsClaimTransformer transformer = mock(McsClaimTransformer.class);
          doReturn(change).when(transformer).transformClaim(message);

          final McsClaimRdaSink sink = new McsClaimRdaSink(appState, transformer, true);

          assertEquals(Optional.empty(), sink.readMaxExistingSequenceNumber());

          int count = sink.writeMessage("", message);
          assertEquals(1, count);

          List<PreAdjMcsClaim> resultClaims =
              entityManager
                  .createQuery("select c from PreAdjMcsClaim c", PreAdjMcsClaim.class)
                  .getResultList();
          assertEquals(1, resultClaims.size());
          PreAdjMcsClaim resultClaim = resultClaims.get(0);
          assertEquals(Long.valueOf(7), resultClaim.getSequenceNumber());
          assertEquals("hc", resultClaim.getIdrHic());
          assertEquals(1, resultClaim.getDetails().size());
          assertEquals(1, resultClaim.getDiagCodes().size());

          assertEquals(Optional.of(7L), sink.readMaxExistingSequenceNumber());
        });
  }
}
