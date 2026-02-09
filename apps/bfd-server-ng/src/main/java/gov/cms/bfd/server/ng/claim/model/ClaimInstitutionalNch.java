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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
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
@Table(name = "claim_institutional_nch", schema = "idr_new")
public class ClaimInstitutionalNch extends ClaimBase {

  @Column(name = "clm_cntrctr_num")
  private Optional<ClaimContractorNumber> claimContractorNumber;

  @Column(name = "clm_disp_cd")
  private Optional<ClaimDispositionCode> claimDispositionCode;

  @Embedded private ClaimDateSupportingInfo claimDateSupportingInfo;
  @Embedded private BloodPints bloodPints;
  @Embedded private TypeOfBillCode typeOfBillCode;
  @Embedded private AdjudicationChargeInstitutionalNch adjudicationCharge;
  @Embedded private ClaimPaymentAmount claimPaymentAmount;
  @Embedded private ClaimNearLineRecordType claimRecordType;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "clm_uniq_id")
  private SortedSet<ClaimItemInstitutionalNch> claimItems;

  @Embedded private ClaimInstitutionalNchSupportingInfo supportingInfo;
  @Embedded private AdjudicationChargeInstitutional adjudicationChargeInstitutional;
  @Embedded private ServiceProviderHistory serviceProviderHistory;
  @Embedded private BillingProviderHistory billingProviderHistory;
  @Embedded private OtherProviderHistory otherProviderHistory;
  @Embedded private OperatingProviderHistory operatingProviderHistory;
  @Embedded private AttendingProviderHistory attendingProviderHistory;
  @Embedded private RenderingProviderHistory renderingProviderHistory;
  @Embedded private ReferringInstitutionalProviderHistory referringProviderHistory;

  /**
   * Accessor for institutional DRG code, if this is an institutional claim.
   *
   * @return optional DRG code
   */
  @Override
  public Optional<Integer> getDrgCode() {
    return supportingInfo.getDiagnosisDrgCode().getDiagnosisDrgCode();
  }

  /**
   * Accessor for institutional bene paid amount, if this is an institutional claim.
   *
   * @return optional institutional bene paid amount
   */
  public Optional<BigDecimal> getBenePaidAmount() {
    return Optional.of(adjudicationChargeInstitutional.getBenePaidAmount());
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

    var consolidatedDiagnoses = computeConsolidatedDiagnoses();

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
          item.getClaimProcedure().toFhirProcedure().ifPresent(eob::addProcedure);
        });
    var diagnosisSequenceGenerator = new SequenceGenerator();
    getClaimTypeCode()
        .toContext()
        .ifPresent(
            ctx ->
                consolidatedDiagnoses.forEach(
                    d ->
                        d.toFhirDiagnosis(diagnosisSequenceGenerator, ctx)
                            .ifPresent(eob::addDiagnosis)));

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

    var recordTypeCodes = claimRecordType.toFhir(supportingInfoFactory);

    var initialSupportingInfo =
        Stream.of(
                claimContractorNumber.map(c -> c.toFhir(supportingInfoFactory)),
                bloodPints.toFhir(supportingInfoFactory),
                typeOfBillCode.toFhir(supportingInfoFactory),
                claimDispositionCode.map(c -> c.toFhir(supportingInfoFactory)))
            .flatMap(Optional::stream)
            .toList();

    var claimRelatedConditionCds =
        claimItems.stream()
            .map(ClaimItemInstitutionalNch::getClaimRelatedCondition)
            .map(crc -> crc.toFhir(supportingInfoFactory))
            .flatMap(Optional::stream)
            .toList();

    Stream.of(
            initialSupportingInfo,
            claimDateSupportingInfo.toFhir(supportingInfoFactory),
            // In real data, this should only ever have one value, but we're explicitly
            // limiting it to be defensive.
            recordTypeCodes.limit(1).toList(),
            supportingInfo.toFhir(supportingInfoFactory),
            claimRelatedConditionCds)
        .flatMap(Collection::stream)
        .forEach(eob::addSupportingInfo);

    var sequenceGenerator = new SequenceGenerator();
    Stream.of(
            attendingProviderHistory,
            operatingProviderHistory,
            otherProviderHistory,
            renderingProviderHistory,
            referringProviderHistory)
        .flatMap(p -> p.toFhirCareTeamComponent(sequenceGenerator).stream())
        .forEach(
            c -> {
              eob.addCareTeam(c.careTeam());
              eob.addContained(c.practitioner());
            });

    adjudicationChargeInstitutional.toFhir(getClaimValues()).forEach(eob::addAdjudication);

    adjudicationCharge.toFhirTotal().forEach(eob::addTotal);
    getBenePaidAmount()
        .map(AdjudicationChargeType.BENE_PAID_AMOUNT::toFhirTotal)
        .ifPresent(eob::addTotal);
    eob.setPayment(claimPaymentAmount.toFhir());

    var insurance = new ExplanationOfBenefit.InsuranceComponent();
    insurance.setFocal(true);
    claimRecordType.toFhirReference(getClaimTypeCode()).ifPresent(insurance::setCoverage);

    getClaimTypeCode().toFhirInsuranceNearLineRecord(claimRecordType).ifPresent(eob::addInsurance);

    return sortedEob(eob);
  }

  private List<ClaimValue> getClaimValues() {
    return claimItems.stream().map(ClaimItemInstitutionalNch::getClaimValue).toList();
  }

  private List<ClaimProcedure> computeConsolidatedDiagnoses() {
    var claimContextOpt = getClaimTypeCode().toContext();
    if (claimContextOpt.isEmpty()) {
      return Collections.emptyList();
    }
    var claimContext = claimContextOpt.get();

    // Group the diagnoses by code + ICD indicator and sort them by rank.
    // We'll pick the first diagnosis from each group and discard the rest.
    var diagnosisMap = new LinkedHashMap<String, PriorityQueue<ClaimProcedure>>();
    var poaDiagnoses = new HashMap<String, String>();
    for (var item : claimItems) {
      var procedure = item.getClaimProcedure();
      var keyOpt = procedure.getDiagnosisKey();
      if (keyOpt.isEmpty()) {
        continue;
      }
      var key = keyOpt.get();
      procedure
          .getClaimPoaIndicator()
          .ifPresent(p -> poaDiagnoses.merge(key, p, (oldVal, newVal) -> oldVal + newVal));

      var queue =
          diagnosisMap.computeIfAbsent(
              key,
              _ ->
                  new PriorityQueue<ClaimProcedure>(
                      Comparator.comparing(a -> a.getDiagnosisPriority(claimContext).orElse(0))));

      queue.add(item.getClaimProcedure());
    }

    return diagnosisMap.values().stream()
        .map(
            d -> {
              var procedure = d.peek();
              // POA may not be set on the diagnosis we pick, but it may be present on one of the
              // duplicates.
              // Check these and set the POA indicator where applicable.
              var poaIndicator = poaDiagnoses.getOrDefault(procedure.getDiagnosisKey().get(), "");
              if (procedure.getClaimPoaIndicator().isEmpty() && !poaIndicator.isEmpty()) {
                procedure.setClaimPoaIndicator(poaIndicator);
              }
              return procedure;
            })
        .toList();
  }

  @Override
  public ClaimSourceId getClaimSourceId() {
    return ClaimSourceId.NATIONAL_CLAIMS_HISTORY;
  }
}
