package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.MappedSuperclass;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Provider History. */
@Getter
@MappedSuperclass
public abstract class ProviderHistoryBase {
  private Optional<String> providerNpiNumber;
  private Optional<String> providerName;

  /** Represents the enum NPI Type. */
  public enum NpiType {
    /** NPI belongs to an individual. */
    INDIVIDUAL,
    /** NPI belongs to an organization. */
    ORGANIZATION
  }

  protected abstract CareTeamType getCareTeamType();

  Optional<ExplanationOfBenefit.CareTeamComponent> toFhirCareTeamComponent(Integer sequence) {
    if (providerNpiNumber.isEmpty()) {
      return Optional.empty();
    }
    var providerReference =
        ProviderFhirHelper.createProviderReference(providerNpiNumber.get(), providerName);

    return Optional.of(
        new ExplanationOfBenefit.CareTeamComponent()
            .setSequence(sequence)
            .setRole(
                new CodeableConcept(
                    new Coding()
                        .setSystem(SystemUrls.CARIN_CODE_SYSTEM_CLAIM_CARE_TEAM_ROLE)
                        .setCode(getCareTeamType().getRoleCode())
                        .setDisplay(getCareTeamType().getRoleDisplay())))
            .setProvider(providerReference));
  }
}
