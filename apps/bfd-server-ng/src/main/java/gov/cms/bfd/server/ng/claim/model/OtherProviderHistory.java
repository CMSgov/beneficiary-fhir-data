package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Other Provider History. * */
@Embeddable
@AttributeOverride(name = "providerNpiNumber", column = @Column(name = "prvdr_othr_prvdr_npi_num"))
@AttributeOverride(name = "providerSk", column = @Column(name = "prvdr_othr_sk"))
@AttributeOverride(
    name = "providerTaxonomyCode",
    column = @Column(name = "prvdr_othr_txnmy_cmpst_cd"))
@AttributeOverride(name = "providerTypeCode", column = @Column(name = "prvdr_othr_type_cd"))
@AttributeOverride(name = "providerOscarNumber", column = @Column(name = "prvdr_othr_oscar_num"))
@AttributeOverride(name = "providerFirstName", column = @Column(name = "prvdr_othr_1st_name"))
@AttributeOverride(name = "providerMiddleName", column = @Column(name = "prvdr_othr_mdl_name"))
@AttributeOverride(name = "providerLastName", column = @Column(name = "prvdr_othr_last_name"))
@AttributeOverride(name = "providerName", column = @Column(name = "prvdr_othr_name"))
@AttributeOverride(name = "providerLegalName", column = @Column(name = "prvdr_othr_lgl_name"))
@AttributeOverride(name = "employerIdNumber", column = @Column(name = "prvdr_othr_emplr_id_num"))
public class OtherProviderHistory extends ProviderHistoryBase {
  @Override
  protected CareTeamType getCareTeamType() {
    return CareTeamType.OTHER;
  }
}
