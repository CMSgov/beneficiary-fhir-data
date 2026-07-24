package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.claim.model.common.CareTeamType;
import gov.cms.bfd.server.ng.claim.model.common.ProviderHistoryBase;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Attending Provider History. NOTE - this is not currently in use anywhere (as far as I can tell)
 */
@Embeddable
@AttributeOverride(name = "providerNpiNumber", column = @Column(name = "prvdr_othr_prvdr_npi_num"))
@AttributeOverride(name = "providerName", column = @Column(name = "bfd_prvdr_othr_careteam_name"))
public class OtherProfessionalCareTeam extends ProviderHistoryBase {

  @Override
  public CareTeamType getCareTeamType() {
    return CareTeamType.OTHER;
  }
}
