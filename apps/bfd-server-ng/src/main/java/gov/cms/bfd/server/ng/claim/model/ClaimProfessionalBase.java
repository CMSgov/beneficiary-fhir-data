package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.ClaimSecurityStatus;
import gov.cms.bfd.server.ng.util.SequenceGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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

  @Embedded private BloodPints bloodPints;
  @Embedded private ClaimNearLineRecordType claimRecordType;
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
   * Returns Rx supporting-info components (format code, line Rx numbers, etc.) that are specific to
   * the subclass.
   *
   * @return list of subclass-specific Rx supporting-info components; may be empty
   */
  abstract List<ExplanationOfBenefit.SupportingInformationComponent> buildRxSupportingInfo();

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

  /**
   * Optionally overrides the EOB outcome for subclass-specific logic (e.g. SS PAC-stage-2 audit
   * trail status).
   *
   * @param eob the EOB being built
   */
  void applyOutcomeOverride(ExplanationOfBenefit eob) {
    // default: no override SS overrides this
  }

  /** {@inheritDoc} */
  @Override
  public ExplanationOfBenefit toFhir(ClaimSecurityStatus securityStatus) {
    var eob = super.toFhir(securityStatus);
    var diagnosisSequenceGenerator = new SequenceGenerator();

    getClaimItems().forEach(item -> addClaimItemToEob(eob, item, diagnosisSequenceGenerator));

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
      ExplanationOfBenefit eob, ClaimItemBase item, SequenceGenerator diagnosisSequenceGenerator) {

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
        .flatMap(
            provider ->
                item.getClaimLine().getClaimLineNumber().flatMap(provider::toFhirCareTeamComponent))
        .ifPresent(eob::addCareTeam);

    item.getProcedure()
        .flatMap(
            procedure ->
                procedure.toFhirDiagnosis(diagnosisSequenceGenerator, ClaimContext.PROFESSIONAL))
        .ifPresent(eob::addDiagnosis);

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
                getBloodPints().toFhir(supportingInfoFactory),
                    claimSubmissionDate.toFhir(supportingInfoFactory),
                Optional.of(claimContractorNumber)
                        .flatMap(opt -> opt)
                        .map(c -> c.toFhir(supportingInfoFactory)),
                    clinicalTrialNumber.toFhir(supportingInfoFactory))
            .flatMap(Optional::stream)
            .toList();

    Stream.of(sharedHeaderSupportingInfo, buildSubclassSupportingInfo(), buildRxSupportingInfo())
        .flatMap(Collection::stream)
        .forEach(eob::addSupportingInfo);
  }

  private void addCareTeam(ExplanationOfBenefit eob) {
    var sequenceGenerator = new SequenceGenerator();
    getReferringProviderHistory()
        .toFhirCareTeamComponent(sequenceGenerator.next())
        .ifPresent(eob::addCareTeam);
    addSubclassCareTeam(eob, sequenceGenerator);
  }

  private void addInsurance(ExplanationOfBenefit eob) {
    getClaimTypeCode()
        .toFhirInsuranceNearLineRecord(getClaimRecordType())
        .ifPresent(eob::addInsurance);
  }

  @Override
  public Optional<Integer> getDrgCode() {
    return Optional.empty();
  }
}
