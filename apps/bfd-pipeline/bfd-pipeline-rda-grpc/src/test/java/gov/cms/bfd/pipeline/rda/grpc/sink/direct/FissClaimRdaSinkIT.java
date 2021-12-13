package gov.cms.bfd.pipeline.rda.grpc.sink.direct;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.model.rda.PreAdjFissDiagnosisCode;
import gov.cms.bfd.model.rda.PreAdjFissProcCode;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.RdaPipelineTestUtils;
import gov.cms.bfd.pipeline.rda.grpc.source.FissClaimTransformer;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public class FissClaimRdaSinkIT {
  @Test
  public void fissClaim() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        FissClaimRdaSinkIT.class,
        Clock.systemUTC(),
        (appState, entityManager) -> {
          final PreAdjFissClaim claim = new PreAdjFissClaim();
          claim.setSequenceNumber(3L);
          claim.setDcn("1");
          claim.setHicNo("h1");
          claim.setCurrStatus('1');
          claim.setCurrLoc1('A');
          claim.setCurrLoc2("1A");
          claim.setPracLocCity("city name can be very long indeed");

          final PreAdjFissProcCode procCode0 = new PreAdjFissProcCode();
          procCode0.setDcn(claim.getDcn());
          procCode0.setPriority((short) 0);
          procCode0.setProcCode("P");
          procCode0.setProcFlag("F");
          procCode0.setProcDate(LocalDate.now());
          procCode0.setLastUpdated(Instant.now());
          claim.getProcCodes().add(procCode0);

          final PreAdjFissDiagnosisCode diagCode0 = new PreAdjFissDiagnosisCode();
          diagCode0.setDcn(claim.getDcn());
          diagCode0.setPriority((short) 0);
          diagCode0.setDiagCd2("cd2");
          diagCode0.setDiagPoaInd("Q");
          claim.getDiagCodes().add(diagCode0);

          final RdaChange<PreAdjFissClaim> change =
              new RdaChange<>(
                  claim.getSequenceNumber(), RdaChange.Type.INSERT, claim, Instant.now());
          final FissClaimChange message =
              FissClaimChange.newBuilder()
                  .setDcn(claim.getDcn())
                  .setSeq(claim.getSequenceNumber())
                  .build();
          final FissClaimTransformer transformer = mock(FissClaimTransformer.class);
          doReturn(change).when(transformer).transformClaim(message);

          final FissClaimRdaSink sink = new FissClaimRdaSink(appState, transformer, true);

          assertEquals(Optional.empty(), sink.readMaxExistingSequenceNumber());

          int count = sink.writeMessage("version", message);
          assertEquals(1, count);

          List<PreAdjFissClaim> claims =
              entityManager
                  .createQuery("select c from PreAdjFissClaim c", PreAdjFissClaim.class)
                  .getResultList();
          assertEquals(1, claims.size());
          PreAdjFissClaim resultClaim = claims.get(0);
          assertEquals(Long.valueOf(3), resultClaim.getSequenceNumber());
          assertEquals("h1", resultClaim.getHicNo());
          assertEquals("city name can be very long indeed", resultClaim.getPracLocCity());
          assertEquals(1, resultClaim.getProcCodes().size());
          assertEquals(1, resultClaim.getDiagCodes().size());

          assertEquals(Optional.of(3L), sink.readMaxExistingSequenceNumber());
        });
  }
}
