package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Attending Provider History. * */
@Embeddable
@AttributeOverride(name = "providerNpiNumber", column = @Column(name = "prvdr_atndg_prvdr_npi_num"))
@AttributeOverride(name = "providerSk", column = @Column(name = "prvdr_atndg_sk"))
@AttributeOverride(
    name = "providerTaxonomyCode",
    column = @Column(name = "prvdr_atndg_txnmy_cmpst_cd"))
@AttributeOverride(name = "providerTypeCode", column = @Column(name = "prvdr_atndg_type_cd"))
@AttributeOverride(name = "providerOscarNumber", column = @Column(name = "prvdr_atndg_oscar_num"))
@AttributeOverride(name = "providerFirstName", column = @Column(name = "prvdr_atndg_1st_name"))
@AttributeOverride(name = "providerMiddleName", column = @Column(name = "prvdr_atndg_mdl_name"))
@AttributeOverride(name = "providerLastName", column = @Column(name = "prvdr_atndg_last_name"))
@AttributeOverride(name = "providerName", column = @Column(name = "prvdr_atndg_name"))
@AttributeOverride(name = "providerLegalName", column = @Column(name = "prvdr_atndg_lgl_name"))
@AttributeOverride(name = "employerIdNumber", column = @Column(name = "prvdr_atndg_emplr_id_num"))
public class AttendingProviderHistory extends ProviderHistoryBase {

  @Override
  protected CareTeamType getCareTeamType() {
    return CareTeamType.ATTENDING;
  }
}
