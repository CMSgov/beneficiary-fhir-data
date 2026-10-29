package gov.cms.bfd.server.ng.claim.model.common;

import gov.cms.bfd.server.ng.claim.EobProfileCategory;
import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;

/** Clam Sub type. */
@Getter
@AllArgsConstructor
public enum ClaimSubtype {
  /** Represents CARRIER type. */
  CARRIER(
      "carrier",
      ClaimType.PROFESSIONAL,
      EobProfileCategory.PROFESSIONAL_NONCLINICIAN,
      "Professional Physician/Supplier Claim"),

  /** Represents DME type. */
  DME(
      "dme",
      ClaimType.PROFESSIONAL,
      EobProfileCategory.PROFESSIONAL_NONCLINICIAN,
      "Durable Medical Equipment Claim"),

  /** Represents HHA type. */
  HHA(
      "hha",
      ClaimType.INSTITUTIONAL,
      EobProfileCategory.INPATIENT_INSTITUTIONAL,
      "Home Health Services"),

  /** Represents HOSPICE type. */
  HOSPICE(
      "hospice",
      ClaimType.INSTITUTIONAL,
      EobProfileCategory.INPATIENT_INSTITUTIONAL,
      "Hospice Care"),

  /** Represents INPATIENT type. */
  INPATIENT(
      "inpatient",
      ClaimType.INSTITUTIONAL,
      EobProfileCategory.INPATIENT_INSTITUTIONAL,
      "Inpatient Hospital Care"),

  /** Represents OUTPATIENT type. */
  OUTPATIENT(
      "outpatient",
      ClaimType.INSTITUTIONAL,
      EobProfileCategory.OUTPATIENT_INSTITUTIONAL,
      "Outpatient Hospital Care"),

  /** Represents PDE type. */
  PDE("pde", ClaimType.PHARMACY, EobProfileCategory.PHARMACY, "Drug Plan Event"),

  /** Represents SNF type. */
  SNF(
      "snf",
      ClaimType.INSTITUTIONAL,
      EobProfileCategory.INPATIENT_INSTITUTIONAL,
      "Skilled Nursing Facility Care");

  private final String code;
  private final ClaimType claimType;
  private final EobProfileCategory profileCategory;
  private final String eobTypeDisplay;

  CodeableConcept toFhir() {
    return new CodeableConcept(
        new Coding().setSystem(SystemUrls.CARIN_CLAIM_SUBTYPE).setCode(code));
  }

  Coding toFhirEobType() {
    return new Coding()
        .setSystem(SystemUrls.BLUE_BUTTON_EOB_TYPE_CODE)
        .setCode(toString())
        .setDisplay(eobTypeDisplay);
  }

  /**
   * Returns the normalized grouping of this {@link ClaimSubtype}, based on its profile category.
   * Inpatient institutional subtypes (INPATIENT, HHA, HOSPICE, SNF) are grouped under {@code
   * INPATIENT}, and OUTPATIENT maps to itself. Other subtypes do not belong to a grouped category
   * and return an empty result.
   *
   * @return ClaimSubtype
   */
  public Optional<ClaimSubtype> grouped() {
    return switch (profileCategory) {
      case INPATIENT_INSTITUTIONAL -> Optional.of(INPATIENT);
      case OUTPATIENT_INSTITUTIONAL -> Optional.of(OUTPATIENT);
      case PHARMACY, PROFESSIONAL_NONCLINICIAN -> Optional.empty();
    };
  }

  /**
   * Gets ClaimSubtype from code.
   *
   * @param code code
   * @return ClaimSubtype
   */
  public static Optional<ClaimSubtype> fromCode(String code) {
    return Arrays.stream(values()).filter(v -> v.code.equalsIgnoreCase(code)).findFirst();
  }
}
