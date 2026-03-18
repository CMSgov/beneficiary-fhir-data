package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Attending Provider History. * */
@Embeddable
@AttributeOverride(name = "providerNpiNumber", column = @Column(name = "prvdr_rndrg_prvdr_npi_num"))
@AttributeOverride(name = "providerName", column = @Column(name = "bfd_prvdr_rndrg_careteam_name"))
@AttributeOverride(name = "specialtyCode", column = @Column(name = "clm_rndrg_fed_prvdr_spclty_cd"))
public class RenderingCareTeam extends CareTeamBase {

  @Override
  protected CareTeamType getCareTeamType() {
    return CareTeamType.RENDERING;
  }
}
