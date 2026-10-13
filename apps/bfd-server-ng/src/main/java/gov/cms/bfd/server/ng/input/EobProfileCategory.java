package gov.cms.bfd.server.ng.input;

import gov.cms.bfd.server.ng.claim.model.common.ClaimSubtype;
import gov.cms.bfd.server.ng.claim.model.common.ClaimType;
import gov.cms.bfd.server.ng.claim.model.common.ClaimTypeCode;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** ExplanationOfBenefit profile categories, shared by the C4BB and CMS profiles. */
@Getter
@AllArgsConstructor
public enum EobProfileCategory {
  /** Represents the Pharmacy category. */
  PHARMACY("Pharmacy", ClaimType.PHARMACY),
  /** Represents the Inpatient Institutional category. */
  INPATIENT_INSTITUTIONAL("Inpatient-Institutional", ClaimType.INSTITUTIONAL),
  /** Represents the Outpatient Institutional category. */
  OUTPATIENT_INSTITUTIONAL("Outpatient-Institutional", ClaimType.INSTITUTIONAL),
  /** Represents the Professional Non-Clinician category. */
  PROFESSIONAL_NONCLINICIAN("Professional-NonClinician", ClaimType.PROFESSIONAL);

  private final String profileName;
  private final ClaimType claimType;

  /**
   * Gets the claim subtypes in this category.
   *
   * @return claim subtypes
   */
  public List<ClaimSubtype> getSubtypes() {
    return Arrays.stream(ClaimSubtype.values())
        .filter(subtype -> subtype.getProfileCategory() == this)
        .toList();
  }

  /**
   * Gets the claim type codes in this category.
   *
   * @return claim type codes
   */
  public List<ClaimTypeCode> getClaimTypeCodes() {
    return getSubtypes().stream()
        .flatMap(
            subtype -> ClaimTypeCode.CLAIM_TYPE_CODE_MAP.getOrDefault(subtype, List.of()).stream())
        .toList();
  }

  /**
   * Gets the categories for a claim type.
   *
   * @param claimType claim type
   * @return categories
   */
  public static Set<EobProfileCategory> fromClaimType(ClaimType claimType) {
    return Arrays.stream(values())
        .filter(category -> category.claimType == claimType)
        .collect(Collectors.toUnmodifiableSet());
  }
}
