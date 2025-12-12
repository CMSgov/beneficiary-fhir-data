package gov.cms.bfd.server.ng.claim.model;

import static gov.cms.bfd.server.ng.claim.model.ClaimTypeCode.CLAIM_TYPE_CODE_MAP;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Map;
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
  CARRIER("carrier", SystemUrls.CARIN_STRUCTURE_DEFINITION_PROFESSIONAL, ClaimType.PROFESSIONAL),

  /** Represents DME type. */
  DME("dme", SystemUrls.CARIN_STRUCTURE_DEFINITION_PROFESSIONAL, ClaimType.PROFESSIONAL),

  /** Represents HHA type. */
  HHA(
      "hha",
      SystemUrls.CARIN_STRUCTURE_DEFINITION_INPATIENT_INSTITUTIONAL,
      ClaimType.INSTITUTIONAL),

  /** Represents HOSPICE type. */
  HOSPICE(
      "hospice",
      SystemUrls.CARIN_STRUCTURE_DEFINITION_INPATIENT_INSTITUTIONAL,
      ClaimType.INSTITUTIONAL),

  /** Represents INPATIENT type. */
  INPATIENT(
      "inpatient",
      SystemUrls.CARIN_STRUCTURE_DEFINITION_INPATIENT_INSTITUTIONAL,
      ClaimType.INSTITUTIONAL),

  /** Represents OUTPATIENT type. */
  OUTPATIENT(
      "outpatient",
      SystemUrls.CARIN_STRUCTURE_DEFINITION_OUTPATIENT_INSTITUTIONAL,
      ClaimType.INSTITUTIONAL),

  /** Represents PDE type. */
  PDE("pde", SystemUrls.CARIN_STRUCTURE_DEFINITION_PHARMACY, ClaimType.PHARMACY),

  /** Represents SNF type. */
  SNF(
      "snf",
      SystemUrls.CARIN_STRUCTURE_DEFINITION_INPATIENT_INSTITUTIONAL,
      ClaimType.INSTITUTIONAL);

  private final String code;
  private final String systemUrl;
  private final ClaimType claimType;

  CodeableConcept toFhir() {
    return new CodeableConcept(
        new Coding().setSystem(SystemUrls.CARIN_CLAIM_SUBTYPE).setCode(code));
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

  /**
   * Gets Grouped ClaimSubtype from a Claim Type Code.
   *
   * @param claimTypeCode code
   * @return ClaimSubtype
   */
  public static Optional<ClaimSubtype> getGroupedClaimSubtype(int claimTypeCode) {
    return subtypeFor(claimTypeCode).flatMap(ClaimSubtype::grouped);
  }

  /**
   * Gets the ClaimSubType for a ClaimTypeCode.
   *
   * @param claimTypeCode code
   * @return ClaimSubtype
   */
  public static Optional<ClaimSubtype> subtypeFor(int claimTypeCode) {
    var claimCode = ClaimTypeCode.fromCode(claimTypeCode);
    return CLAIM_TYPE_CODE_MAP.entrySet().stream()
        .filter(e -> e.getValue().contains(claimCode))
        .map(Map.Entry::getKey)
        .findFirst();
  }
}
