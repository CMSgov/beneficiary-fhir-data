package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;

/** Maps provider identifier qualifier codes to FHIR. */
@Getter
@AllArgsConstructor
@SuppressWarnings("java:S115")
public enum ProviderIdQualifierCode {

  /** 01 - NPI Number. */
  _01("01", "NPI Number"),

  /** 06 - UPIN. */
  _06("06", "UPIN"),

  /** 07 - NCPDP Number. */
  _07("07", "NCPDP Number"),

  /** 08 - State License Number. */
  _08("08", "State License Number"),

  /** 11 - TIN. */
  _11("11", "TIN"),

  /** 12 - DEA. */
  _12("12", "DEA"),

  /** 99 - Other mandatory for Standard Data Format. */
  _99("99", "Other mandatory for Standard Data Format");

  private final String code;
  private final String display;

  static ProviderIdQualifierCode fromCode(String code) {
    return Arrays.stream(values())
        .collect(Collectors.toMap(ProviderIdQualifierCode::getCode, e -> e))
        .get(code);
  }

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return provider id qualifier code
   */
  public static Optional<ProviderIdQualifierCode> fromCodeOptional(String code) {
    return Optional.ofNullable(fromCode(code));
  }

  CodeableConcept toFhir() {
    return new CodeableConcept(
        new Coding().setSystem(SystemUrls.CMS_PRVDR_ID_QLFYR_CD).setCode(code).setDisplay(display));
  }
}
