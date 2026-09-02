package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.claim.model.common.CareTeamBase;
import gov.cms.bfd.server.ng.claim.model.common.CareTeamType;
import gov.cms.bfd.server.ng.claim.model.common.ClaimTypeCode;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;

/** Attending Provider History. * */
@Embeddable
@AttributeOverride(name = "providerNpiNumber", column = @Column(name = "prvdr_oprtg_prvdr_npi_num"))
@AttributeOverride(name = "providerName", column = @Column(name = "bfd_prvdr_oprtg_careteam_name"))
@AttributeOverride(name = "specialtyCode", column = @Column(name = "clm_oprtg_fed_prvdr_spclty_cd"))
@AttributeOverride(name = "npiType", column = @Column(name = "bfd_prvdr_oprtg_npi_type"))
public class OperatingCareTeam extends CareTeamBase {

  @Override
  public CareTeamType getCareTeamType(Optional<ClaimTypeCode> claimTypeCode) {
    return CareTeamType.OPERATING;
  }
}
