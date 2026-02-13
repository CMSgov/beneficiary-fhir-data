package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SequenceGenerator;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;

/** Refering Professional Provider History. * */
@Embeddable
@AttributeOverride(name = "providerNpiNumber", column = @Column(name = "prvdr_rfrg_prvdr_npi_num"))
@AttributeOverride(name = "providerSk", column = @Column(name = "prvdr_rfrg_sk"))
@AttributeOverride(
    name = "providerTaxonomyCode",
    column = @Column(name = "prvdr_rfrg_txnmy_cmpst_cd"))
@AttributeOverride(name = "providerTypeCode", column = @Column(name = "prvdr_rfrg_type_cd"))
@AttributeOverride(name = "providerOscarNumber", column = @Column(name = "prvdr_rfrg_oscar_num"))
@AttributeOverride(name = "providerFirstName", column = @Column(name = "prvdr_rfrg_1st_name"))
@AttributeOverride(name = "providerMiddleName", column = @Column(name = "prvdr_rfrg_mdl_name"))
@AttributeOverride(name = "providerLastName", column = @Column(name = "prvdr_rfrg_last_name"))
@AttributeOverride(name = "providerName", column = @Column(name = "prvdr_rfrg_name"))
@AttributeOverride(name = "providerLegalName", column = @Column(name = "prvdr_rfrg_lgl_name"))
@AttributeOverride(name = "employerIdNumber", column = @Column(name = "prvdr_rfrg_emplr_id_num"))
public class ReferringProfessionalProviderHistory extends ProviderHistoryBase {
  @Column(name = "clm_rfrg_prvdr_pin_num")
  private Optional<String> referringProviderPinNumber;

  @Override
  protected CareTeamType getCareTeamType() {
    return CareTeamType.REFERRING;
  }

  @Override
  Optional<CareTeamType.CareTeamComponents> toFhirCareTeamComponent(
      SequenceGenerator sequenceGenerator) {
    return super.toFhirCareTeamComponent(sequenceGenerator, referringProviderPinNumber);
  }
}
