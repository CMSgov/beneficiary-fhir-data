package gov.cms.bfd.pipeline.rda.grpc.sink.direct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.model.rda.PreAdjMcsDetail;
import gov.cms.bfd.model.rda.PreAdjMcsDiagnosisCode;
import gov.cms.bfd.pipeline.rda.grpc.RdaPipelineTestUtils;
import gov.cms.bfd.pipeline.rda.grpc.source.McsClaimTransformer;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import gov.cms.mpsm.rda.v1.mcs.McsDetail;
import gov.cms.mpsm.rda.v1.mcs.McsDiagnosisCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class McsClaimRdaSinkIT {
  @Test
  public void mcsClaim() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        McsClaimRdaSinkIT.class,
        Clock.systemUTC(),
        (appState, entityManager) -> {
          final LocalDate today = LocalDate.of(2022, 1, 3);
          final Instant now = today.atStartOfDay().toInstant(ZoneOffset.UTC);
          final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
          final PreAdjMcsClaim claim = new PreAdjMcsClaim();
          claim.setSequenceNumber(7L);
          claim.setIdrClmHdIcn("3");
          claim.setIdrContrId("c1");
          claim.setIdrHic("hc");
          claim.setIdrClaimType("c");
          claim.setMbiRecord(new Mbi(1L, "12345678901", "hash-of-12345678901"));
          claim.setIdrClaimMbi(claim.getMbiRecord().getMbi());
          claim.setIdrClaimMbiHash(claim.getMbiRecord().getHash());

          final PreAdjMcsDetail detail = new PreAdjMcsDetail();
          detail.setIdrClmHdIcn(claim.getIdrClmHdIcn());
          detail.setPriority((short) 0);
          detail.setIdrDtlStatus("P");
          claim.getDetails().add(detail);

          final PreAdjMcsDiagnosisCode diagCode = new PreAdjMcsDiagnosisCode();
          diagCode.setIdrClmHdIcn(claim.getIdrClmHdIcn());
          diagCode.setPriority((short) 0);
          diagCode.setIdrDiagIcdType("T");
          diagCode.setIdrDiagCode("D");
          claim.getDiagCodes().add(diagCode);

          final McsDetail detailMessage =
              McsDetail.newBuilder().setIdrDtlStatusUnrecognized(detail.getIdrDtlStatus()).build();

          final McsDiagnosisCode diagCodeMessage =
              McsDiagnosisCode.newBuilder()
                  .setIdrDiagIcdTypeUnrecognized(diagCode.getIdrDiagIcdType())
                  .setIdrDiagCode(diagCode.getIdrDiagCode())
                  .build();

          final McsClaim claimMessage =
              McsClaim.newBuilder()
                  .setIdrClmHdIcn(claim.getIdrClmHdIcn())
                  .setIdrContrId(claim.getIdrContrId())
                  .setIdrClaimMbi(claim.getIdrClaimMbi())
                  .setIdrHic(claim.getIdrHic())
                  .setIdrClaimTypeUnrecognized(claim.getIdrClaimType())
                  .addMcsDetails(detailMessage)
                  .addMcsDiagnosisCodes(diagCodeMessage)
                  .build();

          final McsClaimChange message =
              McsClaimChange.newBuilder()
                  .setSeq(claim.getSequenceNumber())
                  .setIcn(claim.getIdrClmHdIcn())
                  .setClaim(claimMessage)
                  .build();

          final IdHasher hasher = new IdHasher(new IdHasher.Config(1, "notarealpepper"));
          final McsClaimTransformer transformer =
              new McsClaimTransformer(clock, hasher.getConfig());
          final McsClaimRdaSink sink = new McsClaimRdaSink(appState, transformer, true);
          final String expectedMbiHash = hasher.computeIdentifierHash(claim.getIdrClaimMbi());

          assertEquals(Optional.empty(), sink.readMaxExistingSequenceNumber());

          int count = sink.writeMessage("version", message);
          assertEquals(1, count);

          List<PreAdjMcsClaim> resultClaims =
              entityManager
                  .createQuery("select c from PreAdjMcsClaim c", PreAdjMcsClaim.class)
                  .getResultList();
          assertEquals(1, resultClaims.size());
          PreAdjMcsClaim resultClaim = resultClaims.get(0);
          assertEquals(Long.valueOf(7), resultClaim.getSequenceNumber());
          assertEquals(claim.getIdrHic(), resultClaim.getIdrHic());
          assertEquals(claim.getIdrClaimMbi(), resultClaim.getIdrClaimMbi());
          assertEquals(expectedMbiHash, resultClaim.getIdrClaimMbiHash());
          assertEquals(claim.getDetails().size(), resultClaim.getDetails().size());
          assertEquals(claim.getDiagCodes().size(), resultClaim.getDiagCodes().size());

          assertEquals(
              Optional.of(claim.getSequenceNumber()), sink.readMaxExistingSequenceNumber());

          Mbi databaseMbiEntity =
              RdaPipelineTestUtils.lookupCachedMbi(entityManager, claimMessage.getIdrClaimMbi());
          assertNotNull(databaseMbiEntity);
          assertEquals(claim.getIdrClaimMbi(), databaseMbiEntity.getMbi());
          assertEquals(expectedMbiHash, databaseMbiEntity.getHash());
        });
  }
}
