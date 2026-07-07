package gov.cms.bfd.server.ng.claim.model;

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
  protected CareTeamType getCareTeamType() {
    return CareTeamType.RENDERING;
  }

  @Override
  @Transient
  protected ProviderHistoryBase.NpiType getNpiType() {
    if (npiType.isPresent() && npiType.get().equals(2)) {
      return ProviderHistoryBase.NpiType.ORGANIZATION;
    } else {
      return ProviderHistoryBase.NpiType.INDIVIDUAL;
    }
  }

  @Override
  Optional<ExplanationOfBenefit.CareTeamComponent> toFhirCareTeamComponent(
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
