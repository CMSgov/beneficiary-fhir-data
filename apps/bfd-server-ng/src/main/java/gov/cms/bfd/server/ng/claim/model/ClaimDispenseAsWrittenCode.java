package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/**
 * Dispense as Written Product Selection Codes.
 *
 * <p>Valid values are 0 through 9.
 */
@Getter
@AllArgsConstructor
public enum ClaimDispenseAsWrittenCode {
  /** 0: No product selection indicated. */
  _0("0"),
  /** 1: Supplemental drugs (reported by plans that provide Enhanced Alternative coverage). */
  _1("1"),
  /** 2: Substitution allowed – Patient requested that brand be dispensed. */
  _2("2"),
  /** 3: Substitution allowed – Pharmacist selected product dispensed. */
  _3("3"),
  /** 4: Substitution allowed - Generic not in stock. */
  _4("4"),
  /** 5: Substitution allowed – Brand drug dispensed as generic. */
  _5("5"),
  /** 6: Override. */
  _6("6"),
  /** 7: Substitution not allowed – Brand drug mandated by law. */
  _7("7"),
  /** 8: Substitution allowed – Generic drug not available in marketplace. */
  _8("8"),
  /** 9: Other. */
  _9("9");

  private final String code;

  /**
   * Tries to find the enum by its database code value. Returns empty if it does not match exactly.
   *
   * @param code string code
   * @return The matched enum, or empty if none matches
   */
  public static Optional<ClaimDispenseAsWrittenCode> tryFromCode(String code) {
    if (code == null) {
      return Optional.empty();
    }
    return Stream.of(ClaimDispenseAsWrittenCode.values())
        .filter(e -> e.getCode().equals(code))
        .findFirst();
  }

  /**
   * Transforms this enum into a FHIR {@link ExplanationOfBenefit.SupportingInformationComponent}.
   *
   * @param supportingInfoFactory factory used to create the component
   * @return the FHIR component
   */
  public ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    var supportingInfo = supportingInfoFactory.createSupportingInfo();
    supportingInfo.setCategory(CarinSupportingInfoCategory.DAW_CODE.toFhir());
    supportingInfo.setCode(
        new CodeableConcept(
            new Coding().setSystem(SystemUrls.HL7_CLAIM_DAW_PROD_SELECT_CODE).setCode(code)));
    return supportingInfo;
  }
}
