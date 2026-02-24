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
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/**
 * Claim table. Suppress SonarQube Monster Class warning that dependencies to other class should be
 * reduced from 21 to the max 20. Ignore. Class itself is relatively short in lines of code.
 * Suppress SonarQube warning to replace type specification with diamond operator since it can't
 * infer the type for getItems()
 */
@Getter
@Entity
@Table(name = "claim_professional_ss", schema = "idr_new")
@SuppressWarnings({"JpaAttributeTypeInspection", "java:S2293"})
public class ClaimProfessionalSharedSystems extends ClaimProfessionalBase {

  @Column(name = "clm_sbmt_frmt_cd")
  private Optional<ClaimSubmissionFormatCode> claimFormatCode;

  @Column(name = "clm_prvdr_acnt_rcvbl_ofst_amt")
  private BigDecimal providerOffsetAmount;

  @Column(name = "clm_mdcr_prfnl_prvdr_asgnmt_sw")
  private Optional<ProviderAssignmentIndicatorSwitch> providerAssignmentIndicatorSwitch;

  @Embedded private NchPrimaryPayorCode nchPrimaryPayorCode;
  @Embedded private AdjudicationChargeProfessionalSharedSystems adjudicationCharge;
  @Embedded private OtherProfessionalCareTeam otherProviderHistory;

  @Column(name = "clm_audt_trl_stus_cd")
  private Optional<String> claimAuditTrailStatusCode;

  @Column(name = "clm_audt_trl_lctn_cd")
  private ClaimAuditTrailLocationCode claimAuditTrailLocationCode;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "clm_uniq_id")
  private SortedSet<ClaimItemProfessionalSharedSystems> claimItems;

  /**
   * SS-specific supporting info: blood pints, primary payor code, contractor number, submission
   * date, provider assignment switch, and clinical trial number.
   */
  @Override
  protected List<ExplanationOfBenefit.SupportingInformationComponent>
      buildSubclassSupportingInfo() {
    return Stream.of(
            nchPrimaryPayorCode.toFhir(supportingInfoFactory),
            providerAssignmentIndicatorSwitch.map(c -> c.toFhir(supportingInfoFactory)))
        .flatMap(Optional::stream)
        .toList();
  }

  /** SS Rx supporting info: PDE format code (header level) and per-line Rx numbers. */
  @Override
  protected List<ExplanationOfBenefit.SupportingInformationComponent> buildRxSupportingInfo() {
    return Stream.concat(
            // Header-level: format code, only when this is a PDE subtype claim.
            claimFormatCode
                .filter(c -> getClaimTypeCode().isClaimSubtype(PDE))
                .map(c -> c.toFhir(supportingInfoFactory))
                .stream(),
            // Line-level: Rx number from each claim item.
            getClaimItems().stream()
                .map(item -> item.getClaimLineRxNum().toFhir(supportingInfoFactory))
                .flatMap(Optional::stream))
        .toList();
  }

  /** SS adjudication: provider account-receivable offset amount. */
  @Override
  protected void addSubclassAdjudication(ExplanationOfBenefit eob) {
    eob.addAdjudication(
        AdjudicationChargeType.PROVIDER_OFFSET_AMOUNT.toFhirAdjudication(providerOffsetAmount));
  }

  /**
   * SS also adds the {@code otherProviderHistory} care-team member alongside the referring provider
   * that the base class handles.
   */
  @Override
  protected void addSubclassCareTeam(
      ExplanationOfBenefit eob, SequenceGenerator sequenceGenerator) {
    otherProviderHistory
        .toFhirCareTeamComponent(sequenceGenerator.next())
        .ifPresent(eob::addCareTeam);
  }

  /**
   * For PAC claims, the outcome is driven by the audit-trail status code, audit-status code, and
   * the meta source sk rather than the default claim-type outcome.
   */
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

  @Override
  public SortedSet<ClaimItemBase> getItems() {
    return new TreeSet<ClaimItemBase>(getClaimItems());
  }
}
