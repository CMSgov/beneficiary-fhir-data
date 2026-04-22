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
  Optional<ExplanationOfBenefit.CareTeamComponent> toFhirCareTeamComponent(
      Integer sequence, Optional<ClaimContext> claimContext) {
    var careTeamComponent = super.toFhirCareTeamComponent(sequence, claimContext);

    careTeamComponent.ifPresent(
        ctc ->
            specialtyCode.ifPresent(
                sc -> {
                  ctc.setQualification(sc.toFhir());

                  // Determine NPI Type based on provider specialty code
                  var npiType = sc.getNpiType();
                  if (npiType != NpiType.UNKNOWN) {
                    var reference = ctc.getProvider();
                    reference.setType(npiType.getType());
                    ctc.setProvider(reference);
                  }
                }));

    return careTeamComponent;
  }
}
