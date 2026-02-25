package gov.cms.bfd.server.ng.claim.model;

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
import java.util.TreeSet;
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
@Table(name = "claim_institutional_ss", schema = "idr_new")
@SuppressWarnings({"java:S6539", "java:S2293"})
public class ClaimInstitutionalSharedSystems extends ClaimInstitutionalBase {

  @Embedded private ClaimDateInstitutionalSharedSystems claimDateSupportingInfo;
  @Embedded private AdjudicationChargeInstitutionalSharedSystems adjudicationCharge;
  @Embedded private ClaimRecordTypeInstitutional claimRecordType;
  @Embedded private ClaimInstitutionalSupportingInfoBase supportingInfo;

  @Column(name = "clm_src_id")
  private ClaimSourceId claimSourceId;

  @Column(name = "meta_src_sk")
  private MetaSourceSk metaSourceSk;

  @Column(name = "clm_audt_trl_stus_cd")
  private Optional<String> claimAuditTrailStatusCode;

  @Column(name = "clm_audt_trl_lctn_cd")
  private ClaimAuditTrailLocationCode claimAuditTrailLocationCode;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "clm_uniq_id")
  private SortedSet<ClaimItemInstitutionalSharedSystems> claimItems;

  @Override
  protected List<ClaimValue> getClaimValues() {
    return getClaimItems().stream()
        .map(ClaimItemInstitutionalSharedSystems::getClaimValue)
        .toList();
  }

  /**
   * Returns the system type.
   *
   * @return system type
   */
  public static SystemType getSystemType() {
    return SystemType.SS;
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
    var insurance = new ExplanationOfBenefit.InsuranceComponent();
    insurance.setFocal(true);
    claimRecordType.toFhirReference(getClaimTypeCode()).ifPresent(insurance::setCoverage);
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
        () -> eob.setOutcome(ExplanationOfBenefit.RemittanceOutcome.PARTIAL));
  }

  /** NCH has no additional care-team members beyond the referring provider added by the base. */
  @Override
  protected void addSubclassCareTeam(
      ExplanationOfBenefit eob, SequenceGenerator sequenceGenerator) {
    // no-op for SS
  }

  @Override
  public SortedSet<ClaimItemBase> getItems() {
    return new TreeSet<ClaimItemBase>(getClaimItems());
  }
}
