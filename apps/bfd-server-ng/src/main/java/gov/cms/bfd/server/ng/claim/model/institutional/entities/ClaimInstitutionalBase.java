package gov.cms.bfd.server.ng.claim.model.institutional.entities;

import static gov.cms.bfd.server.ng.claim.model.common.ClaimDiagnosisType.*;

import gov.cms.bfd.server.ng.ClaimFilterOptions;
import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimContractorNumber;
import gov.cms.bfd.server.ng.claim.model.common.ClaimProcedureBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimQueryCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimState;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoComponentBase;
import gov.cms.bfd.server.ng.claim.model.common.entities.ClaimBase;
import gov.cms.bfd.server.ng.claim.model.institutional.AttendingCareTeam;
import gov.cms.bfd.server.ng.claim.model.institutional.BillingProviderInstitutional;
import gov.cms.bfd.server.ng.claim.model.institutional.ClaimValue;
import gov.cms.bfd.server.ng.claim.model.institutional.DiagnosisDrgCode;
import gov.cms.bfd.server.ng.claim.model.institutional.OperatingCareTeam;
import gov.cms.bfd.server.ng.claim.model.institutional.OtherInstitutionalCareTeam;
import gov.cms.bfd.server.ng.claim.model.institutional.ReferringInstitutionalCareTeam;
import gov.cms.bfd.server.ng.claim.model.institutional.RenderingCareTeam;
import gov.cms.bfd.server.ng.claim.model.institutional.TypeOfBillCode;
import gov.cms.bfd.server.ng.util.FhirUtil;
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

/** Shared base for institutional claim types (NCH and Shared Systems variants). */
@MappedSuperclass
@Getter
@SuppressWarnings("java:S6539")
public abstract class ClaimInstitutionalBase extends ClaimBase {

  @Column(name = "clm_query_cd")
  private Optional<ClaimQueryCode> claimQueryCode;

  @Embedded private TypeOfBillCode typeOfBillCode;
  @Embedded private DiagnosisDrgCode diagnosisDrgCode;
  @Embedded private BillingProviderInstitutional billingProviderHistory;
  @Embedded private OtherInstitutionalCareTeam otherProviderHistory;
  @Embedded private OperatingCareTeam operatingProviderHistory;
  @Embedded private AttendingCareTeam attendingProviderHistory;
  @Embedded private RenderingCareTeam renderingProviderHistory;
  @Embedded private ReferringInstitutionalCareTeam referringProviderHistory;

  // region Hook Methods

  abstract SupportingInfoComponentBase getDateSupportingInfo();

  abstract SupportingInfoComponentBase getSupportingInfo();

  // Returns the record-type supporting-info stream, limited to one entry defensively. Each subclass
  // produces this
  // from its own concrete record-type field.
  protected abstract List<ExplanationOfBenefit.SupportingInformationComponent>
      buildRecordTypeSupportingInfo();

  // Various supporting info components not covered by previous hooks
  abstract List<ExplanationOfBenefit.SupportingInformationComponent> buildSubclassSupportingInfo();

  abstract List<ClaimValue> getClaimValues();

  protected Optional<ClaimContractorNumber> getClaimContractorNumber() {
    return Optional.empty();
  }

  abstract AdjudicationChargeBase getAdjudicationCharge();

  // Hook to add an adjudication and a total, just for CMS
  protected void addSubclassAdjudication(ExplanationOfBenefit eob) {}

  // Adds care-team members that are unique to the subclass.
  abstract void addSubclassCareTeam(ExplanationOfBenefit eob, SequenceGenerator sequenceGenerator);

  // endregion

  @Override
  public ExplanationOfBenefit toFhir(ClaimFilterOptions options, ClaimState claimState) {
    var eob = super.toFhir(options, claimState);

    addClaimItems(eob, options);
    addDiagnoses(eob);
    addProviders(eob);
    applyOutcomeOverride(eob);
    addAllSupportingInfo(eob);
    addCareTeam(eob);
    addAdjudicationAndPayment(eob);
    addInsurance(eob);

    return sortedEob(eob);
  }

  private void addInsurance(ExplanationOfBenefit eob) {
    eob.addInsurance(getClaimTypeCode().toFhirInsurance(getClaimRecordTypeOptional()));
  }

