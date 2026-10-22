package gov.cms.bfd.server.ng.claim;

import static gov.cms.bfd.server.ng.claim.EobProfileCategory.INPATIENT_INSTITUTIONAL;
import static gov.cms.bfd.server.ng.claim.EobProfileCategory.OUTPATIENT_INSTITUTIONAL;
import static gov.cms.bfd.server.ng.claim.EobProfileCategory.PHARMACY;
import static gov.cms.bfd.server.ng.claim.EobProfileCategory.PROFESSIONAL_NONCLINICIAN;
import static gov.cms.bfd.server.ng.claim.EobProfileType.BASIS;
import static gov.cms.bfd.server.ng.claim.EobProfileType.CMS;
import static gov.cms.bfd.server.ng.claim.EobProfileType.REGULAR;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Supported ExplanationOfBenefit profiles: one per category and claim profile. */
@Getter
@AllArgsConstructor
@SuppressWarnings("checkstyle:JavadocVariable")
public enum EobProfile {

  // CMS
  PHARMACY_CMS(PHARMACY, CMS, SystemUrls.PROFILE_CMS_EOB_PHARMACY, ""),
  INPATIENT_INSTITUTIONAL_CMS(
      INPATIENT_INSTITUTIONAL, CMS, SystemUrls.PROFILE_CMS_EOB_INPATIENT_INSTITUTIONAL, ""),
  OUTPATIENT_INSTITUTIONAL_CMS(
      OUTPATIENT_INSTITUTIONAL, CMS, SystemUrls.PROFILE_CMS_EOB_OUTPATIENT_INSTITUTIONAL, ""),
  PROFESSIONAL_NONCLINICIAN_CMS(
      PROFESSIONAL_NONCLINICIAN, CMS, SystemUrls.PROFILE_CMS_EOB_PROFESSIONAL_NONCLINICIAN, ""),

  // C4BB - Regular
  PHARMACY_REGULAR(
      PHARMACY, REGULAR, SystemUrls.PROFILE_C4BB_EOB_PHARMACY, SystemUrls.PROFILE_C4BB_VERSION),
  INPATIENT_INSTITUTIONAL_REGULAR(
      INPATIENT_INSTITUTIONAL,
      REGULAR,
      SystemUrls.PROFILE_C4BB_EOB_INPATIENT_INSTITUTIONAL,
      SystemUrls.PROFILE_C4BB_VERSION),
  OUTPATIENT_INSTITUTIONAL_REGULAR(
      OUTPATIENT_INSTITUTIONAL,
      REGULAR,
      SystemUrls.PROFILE_C4BB_EOB_OUTPATIENT_INSTITUTIONAL,
      SystemUrls.PROFILE_C4BB_VERSION),
  PROFESSIONAL_NONCLINICIAN_REGULAR(
      PROFESSIONAL_NONCLINICIAN,
      REGULAR,
      SystemUrls.PROFILE_C4BB_EOB_PROFESSIONAL_NONCLINICIAN,
      SystemUrls.PROFILE_C4BB_VERSION),

  // C4BB - Basis
  PHARMACY_BASIS(
      PHARMACY, BASIS, SystemUrls.PROFILE_C4BB_EOB_PHARMACY_BASIS, SystemUrls.PROFILE_C4BB_VERSION),
  INPATIENT_INSTITUTIONAL_BASIS(
      INPATIENT_INSTITUTIONAL,
      BASIS,
      SystemUrls.PROFILE_C4BB_EOB_INPATIENT_INSTITUTIONAL_BASIS,
      SystemUrls.PROFILE_C4BB_VERSION),
  OUTPATIENT_INSTITUTIONAL_BASIS(
      OUTPATIENT_INSTITUTIONAL,
      BASIS,
      SystemUrls.PROFILE_C4BB_EOB_OUTPATIENT_INSTITUTIONAL_BASIS,
      SystemUrls.PROFILE_C4BB_VERSION),
  PROFESSIONAL_NONCLINICIAN_BASIS(
      PROFESSIONAL_NONCLINICIAN,
      BASIS,
      SystemUrls.PROFILE_C4BB_EOB_PROFESSIONAL_NONCLINICIAN_BASIS,
      SystemUrls.PROFILE_C4BB_VERSION);

  private final EobProfileCategory category;
  private final EobProfileType profileType;
  private final String canonicalUrl;
  private final String version;

  /**
   * Gets the profile for a category and claim profile.
   *
   * @param category profile category
   * @param profileType claim profile
   * @return profile
   */
  public static EobProfile of(EobProfileCategory category, EobProfileType profileType) {
    return Arrays.stream(values())
        .filter(p -> p.category == category && p.profileType == profileType)
        .findFirst()
        .orElseThrow();
  }

  /**
   * Finds the profile matching a _profile value, versioned or unversioned.
   *
   * @param uri _profile value
   * @return profile, if supported
   */
  public static Optional<EobProfile> fromUri(String uri) {
    return Arrays.stream(values()).filter(p -> p.getAllowedUris().contains(uri)).findFirst();
  }

  /**
   * Gets the canonical URL with its version, if it has one. CMS does not have one at the moment.
   *
   * @return versioned canonical URL
   */
  public String getVersionedCanonicalUrl() {
    return version.isEmpty() ? canonicalUrl : canonicalUrl + "|" + version;
  }

  /**
   * Gets the _profile values accepted for this profile.
   *
   * @return accepted URIs
   */
  public List<String> getAllowedUris() {
    // Distinct because CMS doesn't currently have a version
    return Stream.of(canonicalUrl, getVersionedCanonicalUrl()).distinct().toList();
  }

  /**
   * Gets the meta.profile values for this profile.
   *
   * @return meta.profile values
   */
  public List<String> getMetaProfiles() {
    // CMS is a superset of C4BB, so it also conforms to the C4BB profile
    return profileType == CMS
        ? List.of(of(category, REGULAR).getVersionedCanonicalUrl(), getVersionedCanonicalUrl())
        : List.of(getVersionedCanonicalUrl());
  }
}
