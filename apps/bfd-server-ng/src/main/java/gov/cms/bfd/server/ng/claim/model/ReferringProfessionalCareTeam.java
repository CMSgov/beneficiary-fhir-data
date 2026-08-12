package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;

/** Attending Provider History. * */
@Embeddable
@AttributeOverride(name = "providerNpiNumber", column = @Column(name = "prvdr_rfrg_prvdr_npi_num"))
@AttributeOverride(name = "providerName", column = @Column(name = "bfd_prvdr_rfrg_careteam_name"))
public class ReferringProfessionalCareTeam extends ProviderHistoryBase {
  @Column(name = "bfd_prvdr_rfrg_npi_type")
  private Optional<Integer> npiType;

  @Override
  protected CareTeamType getCareTeamType() {
    return CareTeamType.REFERRING;
  }

  @Override
  protected NpiType getNpiType() {
    return NpiType.fromNpiTypeCode(npiType);
  }
}
