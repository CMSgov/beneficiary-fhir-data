package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SequenceGenerator;
import jakarta.persistence.AttributeOverride;
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
@Table(name = "claim_professional_nch", schema = "idr_new")
@SuppressWarnings({"JpaAttributeTypeInspection", "java:S2293"})
public class ClaimProfessionalNch extends ClaimProfessionalBase {

  @Column(name = "clm_disp_cd")
  private Optional<ClaimDispositionCode> claimDispositionCode;

  @Column(name = "clm_query_cd")
  private Optional<ClaimQueryCode> claimQueryCode;

  @Column(name = "clm_mdcr_prfnl_prmry_pyr_amt")
  private BigDecimal primaryProviderPaidAmount;

  @Column(name = "clm_carr_pmt_dnl_cd")
  private Optional<ClaimPaymentDenialCode> claimPaymentDenialCode;

  @Embedded private AdjudicationChargeProfessionalNch adjudicationCharge;
  @Embedded private NchWeeklyProcessingDate nchWeeklyProcessingDate;
  @Embedded private BloodPints bloodPints;

  @AttributeOverride(name = "claimRecordTypeCode", column = @Column(name = "clm_nrln_ric_cd"))
  @Embedded
  private ClaimRecordType claimRecordType;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "clm_uniq_id")
  private SortedSet<ClaimItemProfessionalNch> claimItems;

  @Override
  protected List<ExplanationOfBenefit.SupportingInformationComponent>
      buildSubclassSupportingInfo() {
    return Stream.of(
            claimDispositionCode.map(c -> c.toFhir(supportingInfoFactory)),
            claimQueryCode.map(c -> c.toFhir(supportingInfoFactory)),
            nchWeeklyProcessingDate.toFhir(supportingInfoFactory),
            bloodPints.toFhir(supportingInfoFactory),
            claimPaymentDenialCode.map(c -> c.toFhir(supportingInfoFactory)))
        .flatMap(Optional::stream)
        .toList();
  }

  @Override
  Optional<ClaimRecordType> getClaimRecordTypeOptional() {
    return Optional.of(claimRecordType);
  }

  /** NCH adjudication: payer-paid (primary provider paid) amount. */
  @Override
  protected void addSubclassAdjudication(ExplanationOfBenefit eob) {
    eob.addAdjudication(
        AdjudicationChargeType.PAYER_PAID_AMOUNT.toFhirAdjudication(primaryProviderPaidAmount));
  }

  @Override
  ClaimSourceId getClaimSourceId() {
    return ClaimSourceId.NATIONAL_CLAIMS_HISTORY;
  }

  @Override
  MetaSourceSk getMetaSourceSk() {
    return MetaSourceSk.NCH;
  }

  /**
   * Returns the system type.
   *
   * @return system type
   */
  public static SystemType getSystemType() {
    return SystemType.NCH;
  }

  /** NCH has no additional care-team members beyond the referring provider added by the base. */
  @Override
  protected void addSubclassCareTeam(
      ExplanationOfBenefit eob, SequenceGenerator sequenceGenerator) {
    // no-op for NCH
  }

  @Override
  public SortedSet<ClaimItemBase> getItems() {
    return new TreeSet<ClaimItemBase>(getClaimItems());
  }
}
