package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.ClaimSecurityStatus;
import gov.cms.bfd.server.ng.util.SequenceGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Reference;

/**
 * Claim table. Suppress SonarQube Monster Class warning that dependencies to other class should be
 * reduced from 21 to the max 20. Ignore. Class itself is relatively short in lines of code.
 */
@Getter
@Entity
@Table(name = "claim_professional_nch", schema = "idr_new")
@SuppressWarnings("JpaAttributeTypeInspection")
public class ClaimProfessionalNch extends ClaimBase {

  @Column(name = "clm_cntrctr_num")
  private Optional<ClaimContractorNumber> claimContractorNumber;

  @Column(name = "clm_disp_cd")
  private Optional<ClaimDispositionCode> claimDispositionCode;

  @Column(name = "clm_query_cd")
  private Optional<ClaimQueryCode> claimQueryCode;

  private ClaimNearLineRecordType claimRecordType;

  @Embedded private BloodPints bloodPints;
  @Embedded private AdjudicationChargeProfessionalNch adjudicationCharge;
  @Embedded private ClaimPaymentAmount claimPaymentAmount;
  @Embedded private NchWeeklyProcessingDate nchWeeklyProcessingDate;
  @Embedded private ClaimSubmissionDate claimSubmissionDate;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "clm_uniq_id")
  private SortedSet<ClaimItemProfessionalNch> claimItems;

  @Embedded private ReferringProfessionalProviderHistory referringProviderHistory;
  @Embedded private BillingProviderHistory billingProviderHistory;
  @Embedded private ServiceProviderHistory serviceProviderHistory;

  @Column(name = "clm_mdcr_prfnl_prmry_pyr_amt")
  private BigDecimal primaryProviderPaidAmount;

  @Column(name = "clm_carr_pmt_dnl_cd")
  private Optional<ClaimPaymentDenialCode> claimPaymentDenialCode;

  @Embedded ClinicalTrialNumber clinicalTrialNumber;

  List<ExplanationOfBenefit.AdjudicationComponent> toFhirAdjudication() {
    return List.of(
        AdjudicationChargeType.PAYER_PAID_AMOUNT.toFhirAdjudication(primaryProviderPaidAmount));
  }

  /**
   * Convert the claim info to a FHIR ExplanationOfBenefit.
   *
   * @param securityStatus securityStatus
   * @return ExplanationOfBenefit
   */
  @Override
  public ExplanationOfBenefit toFhir(ClaimSecurityStatus securityStatus) {
    var eob = super.toFhir(securityStatus);
    //    var consolidatedDiagnoses = computeConsolidatedDiagnoses();

    claimItems.forEach(
        item -> {
          var claimLine = item.getClaimLine().toFhirItemComponent();
          claimLine.ifPresent(eob::addItem);
          item.getClaimLine()
              .toFhirSupportingInfo(supportingInfoFactory)
              .ifPresent(
                  si -> {
                    eob.addSupportingInfo(si);
                    claimLine.ifPresent(cl -> cl.addInformationSequence(si.getSequence()));
                  });

          item.getClaimLine()
              .getClaimLineRenderingProvider()
              .toFhirCareTeamComponentLine(item.getClaimLine().getClaimLineNumber())
              .ifPresent(
                  c -> {
                    eob.addCareTeam(c.careTeam());
                    eob.addContained(c.practitioner());
                  });
          item.getClaimLine()
              .toFhirObservation(item.getClaimItemId().getBfdRowId())
              .ifPresent(eob::addContained);
        });
    //    var diagnosisSequenceGenerator = new SequenceGenerator();
    //    getClaimTypeCode()
    //        .toContext()
    //        .ifPresent(
    //            ctx ->
    //                consolidatedDiagnoses.forEach(
    //                    d ->
    //                        d.toFhirDiagnosis(diagnosisSequenceGenerator, ctx)
    //                            .ifPresent(eob::addDiagnosis)));

    billingProviderHistory
        .toFhirNpiType()
        .ifPresent(
            p -> {
              eob.addContained(p);
              eob.setProvider(new Reference(p));
            });

    serviceProviderHistory
        .toFhirNpiType()
        .ifPresent(
            p -> {
              eob.addContained(p);
              eob.setProvider(new Reference(p));
            });

    var initialSupportingInfo =
        Stream.of(
                claimContractorNumber.map(c -> c.toFhir(supportingInfoFactory)),
                bloodPints.toFhir(supportingInfoFactory),
                claimDispositionCode.map(c -> c.toFhir(supportingInfoFactory)),
                claimQueryCode.map(c -> c.toFhir(supportingInfoFactory)))
            .flatMap(Optional::stream)
            .toList();

    var claimRxSupportingInfo =
        Stream.of(

                // claim line rx num
                claimItems.stream()
                    .map(item -> item.getClaimLineRxNum().toFhir(supportingInfoFactory))
                    .flatMap(Optional::stream))
            .flatMap(s -> s)
            .toList();

    var claimProfessionalSupportingInfo =
        Stream.of(
                nchWeeklyProcessingDate.toFhir(supportingInfoFactory),
                claimSubmissionDate.toFhir(supportingInfoFactory),
                claimPaymentDenialCode.map(c -> c.toFhir(supportingInfoFactory)),
                clinicalTrialNumber.toFhir(supportingInfoFactory))
            .flatMap(Optional::stream)
            .toList();

    Stream.of(initialSupportingInfo, claimProfessionalSupportingInfo, claimRxSupportingInfo)
        .flatMap(Collection::stream)
        .forEach(eob::addSupportingInfo);

    var sequenceGenerator = new SequenceGenerator();
    Stream.of(referringProviderHistory)
        .flatMap(p -> p.toFhirCareTeamComponent(sequenceGenerator).stream())
        .forEach(
            c -> {
              eob.addCareTeam(c.careTeam());
              eob.addContained(c.practitioner());
            });

    adjudicationCharge.toFhirTotal().forEach(eob::addTotal);
    adjudicationCharge.toFhirAdjudication().forEach(eob::addAdjudication);
    eob.setPayment(claimPaymentAmount.toFhir());

    toFhirAdjudication().forEach(eob::addAdjudication);

    var insurance = new ExplanationOfBenefit.InsuranceComponent();
    insurance.setFocal(true);
    claimRecordType.toFhirReference(getClaimTypeCode()).ifPresent(insurance::setCoverage);

    getClaimTypeCode().toFhirInsuranceNearLineRecord(claimRecordType).ifPresent(eob::addInsurance);

    return sortedEob(eob);
  }

  @Override
  public Optional<Integer> getDrgCode() {
    return Optional.empty();
  }

  @Override
  public ClaimSourceId getClaimSourceId() {
    return ClaimSourceId.NATIONAL_CLAIMS_HISTORY;
  }
}
