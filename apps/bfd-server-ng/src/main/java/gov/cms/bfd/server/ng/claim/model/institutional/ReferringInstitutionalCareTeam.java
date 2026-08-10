package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.claim.model.common.CareTeamBase;
import gov.cms.bfd.server.ng.claim.model.common.CareTeamType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Attending Provider History. * */
@Embeddable
@AttributeOverride(name = "providerNpiNumber", column = @Column(name = "prvdr_rfrg_prvdr_npi_num"))
@AttributeOverride(name = "providerName", column = @Column(name = "bfd_prvdr_rfrg_careteam_name"))
@AttributeOverride(name = "specialtyCode", column = @Column(name = "clm_rfrg_fed_prvdr_spclty_cd"))
public class ReferringInstitutionalCareTeam extends CareTeamBase {

  @Override
  public CareTeamType getCareTeamType() {
    return CareTeamType.REFERRING;
  }
}
