package gov.cms.bfd.server.ng.claim.model.professional;

import static gov.cms.bfd.server.ng.claim.model.common.ClaimSubtype.DME;

import gov.cms.bfd.server.ng.claim.model.common.CareTeamType;
import gov.cms.bfd.server.ng.claim.model.common.ClaimTypeCode;
import gov.cms.bfd.server.ng.claim.model.common.ProviderHistoryBase;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;

/** Other Shared Systems Provider History. * */
@Embeddable
@AttributeOverride(name = "providerNpiNumber", column = @Column(name = "prvdr_othr_prvdr_npi_num"))
@AttributeOverride(name = "providerName", column = @Column(name = "bfd_prvdr_othr_careteam_name"))
@AttributeOverride(name = "npiType", column = @Column(name = "bfd_prvdr_othr_npi_type"))
public class OtherProfessionalSharedSystemsCareTeam extends ProviderHistoryBase {

  @Override
  public CareTeamType getCareTeamType(Optional<ClaimTypeCode> claimTypeCode) {
    return claimTypeCode
        .filter(typeCode -> !typeCode.isClaimSubtype(DME))
        .map(_ -> CareTeamType.SUPERVISOR)
        .orElse(CareTeamType.OTHER);
  }
}
