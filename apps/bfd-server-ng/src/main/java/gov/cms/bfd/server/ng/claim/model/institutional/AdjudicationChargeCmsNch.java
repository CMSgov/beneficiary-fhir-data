package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeBase;
import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/**
 * Adjudication fields for institutional claim, cms profile, nch system. Note: this leverages the
 * fact that CMS is a subset of REGULAR.
 */
@Embeddable
public class AdjudicationChargeCmsNch implements AdjudicationChargeBase {

  @Embedded AdjudicationChargeRegular adjudicationChargeBase;

  @Column(name = "clm_blood_chrg_amt")
  private BigDecimal bloodChargeAmount;

  @Column(name = "clm_blood_lblty_amt")
  private BigDecimal bloodLiabilityAmount;

  @Column(name = "clm_blood_ncvrd_chrg_amt")
  private BigDecimal bloodNoncoveredChargeAmount;

  @Override
  public List<ExplanationOfBenefit.TotalComponent> toFhirTotal() {
    return adjudicationChargeBase.toFhirTotal();
  }

  @Override
  public List<ExplanationOfBenefit.AdjudicationComponent> toFhirAdjudication() {
    var result = new ArrayList<>(adjudicationChargeBase.toFhirAdjudication());
    result.add(AdjudicationChargeType.BLOOD_CHARGE_AMOUNT.toFhirAdjudication(bloodChargeAmount));
    result.add(
        AdjudicationChargeType.BENE_BLOOD_DEDUCTIBLE_LIABILITY_AMOUNT.toFhirAdjudication(
            bloodLiabilityAmount));
    result.add(
        AdjudicationChargeType.BLOOD_NONCOVERED_CHARGE_AMOUNT.toFhirAdjudication(
            bloodNoncoveredChargeAmount));
    return result;
  }
}
