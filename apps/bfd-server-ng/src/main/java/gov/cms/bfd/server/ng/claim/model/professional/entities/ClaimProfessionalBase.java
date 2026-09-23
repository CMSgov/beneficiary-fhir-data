package gov.cms.bfd.server.ng.claim.model.professional.entities;

import gov.cms.bfd.server.ng.ClaimFilterOptions;
import gov.cms.bfd.server.ng.claim.model.common.AdjudicationEmbedded;
import gov.cms.bfd.server.ng.claim.model.common.BlueButtonSupportingInfoCategory;
import gov.cms.bfd.server.ng.claim.model.common.ClaimContractorNumber;
import gov.cms.bfd.server.ng.claim.model.common.ClaimItemBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimState;
import gov.cms.bfd.server.ng.claim.model.common.ClaimSubmissionDate;
import gov.cms.bfd.server.ng.claim.model.common.ProcedureBase;
import gov.cms.bfd.server.ng.claim.model.common.entities.ClaimBase;
import gov.cms.bfd.server.ng.claim.model.professional.BillingProviderProfessional;
import gov.cms.bfd.server.ng.claim.model.professional.ReferringProfessionalCareTeam;
import gov.cms.bfd.server.ng.util.FhirUtil;
import gov.cms.bfd.server.ng.util.SequenceGenerator;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import java.util.*;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;

/** Shared base for professional claim types (NCH and Shared Systems). */
@MappedSuperclass
@Getter
abstract class ClaimProfessionalBase extends ClaimBase {

  @Column(name = "clm_ptnt_cntl_num")
  private Optional<String> patientControlNumber;

  @Embedded private ClaimSubmissionDate claimSubmissionDate;
  @Embedded private ReferringProfessionalCareTeam referringProviderHistory;
  @Embedded private BillingProviderProfessional billingProviderHistory;

  // region Hook Methods

  /**
   * retrieve total components and adjudication components.
   *
   * @return an AdjudicationEmbedded with toFhirTotal and toFhirAdjudication
   */
  abstract Optional<AdjudicationEmbedded> getAdjudication();

  /**
   * get a ClaimContractorNumber from some data sources.
   *
   * @return the ClaimContractorNumber
   */
  public Optional<ClaimContractorNumber> getClaimContractorNumber() {
    return Optional.empty();
  }

  /**
   * Returns supporting-info components that are specific to the subclass.
   *
   * @return list of subclass-specific supporting-info components
   */
  abstract List<ExplanationOfBenefit.SupportingInformationComponent> getSubclassSupportingInfo();

  /**
   * return Observations while building SupportingInfo (Shared Systems), default no-op.
   *
   * @return Observations to be added to the eob's contained
   */
  protected List<Observation> getSubclassContainedObservations() {
    return List.of();
  }

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

  // endregion

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
    getAdjudication().ifPresent(ac -> ac.toFhirTotal().forEach(eob::addTotal));
    getAdjudication().ifPresent(ac -> ac.toFhirAdjudication().forEach(eob::addAdjudication));
    getPaymentComponent().toFhir().ifPresent(eob::setPayment);
    addPatientControlNumberIdentifier(eob);
    addSubclassAdjudication(eob);
    applyOutcomeOverride(eob);
    addInsurance(eob);
    addContainedObservations(eob);

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
            item.getProcedureOptional()
                .flatMap(ProcedureBase::getDiagnosisKey)
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

    item.getClaimLine()
        .getClaimLineRenderingProvider()
        .flatMap(
            provider ->
                item.getClaimLine()
                    .getClaimLineNumber()
                    .flatMap(
                        sequence ->
                            provider.toFhirCareTeamComponent(
                                sequence, Optional.of(getClaimTypeCode()))))
        .ifPresent(eob::addCareTeam);

    // Procedure is present on SS items but not on NCH items; the item exposes it as Optional.
    item.getProcedureOptional()
        .flatMap(ProcedureBase::toFhirProcedure)
        .ifPresent(eob::addProcedure);

    // Line-level observation (NCH only; SS items return empty).
    item.getClaimLine()
        .toFhirObservation(item.getClaimItemId().getBfdRowId())
        .ifPresent(
            observation -> {
              var supportingInfo =
                  supportingInfoFactory
                      .createSupportingInfo()
                      .setCategory(
                          BlueButtonSupportingInfoCategory.CLM_LINE_HCT_HGB_RSLT_NUM.toFhir());
              eob.addContained(observation);
              supportingInfo.setValue(new Reference(observation));
              eob.addSupportingInfo(supportingInfo);
            });
  }

  private void addProviders(ExplanationOfBenefit eob) {
    getBillingProviderHistory()
        .toFhirNpiType()
        .ifPresentOrElse(
            p -> {
              eob.setProvider(new Reference("#" + p.getId()));
              eob.addContained(p);
            },
            () -> eob.setProvider(FhirUtil.setDataAbsentReasonUnknown(new Reference())));
  }

  private void addAllSupportingInfo(ExplanationOfBenefit eob) {
    var sharedHeaderSupportingInfo =
        Stream.of(
                claimSubmissionDate.toFhir(supportingInfoFactory),
                getClaimContractorNumber().map(c -> c.toFhir(supportingInfoFactory)))
            .flatMap(Optional::stream)
            .toList();

    Stream.of(sharedHeaderSupportingInfo, getSubclassSupportingInfo())
        .flatMap(Collection::stream)
        .forEach(eob::addSupportingInfo);
  }

  private void addCareTeam(ExplanationOfBenefit eob) {
    var sequenceGenerator = new SequenceGenerator(eob.getCareTeam().size() + 1);
    getReferringProviderHistory()
        .toFhirCareTeamComponent(sequenceGenerator.next(), Optional.of(getClaimTypeCode()))
        .ifPresent(eob::addCareTeam);
    addSubclassCareTeam(eob, sequenceGenerator);
  }

  private void addInsurance(ExplanationOfBenefit eob) {
    eob.addInsurance(getClaimTypeCode().toFhirInsurance(getClaimRecordTypeOptional()));
  }

  private void addPatientControlNumberIdentifier(ExplanationOfBenefit eob) {
    patientControlNumber.ifPresent(
        s ->
            eob.addIdentifier(
                new Identifier()
                    .setSystem(SystemUrls.BLUE_BUTTON_PATIENT_CONTROL_NUMBER)
                    .setValue(s)));
  }

  @Override
  public Optional<Integer> getDrgCode() {
    return Optional.empty();
  }

  private Map<String, List<Integer>> buildDiagnosisSequences(
      ExplanationOfBenefit eob, SequenceGenerator sequenceGenerator) {
    var diagnosisSequenceMap = new HashMap<String, List<Integer>>();

    for (var item : getItems()) {
      item.getProcedureOptional()
          .ifPresent(
              procedure ->
                  addDiagnosisAndTrackSequence(
                      procedure, eob, sequenceGenerator, diagnosisSequenceMap));
    }
    return diagnosisSequenceMap;
  }

  private void addDiagnosisAndTrackSequence(
      ProcedureBase procedure,
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

  private void addContainedObservations(ExplanationOfBenefit eob) {
    getSubclassContainedObservations().forEach(eob::addContained);
  }
}
