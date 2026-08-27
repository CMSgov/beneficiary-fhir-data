package gov.cms.bfd.server.ng.claim.model.priorauth;

import gov.cms.bfd.server.ng.claim.model.common.CareTeamType;
import gov.cms.bfd.server.ng.claim.model.common.ClaimContext;
import gov.cms.bfd.server.ng.claim.model.common.ProviderFhirHelper;
import gov.cms.bfd.server.ng.claim.model.common.ProviderHistoryBase;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import org.hl7.fhir.r4.model.*;

/** CareTeamComponent for an attending physician. * */
@Embeddable
@AttributeOverride(name = "providerNpiNumber", column = @Column(name = "att_phy_npi"))
@AttributeOverride(name = "providerName", column = @Column(name = "bfd_att_phy_careteam_name"))
@AttributeOverride(name = "npiType", column = @Column(name = "bfd_att_phy_npi_type"))
public class AttendingPhysicianCareTeam extends ProviderHistoryBase {

  @Override
  public CareTeamType getCareTeamType() {
    return CareTeamType.ATTENDING;
  }

  @Override
  public Optional<ExplanationOfBenefit.CareTeamComponent> toFhirCareTeamComponent(
      Integer sequence, Optional<ClaimContext> claimContext) {
    return getProviderNpiNumber()
        .flatMap(
            npi -> {
              var providerReference =
                  ProviderFhirHelper.createProviderReference(npi, getProviderName());
              providerReference.setType(getNpiType().getType());
              return getCareTeamComponent(sequence, providerReference);
            });
  }
}
