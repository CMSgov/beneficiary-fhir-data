package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.beneficiary.model.BeneficiarySimple;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.jetbrains.annotations.Nullable;

/** Claim table. */
@Entity
@Getter
@Table(name = "claim", schema = "idr")
public class Claim {
  @Id
  @Column(name = "clm_uniq_id", insertable = false, updatable = false)
  private long claimUniqueId;

  @Column(name = "clm_type_cd")
  private ClaimTypeCode claimTypeCode;

  @Column(name = "clm_src_id")
  private ClaimSourceId claimSourceId;

  @Column(name = "clm_efctv_dt")
  private LocalDate claimEffectiveDate;

  @Embedded private Meta meta;
  @Embedded private Identifiers identifiers;
  @Embedded private BillablePeriod billablePeriod;
  @Embedded private ClaimExtensions claimExtensions;
  @Embedded private BillingProvider billingProvider;
  @Embedded private BloodPints bloodPints;
  @Embedded private NchPrimaryPayorCode nchPrimaryPayorCode;
  @Embedded private TypeOfBillCode typeOfBillCode;
  @Embedded private CareTeam careTeam;
  @Embedded private BenefitBalance benefitBalance;
  @Embedded private AdjudicationCharge adjudicationCharge;
  @Embedded private ClaimPaymentAmount claimPaymentAmount;

  @OneToOne
  @JoinColumn(name = "bene_sk")
  private BeneficiarySimple beneficiary;

  @OneToOne
  @JoinColumn(name = "clm_dt_sgntr_sk")
  private ClaimDateSignature claimDateSignature;

  @Nullable
  @OneToOne
  @JoinColumn(name = "clm_uniq_id")
  private ClaimFiss claimFiss;

  @Nullable
  @OneToOne
  @JoinColumn(name = "clm_uniq_id")
  private ClaimInstitutional claimInstitutional;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "clm_uniq_id")
  private Set<ClaimItem> claimItems;

  Optional<ClaimInstitutional> getClaimInstitutional() {
    return Optional.ofNullable(claimInstitutional);
  }

  Optional<ClaimFiss> getClaimFiss() {
    return Optional.ofNullable(claimFiss);
  }

  /**
   * Convert the claim info to a FHIR ExplanationOfBenefit.
   *
   * @return ExplanationOfBenefit representing this claim
   */
  public ExplanationOfBenefit toFhir() {
    return ClaimToFhirTranslator.toFhir(this);
  }

  /**
   * Returns the latest bfd_updated_ts across this claim and related entities that are part of the
   * FHIR representation (claim items, institutional lines, ansi signatures, institutional claim).
   * If none are available, returns the claim's own meta timestamp.
   *
   * @return the latest updated timestamp for this claim or null if not available
   */
  public ZonedDateTime getLatestUpdatedTimestamp() {
    var candidate = meta == null ? null : meta.getUpdatedTimestamp();

    candidate = getMaxFromClaimDateSignature(candidate);
    candidate = getMaxFromClaimInstitutional(candidate);
    candidate = getMaxFromClaimFiss(candidate);
    candidate = getMaxFromClaimItems(candidate);

    return candidate;
  }

  private ZonedDateTime getMaxFromClaimDateSignature(ZonedDateTime candidate) {
    candidate = max(candidate, claimDateSignature.getBfdUpdatedTimestamp());
    return candidate;
  }

  private ZonedDateTime getMaxFromClaimInstitutional(ZonedDateTime candidate) {
    return getClaimInstitutional()
        .map(ClaimInstitutional::getBfdUpdatedTimestamp)
        .map(ts -> max(candidate, ts))
        .orElse(candidate);
  }

  private ZonedDateTime getMaxFromClaimFiss(ZonedDateTime candidate) {
    return getClaimFiss()
        .map(ClaimFiss::getBfdUpdatedTimestamp)
        .map(ts -> max(candidate, ts))
        .orElse(candidate);
  }

  private ZonedDateTime getMaxFromClaimItems(ZonedDateTime candidate) {
    if (claimItems != null) {
      for (var item : claimItems) {
        if (item == null) continue;
        candidate = getMaxFromClaimItem(candidate, item);
      }
    }
    return candidate;
  }

  private ZonedDateTime getMaxFromClaimItem(ZonedDateTime candidate, ClaimItem item) {
    candidate = max(candidate, item.getBfdUpdatedTimestamp());
    var lineInstOpt = item.getClaimLineInstitutional();
    if (lineInstOpt.isPresent()) {
      var lineInst = lineInstOpt.get();
      candidate = getMaxFromLineInstitutional(candidate, lineInst);
    }
    return candidate;
  }

  private ZonedDateTime getMaxFromLineInstitutional(
      ZonedDateTime candidate, ClaimLineInstitutional lineInst) {
    // lineInst is present because we only call this when the optional is present.
    // bfd_updated_ts columns are NOT NULL in the DDL, so use the timestamps
    // directly.
    candidate = max(candidate, lineInst.getBfdUpdatedTimestamp());
    var ansiOpt = lineInst.getAnsiSignature();
    if (ansiOpt.isPresent()) {
      candidate = max(candidate, ansiOpt.get().getBfdUpdatedTimestamp());
    }
    return candidate;
  }

  private ZonedDateTime max(ZonedDateTime a, ZonedDateTime b) {
    if (a == null) return b;
    if (b == null) return a;
    return a.isAfter(b) ? a : b;
  }

  /**
   * Returns a list of ANSI signatures attached to this claim (one per institutional line where
   * present).
   *
   * @return list of {@link ClaimAnsiSignature} instances attached to this claim (may be empty)
   */
  public List<ClaimAnsiSignature> getAnsiSignatures() {
    if (claimItems == null) return List.of();
    return claimItems.stream()
        .map(ClaimItem::getClaimLineInstitutional)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(ClaimLineInstitutional::getAnsiSignature)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }
}
