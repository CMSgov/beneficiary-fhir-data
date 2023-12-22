package gov.cms.bfd.pipeline.rda.grpc.sink.direct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.model.rda.MessageError;
import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaFissDiagnosisCode;
import gov.cms.bfd.model.rda.entities.RdaFissProcCode;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaPipelineTestUtils;
import gov.cms.bfd.pipeline.rda.grpc.source.FissClaimTransformer;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.model.dsl.codegen.library.DataTransformer;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.fiss.FissClaimStatus;
import gov.cms.mpsm.rda.v1.fiss.FissClaimTypeIndicator;
import gov.cms.mpsm.rda.v1.fiss.FissDiagnosisCode;
import gov.cms.mpsm.rda.v1.fiss.FissProcedureCode;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Tests the {@link FissClaimRdaSink} with integrated dependencies. */
public class FissClaimRdaSinkIT {

  /** The alphabetical sorting mapper to use for testing. */
  private static final ObjectMapper mapper =
      JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();

  /**
   * Checks if writing valid FISS claim messages results in the entities being persisted to the
   * database with the expected field values.
   *
   * @throws Exception If any unexpected exceptions are thrown.
   */
  @Test
  public void fissClaim() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        Clock.systemUTC(),
        (appState, transactionManager) -> {
          final LocalDate today = LocalDate.of(2022, 1, 3);
          final Instant now = today.atStartOfDay().toInstant(ZoneOffset.UTC);
          final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
          final RdaFissClaim claim = new RdaFissClaim();
          claim.setSequenceNumber(3L);
          claim.setClaimId("1id");
          claim.setDcn("1");
          claim.setIntermediaryNb("12345");
          claim.setHicNo("h1");
          claim.setClmTypInd("1");
          claim.setCurrStatus('T');
          claim.setCurrLoc1('A');
          claim.setCurrLoc2("1A");
          claim.setPracLocCity("city name can be very long indeed");
          claim.setMbiRecord(new Mbi(1L, "12345678901", "hash-of-12345678901"));

          final RdaFissProcCode procCode0 = new RdaFissProcCode();
          procCode0.setClaimId(claim.getClaimId());
          procCode0.setRdaPosition((short) 1);
          procCode0.setProcCode("P");
          procCode0.setProcFlag("F");
          procCode0.setProcDate(today);
          claim.getProcCodes().add(procCode0);

          final RdaFissDiagnosisCode diagCode0 = new RdaFissDiagnosisCode();
          diagCode0.setClaimId(claim.getClaimId());
          diagCode0.setRdaPosition((short) 1);
          diagCode0.setDiagCd2("cd2");
          diagCode0.setDiagPoaInd("Q");
          claim.getDiagCodes().add(diagCode0);

          final FissProcedureCode procCodeMessage =
              FissProcedureCode.newBuilder()
                  .setProcCd("P")
                  .setRdaPosition(1)
                  .setProcFlag("F")
                  .setProcDt("2022-01-03")
                  .build();

          final FissDiagnosisCode diagCodeMessage =
              FissDiagnosisCode.newBuilder()
                  .setDiagCd2("cd2")
                  .setRdaPosition(1)
                  .setDiagPoaIndUnrecognized("Q")
                  .build();

          final FissClaim claimMessage =
              FissClaim.newBuilder()
                  .setRdaClaimKey(claim.getClaimId())
                  .setDcn(claim.getDcn())
                  .setIntermediaryNb(claim.getIntermediaryNb())
                  .setHicNo(claim.getHicNo())
                  .setClmTypIndEnum(FissClaimTypeIndicator.CLAIM_TYPE_INPATIENT)
                  .setCurrStatusEnum(FissClaimStatus.CLAIM_STATUS_RTP)
                  .setCurrLoc1Unrecognized(String.valueOf(claim.getCurrLoc1()))
                  .setCurrLoc2Unrecognized(claim.getCurrLoc2())
                  .setPracLocCity("city name can be very long indeed")
                  .addFissProcCodes(0, procCodeMessage)
                  .addFissDiagCodes(0, diagCodeMessage)
                  .setMbi(claim.getMbi())
                  .build();

          final FissClaimChange message =
              FissClaimChange.newBuilder()
                  .setSeq(claim.getSequenceNumber())
                  .setDcn(claim.getDcn())
                  .setRdaClaimKey(claim.getClaimId())
                  .setIntermediaryNb(claim.getIntermediaryNb())
                  .setClaim(claimMessage)
                  .build();

          final IdHasher defaultIdHasher = new IdHasher(new IdHasher.Config(1, "notarealpepper"));
          final FissClaimTransformer transformer =
              new FissClaimTransformer(clock, MbiCache.computedCache(defaultIdHasher.getConfig()));
          final FissClaimRdaSink sink = new FissClaimRdaSink(appState, transformer, true, 0);
          final String expectedMbiHash = defaultIdHasher.computeIdentifierHash(claim.getMbi());

          assertEquals(Optional.empty(), sink.readMaxExistingSequenceNumber());

          int count = sink.writeMessage("version", message);
          assertEquals(1, count);

          List<RdaFissClaim> claims =
              transactionManager.executeFunction(
                  entityManager ->
                      entityManager
                          .createQuery("select c from RdaFissClaim c", RdaFissClaim.class)
                          .getResultList());
          assertEquals(1, claims.size());
          RdaFissClaim resultClaim = claims.get(0);
          assertEquals(Long.valueOf(3), resultClaim.getSequenceNumber());
          assertEquals(claim.getHicNo(), resultClaim.getHicNo());
          assertEquals(claim.getPracLocCity(), resultClaim.getPracLocCity());
          assertEquals(claim.getMbi(), resultClaim.getMbi());
          assertEquals(expectedMbiHash, resultClaim.getMbiHash());
          assertEquals(claim.getProcCodes().size(), resultClaim.getProcCodes().size());
          assertEquals(claim.getDiagCodes().size(), resultClaim.getDiagCodes().size());

          assertEquals(
              Optional.of(claim.getSequenceNumber()), sink.readMaxExistingSequenceNumber());

          Mbi databaseMbiEntity =
              RdaPipelineTestUtils.lookupCachedMbi(transactionManager, claimMessage.getMbi());
          assertNotNull(databaseMbiEntity);
          assertEquals(claim.getMbi(), databaseMbiEntity.getMbi());
          assertEquals(expectedMbiHash, databaseMbiEntity.getHash());
        });
  }

  /**
   * Checks if writing invalid FISS claim messages results in a {@link
   * DataTransformer.TransformationException} being thrown and if {@link MessageError} entities were
   * written out to the database.
   *
   * @throws Exception If any unexpected exceptions were thrown.
   */
  @Test
  public void invalidFissClaim() throws Exception {
    final String invalidLoc2 = "1A11111111";
    final String invalidDateFormat = "invalid_date";

    RdaPipelineTestUtils.runTestWithTemporaryDb(
        Clock.systemUTC(),
        (appState, transactionManager) -> {
          final LocalDate today = LocalDate.of(2022, 1, 3);
          final Instant now = today.atStartOfDay().toInstant(ZoneOffset.UTC);
          final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
          final RdaFissClaim claim = new RdaFissClaim();
          claim.setSequenceNumber(3L);
          claim.setClaimId("1id");
          claim.setDcn("1");
          claim.setIntermediaryNb("12345");
          claim.setHicNo("h1");
          claim.setCurrStatus('T');
          claim.setCurrLoc1('A');
          claim.setCurrLoc2(invalidLoc2);
          claim.setPracLocCity("city name can be very long indeed");
          claim.setMbiRecord(new Mbi(1L, "12345678901", "hash-of-12345678901"));

          final RdaFissProcCode procCode0 = new RdaFissProcCode();
          procCode0.setClaimId(claim.getClaimId());
          procCode0.setRdaPosition((short) 1);
          procCode0.setProcCode("P");
          procCode0.setProcFlag("F");
          procCode0.setProcDate(today);
          claim.getProcCodes().add(procCode0);

          final RdaFissDiagnosisCode diagCode0 = new RdaFissDiagnosisCode();
          diagCode0.setClaimId(claim.getClaimId());
          diagCode0.setRdaPosition((short) 1);
          diagCode0.setDiagCd2("cd2");
          diagCode0.setDiagPoaInd("Q");
          claim.getDiagCodes().add(diagCode0);

          final FissProcedureCode procCodeMessage =
              FissProcedureCode.newBuilder()
                  .setProcCd("P")
                  .setRdaPosition(1)
                  .setProcFlag("F")
                  .setProcDt(invalidDateFormat)
                  .build();

          final FissDiagnosisCode diagCodeMessage =
              FissDiagnosisCode.newBuilder()
                  .setDiagCd2("cd2")
                  .setRdaPosition(1)
                  .setDiagPoaIndUnrecognized("Q")
                  .build();

          final FissClaim claimMessage =
              FissClaim.newBuilder()
                  .setRdaClaimKey(claim.getClaimId())
                  .setDcn(claim.getDcn())
                  .setIntermediaryNb("12345")
                  .setHicNo(claim.getHicNo())
                  .setCurrStatusEnum(FissClaimStatus.CLAIM_STATUS_RTP)
                  .setCurrLoc1Unrecognized(String.valueOf(claim.getCurrLoc1()))
                  .setCurrLoc2Unrecognized(claim.getCurrLoc2())
                  .setPracLocCity("city name can be very long indeed")
                  .addFissProcCodes(0, procCodeMessage)
                  .addFissDiagCodes(0, diagCodeMessage)
                  .setMbi(claim.getMbi())
                  .setClmTypIndEnum(FissClaimTypeIndicator.CLAIM_TYPE_INPATIENT)
                  .build();

          final FissClaimChange message =
              FissClaimChange.newBuilder()
                  .setSeq(claim.getSequenceNumber())
                  .setDcn(claim.getDcn())
                  .setRdaClaimKey(claim.getClaimId())
                  .setIntermediaryNb(claim.getIntermediaryNb())
                  .setClaim(claimMessage)
                  .build();

          final IdHasher defaultIdHasher = new IdHasher(new IdHasher.Config(1, "notarealpepper"));
          final FissClaimTransformer transformer =
              new FissClaimTransformer(clock, MbiCache.computedCache(defaultIdHasher.getConfig()));
          final FissClaimRdaSink sink = new FissClaimRdaSink(appState, transformer, true, 0);

          assertEquals(Optional.empty(), sink.readMaxExistingSequenceNumber());

          assertThrows(ProcessingException.class, () -> sink.writeMessage("version", message));

          List<RdaFissClaim> claims =
              transactionManager.executeFunction(
                  entityManager ->
                      entityManager
                          .createQuery("select c from RdaFissClaim c", RdaFissClaim.class)
                          .getResultList());
          assertEquals(0, claims.size());

          List<MessageError> errors =
              transactionManager.executeFunction(
                  entityManager ->
                      entityManager
                          .createQuery("select e from MessageError e", MessageError.class)
                          .getResultList());
          assertEquals(1, errors.size());

          for (MessageError error : errors) {
            List<DataTransformer.ErrorMessage> expectedTransformErrors =
                List.of(
                    new DataTransformer.ErrorMessage(
                        "currLoc2", "invalid length: expected=[0,5] actual=10"),
                    new DataTransformer.ErrorMessage("procCodes-0-procDate", "invalid date"));

            assertEquals(Long.valueOf(3), error.getSequenceNumber());
            assertEquals(MessageError.ClaimType.FISS, error.getClaimType());
            String decodedClaimId =
                new String(
                    Base64.getUrlDecoder()
                        .decode(error.getClaimId().getBytes(StandardCharsets.UTF_8)));
            assertEquals(claim.getClaimId(), decodedClaimId);

            // Errors occur misordered, so check the expected pieces of the error exist in the full
            // error list
            for (DataTransformer.ErrorMessage expectedError : expectedTransformErrors) {
              assertTrue(error.getErrors().contains(expectedError.getFieldName()));
              assertTrue(error.getErrors().contains(expectedError.getErrorMessage()));
            }
          }
        });
  }
}
