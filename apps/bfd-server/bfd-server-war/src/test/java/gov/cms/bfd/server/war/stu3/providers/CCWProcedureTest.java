package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.server.war.commons.CCWProcedure;
import java.time.LocalDate;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.jupiter.api.Test;

/*
 * Unit tests for {@link CCWProcedure}.
 */
public class CCWProcedureTest {

  /**
   * Verifies that {@link CCWProcedure(Object)} works as expected. {@link
   * gov.cms.bfd.server.war.stu3.providers.CCWProcedure}.
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
        String.format("http://hl7.org/fhir/sid/unknown-icd-version/%s", versionIcdUnknown);
    assertMatches(versionIcdUnknown, systemIcdUnknown);

    assertDateNotPresent(versionIcdUnknown, systemIcdUnknown);
  }

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

  /** Verifies that a procedure date isn't present even though there is a procedure code present */
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
