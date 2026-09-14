package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** clm_line adjudication information for Professional-CMS-SharedSystems. */
@Embeddable
class ClaimLineAdjudicationProfessionalCmsSharedSystems
    extends ClaimLineAdjudicationProfessionalCms {

  @Column(name = "clm_line_otaf_amt")
  private BigDecimal providerObligationToAcceptFullAmount;

  @Override
  Stream<ExplanationOfBenefit.AdjudicationComponent> subClassCharges() {
    return Stream.concat(
        super.subClassCharges(),
        Stream.of(
            AdjudicationChargeType.LINE_PROVIDER_OBLIGATION_FULL_AMOUNT.toFhirAdjudication(
                providerObligationToAcceptFullAmount)));
  }
}
