package gov.cms.bfd.server.war.commons;

import static gov.cms.bfd.server.war.commons.ProfileConstants.C4BB_VERSION_SUFFIX;
import static gov.cms.bfd.server.war.commons.ProfileConstants.C4DIC_VERSION_SUFFIX;
import static java.util.Objects.requireNonNull;

import java.util.EnumSet;

/** Represents the supported CARIN profiles. */
public enum Profile {

  /** Represents the CARIN Blue Button Implementation Guide. */
  C4BB(
      ProfileConstants.C4BB_PATIENT_URL,
      ProfileConstants.C4BB_ORGANIZATION_URL,
      ProfileConstants.C4BB_COVERAGE_URL,
      C4BB_VERSION_SUFFIX),
  /** Represents the CARIN Digital Insurance Card Implementation Guide. */
  C4DIC(
      ProfileConstants.C4DIC_PATIENT_URL,
      ProfileConstants.C4DIC_ORGANIZATION_URL,
      ProfileConstants.C4DIC_COVERAGE_URL,
      C4DIC_VERSION_SUFFIX);

  /** Patient resource URL. */
  private final String patientUrl;

  /** Organization resource URL. */
  private final String organizationUrl;

  /** Coverage resource URL. */
  private final String coverageUrl;

  /** Profile version suffix. */
  private final String versionSuffix;

  /**
   * Creates a new {@link Profile}.
   *
   * @param patientUrl Patient resource URL
   * @param organizationUrl Organization resource URL
   * @param coverageUrl Coverage resource URl
   * @param versionSuffix Profile version suffix
   */
  Profile(String patientUrl, String organizationUrl, String coverageUrl, String versionSuffix) {
    this.patientUrl = requireNonNull(patientUrl);
    this.organizationUrl = requireNonNull(organizationUrl);
    this.coverageUrl = requireNonNull(coverageUrl);
    this.versionSuffix = requireNonNull(versionSuffix);
  }

  /**
   * Returns the set of enabled profiles based on the C4DIC feature flag.
   *
   * @param c4DicEnabled C4DIC feature flag
   * @return Set of enabled profiles
   */
  public static EnumSet<Profile> getEnabledProfiles(boolean c4DicEnabled) {
    return c4DicEnabled ? EnumSet.of(Profile.C4BB, Profile.C4DIC) : EnumSet.of(Profile.C4BB);
  }

  /**
   * Returns the Organization resource URL with the version appended.
   *
   * @return versioned Organization URL
   */
  public String getVersionedOrganizationUrl() {
    return this.organizationUrl + this.versionSuffix;
  }

  /**
   * Returns the Patient resource URL with the version appended.
   *
   * @return versioned Patient URL
   */
  public String getVersionedPatientUrl() {
    return this.patientUrl + this.versionSuffix;
  }

  /**
   * Returns the Coverage resource URL with the version appended.
   *
   * @return versioned Coverage URL
   */
  public String getVersionedCoverageUrl() {
    return this.coverageUrl + this.versionSuffix;
  }
}
