package gov.cms.bfd.server.ng.claim.model.professional.entities;

import gov.cms.bfd.server.ng.ClaimFilterOptions;
import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeBase;
import gov.cms.bfd.server.ng.claim.model.common.entities.ClaimBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimContractorNumber;
import gov.cms.bfd.server.ng.claim.model.common.ClaimItemBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimPaymentAmount;
import gov.cms.bfd.server.ng.claim.model.common.ClaimProcedureBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimRecordType;
import gov.cms.bfd.server.ng.claim.model.common.ClaimState;
import gov.cms.bfd.server.ng.claim.model.common.ClaimSubmissionDate;
import gov.cms.bfd.server.ng.claim.model.professional.BillingProviderProfessional;
import gov.cms.bfd.server.ng.claim.model.professional.ClinicalTrialNumber;
import gov.cms.bfd.server.ng.claim.model.professional.ReferringProfessionalCareTeam;
import gov.cms.bfd.server.ng.util.SequenceGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import java.util.*;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Reference;

/** Shared base for professional claim types (NCH and Shared Systems). */
@MappedSuperclass
@Getter
public abstract class ClaimProfessionalBase extends ClaimBase {

  @Column(name = "clm_cntrctr_num")
  private Optional<ClaimContractorNumber> claimContractorNumber;

  @Embedded private ClaimPaymentAmount claimPaymentAmount;
  @Embedded private ClaimSubmissionDate claimSubmissionDate;
  @Embedded private ReferringProfessionalCareTeam referringProviderHistory;
  @Embedded private BillingProviderProfessional billingProviderHistory;
  @Embedded private ClinicalTrialNumber clinicalTrialNumber;

  abstract AdjudicationChargeBase getAdjudicationCharge();

  /**
   * Returns supporting-info components that are specific to the subclass.
   *
   * @return list of subclass-specific supporting-info components
   */
  abstract List<ExplanationOfBenefit.SupportingInformationComponent> buildSubclassSupportingInfo();

  /**
   * Adds any adjudication entries that are unique to the subclass.
   *
   * @param eob the EOB being built
   */
  abstract void addSubclassAdjudication(ExplanationOfBenefit eob);

  /**
   * Adds care-team members that are unique to the subclass.
   *
   * @param eob the EOB being built
   * @param sequenceGenerator shared sequence generator for care-team entries
   */
  abstract void addSubclassCareTeam(ExplanationOfBenefit eob, SequenceGenerator sequenceGenerator);

  abstract Optional<ClaimRecordType> getClaimRecordTypeOptional();

  /** {@inheritDoc} */
  @Override
  public ExplanationOfBenefit toFhir(ClaimFilterOptions options, ClaimState claimState) {
    var eob = super.toFhir(options, claimState);
    var diagnosisSequenceGenerator = new SequenceGenerator();
    var diagnosisSequenceMap = buildDiagnosisSequences(eob, diagnosisSequenceGenerator);

    getItems().forEach(item -> addClaimItemToEob(eob, item, diagnosisSequenceMap, options));
    addProviders(eob);
    addAllSupportingInfo(eob);
    addCareTeam(eob);
    getAdjudicationCharge().toFhirTotal().forEach(eob::addTotal);
    getAdjudicationCharge().toFhirAdjudication().forEach(eob::addAdjudication);
    eob.setPayment(getClaimPaymentAmount().toFhir());
    addSubclassAdjudication(eob);
    applyOutcomeOverride(eob);
    addInsurance(eob);

    return sortedEob(eob);
  }

