package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Attending Provider History. * */
@Embeddable
@AttributeOverride(name = "providerNpiNumber", column = @Column(name = "prvdr_othr_prvdr_npi_num"))
@AttributeOverride(name = "providerName", column = @Column(name = "bfd_prvdr_othr_careteam_name"))
public class OtherProfessionalSharedSystemsCareTeam extends ProviderHistoryBase {

  @Override
  protected CareTeamType getCareTeamType() {
    return CareTeamType.SUPERVISOR;
  }
}
