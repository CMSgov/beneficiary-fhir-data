package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.server.war.commons.Diagnosis;
import gov.cms.bfd.server.war.commons.Diagnosis.DiagnosisLabel;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.jupiter.api.Test;

/*
 * Unit tests for {@link diagnosis}.
 */
public class DiagnosisTest {

  /**
   * Verifies that {@link diagnosis(Object)} works as expected. {@link diagnosis}.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void testdiagnosis() throws FHIRException {

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

    assertDiagnosisLabelsMatch();
  }

  static void assertMatches(Character version, String system) {

    Optional<String> code = Optional.of("code");
    Optional<Character> prsntOnAdmsn = Optional.of('Y');

    Optional<Diagnosis> diagnosis = Diagnosis.from(code, Optional.of(version), prsntOnAdmsn);

    assertEquals(prsntOnAdmsn, diagnosis.get().getPresentOnAdmission());
    assertEquals(system, diagnosis.get().getFhirSystem());

    TransformerTestUtils.assertHasCoding(
        system, code.get(), diagnosis.get().toCodeableConcept().getCoding());

    CodeableConcept codeableConcept = new CodeableConcept();
    Coding coding = codeableConcept.addCoding();
    coding.setSystem(system).setCode(code.get());

    assertTrue(diagnosis.get().isContainedIn(codeableConcept));
  }

  static void assertDiagnosisLabelsMatch() {

    Optional<String> code = Optional.of("code");
    Optional<Character> version = Optional.of('9');

    Set<DiagnosisLabel> setAdmitting = new HashSet<>(Arrays.asList(DiagnosisLabel.ADMITTING));
    Set<DiagnosisLabel> setFirstExternal =
        new HashSet<>(Arrays.asList(DiagnosisLabel.FIRSTEXTERNAL));
    Set<DiagnosisLabel> setPrincipal = new HashSet<>(Arrays.asList(DiagnosisLabel.PRINCIPAL));

    Optional<Diagnosis> diagnosis1 = Diagnosis.from(code, version, DiagnosisLabel.ADMITTING);
    assertEquals(setAdmitting, diagnosis1.get().getLabels());

    Optional<Diagnosis> diagnosis2 = Diagnosis.from(code, version, DiagnosisLabel.FIRSTEXTERNAL);
    assertEquals(setFirstExternal, diagnosis2.get().getLabels());

    Optional<Diagnosis> diagnosis3 = Diagnosis.from(code, version, DiagnosisLabel.PRINCIPAL);
    assertEquals(setPrincipal, diagnosis3.get().getLabels());
  }
}