  private void addClaimItemToEob(
      ExplanationOfBenefit eob,
      ClaimItemBase item,
      Map<String, List<Integer>> diagnosisSequenceMap,
      ClaimFilterOptions options) {

    var claimLine = item.getClaimLine().toFhirItemComponent(options);
    claimLine.ifPresent(eob::addItem);

    // populates diagnosisSequence only if CLM_LINE_DGNS_CD is present in D-type codes
    claimLine.ifPresent(
        line -> {
          var hasLineDiagnosis = item.getClaimLine().getClaimLineDiagnosisCode().isPresent();
          if (hasLineDiagnosis) {
            item.getProcedure()
                .flatMap(ClaimProcedureBase::getDiagnosisKey)
                .map(diagnosisSequenceMap::get)
                .ifPresent(sequences -> sequences.forEach(line::addDiagnosisSequence));
          }
        });

    item.getClaimLine()
        .toFhirSupportingInfo(supportingInfoFactory)
        .forEach(
            si -> {
              eob.addSupportingInfo(si);
              claimLine.ifPresent(cl -> cl.addInformationSequence(si.getSequence()));
            });

    var claimContext = getClaimTypeCode().toContext();
    item.getClaimLine()
        .getClaimLineRenderingProvider()
        .flatMap(
            provider ->
                item.getClaimLine()
                    .getClaimLineNumber()
                    .flatMap(sequence -> provider.toFhirCareTeamComponent(sequence, claimContext)))
        .ifPresent(eob::addCareTeam);

    // Procedure is present on SS items but not on NCH items; the item exposes it as Optional.
    item.getProcedure().flatMap(ClaimProcedureBase::toFhirProcedure).ifPresent(eob::addProcedure);

    // Line-level observation (NCH only; SS items return empty).
    item.getClaimLine()
        .toFhirObservation(item.getClaimItemId().getBfdRowId())
        .ifPresent(eob::addContained);
  }

  private void addProviders(ExplanationOfBenefit eob) {
    getBillingProviderHistory()
        .toFhirNpiType()
        .ifPresent(
            p -> {
              eob.addContained(p);
              eob.setProvider(new Reference("#" + p.getId()));
            });
  }

  private void addAllSupportingInfo(ExplanationOfBenefit eob) {
    var sharedHeaderSupportingInfo =
        Stream.of(
                claimSubmissionDate.toFhir(supportingInfoFactory),
                Optional.of(claimContractorNumber)
                    .flatMap(opt -> opt)
                    .map(c -> c.toFhir(supportingInfoFactory)),
                clinicalTrialNumber.toFhir(supportingInfoFactory))
            .flatMap(Optional::stream)
            .toList();

    Stream.of(sharedHeaderSupportingInfo, buildSubclassSupportingInfo())
        .flatMap(Collection::stream)
        .forEach(eob::addSupportingInfo);
  }

  private void addCareTeam(ExplanationOfBenefit eob) {
    var sequenceGenerator = new SequenceGenerator(eob.getCareTeam().size() + 1);
    getReferringProviderHistory()
        .toFhirCareTeamComponent(sequenceGenerator.next(), getClaimTypeCode().toContext())
        .ifPresent(eob::addCareTeam);
    addSubclassCareTeam(eob, sequenceGenerator);
  }

  private void addInsurance(ExplanationOfBenefit eob) {
    eob.addInsurance(getClaimTypeCode().toFhirInsurance(getClaimRecordTypeOptional()));
  }

  @Override
  public Optional<Integer> getDrgCode() {
    return Optional.empty();
  }

  private Map<String, List<Integer>> buildDiagnosisSequences(
      ExplanationOfBenefit eob, SequenceGenerator sequenceGenerator) {
    var diagnosisSequenceMap = new HashMap<String, List<Integer>>();

    for (var item : getItems()) {
      item.getProcedure()
          .ifPresent(
              procedure ->
                  addDiagnosisAndTrackSequence(
                      procedure, eob, sequenceGenerator, diagnosisSequenceMap));
    }
    return diagnosisSequenceMap;
  }

  private void addDiagnosisAndTrackSequence(
      ClaimProcedureBase procedure,
      ExplanationOfBenefit eob,
      SequenceGenerator sequenceGenerator,
      Map<String, List<Integer>> diagnosisSequenceMap) {

    var diagnosisOpt = procedure.toFhirDiagnosis(sequenceGenerator);
    if (diagnosisOpt.isEmpty()) return;

    var diagnosisComponent = diagnosisOpt.get();
    eob.addDiagnosis(diagnosisComponent);

    procedure
        .getDiagnosisKey()
        .ifPresent(
            key ->
                diagnosisSequenceMap
                    .computeIfAbsent(key, _ -> new ArrayList<>())
                    .add(diagnosisComponent.getSequence()));
  }
}
