package gov.cms.bfd.server.ng.claim.model.common;

import jakarta.persistence.MappedSuperclass;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Provider History. */
@Getter
@MappedSuperclass
public abstract class CareTeamBase extends ProviderHistoryBase {
  private Optional<ProviderSpecialtyCode> specialtyCode;

  @Override
  public Optional<ExplanationOfBenefit.CareTeamComponent> toFhirCareTeamComponent(
      Integer sequence, Optional<ClaimContext> claimContext) {
    var careTeamComponent = super.toFhirCareTeamComponent(sequence, claimContext);
    careTeamComponent.ifPresent(
        ctc -> specialtyCode.ifPresent(sc -> ctc.setQualification(sc.toFhir())));
    return careTeamComponent;
  }
}
