package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.claim.model.common.CareTeamType;
import gov.cms.bfd.server.ng.claim.model.common.ClaimTypeCode;
import gov.cms.bfd.server.ng.claim.model.common.ProviderHistoryBase;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;

/** Service Provider History. * */
@Embeddable
@AttributeOverride(name = "providerNpiNumber", column = @Column(name = "prvdr_srvc_prvdr_npi_num"))
@AttributeOverride(name = "providerName", column = @Column(name = "bfd_prvdr_srvc_careteam_name"))
@AttributeOverride(name = "npiType", column = @Column(name = "bfd_prvdr_srvc_npi_type"))
public class ServiceCareTeam extends ProviderHistoryBase {

  @Override
  public CareTeamType getCareTeamType(Optional<ClaimTypeCode> claimTypeCode) {
    return CareTeamType.SERVICE;
  }
}
