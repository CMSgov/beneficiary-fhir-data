package gov.cms.bfd.server.ng.claim.model.priorauth;

import gov.cms.bfd.server.ng.claim.model.common.CareTeamType;
import gov.cms.bfd.server.ng.claim.model.common.ClaimContext;
import gov.cms.bfd.server.ng.claim.model.common.ProviderFhirHelper;
import gov.cms.bfd.server.ng.claim.model.common.ProviderHistoryBase;
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
public class PriorAuthRenderingCareTeam extends ProviderHistoryBase {
  @Column(name = "bfd_render_npi_type")
  private Optional<Integer> npiType;

  @Override
  public CareTeamType getCareTeamType() {
    return CareTeamType.RENDERING;
  }

  @Override
  @Transient
  public ProviderHistoryBase.NpiType getNpiType() {
    return ProviderHistoryBase.NpiType.fromNpiTypeCode(npiType);
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
