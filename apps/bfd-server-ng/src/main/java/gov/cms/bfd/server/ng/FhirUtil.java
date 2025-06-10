package gov.cms.bfd.server.ng;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;

public class FhirUtil {
  private FhirUtil() {}

  public static CodeableConcept checkDataAbsent(CodeableConcept codeableConcept) {
    if (codeableConcept.getCoding().isEmpty()) {
      return codeableConcept.addCoding(
          new Coding()
              .setSystem(SystemUrls.HL7_DATA_ABSENT)
              .setCode("not-applicable")
              .setDisplay("Not Applicable"));
    }
  }
}
