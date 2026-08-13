package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.claim.model.common.*;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Ordering or Referring Provider History. * */
@Embeddable
@AttributeOverride(name = "providerNpiNumber", column = @Column(name = "order_refer_npi"))
@AttributeOverride(name = "providerName", column = @Column(name = "bfd_order_refer_careteam_name"))
class OrderingOrReferringCareTeam extends ProviderHistoryBase {

  @Column(name = "bfd_order_refer_npi_type")
  private Optional<Integer> npiType;

  @Override
  public CareTeamType getCareTeamType(Optional<ClaimTypeCode> claimTypeCode) {
    return CareTeamType.REFERRING;
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
