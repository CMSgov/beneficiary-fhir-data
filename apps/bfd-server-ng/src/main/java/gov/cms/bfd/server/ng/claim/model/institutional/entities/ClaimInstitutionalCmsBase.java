package gov.cms.bfd.server.ng.claim.model.institutional.entities;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeType;
import gov.cms.bfd.server.ng.claim.model.common.BenefitEnhancementCodes;
import gov.cms.bfd.server.ng.claim.model.common.ClaimContractorNumber;
import gov.cms.bfd.server.ng.claim.model.common.ClaimDispositionCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimIdrLoadDate;
import gov.cms.bfd.server.ng.claim.model.common.NchPrimaryPayorCode;
import gov.cms.bfd.server.ng.claim.model.common.PaymentComponentAmount;
import gov.cms.bfd.server.ng.claim.model.common.PaymentComponentBase;
import gov.cms.bfd.server.ng.claim.model.institutional.AdjudicationClaimValue;
import gov.cms.bfd.server.ng.claim.model.institutional.AdjudicationPpsCms;
import gov.cms.bfd.server.ng.claim.model.institutional.ClaimValue;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** The institutional claim, full (CMS) profile base class. */
@Getter
@MappedSuperclass
abstract class ClaimInstitutionalCmsBase extends ClaimInstitutionalBase {

  @Column(name = "clm_disp_cd")
  private Optional<ClaimDispositionCode> claimDispositionCode;

  @Column(name = "clm_mdcr_instnl_bene_pd_amt")
  private BigDecimal benePaidAmount;

  @Column(name = "clm_cntrctr_num")
  private Optional<ClaimContractorNumber> claimContractorNumber;

  @Embedded private NchPrimaryPayorCode nchPrimaryPayorCode;
  @Embedded private AdjudicationPpsCms adjudicationPpsCms;
  @Embedded private BenefitEnhancementCodes benefitEnhancementCodes;
  @Embedded private ClaimIdrLoadDate claimIdrLoadDate;
  @Embedded private PaymentComponentAmount paymentComponent;

  //region Hook Methods

  abstract List<ClaimValue> getClaimValues();

  //endregion

  //region Overrides

  @Override
  public Optional<ClaimIdrLoadDate> getClaimIdrLoadDate() {
    return Optional.of(claimIdrLoadDate);
  }

  @Override
  public PaymentComponentBase getPaymentComponent() {
    return paymentComponent;
  }

  @Override
  protected Optional<ClaimContractorNumber> getClaimContractorNumber() {
    return claimContractorNumber;
  }

  @Override
  protected List<ExplanationOfBenefit.SupportingInformationComponent>
      buildSubclassSupportingInfo() {
    return Stream.of(
            super.buildSubclassSupportingInfo().stream(),
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
    getAdjudicationPpsCms().toFhirAdjudication().forEach(eob::addAdjudication);
    AdjudicationClaimValue.toFhir(getClaimValues()).forEach(eob::addAdjudication);
    eob.addTotal(AdjudicationChargeType.BENE_PAID_AMOUNT.toFhirTotal(getBenePaidAmount()));
  }

  //endregion
}
