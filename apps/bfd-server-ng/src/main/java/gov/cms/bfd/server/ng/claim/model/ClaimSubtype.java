package gov.cms.bfd.server.ng.claim.model;

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
      SystemUrls.CARIN_STRUCTURE_DEFINITION_PROFESSIONAL,
      ClaimType.PROFESSIONAL,
      "Professional physician/supplier claim"),

  /** Represents DME type. */
  DME(
      "dme",
      SystemUrls.CARIN_STRUCTURE_DEFINITION_PROFESSIONAL,
      ClaimType.PROFESSIONAL,
      "Durable Medical Equipment Claim"),

  /** Represents HHA type. */
  HHA(
      "hha",
      SystemUrls.CARIN_STRUCTURE_DEFINITION_INPATIENT_INSTITUTIONAL,
      ClaimType.INSTITUTIONAL,
      "Home Health Services"),

  /** Represents HOSPICE type. */
  HOSPICE(
      "hospice",
      SystemUrls.CARIN_STRUCTURE_DEFINITION_INPATIENT_INSTITUTIONAL,
      ClaimType.INSTITUTIONAL,
      "Hospice Care"),

  /** Represents INPATIENT type. */
  INPATIENT(
      "inpatient",
      SystemUrls.CARIN_STRUCTURE_DEFINITION_INPATIENT_INSTITUTIONAL,
      ClaimType.INSTITUTIONAL,
      "Inpatient Hospital Care"),

  /** Represents OUTPATIENT type. */
  OUTPATIENT(
      "outpatient",
      SystemUrls.CARIN_STRUCTURE_DEFINITION_OUTPATIENT_INSTITUTIONAL,
      ClaimType.INSTITUTIONAL,
      "Outpatient Hospital Care"),

  /** Represents PDE type. */
  PDE("pde", SystemUrls.CARIN_STRUCTURE_DEFINITION_PHARMACY, ClaimType.PHARMACY, "Drug Plan Event"),

  /** Represents SNF type. */
  SNF(
      "snf",
      SystemUrls.CARIN_STRUCTURE_DEFINITION_INPATIENT_INSTITUTIONAL,
      ClaimType.INSTITUTIONAL,
      "Skilled Nursing Facility Care");

  private final String code;
  private final String systemUrl;
  private final ClaimType claimType;
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
   * Returns the normalized grouping of this {@link ClaimSubtype}. INPATIENT, HHA, HOSPICE, and SNF
   * are grouped under {@code INPATIENT}, and OUTPATIENT maps to itself. CARRIER, DME, and PDE do
   * not belong to a grouped category and therefore return an empty result.
   *
   * @return ClaimSubtype
   */
  public Optional<ClaimSubtype> grouped() {
    return switch (this) {
      case INPATIENT, HHA, HOSPICE, SNF -> Optional.of(INPATIENT);
      case OUTPATIENT -> Optional.of(OUTPATIENT);
      case CARRIER, DME, PDE -> Optional.empty();
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
