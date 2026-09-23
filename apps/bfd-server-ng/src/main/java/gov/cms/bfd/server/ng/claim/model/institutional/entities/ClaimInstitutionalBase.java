package gov.cms.bfd.server.ng.claim.model.institutional.entities;

import static gov.cms.bfd.server.ng.claim.model.common.ClaimDiagnosisType.*;

import gov.cms.bfd.server.ng.ClaimFilterOptions;
import gov.cms.bfd.server.ng.claim.model.common.AdjudicationEmbedded;
import gov.cms.bfd.server.ng.claim.model.common.ClaimContractorNumber;
import gov.cms.bfd.server.ng.claim.model.common.ClaimQueryCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimRelatedCondition;
import gov.cms.bfd.server.ng.claim.model.common.ClaimState;
import gov.cms.bfd.server.ng.claim.model.common.ProcedureBase;
import gov.cms.bfd.server.ng.claim.model.common.entities.ClaimBase;
import gov.cms.bfd.server.ng.claim.model.institutional.AttendingCareTeam;
import gov.cms.bfd.server.ng.claim.model.institutional.BillingProviderInstitutional;
import gov.cms.bfd.server.ng.claim.model.institutional.DiagnosisDrgCode;
import gov.cms.bfd.server.ng.claim.model.institutional.OperatingCareTeam;
import gov.cms.bfd.server.ng.claim.model.institutional.OtherInstitutionalCareTeam;
import gov.cms.bfd.server.ng.claim.model.institutional.ReferringInstitutionalCareTeam;
import gov.cms.bfd.server.ng.claim.model.institutional.RenderingCareTeam;
import gov.cms.bfd.server.ng.claim.model.institutional.TypeOfBillCode;
import gov.cms.bfd.server.ng.util.FhirUtil;
import gov.cms.bfd.server.ng.util.SequenceGenerator;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;

/** Shared base for institutional claim types (NCH and Shared Systems variants). */
@MappedSuperclass
@Getter
@SuppressWarnings("java:S6539")
abstract class ClaimInstitutionalBase extends ClaimBase {

  @Column(name = "clm_query_cd")
  private Optional<ClaimQueryCode> claimQueryCode;

  @Column(name = "clm_ptnt_cntl_num")
  private Optional<String> patientControlNumber;

  @Embedded private TypeOfBillCode typeOfBillCode;
  @Embedded private DiagnosisDrgCode diagnosisDrgCode;
  @Embedded private BillingProviderInstitutional billingProviderHistory;
  @Embedded private OtherInstitutionalCareTeam otherProviderHistory;
  @Embedded private OperatingCareTeam operatingProviderHistory;
  @Embedded private AttendingCareTeam attendingProviderHistory;
  @Embedded private RenderingCareTeam renderingProviderHistory;
  @Embedded private ReferringInstitutionalCareTeam referringProviderHistory;
  @Embedded private ClaimRelatedCondition claimRelatedCondition;

  // region Hook Methods

  // construct a list of supportinginfo from all base classes, call super.
  protected List<ExplanationOfBenefit.SupportingInformationComponent>
      buildSubclassSupportingInfo() {
    return List.of();
  }

  protected Optional<ClaimContractorNumber> getClaimContractorNumber() {
    return Optional.empty();
  }

  // add an adjudication and a total, just for CMS
  protected void addSubclassAdjudication(ExplanationOfBenefit eob) {}

  // Adds care-team members that are unique to the subclass, irrelevant to SharedSystems
  protected void addSubclassCareTeam(
      ExplanationOfBenefit eob, SequenceGenerator sequenceGenerator) {}

  protected Optional<AdjudicationEmbedded> getAdjudication() {
    return Optional.empty();
  }

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
    addPatientControlNumberIdentifier(eob);

    return sortedEob(eob);
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

  private void addClaimItems(ExplanationOfBenefit eob, ClaimFilterOptions options) {
    getItems()
        .forEach(
            item -> {
              var claimLine = item.getClaimLine().toFhirItemComponent(options);

              claimLine.ifPresent(eob::addItem);
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
              item.getClaimLine()
                  .toFhirSupportingInfo(supportingInfoFactory)
                  .forEach(
                      si -> {
                        eob.addSupportingInfo(si);
                        claimLine.ifPresent(cl -> cl.addInformationSequence(si.getSequence()));
                      });
              item.getProcedureOptional()
                  .flatMap(ProcedureBase::toFhirProcedure)
                  .ifPresent(eob::addProcedure);
            });
  }

  private void addDiagnoses(ExplanationOfBenefit eob) {
    var diagnosisSequenceGenerator = new SequenceGenerator();

    // We ignore procedures with claim diagnosis type 1 since it's always the same as claim
    // diagnosis type E with sequence number 1
    for (var item : getItems()) {
      item.getProcedureOptional()
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
    var initialSupportingInfo =
        Stream.of(
                claimQueryCode.map(c -> c.toFhir(supportingInfoFactory)),
                getClaimContractorNumber().map(c -> c.toFhir(supportingInfoFactory)))
            .flatMap(Optional::stream)
            .toList();

    Stream.of(
            initialSupportingInfo,
            getTypeOfBillCode().toFhir(supportingInfoFactory).stream().toList(),
            buildSubclassSupportingInfo(),
            getDiagnosisDrgCode().toFhir(supportingInfoFactory).stream().toList(),
            getClaimRelatedCondition().toFhir(supportingInfoFactory).stream().toList())
        .flatMap(Collection::stream)
        .forEach(eob::addSupportingInfo);
  }

  private void addCareTeam(ExplanationOfBenefit eob) {
    var sequenceGenerator = new SequenceGenerator(eob.getCareTeam().size() + 1);
    Stream.of(
            getAttendingProviderHistory(),
            getOperatingProviderHistory(),
            getOtherProviderHistory(),
            getRenderingProviderHistory(),
            getReferringProviderHistory())
        .flatMap(
            p ->
                p
                    .toFhirCareTeamComponent(
                        sequenceGenerator.next(), Optional.of(getClaimTypeCode()))
                    .stream())
        .forEach(eob::addCareTeam);

    addSubclassCareTeam(eob, sequenceGenerator);
  }

  private void addAdjudicationAndPayment(ExplanationOfBenefit eob) {
    getPaymentComponent().toFhir().ifPresent(eob::setPayment);
    addSubclassAdjudication(eob);
  }

  @Override
  public Optional<Integer> getDrgCode() {
    return getDiagnosisDrgCode().getDiagnosisDrgCode();
  }
}