  /**
   * Overridable by subclasses to allow injecting profile specific supporting info.
   *
   * @return a list of
   */
  protected List<ExplanationOfBenefit.SupportingInformationComponent>
      buildSubclassInitialSupportingInfo() {
    return Stream.of(
            claimQueryCode.map(c -> c.toFhir(supportingInfoFactory)),
            getClaimContractorNumber().map(c -> c.toFhir(supportingInfoFactory)))
        .flatMap(Optional::stream)
        .toList();
  }

  private void addClaimItems(ExplanationOfBenefit eob, ClaimFilterOptions options) {
    getItems()
        .forEach(
            item -> {
              var claimLine = item.getClaimLine().toFhirItemComponent(options);
              var claimContext = getClaimTypeCode().toContext();

              claimLine.ifPresent(eob::addItem);
              item.getClaimLine()
                  .getClaimLineRenderingProvider()
                  .flatMap(
                      provider ->
                          item.getClaimLine()
                              .getClaimLineNumber()
                              .flatMap(
                                  sequence ->
                                      provider.toFhirCareTeamComponent(sequence, claimContext)))
                  .ifPresent(eob::addCareTeam);
              item.getClaimLine()
                  .toFhirSupportingInfo(supportingInfoFactory)
                  .forEach(
                      si -> {
                        eob.addSupportingInfo(si);
                        claimLine.ifPresent(cl -> cl.addInformationSequence(si.getSequence()));
                      });
              item.getProcedure()
                  .flatMap(ClaimProcedureBase::toFhirProcedure)
                  .ifPresent(eob::addProcedure);
            });
  }

  private void addDiagnoses(ExplanationOfBenefit eob) {
    var diagnosisSequenceGenerator = new SequenceGenerator();

    // We ignore procedures with claim diagnosis type 1 since it's always the same as claim
    // diagnosis type E with sequence number 1
    for (var item : getItems()) {
      item.getProcedure()
          .flatMap(
              procedure ->
                  procedure
                      .getDiagnosisType()
                      .filter(type -> type != FIRST)
                      .flatMap(_ -> procedure.toFhirDiagnosis(diagnosisSequenceGenerator)))
          .ifPresent(eob::addDiagnosis);
    }
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
    var sharedInitialSupportingInfo =
        Stream.of(
                getTypeOfBillCode().toFhir(supportingInfoFactory).stream().toList(),
                buildSubclassSupportingInfo())
            .flatMap(Collection::stream)
            .toList();

    // Handle claim related condition codes after BFD-4523
    var claimRelatedConditionCodes =
        getClaimRelatedCondition().stream()
            .flatMap(c -> c.toFhir(supportingInfoFactory).stream())
            .toList();

    Stream.of(
            buildSubclassInitialSupportingInfo(),
            sharedInitialSupportingInfo,
            getDateSupportingInfo().toFhir(supportingInfoFactory),
            buildRecordTypeSupportingInfo(),
            getSupportingInfo().toFhir(supportingInfoFactory),
            getDiagnosisDrgCode().toFhir(supportingInfoFactory).stream().toList(),
            claimRelatedConditionCodes)
        .flatMap(Collection::stream)
        .forEach(eob::addSupportingInfo);
  }

  private void addCareTeam(ExplanationOfBenefit eob) {
    var sequenceGenerator = new SequenceGenerator(eob.getCareTeam().size() + 1);
    var claimContext = getClaimTypeCode().toContext();
    Stream.of(
            getAttendingProviderHistory(),
            getOperatingProviderHistory(),
            getOtherProviderHistory(),
            getRenderingProviderHistory(),
            getReferringProviderHistory())
        .flatMap(p -> p.toFhirCareTeamComponent(sequenceGenerator.next(), claimContext).stream())
        .forEach(eob::addCareTeam);

    addSubclassCareTeam(eob, sequenceGenerator);
  }

  private void addAdjudicationAndPayment(ExplanationOfBenefit eob) {
    getAdjudicationCharge().toFhirTotal().forEach(eob::addTotal);
    getAdjudicationCharge().toFhirAdjudication().forEach(eob::addAdjudication);
    getPaymentComponent().toFhir().ifPresent(eob::setPayment);

    addSubclassAdjudication(eob);
  }

  @Override
  public Optional<Integer> getDrgCode() {
    return getDiagnosisDrgCode().getDiagnosisDrgCode();
  }
}
