package gov.cms.bfd.server.ng.claim.model;

import static gov.cms.bfd.server.ng.claim.model.ClaimSubtype.PDE;

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
@Table(name = "claim_professional_ss", schema = "idr_new")
@SuppressWarnings("JpaAttributeTypeInspection")
public class ClaimProfessionalSharedSystems extends ClaimBase {

  @Column(name = "clm_sbmt_frmt_cd")
  private Optional<ClaimSubmissionFormatCode> claimFormatCode;

  @Column(name = "clm_cntrctr_num")
  private Optional<ClaimContractorNumber> claimContractorNumber;

  @Column(name = "clm_src_id")
  private ClaimSourceId claimSourceId;

  @Embedded private BloodPints bloodPints;
  @Embedded private NchPrimaryPayorCode nchPrimaryPayorCode;
  @Embedded private AdjudicationChargeProfessionalSharedSystems adjudicationCharge;
  @Embedded private ClaimPaymentAmount claimPaymentAmount;
  @Embedded private ClaimNearLineRecordType claimRecordType;
  @Embedded private ClaimSubmissionDate claimSubmissionDate;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "clm_uniq_id")
  private SortedSet<ClaimItemProfessionalSharedSystems> claimItems;

  @Embedded private ReferringProfessionalProviderHistory referringProviderHistory;
  @Embedded private ServiceProviderHistory serviceProviderHistory;
  @Embedded private BillingProviderHistory billingProviderHistory;
  @Embedded private OtherProviderHistory otherProviderHistory;

  @Column(name = "clm_prvdr_acnt_rcvbl_ofst_amt")
  private BigDecimal providerOffsetAmount;

  @Column(name = "clm_audt_trl_stus_cd")
  private Optional<ClaimAuditTrailStatusCode> claimAuditTrailStatusCode;

  @Column(name = "clm_mdcr_prfnl_prvdr_asgnmt_sw")
  private Optional<ProviderAssignmentIndicatorSwitch> providerAssignmentIndicatorSwitch;

  @Embedded ClinicalTrialNumber clinicalTrialNumber;

  ExplanationOfBenefit.AdjudicationComponent toFhirAdjudication() {
    return AdjudicationChargeType.PROVIDER_OFFSET_AMOUNT.toFhirAdjudication(providerOffsetAmount);
  }

  Optional<ExplanationOfBenefit.RemittanceOutcome> toFhirOutcome() {
    if (getClaimTypeCode().isPacStage2()) {
      return claimAuditTrailStatusCode.map(ClaimAuditTrailStatusCode::getOutcome);
    }
    return Optional.empty();
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
    var diagnosisSequenceGenerator = new SequenceGenerator();

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
          getClaimTypeCode()
              .toContext()
              .flatMap(
                  ctx -> item.getClaimProcedure().toFhirDiagnosis(diagnosisSequenceGenerator, ctx))
              .ifPresent(eob::addDiagnosis);
          item.getClaimProcedure().toFhirProcedure().ifPresent(eob::addProcedure);
        });

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
                bloodPints.toFhir(supportingInfoFactory),
                nchPrimaryPayorCode.toFhir(supportingInfoFactory),
                claimContractorNumber.map(c -> c.toFhir(supportingInfoFactory)))
            .flatMap(Optional::stream)
            .toList();

    var claimRxSupportingInfo =
        Stream.of(
                // claim rx header lvl
                getClaimFormatCode()
                    .filter(c -> getClaimTypeCode().isClaimSubtype(PDE))
                    .map(c -> c.toFhir(supportingInfoFactory))
                    .stream(),

                // claim line rx num
                claimItems.stream()
                    .map(item -> item.getClaimLineRxNum().toFhir(supportingInfoFactory))
                    .flatMap(Optional::stream))
            .flatMap(s -> s)
            .toList();

    var claimProfessionalSupportingInfo =
        Stream.of(
                claimSubmissionDate.toFhir(supportingInfoFactory),
                providerAssignmentIndicatorSwitch.map(c -> c.toFhir(supportingInfoFactory)),
                clinicalTrialNumber.toFhir(supportingInfoFactory))
            .flatMap(Optional::stream)
            .toList();

    Stream.of(initialSupportingInfo, claimProfessionalSupportingInfo, claimRxSupportingInfo)
        .flatMap(Collection::stream)
        .forEach(eob::addSupportingInfo);

    var sequenceGenerator = new SequenceGenerator();
    Stream.of(otherProviderHistory, referringProviderHistory)
        .flatMap(p -> p.toFhirCareTeamComponent(sequenceGenerator).stream())
        .forEach(
            c -> {
              eob.addCareTeam(c.careTeam());
              eob.addContained(c.practitioner());
            });

    adjudicationCharge.toFhirTotal().forEach(eob::addTotal);
    adjudicationCharge.toFhirAdjudication().forEach(eob::addAdjudication);
    eob.setPayment(claimPaymentAmount.toFhir());

    eob.addAdjudication(toFhirAdjudication());
    toFhirOutcome().ifPresent(eob::setOutcome);

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
}
