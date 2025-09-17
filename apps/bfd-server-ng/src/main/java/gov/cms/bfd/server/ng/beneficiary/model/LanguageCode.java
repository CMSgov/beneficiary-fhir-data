package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Patient;

/** Beneficiary language code. */
@Getter
@AllArgsConstructor
public enum LanguageCode {
  /** English language code. */
  ENGLISH("ENG", "en"),
  /** Spanish language code. */
  SPANISH("SPA", "es"),
  /** Unknown language code. */
  UNKNOWN("", "unknown");

  private final String idrCode;
  private final String ietfCode;

  /**
   * Creates an instance from the IDR representation of a language code.
   *
   * @param idrCode IDR language code
   * @return {@link LanguageCode} enum
   */
  public static LanguageCode fromIdrCode(String idrCode) {
    return Arrays.stream(values())
        .filter(v -> v.idrCode.equals(idrCode))
        .findFirst()
        .orElse(LanguageCode.UNKNOWN);
  }

  Patient.PatientCommunicationComponent toFhir() {
    return new Patient.PatientCommunicationComponent()
        .setLanguage(
            new CodeableConcept()
                .addCoding(new Coding().setSystem(SystemUrls.IETF_LANGUAGE).setCode(ietfCode)));
  }
}
