package gov.cms.bfd.server.ng.claim.model;

import static gov.cms.bfd.server.ng.claim.model.ClaimSubtype.PDE;

import gov.cms.bfd.server.ng.util.SequenceGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/**
 * Claim table. Suppress SonarQube Monster Class warning that dependencies to other class should be
 * reduced from 21 to the max 20. Ignore. Class itself is relatively short in lines of code.
 */
@Getter
@Entity
@Table(name = "claim_institutional_ss", schema = "idr_new")
@SuppressWarnings("java:S6539")
public class ClaimInstitutionalSharedSystems extends ClaimInstitutionalBase {

  @Column(name = "clm_sbmt_frmt_cd")
  private Optional<ClaimSubmissionFormatCode> claimFormatCode;

  @Column(name = "clm_cntrctr_num")
  private Optional<ClaimContractorNumber> claimContractorNumber;

  @Embedded private BloodPints bloodPints;
  @Embedded private NchPrimaryPayorCode nchPrimaryPayorCode;
  @Embedded private TypeOfBillCode typeOfBillCode;
  @Embedded private ClaimDateInstitutionalSharedSystems claimDateSupportingInfo;
  @Embedded private BillingProviderInstitutional billingProviderHistory;
  @Embedded private OtherInstitutionalCareTeam otherProviderHistory;
  @Embedded private OperatingCareTeam operatingProviderHistory;
  @Embedded private AttendingCareTeam attendingProviderHistory;
  @Embedded private RenderingCareTeam renderingProviderHistory;
  @Embedded private ReferringInstitutionalCareTeam referringProviderHistory;
  @Embedded private AdjudicationCharge adjudicationCharge;
  @Embedded private ClaimPaymentAmount claimPaymentAmount;
  @Embedded private ClaimRecordTypeInstitutional claimRecordType;
  @Embedded private ClaimInstitutionalSupportingInfoBase supportingInfo;
  @Embedded private AdjudicationChargeInstitutional adjudicationChargeInstitutional;
  @Embedded private DiagnosisDrgCode diagnosisDrgCode;

  @Column(name = "clm_audt_trl_stus_cd")
  private Optional<String> claimAuditTrailStatusCode;

  @Column(name = "clm_audt_trl_lctn_cd")
  private ClaimAuditTrailLocationCode claimAuditTrailLocationCode;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "clm_uniq_id")
  private SortedSet<ClaimItemInstitutionalSharedSystems> claimItems;

  @Override
  protected List<ClaimValue> getClaimValues() {
    return claimItems.stream().map(ClaimItemInstitutionalSharedSystems::getClaimValue).toList();
  }

  /**
   * SS-specific initial supporting info: contractor number, primary payor code, and optionally the
   * PDE submission format code. Blood pints and type-of-bill are in the shared bucket handled by
   * the base class.
   */
  @Override
  protected List<ExplanationOfBenefit.SupportingInformationComponent>
      buildSubclassInitialSupportingInfo() {
    return Stream.of(
            claimContractorNumber.map(c -> c.toFhir(supportingInfoFactory)),
            nchPrimaryPayorCode.toFhir(supportingInfoFactory),
            claimFormatCode
                .filter(c -> getClaimTypeCode().isClaimSubtype(PDE))
                .map(c -> c.toFhir(supportingInfoFactory)))
        .flatMap(Optional::stream)
        .toList();
  }

  /** SS record-type supporting info from limited to one entry. */
  @Override
  protected List<ExplanationOfBenefit.SupportingInformationComponent>
      buildRecordTypeSupportingInfo() {
    return claimRecordType.toFhir(supportingInfoFactory).limit(1).toList();
  }

  /** SS insurance uses the institutional variant of the insurance builder. */
  @Override
  protected void addInsurance(ExplanationOfBenefit eob) {
    getClaimTypeCode().toFhirInsuranceInstitutional(claimRecordType).ifPresent(eob::addInsurance);
  }

  @Override
  protected void applyOutcomeOverride(ExplanationOfBenefit eob) {
    var auditTrailStatusCode =
        claimAuditTrailStatusCode.flatMap(
            status ->
                ClaimAuditTrailStatusCode.tryFromCode(
                    getMetaSourceSk(), status, claimAuditTrailLocationCode));
    auditTrailStatusCode.ifPresentOrElse(
        status -> eob.setOutcome(status.getOutcome(getFinalAction())),
        () -> {
          if (getClaimTypeCode().isPac()) {
            eob.setOutcome(ExplanationOfBenefit.RemittanceOutcome.PARTIAL);
          }
        });
  }

  /** NCH has no additional care-team members beyond the referring provider added by the base. */
  @Override
  protected void addSubclassCareTeam(
      ExplanationOfBenefit eob, SequenceGenerator sequenceGenerator) {
    // no-op for SS
  }
}
