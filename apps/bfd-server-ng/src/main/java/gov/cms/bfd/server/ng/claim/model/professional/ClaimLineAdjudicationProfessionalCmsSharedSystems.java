package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** clm_line adjudication information for Professional-CMS-SharedSystems. */
@Embeddable
class ClaimLineAdjudicationProfessionalCmsSharedSystems
    extends ClaimLineAdjudicationProfessionalCms {

  @Column(name = "clm_line_otaf_amt")
  private BigDecimal providerObligationToAcceptFullAmount;

  @Override
  List<ExplanationOfBenefit.AdjudicationComponent> addSubclassAdjudications() {
    var adjudicationComponents = new ArrayList<>(super.addSubclassAdjudications());
    adjudicationComponents.add(
        AdjudicationChargeType.LINE_PROVIDER_OBLIGATION_FULL_AMOUNT.toFhirAdjudication(
            providerObligationToAcceptFullAmount));
    return adjudicationComponents;
  }
}
