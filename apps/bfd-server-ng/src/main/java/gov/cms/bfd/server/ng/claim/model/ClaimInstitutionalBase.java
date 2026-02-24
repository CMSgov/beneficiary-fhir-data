package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.ClaimSecurityStatus;
import gov.cms.bfd.server.ng.util.SequenceGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Reference;

/** Shared base for institutional claim types (NCH and Shared Systems variants). */
@MappedSuperclass
@Getter
public abstract class ClaimInstitutionalBase extends ClaimBase {

  @Column(name = "clm_cntrctr_num")
  private Optional<ClaimContractorNumber> claimContractorNumber;

  @Column(name = "clm_disp_cd")
  private Optional<ClaimDispositionCode> claimDispositionCode;

  @Column(name = "clm_query_cd")
  private Optional<ClaimQueryCode> claimQueryCode;

  @Embedded private NchPrimaryPayorCode nchPrimaryPayorCode;
  @Embedded private BloodPints bloodPints;
  @Embedded private TypeOfBillCode typeOfBillCode;
  @Embedded private ClaimPaymentAmount claimPaymentAmount;
  @Embedded private DiagnosisDrgCode diagnosisDrgCode;
  @Embedded private BillingProviderInstitutional billingProviderHistory;
  @Embedded private OtherInstitutionalCareTeam otherProviderHistory;
  @Embedded private OperatingCareTeam operatingProviderHistory;
  @Embedded private AttendingCareTeam attendingProviderHistory;
  @Embedded private RenderingCareTeam renderingProviderHistory;
  @Embedded private ReferringInstitutionalCareTeam referringProviderHistory;

  abstract SupportingInfoComponentBase getClaimDateSupportingInfo();

  abstract SupportingInfoComponentBase getSupportingInfo();

  abstract AdjudicationChargeInstitutional getAdjudicationChargeInstitutional();

  abstract AdjudicationChargeBase getAdjudicationCharge();

  /**
   * Returns the record-type supporting-info stream, limited to one entry defensively. Each subclass
   * produces this from its own concrete record-type field.
   *
   * @return list containing at most one record-type supporting-info component
   */
  protected abstract List<ExplanationOfBenefit.SupportingInformationComponent>
      buildRecordTypeSupportingInfo();

  /**
   * Adds the insurance component to the EOB.
   *
   * @param eob the EOB being built
   */
  protected abstract void addInsurance(ExplanationOfBenefit eob);

  /**
   * Returns the claim values from all items, used to populate institutional adjudication.
   *
   * @return list of ClaimValues from each item
   */
  protected abstract List<ClaimValue> getClaimValues();

  /**
   * Adds care-team members that are unique to the subclass.
   *
   * @param eob the EOB being built
   * @param sequenceGenerator shared sequence generator for care-team entries
   */
  abstract void addSubclassCareTeam(ExplanationOfBenefit eob, SequenceGenerator sequenceGenerator);

  /**
   * Optionally overrides the EOB outcome. NCH has no override
   *
   * @param eob the EOB being built
   */
  protected void applyOutcomeOverride(ExplanationOfBenefit eob) {}

  @Override
  public ExplanationOfBenefit toFhir(ClaimSecurityStatus securityStatus) {
    var eob = super.toFhir(securityStatus);

    var consolidatedDiagnoses = computeConsolidatedDiagnoses();

    addClaimItems(eob);
    addDiagnoses(eob, consolidatedDiagnoses);
    addProviders(eob);
    applyOutcomeOverride(eob);
    addAllSupportingInfo(eob);
    addCareTeam(eob);
    addAdjudicationAndPayment(eob);
    addInsurance(eob);

    return sortedEob(eob);
  }

  private List<ExplanationOfBenefit.SupportingInformationComponent>
      buildSubclassInitialSupportingInfo() {
    return Stream.of(
            claimQueryCode.map(c -> c.toFhir(supportingInfoFactory)),
            claimContractorNumber.map(c -> c.toFhir(supportingInfoFactory)),
            nchPrimaryPayorCode.toFhir(supportingInfoFactory),
            claimDispositionCode.map(c -> c.toFhir(supportingInfoFactory)))
        .flatMap(Optional::stream)
        .toList();
  }

  private void addClaimItems(ExplanationOfBenefit eob) {
    getItems()
        .forEach(
            item -> {
              var claimLine = item.getClaimLine().toFhirItemComponent();

              claimLine.ifPresent(eob::addItem);
              item.getClaimLine()
                  .getClaimLineRenderingProvider()
                  .flatMap(
                      provider ->
                          item.getClaimLine()
                              .getClaimLineNumber()
                              .flatMap(provider::toFhirCareTeamComponent))
                  .ifPresent(eob::addCareTeam);
              item.getClaimLine()
                  .toFhirSupportingInfo(supportingInfoFactory)
                  .ifPresent(
                      si -> {
                        eob.addSupportingInfo(si);
                        claimLine.ifPresent(cl -> cl.addInformationSequence(si.getSequence()));
                      });
              item.getProcedure()
                  .flatMap(ClaimProcedureBase::toFhirProcedure)
                  .ifPresent(eob::addProcedure);
            });
  }

