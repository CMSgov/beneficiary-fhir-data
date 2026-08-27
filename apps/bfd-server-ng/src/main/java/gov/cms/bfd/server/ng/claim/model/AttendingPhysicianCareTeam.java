package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.claim.model.common.CareTeamType;
import gov.cms.bfd.server.ng.claim.model.common.ClaimTypeCode;
import gov.cms.bfd.server.ng.claim.model.common.ProviderFhirHelper;
import gov.cms.bfd.server.ng.claim.model.common.ProviderHistoryBase;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
@AttributeOverride(name = "providerNpiNumber", column = @Column(name = "att_phy_npi"))
@AttributeOverride(name = "providerName", column = @Column(name = "bfd_att_phy_careteam_name"))
@AttributeOverride(name = "npiType", column = @Column(name = "bfd_att_phy_npi_type"))
class AttendingPhysicianCareTeam extends ProviderHistoryBase {

  @Override
  public CareTeamType getCareTeamType(Optional<ClaimTypeCode> claimTypeCode) {
    return CareTeamType.ATTENDING;
  }

  @Override
  public Optional<ExplanationOfBenefit.CareTeamComponent> toFhirCareTeamComponent(
      Integer sequence, Optional<ClaimTypeCode> claimTypeCode) {
    return getProviderNpiNumber()
        .flatMap(
            npi -> {
              var providerReference =
                  ProviderFhirHelper.createProviderReference(npi, getProviderName());
              providerReference.setType(getNpiType().getType());
              return getCareTeamComponent(sequence, providerReference, claimTypeCode);
            });
  }
}
