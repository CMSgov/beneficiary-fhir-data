package gov.cms.bfd.pipeline.rda.grpc.sink;

import static org.junit.Assert.assertEquals;

import gov.cms.bfd.model.rda.PartAdjFissClaim;
import gov.cms.bfd.model.rda.PartAdjFissDiagnosisCode;
import gov.cms.bfd.model.rda.PartAdjFissProcCode;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.RdaPipelineTestUtils;
import gov.cms.bfd.pipeline.rda.grpc.server.RandomFissClaimGenerator;
import gov.cms.bfd.pipeline.rda.grpc.source.FissClaimTransformer;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.mpsm.rda.v1.ChangeType;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.Test;

public class FissClaimRdaSinkIT {
  @Test
  public void fissClaim() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        FissClaimRdaSinkIT.class,
        Clock.systemUTC(),
        (appState, entityManager) -> {
          final FissClaimRdaSink sink = new FissClaimRdaSink(appState);

          assertEquals(Optional.empty(), sink.readMaxExistingSequenceNumber());

          final PartAdjFissClaim claim = new PartAdjFissClaim();
          claim.setSequenceNumber(3L);
          claim.setDcn("1");
          claim.setHicNo("h1");
          claim.setCurrStatus('1');
          claim.setCurrLoc1('A');
          claim.setCurrLoc2("1A");
          claim.setPracLocCity("city name can be very long indeed");

          final PartAdjFissProcCode procCode0 = new PartAdjFissProcCode();
          procCode0.setDcn(claim.getDcn());
          procCode0.setPriority((short) 0);
          procCode0.setProcCode("P");
          procCode0.setProcFlag("F");
          procCode0.setProcDate(LocalDate.now());
          procCode0.setLastUpdated(Instant.now());
          claim.getProcCodes().add(procCode0);

          final PartAdjFissDiagnosisCode diagCode0 = new PartAdjFissDiagnosisCode();
          diagCode0.setDcn(claim.getDcn());
          diagCode0.setPriority((short) 0);
          diagCode0.setDiagCd2("cd2");
          diagCode0.setDiagPoaInd("Q");
          claim.getDiagCodes().add(diagCode0);

          int count =
              sink.writeObject(
                  new RdaChange<>(
                      claim.getSequenceNumber(), RdaChange.Type.INSERT, claim, Instant.now()));
          assertEquals(1, count);

          List<PartAdjFissClaim> claims =
              entityManager
                  .createQuery("select c from PartAdjFissClaim c", PartAdjFissClaim.class)
                  .getResultList();
          assertEquals(1, claims.size());
          PartAdjFissClaim resultClaim = claims.get(0);
          assertEquals(Long.valueOf(3), resultClaim.getSequenceNumber());
          assertEquals("h1", resultClaim.getHicNo());
          assertEquals("city name can be very long indeed", resultClaim.getPracLocCity());
          assertEquals(1, resultClaim.getProcCodes().size());
          assertEquals(1, resultClaim.getDiagCodes().size());

          assertEquals(Optional.of(3L), sink.readMaxExistingSequenceNumber());
        });
  }

  /**
   * Tests that a single batch can safely contains multiple objects with the same claim id and that
   * when this happens the version written to the database has the correct detail records in the
   * database. Verifies that {@code dedupChanges()} is working as expected.
   */
  @Test
  public void multipleVersionsOfSameClaimInBatch() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        FissClaimRdaSinkIT.class,
        Clock.systemUTC(),
        (appState, entityManager) -> {
          final int numberOfClaims = 20;
          final int numberOfUniqueClaims = 5;
          final int lastUniqueOffset = numberOfClaims - numberOfUniqueClaims;
          final RandomFissClaimGenerator generator = new RandomFissClaimGenerator(1000);
          final FissClaimTransformer transformer =
              new FissClaimTransformer(
                  Clock.systemUTC(), new IdHasher(new IdHasher.Config(1, "asdkfjbasdbfd")));

          List<RdaChange<PartAdjFissClaim>> claims = new ArrayList<>();
          for (int i = 0; i < numberOfClaims; ++i) {
            FissClaim rdaClaim = generator.randomClaim();
            if (i >= numberOfUniqueClaims) {
              rdaClaim =
                  rdaClaim
                      .toBuilder()
                      .setDcn(claims.get(i % numberOfUniqueClaims).getClaim().getDcn())
                      .build();
            }
            FissClaimChange rdaChange =
                FissClaimChange.newBuilder()
                    .setChangeType(
                        i < numberOfUniqueClaims
                            ? ChangeType.CHANGE_TYPE_INSERT
                            : ChangeType.CHANGE_TYPE_UPDATE)
                    .setSeq(i)
                    .setClaim(rdaClaim)
                    .build();
            final RdaChange<PartAdjFissClaim> claim = transformer.transformClaim(rdaChange);
            claims.add(claim);
          }

          final FissClaimRdaSink sink = new FissClaimRdaSink(appState);

          assertEquals(Optional.empty(), sink.readMaxExistingSequenceNumber());

          int count = sink.writeBatch(claims);
          assertEquals(numberOfUniqueClaims, count);

          List<PartAdjFissClaim> dbClaims =
              entityManager.createQuery("select c from PartAdjFissClaim c", PartAdjFissClaim.class)
                  .getResultList().stream()
                  .sorted(Comparator.comparingLong(PartAdjFissClaim::getSequenceNumber))
                  .collect(Collectors.toList());
          assertEquals(numberOfUniqueClaims, dbClaims.size());
          for (int i = 0; i < numberOfUniqueClaims; ++i) {
            PartAdjFissClaim dbClaim = dbClaims.get(i);
            PartAdjFissClaim origClaim = claims.get(lastUniqueOffset + i).getClaim();
            assertEquals(origClaim.getDcn(), dbClaim.getDcn());
            assertEquals(origClaim.getDiagCodes().size(), dbClaim.getDiagCodes().size());
            assertEquals(origClaim.getPayers().size(), dbClaim.getPayers().size());
            assertEquals(origClaim.getProcCodes().size(), dbClaim.getProcCodes().size());
          }

          assertEquals(
              Optional.of((long) (numberOfClaims - 1)), sink.readMaxExistingSequenceNumber());
        });
  }
}