  private void addDiagnoses(
      ExplanationOfBenefit eob, List<ClaimProcedureBase> consolidatedDiagnoses) {
    var diagnosisSequenceGenerator = new SequenceGenerator();
    getClaimTypeCode()
        .toContext()
        .ifPresent(
            ctx ->
                consolidatedDiagnoses.forEach(
                    d ->
                        d.toFhirDiagnosis(diagnosisSequenceGenerator, ctx)
                            .ifPresent(eob::addDiagnosis)));
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
    var sharedInitialSupportingInfo =
        Stream.of(
                getBloodPints().toFhir(supportingInfoFactory),
                getTypeOfBillCode().toFhir(supportingInfoFactory))
            .flatMap(Optional::stream)
            .toList();

    var claimRelatedConditionCds =
        getItems().stream()
            .flatMap(item -> item.getClaimRelatedCondition().stream())
            .flatMap(c -> c.toFhir(supportingInfoFactory).stream())
            .toList();

    Stream.of(
            buildSubclassInitialSupportingInfo(),
            sharedInitialSupportingInfo,
            getClaimDateSupportingInfo().toFhir(supportingInfoFactory),
            buildRecordTypeSupportingInfo(),
            getSupportingInfo().toFhir(supportingInfoFactory),
            getDiagnosisDrgCode().toFhir(supportingInfoFactory).stream().toList(),
            claimRelatedConditionCds)
        .flatMap(Collection::stream)
        .forEach(eob::addSupportingInfo);
  }

  private void addCareTeam(ExplanationOfBenefit eob) {
    var sequenceGenerator = new SequenceGenerator();
    Stream.of(
            getAttendingProviderHistory(),
            getOperatingProviderHistory(),
            getOtherProviderHistory(),
            getRenderingProviderHistory(),
            getReferringProviderHistory())
        .flatMap(p -> p.toFhirCareTeamComponent(sequenceGenerator.next()).stream())
        .forEach(eob::addCareTeam);

    addSubclassCareTeam(eob, sequenceGenerator);
  }

  private void addAdjudicationAndPayment(ExplanationOfBenefit eob) {
    getAdjudicationChargeInstitutional().toFhir(getClaimValues()).forEach(eob::addAdjudication);
    getAdjudicationCharge().toFhirTotal().forEach(eob::addTotal);
    getBenePaidAmount()
        .map(AdjudicationChargeType.BENE_PAID_AMOUNT::toFhirTotal)
        .ifPresent(eob::addTotal);
    getAdjudicationCharge().toFhirAdjudication().forEach(eob::addAdjudication);
    eob.setPayment(getClaimPaymentAmount().toFhir());
  }

  /**
   * Computes the deduplicated, priority-sorted list of diagnoses across all claim items.
   *
   * @return deduplicated list
   */
  protected List<ClaimProcedureBase> computeConsolidatedDiagnoses() {

    // Group the diagnoses by code + ICD indicator and sort them by rank.
    // We'll pick the first diagnosis from each group and discard the rest.
    var diagnosisMap = new LinkedHashMap<String, PriorityQueue<ClaimProcedureBase>>();
    var poaDiagnoses = new HashMap<String, String>();

    for (var item : getItems()) {
      item.getProcedure()
          .ifPresent(
              procedure -> {
                var keyOpt = procedure.getDiagnosisKey();
                keyOpt.ifPresent(
                    key -> {
                      procedure
                          .getClaimPoaIndicator()
                          .ifPresent(
                              p -> poaDiagnoses.merge(key, p, (oldVal, newVal) -> oldVal + newVal));

                      diagnosisMap
                          .computeIfAbsent(
                              key,
                              _ ->
                                  new PriorityQueue<>(
                                      Comparator.comparingInt(
                                          a ->
                                              a.getDiagnosisPriority(ClaimContext.INSTITUTIONAL)
                                                  .orElse(0))))
                          .add(procedure);
                    });
              });
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

  /**
   * Returns the beneficiary-paid amount from the institutional adjudication charge.
   *
   * @return the bene paid amount
   */
  public Optional<BigDecimal> getBenePaidAmount() {
    return Optional.of(getAdjudicationChargeInstitutional().getBenePaidAmount());
  }

  @Override
  public Optional<Integer> getDrgCode() {
    return getDiagnosisDrgCode().getDiagnosisDrgCode();
  }
}
