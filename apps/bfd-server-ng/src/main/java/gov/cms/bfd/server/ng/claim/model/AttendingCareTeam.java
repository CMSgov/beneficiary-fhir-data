package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Attending Provider History. * */
@Embeddable
@AttributeOverride(name = "providerNpiNumber", column = @Column(name = "prvdr_atndg_prvdr_npi_num"))
@AttributeOverride(name = "providerName", column = @Column(name = "bfd_prvdr_atndg_careteam_name"))
@AttributeOverride(name = "specialtyCode", column = @Column(name = "clm_atndg_fed_prvdr_spclty_cd"))
public class AttendingCareTeam extends CareTeamBase {

  @Override
  protected CareTeamType getCareTeamType() {
    return CareTeamType.ATTENDING;
  }
}
