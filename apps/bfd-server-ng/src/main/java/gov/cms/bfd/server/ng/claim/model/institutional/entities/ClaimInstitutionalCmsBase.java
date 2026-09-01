package gov.cms.bfd.server.ng.claim.model.institutional.entities;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeType;
import gov.cms.bfd.server.ng.claim.model.common.BenefitEnhancementCodes;
import gov.cms.bfd.server.ng.claim.model.common.ClaimContractorNumber;
import gov.cms.bfd.server.ng.claim.model.common.ClaimDispositionCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimIdrLoadDate;
import gov.cms.bfd.server.ng.claim.model.common.ClaimPaymentComponentAmount;
import gov.cms.bfd.server.ng.claim.model.common.ClaimPaymentComponentBase;
import gov.cms.bfd.server.ng.claim.model.common.NchPrimaryPayorCode;
import gov.cms.bfd.server.ng.claim.model.institutional.AdjudicationChargeCms;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** The institutional claim, full (CMS) profile base class. */
@Getter
@MappedSuperclass
public abstract class ClaimInstitutionalCmsBase extends ClaimInstitutionalBase {

  @Column(name = "clm_disp_cd")
  private Optional<ClaimDispositionCode> claimDispositionCode;

  @Embedded private NchPrimaryPayorCode nchPrimaryPayorCode;
  @Embedded private AdjudicationChargeCms adjudicationChargeInstitutionalCms;
  @Embedded private BenefitEnhancementCodes benefitEnhancementCodes;

  // region Claim IDR Load Date
  @Embedded private ClaimIdrLoadDate claimIdrLoadDate;

  @Override
  public Optional<ClaimIdrLoadDate> getClaimIdrLoadDate() {
    return Optional.of(claimIdrLoadDate);
  }

  // endregion

  // region PaymentComponent
  @Embedded private ClaimPaymentComponentAmount paymentComponent;

  @Override
  public ClaimPaymentComponentBase getPaymentComponent() {
    return paymentComponent;
  }

  // endregion

  // region Claim Contractor Number
  @Column(name = "clm_cntrctr_num")
  private Optional<ClaimContractorNumber> claimContractorNumber;

  @Override
  protected Optional<ClaimContractorNumber> getClaimContractorNumber() {
    return claimContractorNumber;
  }

  // endregion

  // region Overrides
  @Override
  protected List<ExplanationOfBenefit.SupportingInformationComponent>
      buildSubclassInitialSupportingInfo() {
    return Stream.of(
            super.buildSubclassInitialSupportingInfo().stream(),
            Stream.of(
                    nchPrimaryPayorCode.toFhir(supportingInfoFactory),
                    claimDispositionCode.map(c -> c.toFhir(supportingInfoFactory)))
                .flatMap(Optional::stream),
            benefitEnhancementCodes.toFhir(supportingInfoFactory).stream())
        .flatMap(s -> s)
        .toList();
  }

  @Override
  protected void addSubclassAdjudication(ExplanationOfBenefit eob) {
    getClaimValues()
        .map(getAdjudicationChargeInstitutionalCms()::toFhir)
        .ifPresent(list -> list.forEach(eob::addAdjudication));
    getBenePaidAmount()
        .map(AdjudicationChargeType.BENE_PAID_AMOUNT::toFhirTotal)
        .ifPresent(eob::addTotal);
  }

  // endregion

  /**
   * Helper method to get the paid amount from the institutional adjudication charge.
   *
   * @return the bene paid amount
   */
  public Optional<BigDecimal> getBenePaidAmount() {
    return Optional.of(getAdjudicationChargeInstitutionalCms().getBenePaidAmount());
  }
}
