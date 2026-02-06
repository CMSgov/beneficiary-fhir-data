package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Rendering Provider History. * */
@Embeddable
@AttributeOverride(
    name = "providerNpiNumber",
    column = @Column(name = "prvdr_rndrng_prvdr_npi_num"))
@AttributeOverride(name = "providerSk", column = @Column(name = "prvdr_rndrng_sk"))
@AttributeOverride(
    name = "providerTaxonomyCode",
    column = @Column(name = "prvdr_rndrng_txnmy_cmpst_cd"))
@AttributeOverride(name = "providerTypeCode", column = @Column(name = "prvdr_rndrng_type_cd"))
@AttributeOverride(name = "providerOscarNumber", column = @Column(name = "prvdr_rndrng_oscar_num"))
@AttributeOverride(name = "providerFirstName", column = @Column(name = "prvdr_rndrng_1st_name"))
@AttributeOverride(name = "providerMiddleName", column = @Column(name = "prvdr_rndrng_mdl_name"))
@AttributeOverride(name = "providerLastName", column = @Column(name = "prvdr_rndrng_last_name"))
@AttributeOverride(name = "providerName", column = @Column(name = "prvdr_rndrng_name"))
@AttributeOverride(name = "providerLegalName", column = @Column(name = "prvdr_rndrng_lgl_name"))
@AttributeOverride(name = "employerIdNumber", column = @Column(name = "prvdr_rndrng_emplr_id_num"))
public class RenderingProviderHistory extends ProviderHistoryBase {
  @Override
  protected CareTeamType getCareTeamType() {
    return CareTeamType.RENDERING;
  }
}
