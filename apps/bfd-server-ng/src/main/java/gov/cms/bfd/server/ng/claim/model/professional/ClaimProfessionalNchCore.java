package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.claim.model.common.ClaimQueryCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimRecordType;
import gov.cms.bfd.server.ng.claim.model.common.ClaimTypeCode;
import gov.cms.bfd.server.ng.claim.model.institutional.ServiceCareTeam;
import gov.cms.bfd.server.ng.util.SequenceGenerator;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Common properties and logic for Professional-NCH claims in an embeddable. */
@Embeddable
@Getter
@SuppressWarnings("checkstyle:MissingJavadocMethod")
public class ClaimProfessionalNchCore {

  @Column(name = "clm_query_cd")
  private Optional<ClaimQueryCode> claimQueryCode;

  @AttributeOverride(name = "claimRecordTypeCode", column = @Column(name = "clm_nrln_ric_cd"))
  @Embedded
  private ClaimRecordType claimRecordType;

  @Embedded private ServiceCareTeam serviceProviderHistory;

  // region Common Logic

  public void addServiceCareTeam(
      ExplanationOfBenefit eob, SequenceGenerator sequenceGenerator, ClaimTypeCode claimTypeCode) {
    serviceProviderHistory
        .toFhirCareTeamComponent(sequenceGenerator.next(), Optional.of(claimTypeCode))
        .ifPresent(eob::addCareTeam);
  }

  // endregion
}
