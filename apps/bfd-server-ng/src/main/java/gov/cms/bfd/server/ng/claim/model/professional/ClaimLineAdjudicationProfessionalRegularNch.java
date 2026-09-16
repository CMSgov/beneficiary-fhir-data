package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** clm_line adjudication data for Professional-Regular-NCH. */
@Embeddable
public class ClaimLineAdjudicationProfessionalRegularNch extends ClaimLineAdjudicationProfessional {

  @Column(name = "clm_bene_prmry_pyr_pd_amt")
  private BigDecimal primaryPayerPaidAmount;

  @Override
  List<ExplanationOfBenefit.AdjudicationComponent> addSubclassAdjudications() {
    var adjudicationComponents = new ArrayList<>(super.addSubclassAdjudications());
    adjudicationComponents.add(
        AdjudicationChargeType.LINE_PROFESSIONAL_PRIMARY_PAYER_PAID_AMOUNT.toFhirAdjudication(
            primaryPayerPaidAmount));
    return adjudicationComponents;
  }
}
