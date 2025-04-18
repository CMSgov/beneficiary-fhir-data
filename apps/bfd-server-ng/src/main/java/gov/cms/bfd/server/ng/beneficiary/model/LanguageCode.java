package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.SystemUrl;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Patient;

@Getter
@AllArgsConstructor
public enum LanguageCode {
  ENGLISH("ENG", "en"),
  SPANISH("SPA", "es"),
  UNKNOWN("", "unknown");

  private final String idrCode;
  private final String ietfCode;

  public static LanguageCode fromIdrCode(String code) {
    return Arrays.stream(values())
        .filter(v -> v.idrCode.equals(code))
        .findFirst()
        .orElse(LanguageCode.UNKNOWN);
  }

  Patient.PatientCommunicationComponent toFhir() {
    return new Patient.PatientCommunicationComponent()
        .setLanguage(
            new CodeableConcept()
                .addCoding(new Coding().setSystem(SystemUrl.IETF_LANGUAGE).setCode(ietfCode)));
  }
}
