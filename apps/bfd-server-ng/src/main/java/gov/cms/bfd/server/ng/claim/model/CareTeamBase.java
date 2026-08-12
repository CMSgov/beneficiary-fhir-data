package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.MappedSuperclass;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Provider History. */
@Getter
@MappedSuperclass
public abstract class CareTeamBase extends ProviderHistoryBase {
  private Optional<ProviderSpecialtyCode> specialtyCode;
  private Optional<Integer> npiType;

  @Override
  protected NpiType getNpiType() {
    return NpiType.fromNpiTypeCode(npiType);
  }

  @Override
  Optional<ExplanationOfBenefit.CareTeamComponent> toFhirCareTeamComponent(
      Integer sequence, Optional<ClaimContext> claimContext) {
    var careTeamComponent = super.toFhirCareTeamComponent(sequence, claimContext);
    careTeamComponent.ifPresent(
        ctc -> specialtyCode.ifPresent(sc -> ctc.setQualification(sc.toFhir())));
    return careTeamComponent;
  }
}
