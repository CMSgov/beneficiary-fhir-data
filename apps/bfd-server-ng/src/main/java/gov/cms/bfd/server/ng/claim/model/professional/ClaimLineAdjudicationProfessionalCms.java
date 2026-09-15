package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeType;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@MappedSuperclass
abstract class ClaimLineAdjudicationProfessionalCms extends ClaimLineAdjudicationProfessional {

  @Column(name = "clm_line_prvdr_pmt_amt")
  private BigDecimal providerPaymentAmount;

  @Column(name = "clm_line_carr_psych_ot_lmt_amt")
  private BigDecimal therapyAmountAppliedToLimit;

  @Override
  List<ExplanationOfBenefit.AdjudicationComponent> addSubclassAdjudications() {
    var charges = new ArrayList<>(super.addSubclassAdjudications());
    charges.add(
        AdjudicationChargeType.LINE_PROVIDER_PAYMENT_AMOUNT.toFhirAdjudication(
            providerPaymentAmount));
    charges.add(
        AdjudicationChargeType.LINE_PROFESSIONAL_THERAPY_LMT_AMOUNT.toFhirAdjudication(
            therapyAmountAppliedToLimit));
    return charges;
  }
}
