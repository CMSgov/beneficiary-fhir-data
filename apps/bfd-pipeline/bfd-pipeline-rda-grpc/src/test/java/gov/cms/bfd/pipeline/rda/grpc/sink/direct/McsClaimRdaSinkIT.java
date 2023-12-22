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
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import gov.cms.bfd.model.rda.entities.RdaMcsDetail;
import gov.cms.bfd.model.rda.entities.RdaMcsDiagnosisCode;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaPipelineTestUtils;
import gov.cms.bfd.pipeline.rda.grpc.source.McsClaimTransformer;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.model.dsl.codegen.library.DataTransformer;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import gov.cms.mpsm.rda.v1.mcs.McsDetail;
import gov.cms.mpsm.rda.v1.mcs.McsDiagnosisCode;
import gov.cms.mpsm.rda.v1.mcs.McsStatusCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Tests the {@link McsClaimRdaSink} with integrated dependencies. */
public class McsClaimRdaSinkIT {

  /** The alphabetical sorting mapper to use for testing. */
  private static final ObjectMapper mapper =
      JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();

  /**
   * Checks if writing valid MCS claim messages results in the entities being persisted to the
   * database with the expected field values.
   *
   * @throws Exception If any unexpected exceptions are thrown.
   */
  @Test
  public void mcsClaim() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        Clock.systemUTC(),
        (appState, transactionManager) -> {
          final LocalDate today = LocalDate.of(2022, 1, 3);
          final Instant now = today.atStartOfDay().toInstant(ZoneOffset.UTC);
          final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
          final RdaMcsClaim claim = new RdaMcsClaim();
          claim.setSequenceNumber(7L);
          claim.setIdrClmHdIcn("3");
          claim.setIdrContrId("c1");
          claim.setIdrHic("hc");
          claim.setIdrClaimType("c");
          claim.setIdrStatusCode("A");
          claim.setMbiRecord(new Mbi(1L, "12345678901", "hash-of-12345678901"));

          final RdaMcsDetail detail = new RdaMcsDetail();
          detail.setIdrClmHdIcn(claim.getIdrClmHdIcn());
          detail.setIdrDtlNumber((short) 0);
          detail.setIdrDtlStatus("P");
          claim.getDetails().add(detail);

          final RdaMcsDiagnosisCode diagCode = new RdaMcsDiagnosisCode();
          diagCode.setIdrClmHdIcn(claim.getIdrClmHdIcn());
          diagCode.setRdaPosition((short) 1);
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
                  .setIdrStatusCodeEnum(McsStatusCode.STATUS_CODE_ACTIVE_A)
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
              new McsClaimTransformer(clock, MbiCache.computedCache(hasher.getConfig()));
          final McsClaimRdaSink sink = new McsClaimRdaSink(appState, transformer, true, 0);
          final String expectedMbiHash = hasher.computeIdentifierHash(claim.getIdrClaimMbi());

          assertEquals(Optional.empty(), sink.readMaxExistingSequenceNumber());

          int count = sink.writeMessage("version", message);
          assertEquals(1, count);

          List<RdaMcsClaim> resultClaims =
              transactionManager.executeFunction(
                  entityManager ->
                      entityManager
                          .createQuery("select c from RdaMcsClaim c", RdaMcsClaim.class)
                          .getResultList());
          assertEquals(1, resultClaims.size());
          RdaMcsClaim resultClaim = resultClaims.get(0);
          assertEquals(Long.valueOf(7), resultClaim.getSequenceNumber());
          assertEquals(claim.getIdrHic(), resultClaim.getIdrHic());
          assertEquals(claim.getIdrClaimMbi(), resultClaim.getIdrClaimMbi());
          assertEquals(expectedMbiHash, resultClaim.getIdrClaimMbiHash());
          assertEquals(claim.getDetails().size(), resultClaim.getDetails().size());
          assertEquals(claim.getDiagCodes().size(), resultClaim.getDiagCodes().size());

          assertEquals(
              Optional.of(claim.getSequenceNumber()), sink.readMaxExistingSequenceNumber());

          Mbi databaseMbiEntity =
              RdaPipelineTestUtils.lookupCachedMbi(
                  transactionManager, claimMessage.getIdrClaimMbi());
          assertNotNull(databaseMbiEntity);
          assertEquals(claim.getIdrClaimMbi(), databaseMbiEntity.getMbi());
          assertEquals(expectedMbiHash, databaseMbiEntity.getHash());
        });
  }

  /**
   * Checks if writing invalid MCS claim messages results in a {@link
   * DataTransformer.TransformationException} being thrown and if {@link MessageError} entities were
   * written out to the database.
   *
   * @throws Exception If any unexpected exceptions were thrown.
   */
  @Test
  public void invalidMcsClaim() throws Exception {
    final String invalidControlId = "invalid_control_id";
    final String invalidDiagIcdType = "invalid_icd_type";

    RdaPipelineTestUtils.runTestWithTemporaryDb(
        Clock.systemUTC(),
        (appState, transactionManager) -> {
          final LocalDate today = LocalDate.of(2022, 1, 3);
          final Instant now = today.atStartOfDay().toInstant(ZoneOffset.UTC);
          final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
          final RdaMcsClaim claim = new RdaMcsClaim();
          claim.setSequenceNumber(7L);
          claim.setIdrClmHdIcn("3");
          claim.setIdrContrId(invalidControlId);
          claim.setIdrHic("hc");
          claim.setIdrClaimType("c");
          claim.setIdrStatusCode("A");
          claim.setMbiRecord(new Mbi(1L, "12345678901", "hash-of-12345678901"));

          final RdaMcsDetail detail = new RdaMcsDetail();
          detail.setIdrClmHdIcn(claim.getIdrClmHdIcn());
          detail.setIdrDtlNumber((short) 0);
          detail.setIdrDtlStatus("P");
          claim.getDetails().add(detail);

          final RdaMcsDiagnosisCode diagCode = new RdaMcsDiagnosisCode();
          diagCode.setIdrClmHdIcn(claim.getIdrClmHdIcn());
          diagCode.setRdaPosition((short) 1);
          diagCode.setIdrDiagIcdType(invalidDiagIcdType);
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
                  .setIdrStatusCodeEnum(McsStatusCode.STATUS_CODE_ACTIVE_A)
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
              new McsClaimTransformer(clock, MbiCache.computedCache(hasher.getConfig()));
          final McsClaimRdaSink sink = new McsClaimRdaSink(appState, transformer, true, 0);

          assertEquals(Optional.empty(), sink.readMaxExistingSequenceNumber());

          assertThrows(ProcessingException.class, () -> sink.writeMessage("version", message));

          List<RdaMcsClaim> resultClaims =
              transactionManager.executeFunction(
                  entityManager ->
                      entityManager
                          .createQuery("select c from RdaMcsClaim c", RdaMcsClaim.class)
                          .getResultList());
          assertEquals(0, resultClaims.size());

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
                        "idrContrId", "invalid length: expected=[1,5] actual=18"),
                    new DataTransformer.ErrorMessage(
                        "diagCodes-0-idrDiagIcdType", "invalid length: expected=[0,1] actual=16"));

            assertEquals(Long.valueOf(7), error.getSequenceNumber());
            assertEquals(MessageError.ClaimType.MCS, error.getClaimType());
            assertEquals(claim.getIdrClmHdIcn(), error.getClaimId());
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
