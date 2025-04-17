package gov.cms.bfd.server.war.stu3.providers;

import static gov.cms.bfd.server.war.commons.IcdCode.CODING_SYSTEM_ICD_UNKNOWN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.server.war.commons.CCWProcedure;
import java.time.LocalDate;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link CCWProcedure}. */
public class CCWProcedureTest {

  /**
   * Verifies that {@link CCWProcedure} works as expected.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void testCCWProcedure() throws FHIRException {

    Character versionIcd9 = '9';
    String systemIcd9 = "http://hl7.org/fhir/sid/icd-9-cm";
    assertMatches(versionIcd9, systemIcd9);

    Character versionIcd10 = '0';
    String systemIcd10 = "http://hl7.org/fhir/sid/icd-10";
    assertMatches(versionIcd10, systemIcd10);

    Character versionIcdUnknown = 'U';
    String systemIcdUnknown =
        String.format(CODING_SYSTEM_ICD_UNKNOWN, versionIcdUnknown.toString().trim());
    assertMatches(versionIcdUnknown, systemIcdUnknown);

    assertDateNotPresent(versionIcdUnknown, systemIcdUnknown);
  }

  /**
   * Assert the version code matches the specified system value when the version is passed to a
   * {@link CCWProcedure}.
   *
   * @param version the version code
   * @param system the expected system value
   */
  static void assertMatches(Character version, String system) {

    Optional<String> code = Optional.of("code");
    Optional<LocalDate> procDate = Optional.of(LocalDate.now());

    Optional<CCWProcedure> diagnosis = CCWProcedure.from(code, Optional.of(version), procDate);

    assertEquals(procDate.get(), diagnosis.get().getProcedureDate().get());
    assertEquals(system, diagnosis.get().getFhirSystem());

    TransformerTestUtils.assertHasCoding(
        system, code.get(), diagnosis.get().toCodeableConcept().getCoding());

    CodeableConcept codeableConcept = new CodeableConcept();
    Coding coding = codeableConcept.addCoding();
    coding.setSystem(system).setCode(code.get());

    assertTrue(diagnosis.get().isContainedIn(codeableConcept));
  }

  /**
   * Verifies that a procedure date isn't present even though there is a procedure code present.
   *
   * @param version the version
   * @param system the system
   */
  static void assertDateNotPresent(Character version, String system) {

    Optional<String> code = Optional.of("code");
    Optional<LocalDate> procDate = Optional.empty();

    Optional<CCWProcedure> diagnosis = CCWProcedure.from(code, Optional.of(version), procDate);

    assertEquals(Optional.empty(), diagnosis.get().getProcedureDate());

    assertEquals(system, diagnosis.get().getFhirSystem());

    TransformerTestUtils.assertHasCoding(
        system, code.get(), diagnosis.get().toCodeableConcept().getCoding());

    CodeableConcept codeableConcept = new CodeableConcept();
    Coding coding = codeableConcept.addCoding();
    coding.setSystem(system).setCode(code.get());

    assertTrue(diagnosis.get().isContainedIn(codeableConcept));
  }
}
