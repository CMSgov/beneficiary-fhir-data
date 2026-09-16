package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeType;
import gov.cms.bfd.server.ng.claim.model.common.AdjudicationEmbedded;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** clm_line adjudication data for Professional-Regular-NCH. */
@Embeddable
public class ClaimLineAdjudicationProfessionalRegularNch implements AdjudicationEmbedded {

  @Embedded private ClaimLineAdjudicationProfessionalRegular baseAdjudication;

  @Column(name = "clm_bene_prmry_pyr_pd_amt")
  private BigDecimal primaryPayerPaidAmount;

  @Override
  public List<ExplanationOfBenefit.AdjudicationComponent> toFhirAdjudication() {
    return Stream.concat(
            baseAdjudication.toFhirAdjudication().stream(),
            Stream.of(
                AdjudicationChargeType.LINE_PROFESSIONAL_PRIMARY_PAYER_PAID_AMOUNT
                    .toFhirAdjudication(primaryPayerPaidAmount)))
        .toList();
  }
}
