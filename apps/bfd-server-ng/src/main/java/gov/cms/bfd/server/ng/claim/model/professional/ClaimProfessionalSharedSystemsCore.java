package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.claim.model.common.ClaimTypeCode;
import gov.cms.bfd.server.ng.util.SequenceGenerator;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Core logic and fields for Professional-SharedSystems as an embeddable. */
@Embeddable
@Getter
@SuppressWarnings("checkstyle:MissingJavadocMethod")
public class ClaimProfessionalSharedSystemsCore {

  @Embedded private CareTeamOtherProfessionalSharedSystems otherProviderHistory;

  public void addOtherCareTeam(
      ExplanationOfBenefit eob, SequenceGenerator sequenceGenerator, ClaimTypeCode code) {
    otherProviderHistory
        .toFhirCareTeamComponent(sequenceGenerator.next(), Optional.of(code))
        .ifPresent(eob::addCareTeam);
  }
}
