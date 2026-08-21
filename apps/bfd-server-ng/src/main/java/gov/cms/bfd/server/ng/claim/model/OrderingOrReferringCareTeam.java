package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.claim.model.common.CareTeamType;
import gov.cms.bfd.server.ng.claim.model.common.ClaimContext;
import gov.cms.bfd.server.ng.claim.model.common.ProviderFhirHelper;
import gov.cms.bfd.server.ng.claim.model.common.ProviderHistoryBase;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Ordering or Referring Provider History. * */
@Embeddable
@AttributeOverride(name = "providerNpiNumber", column = @Column(name = "order_refer_npi"))
@AttributeOverride(name = "providerName", column = @Column(name = "bfd_order_refer_careteam_name"))
@AttributeOverride(name = "npiType", column = @Column(name = "bfd_order_refer_npi_type"))
class OrderingOrReferringCareTeam extends ProviderHistoryBase {

  @Override
  public CareTeamType getCareTeamType() {
    return CareTeamType.REFERRING;
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
