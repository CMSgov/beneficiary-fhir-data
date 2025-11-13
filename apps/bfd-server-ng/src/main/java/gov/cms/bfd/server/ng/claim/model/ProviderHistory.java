package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Meta;

/** Provider History table. */
@Entity
@Getter
@Table(name = "provider_history", schema = "idr")
public class ProviderHistory {
  @Id
  @Column(name = "prvdr_sk", insertable = false, updatable = false)
  private long providerSk;

  @Column(name = "prvdr_hstry_efctv_dt")
  private Optional<LocalDate> providerHistoryEffectiveDate;

  @Column(name = "prvdr_txnmy_cmpst_cd")
  private Optional<String> contractName;

  @Column(name = "prvdr_oscar_num")
  private Optional<String> contractNumber;

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

  @Column(name = "prvdr_npi_num")
  private Optional<String> providerNpiNumber;

  @Column(name = "prvdr_lgl_name")
  private Optional<String> providerLegalName;

  @Column(name = "prvdr_emplr_id_num")
  private Optional<String> employerIdNumber;

  @OneToOne(mappedBy = "providerHistory")
  private Claim claim;

  /**
   * Derives the NPI_TYPE based on the presence of the providerLegalName. NPI_TYPE = 1 means the NPI
   * is for an individual (legal name is null/empty). NPI_TYPE = 2 means the NPI is for an
   * organization (legal name is present).
   *
   * @return the NPI type as an Integer (1 or 2).
   */
  @Transient
  public Integer getNpiType() {
    if (providerLegalName.isPresent() && !providerLegalName.get().isBlank()) {
      return 2;
    } else {
      return 1;
    }
  }

  private static final String PROVIDER_PRACTITIONER = "provider-practitioner";
  private static final String PROVIDER_ORG = "provider-org";

  Optional<Practitioner> toPractitionerFhir(
      ClaimTypeCode claimTypeCode, String serviceProviderNpiNumber) {
    if (!claimTypeCode.isBetween(1, 4)) {
      return Optional.empty();
    }

    var practitioner = new Practitioner();
    practitioner.setId(PROVIDER_PRACTITIONER);
    practitioner.setMeta(
        new Meta()
            .addProfile(SystemUrls.PROFILE_CARIN_BB_ORGANIZATION_2_1_0)
            .addProfile(SystemUrls.PROFILE_US_CORE_ORGANIZATION_6_1_0));

    practitioner.addIdentifier(
        new Identifier()
            .setSystem(SystemUrls.NPI)
            .setValue(serviceProviderNpiNumber)
            .setType(
                new CodeableConcept()
                    .addCoding(
                        new Coding()
                            .setSystem(SystemUrls.HL7_IDENTIFIER)
                            .setCode("NPI")
                            .setDisplay("National provider identifier"))));
    var name = new HumanName();
    providerFirstName.ifPresent(name::addGiven);
    providerLastName.ifPresent(name::setFamily);
    name.setUse(HumanName.NameUse.OFFICIAL);
    practitioner.setName(List.of(name));

    return Optional.of(practitioner);
  }

  Optional<Organization> toOrganizationFhir(
      ClaimTypeCode claimTypeCode, String serviceProviderNpiNumber) {
    if (!claimTypeCode.isBetween(1, 4)) {
      return Optional.empty();
    }

    var organization = OrganizationFactory.toFhir();
    organization.setId(PROVIDER_ORG);
    organization.addIdentifier(
        new Identifier()
            .setSystem(SystemUrls.NPI)
            .setValue(serviceProviderNpiNumber)
            .setType(
                new CodeableConcept()
                    .addCoding(
                        new Coding()
                            .setSystem(SystemUrls.HL7_IDENTIFIER)
                            .setCode("NPI")
                            .setDisplay("National provider identifier"))));

    providerLegalName.ifPresent(organization::setName);

    return Optional.of(organization);
  }
}
