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

  @Override
  Optional<ExplanationOfBenefit.CareTeamComponent> toFhirCareTeamComponent(Integer sequence) {
    var careTeamComponent = super.toFhirCareTeamComponent(sequence);

    // If present, set the qualification
    careTeamComponent.ifPresent(
        ctc -> specialtyCode.ifPresent(sc -> ctc.setQualification(sc.toFhir())));

    return careTeamComponent;
  }
}
