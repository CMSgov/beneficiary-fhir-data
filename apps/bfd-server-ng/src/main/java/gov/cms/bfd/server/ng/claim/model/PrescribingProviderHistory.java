package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Prescribing Provider History. * */
@Embeddable
@AttributeOverride(
    name = "providerNpiNumber",
    column = @Column(name = "prvdr_prscrbng_prvdr_npi_num"))
@AttributeOverride(
    name = "providerTaxonomyCode",
    column = @Column(name = "prvdr_prscrbng_txnmy_cmpst_cd"))
@AttributeOverride(name = "providerTypeCode", column = @Column(name = "prvdr_prscrbng_type_cd"))
@AttributeOverride(
    name = "providerOscarNumber",
    column = @Column(name = "prvdr_prscrbng_oscar_num"))
@AttributeOverride(name = "providerFirstName", column = @Column(name = "prvdr_prscrbng_1st_name"))
@AttributeOverride(name = "providerMiddleName", column = @Column(name = "prvdr_prscrbng_mdl_name"))
@AttributeOverride(name = "providerLastName", column = @Column(name = "prvdr_prscrbng_last_name"))
@AttributeOverride(name = "providerName", column = @Column(name = "prvdr_prscrbng_name"))
@AttributeOverride(name = "providerLegalName", column = @Column(name = "prvdr_prscrbng_lgl_name"))
@AttributeOverride(
    name = "employerIdNumber",
    column = @Column(name = "prvdr_prscrbng_emplr_id_num"))
public class PrescribingProviderHistory extends ProviderHistoryBase {
  @Override
  protected CareTeamType getCareTeamType() {
    return CareTeamType.PRESCRIBING;
  }
}
