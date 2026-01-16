package gov.cms.bfd.server.war.r4.providers.pac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import gov.cms.bfd.model.rda.entities.RdaMcsDetail;
import gov.cms.bfd.model.rda.entities.RdaMcsDiagnosisCode;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** MCS claim transformer tests. */
public class McsClaimTransformerV2Test {
  /** Test diagnosis code 1. */
  private static final String DIAG_CODE1 = "DIAG_CODE1";

  /** Test diagnosis code 2. */
  private static final String DIAG_CODE2 = "DIAG_CODE2";

  /** Test diagnosis code 3. */
  private static final String DIAG_CODE3 = "DIAG_CODE3";

  /**
   * Test arguments.
   *
   * @return test arguments.
   */
  public static Stream<Arguments> diagnosisCodeTest() {
    return Stream.of(
        arguments(
            "Same diagnosis codes and primary diagnosis code",
            List.of("0:" + DIAG_CODE1, "0:" + DIAG_CODE2),
            List.of("0:" + DIAG_CODE1, "0:" + DIAG_CODE2),
            List.of(DIAG_CODE1, DIAG_CODE2),
            2),
        arguments(
            "One different primary diagnosis code",
            List.of("0:" + DIAG_CODE1, "0:" + DIAG_CODE2),
            List.of("0:" + DIAG_CODE3, "0:" + DIAG_CODE2),
            List.of(DIAG_CODE1, DIAG_CODE2, DIAG_CODE3),
            3),
        arguments(
            "Two different primary diagnosis codes",
            List.of("0:" + DIAG_CODE1),
            List.of("0:" + DIAG_CODE2, "0:" + DIAG_CODE3),
            List.of(DIAG_CODE1, DIAG_CODE2, DIAG_CODE3),
            3),
        arguments(
            "Null diagnosis code",
            new ArrayList<String>() {
              {
                add(null);
                add("0:" + DIAG_CODE1);
              }
            },
            List.of(),
            List.of(DIAG_CODE1),
            1),
        arguments(
            "Null primary diagnosis code",
            List.of("0:" + DIAG_CODE1),
            new ArrayList<String>() {
              {
                add(null);
              }
            },
            List.of(DIAG_CODE1),
            1));
  }

  /**
   * Tests that the diagnosis codes are set correctly.
   *
   * @param testName test name
   * @param diagCodes diagnosis codes
   * @param primaryDiagCodes primary diagnosis codes
   * @param expectedCodes expected codes
   * @param numberOfRecords number of expected records
   */
  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource
  public void diagnosisCodeTest(
      String testName,
      List<String> diagCodes,
      List<String> primaryDiagCodes,
      List<String> expectedCodes,
      int numberOfRecords) {

    RdaMcsClaim entity = new RdaMcsClaim();
    SecurityTagManager securityTagManager = mock(SecurityTagManager.class);

    entity.setLastUpdated(Instant.ofEpochMilli(1));

    Set<RdaMcsDiagnosisCode> diagnoses =
        IntStream.range(0, diagCodes.size())
            .mapToObj(
                i -> {
                  RdaMcsDiagnosisCode diagCode = new RdaMcsDiagnosisCode();
                  diagCode.setRdaPosition((short) (i + 1));
                  if (diagCodes.get(i) != null) {
                    String[] dx = diagCodes.get(i).split(":");
                    diagCode.setIdrDiagIcdType(dx[0]);
                    diagCode.setIdrDiagCode(dx[1]);
                  }

                  return diagCode;
                })
            .collect(Collectors.toSet());

    List<RdaMcsDetail> procedures =
        IntStream.range(0, primaryDiagCodes.size())
            .mapToObj(
                i -> {
                  RdaMcsDetail procCode = new RdaMcsDetail();
                  procCode.setIdrDtlToDate(LocalDate.EPOCH);
                  procCode.setIdrDtlNumber((short) (i + 1));
                  procCode.setIdrProcCode("testProc");

                  if (primaryDiagCodes.get(i) != null) {
                    String[] dx = primaryDiagCodes.get(i).split(":");
                    procCode.setIdrDtlDiagIcdType(dx[0]);
                    procCode.setIdrDtlPrimaryDiagCode(dx[1]);
                  }

                  return procCode;
                })
            .toList();

    entity.setDiagCodes(diagnoses);
    entity.setDetails(new HashSet<>(procedures));

    McsClaimTransformerV2 mcsClaimTransformerV2 =
        new McsClaimTransformerV2(new MetricRegistry(), securityTagManager, false);
    Set<String> securityTags = new HashSet<>();

    Claim claim =
        mcsClaimTransformerV2.transform(new ClaimWithSecurityTags<>(entity, securityTags));
    assertEquals(numberOfRecords, claim.getDiagnosis().size());

    for (int i = 0; i < claim.getDiagnosis().size(); i++) {
      Claim.DiagnosisComponent component = claim.getDiagnosis().get(i);
      CodeableConcept diagnosis = (CodeableConcept) component.getDiagnosis();
      assertEquals(1, diagnosis.getCoding().size());

      assertEquals(expectedCodes.get(i), diagnosis.getCoding().getFirst().getCode());
      assertEquals(i + 1, component.getSequence());
    }
  }
}
