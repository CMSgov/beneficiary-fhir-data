package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.claim.model.common.*;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Prior Auth Rendering Provider History. * */
@Embeddable
@AttributeOverride(name = "providerNpiNumber", column = @Column(name = "render_npi"))
@AttributeOverride(name = "providerName", column = @Column(name = "bfd_render_careteam_name"))
class PriorAuthRenderingCareTeam extends ProviderHistoryBase {
  @Column(name = "bfd_render_npi_type")
  private Optional<Integer> npiType;

  @Override
  public CareTeamType getCareTeamType(Optional<ClaimTypeCode> claimTypeCode) {
    return CareTeamType.RENDERING;
  }

  @Override
  @Transient
  public ProviderHistoryBase.NpiType getNpiType() {
    return ProviderHistoryBase.NpiType.fromNpiTypeCode(npiType);
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
