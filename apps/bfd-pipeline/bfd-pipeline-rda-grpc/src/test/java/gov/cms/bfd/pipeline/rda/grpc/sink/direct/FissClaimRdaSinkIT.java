package gov.cms.bfd.pipeline.rda.grpc.sink.direct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.model.rda.PreAdjFissDiagnosisCode;
import gov.cms.bfd.model.rda.PreAdjFissProcCode;
import gov.cms.bfd.pipeline.rda.grpc.RdaPipelineTestUtils;
import gov.cms.bfd.pipeline.rda.grpc.source.FissClaimTransformer;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.fiss.FissClaimStatus;
import gov.cms.mpsm.rda.v1.fiss.FissDiagnosisCode;
import gov.cms.mpsm.rda.v1.fiss.FissProcedureCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class FissClaimRdaSinkIT {
  @Test
  public void fissClaim() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        FissClaimRdaSinkIT.class,
        Clock.systemUTC(),
        (appState, entityManager) -> {
          final LocalDate today = LocalDate.of(2022, 1, 3);
          final Instant now = today.atStartOfDay().toInstant(ZoneOffset.UTC);
          final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
          final PreAdjFissClaim claim = new PreAdjFissClaim();
          claim.setSequenceNumber(3L);
          claim.setDcn("1");
          claim.setHicNo("h1");
          claim.setCurrStatus('T');
          claim.setCurrLoc1('A');
          claim.setCurrLoc2("1A");
          claim.setPracLocCity("city name can be very long indeed");
          claim.setMbiRecord(
              new Mbi(1L, "12345678901", "hash-of-12345678901", null, Instant.now()));

          final PreAdjFissProcCode procCode0 = new PreAdjFissProcCode();
          procCode0.setDcn(claim.getDcn());
          procCode0.setPriority((short) 0);
          procCode0.setProcCode("P");
          procCode0.setProcFlag("F");
          procCode0.setProcDate(today);
          claim.getProcCodes().add(procCode0);

          final PreAdjFissDiagnosisCode diagCode0 = new PreAdjFissDiagnosisCode();
          diagCode0.setDcn(claim.getDcn());
          diagCode0.setPriority((short) 0);
          diagCode0.setDiagCd2("cd2");
          diagCode0.setDiagPoaInd("Q");
          claim.getDiagCodes().add(diagCode0);

          final FissProcedureCode procCodeMessage =
              FissProcedureCode.newBuilder()
                  .setProcCd("P")
                  .setProcFlag("F")
                  .setProcDt("2022-01-03")
                  .build();

          final FissDiagnosisCode diagCodeMessage =
              FissDiagnosisCode.newBuilder()
                  .setDiagCd2("cd2")
                  .setDiagPoaIndUnrecognized("Q")
                  .build();

          final FissClaim claimMessage =
              FissClaim.newBuilder()
                  .setDcn(claim.getDcn())
                  .setHicNo(claim.getHicNo())
                  .setCurrStatusEnum(FissClaimStatus.CLAIM_STATUS_RTP)
                  .setCurrLoc1Unrecognized(String.valueOf(claim.getCurrLoc1()))
                  .setCurrLoc2Unrecognized(claim.getCurrLoc2())
                  .setPracLocCity("city name can be very long indeed")
                  .addFissProcCodes(0, procCodeMessage)
                  .addFissDiagCodes(0, diagCodeMessage)
                  .setMbi(claim.getMbiRecord().getMbi())
                  .build();

          final FissClaimChange message =
              FissClaimChange.newBuilder()
                  .setSeq(claim.getSequenceNumber())
                  .setDcn(claim.getDcn())
                  .setClaim(claimMessage)
                  .build();

          final IdHasher defaultIdHasher = new IdHasher(new IdHasher.Config(1, "notarealpepper"));
          final FissClaimTransformer transformer =
              new FissClaimTransformer(clock, MbiCache.computedCache(defaultIdHasher.getConfig()));
          final FissClaimRdaSink sink = new FissClaimRdaSink(appState, transformer, true);
          final String expectedMbiHash =
              defaultIdHasher.computeIdentifierHash(claim.getMbiRecord().getMbi());

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
          assertEquals(claim.getHicNo(), resultClaim.getHicNo());
          assertEquals(claim.getPracLocCity(), resultClaim.getPracLocCity());
          assertEquals(claim.getMbiRecord().getMbi(), resultClaim.getMbiRecord().getMbi());
          assertEquals(expectedMbiHash, resultClaim.getMbiRecord().getHash());
          assertEquals(claim.getProcCodes().size(), resultClaim.getProcCodes().size());
          assertEquals(claim.getDiagCodes().size(), resultClaim.getDiagCodes().size());

          assertEquals(
              Optional.of(claim.getSequenceNumber()), sink.readMaxExistingSequenceNumber());

          Mbi databaseMbiEntity =
              RdaPipelineTestUtils.lookupCachedMbi(entityManager, claimMessage.getMbi());
          assertNotNull(databaseMbiEntity);
          assertEquals(claim.getMbiRecord().getMbi(), databaseMbiEntity.getMbi());
          assertEquals(expectedMbiHash, databaseMbiEntity.getHash());
        });
  }
}
