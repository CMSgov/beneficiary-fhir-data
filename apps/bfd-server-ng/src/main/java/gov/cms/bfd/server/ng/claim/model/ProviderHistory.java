package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDate;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;

/** Provider History table. */
@Entity
@Getter
@Table(name = "provider_history", schema = "idr")
public class ProviderHistory {
  @Id
  @Column(name = "prvdr_npi_num")
  private String providerNpiNumber;

  @Column(name = "prvdr_sk", insertable = false, updatable = false)
  private long providerSk;

  @Column(name = "prvdr_hstry_efctv_dt")
  private Optional<LocalDate> providerHistoryEffectiveDate;

  @Column(name = "prvdr_txnmy_cmpst_cd")
  private Optional<String> providerTaxonomyCode;

  @Column(name = "prvdr_type_cd")
  private Optional<String> providerTypeCode;

  @Column(name = "prvdr_oscar_num")
  private Optional<String> providerOscarNumber;

  @Column(name = "prvdr_1st_name")
  private Optional<String> providerFirstName;

  @Column(name = "prvdr_mdl_name")
  private Optional<String> providerMiddleName;

  @Column(name = "prvdr_last_name")
  private Optional<String> providerLastName;

  @Column(name = "prvdr_name")
  private Optional<String> providerName;

  @Column(name = "prvdr_hstry_obslt_dt")
  private Optional<LocalDate> providerObsoleteDate;

  @Column(name = "prvdr_lgl_name")
  private Optional<String> providerLegalName;

  @Column(name = "prvdr_emplr_id_num")
  private Optional<String> employerIdNumber;

  /** Represents the enum NPI Type. */
  public enum NPI_TYPE {
    /** NPI belongs to an individual. */
    INDIVIDUAL(1),
    /** NPI belongs to an organization. */
    ORGANIZATION(2);

    private final int value;

    NPI_TYPE(int value) {
      this.value = value;
    }
  }

  /**
   * Derives the NPI_TYPE based on the presence of the providerLegalName. NPI_TYPE = INDIVIDUAL
   * means the NPI is for an individual (legal name is null/empty). NPI_TYPE = ORGANIZATION means
   * the NPI is for an organization (legal name is present).
   *
   * @return the NPI type
   */
  @Transient
  public NPI_TYPE getNpiType() {
    if (providerLegalName.isPresent() && !providerLegalName.get().isBlank()) {
      return NPI_TYPE.ORGANIZATION;
    } else {
      return NPI_TYPE.INDIVIDUAL;
    }
  }

  private static final String PROVIDER_PRACTITIONER = "provider-practitioner";
  private static final String PROVIDER_ORG = "provider-org";

  Optional<Practitioner> toPractitionerFhir(
      ClaimTypeCode claimTypeCode, String serviceProviderNpiNumber) {
    if (!claimTypeCode.isBetween(1, 4)) {
      return Optional.empty();
    }

    var practitioner =
        ProviderFhirHelper.createPractitioner(
            PROVIDER_PRACTITIONER, serviceProviderNpiNumber, providerFirstName, providerLastName);
    return Optional.of(practitioner);
  }

  Optional<Organization> toOrganizationFhir(
      ClaimTypeCode claimTypeCode, String serviceProviderNpiNumber) {
    if (!claimTypeCode.isBetween(1, 4)) {
      return Optional.empty();
    }
    var organization =
        ProviderFhirHelper.createOrganizationWithNpi(
            PROVIDER_ORG, serviceProviderNpiNumber, providerLegalName);
    return Optional.of(organization);
  }
}
