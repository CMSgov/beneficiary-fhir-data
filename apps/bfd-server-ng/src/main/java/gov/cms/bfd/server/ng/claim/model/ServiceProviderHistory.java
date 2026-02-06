package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Service Provider History. * */
@Embeddable
@AttributeOverride(name = "providerNpiNumber", column = @Column(name = "prvdr_srvc_prvdr_npi_num"))
@AttributeOverride(
    name = "providerTaxonomyCode",
    column = @Column(name = "prvdr_srvc_txnmy_cmpst_cd"))
@AttributeOverride(name = "providerTypeCode", column = @Column(name = "prvdr_srvc_type_cd"))
@AttributeOverride(name = "providerOscarNumber", column = @Column(name = "prvdr_srvc_oscar_num"))
@AttributeOverride(name = "providerFirstName", column = @Column(name = "prvdr_srvc_1st_name"))
@AttributeOverride(name = "providerMiddleName", column = @Column(name = "prvdr_srvc_mdl_name"))
@AttributeOverride(name = "providerLastName", column = @Column(name = "prvdr_srvc_last_name"))
@AttributeOverride(name = "providerName", column = @Column(name = "prvdr_srvc_name"))
@AttributeOverride(name = "providerLegalName", column = @Column(name = "prvdr_srvc_lgl_name"))
@AttributeOverride(name = "employerIdNumber", column = @Column(name = "prvdr_srvc_emplr_id_num"))
public class ServiceProviderHistory extends ProviderHistoryBase {

  @Override
  protected CareTeamType getCareTeamType() {
    return CareTeamType.SERVICE;
  }
}
