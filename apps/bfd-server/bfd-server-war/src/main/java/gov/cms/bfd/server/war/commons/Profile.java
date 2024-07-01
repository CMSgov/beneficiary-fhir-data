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
      ProfileConstants.C4BB_EOB_INPATIENT_PROFILE_URL,
      ProfileConstants.C4BB_EOB_OUTPATIENT_PROFILE_URL,
      ProfileConstants.C4BB_EOB_PHARMACY_PROFILE_URL,
      ProfileConstants.C4BB_EOB_NONCLINICIAN_PROFILE_URL,
      C4BB_VERSION_SUFFIX),
  /** Represents the CARIN Digital Insurance Card Implementation Guide. */
  C4DIC(
      ProfileConstants.C4DIC_PATIENT_URL,
      ProfileConstants.C4DIC_ORGANIZATION_URL,
      ProfileConstants.C4DIC_COVERAGE_URL,
      null,
      null,
      null,
      null,
      C4DIC_VERSION_SUFFIX);

  /** Patient resource URL. */
  private final String patientUrl;

  /** Organization resource URL. */
  private final String organizationUrl;

  /** Coverage resource URL. */
  private final String coverageUrl;

  /** EOB inpatient URL. */
  private final String eobInpatientUrl;

  /** EOB outpatient URL. */
  private final String eobOutpatientUrl;

  /** EOB Pharmacy URL. */
  private final String eobPharmacyUrl;

  /** EOB Nonclinician URL. */
  private final String eobNonclinicianUrl;

  /** Profile version suffix. */
  private final String versionSuffix;

  /**
   * Creates a new {@link Profile}.
   *
   * @param patientUrl Patient resource URL
   * @param organizationUrl Organization resource URL
   * @param coverageUrl Coverage resource URL
   * @param eobInpatientUrl EOB inpatient URL
   * @param eobOutpatientUrl EOB outpatient URL
   * @param eobPharmacyUrl EOB pharmacy URL
   * @param eobNonclinicianUrl EOB nonclinician URL
   * @param versionSuffix Profile version suffix
   */
  Profile(
      String patientUrl,
      String organizationUrl,
      String coverageUrl,
      String eobInpatientUrl,
      String eobOutpatientUrl,
      String eobPharmacyUrl,
      String eobNonclinicianUrl,
      String versionSuffix) {
    this.patientUrl = requireNonNull(patientUrl);
    this.organizationUrl = requireNonNull(organizationUrl);
    this.coverageUrl = requireNonNull(coverageUrl);

    // These aren't used for all profiles
    this.eobInpatientUrl = eobInpatientUrl;
    this.eobOutpatientUrl = eobOutpatientUrl;
    this.eobPharmacyUrl = eobPharmacyUrl;
    this.eobNonclinicianUrl = eobNonclinicianUrl;

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

  /**
   * Returns the EOB inpatient resource URL with the version appended.
   *
   * @return versioned EOB inpatient URL
   */
  public String getVersionedEobInpatientUrl() {
    return this.eobInpatientUrl + this.versionSuffix;
  }

  /**
   * Returns the EOB outpatient resource URL with the version appended.
   *
   * @return versioned EOB outpatient URL
   */
  public String getVersionedEobOutpatientUrl() {
    return this.eobOutpatientUrl + this.versionSuffix;
  }

  /**
   * Returns the EOB pharmacy resource URL with the version appended.
   *
   * @return versioned EOB pharmacy URL
   */
  public String getVersionedEobPharmacyUrl() {
    return this.eobPharmacyUrl + this.versionSuffix;
  }

  /**
   * Returns the EOB nonclinician resource URL with the version appended.
   *
   * @return versioned Coverage URL
   */
  public String getVersionedEobNonclinicianUrl() {
    return this.eobNonclinicianUrl + this.versionSuffix;
  }
}
