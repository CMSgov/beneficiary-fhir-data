package gov.cms.bfd.server.ng.claim.model.institutional.entities;

import gov.cms.bfd.server.ng.ClaimFilterOptions;
import gov.cms.bfd.server.ng.claim.model.common.ClaimAuditTrailLocationCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimAuditTrailStatusCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimItemBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimPaidStatusCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimRecordType;
import gov.cms.bfd.server.ng.claim.model.common.ClaimSourceId;
import gov.cms.bfd.server.ng.claim.model.common.ClaimState;
import gov.cms.bfd.server.ng.claim.model.common.MetaSourceSk;
import gov.cms.bfd.server.ng.claim.model.common.SharedSystemsClaim;
import gov.cms.bfd.server.ng.claim.model.common.SystemType;
import gov.cms.bfd.server.ng.claim.model.institutional.AdjudicationInstitutionalCmsSharedSystems;
import gov.cms.bfd.server.ng.claim.model.institutional.ClaimValue;
import gov.cms.bfd.server.ng.claim.model.institutional.DateSupportingInfoCmsSharedSystems;
import gov.cms.bfd.server.ng.claim.model.institutional.InstitutionalSupportingInfoCmsSharedSystems;
import gov.cms.bfd.server.ng.converter.ClaimPaidStatusCodeConverter;
import gov.cms.bfd.server.ng.util.SequenceGenerator;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.Collection;
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
@Table(name = "claim_institutional_ss", schema = "idr")
@SuppressWarnings({"java:S6539", "java:S2293"})
public class ClaimInstitutionalCmsSharedSystems extends ClaimInstitutionalCmsBase
    implements SharedSystemsClaim {

  @Embedded private DateSupportingInfoCmsSharedSystems dateSupportingInfo;
  @Embedded private AdjudicationInstitutionalCmsSharedSystems adjudicationCharge;
  @Embedded private InstitutionalSupportingInfoCmsSharedSystems supportingInfo;

  @AttributeOverride(name = "claimRecordTypeCode", column = @Column(name = "clm_ric_cd"))
  @Embedded
  private ClaimRecordType claimRecordType;

  @Column(name = "clm_src_id")
  private ClaimSourceId claimSourceId;

  @Column(name = "meta_src_sk")
  private MetaSourceSk metaSourceSk;

  @Column(name = "clm_audt_trl_stus_cd")
  private Optional<String> claimAuditTrailStatusCode;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "clm_uniq_id")
  private SortedSet<ClaimItemInstitutionalCmsSharedSystems> claimItems;

  @Column(name = "clm_pd_stus_cd")
  @Convert(converter = ClaimPaidStatusCodeConverter.class)
  private ClaimPaidStatusCode claimPaidStatusCode;

  /**
   * Creates ExplanationOfBenefit components.
   *
   * @param options claim filter options
   * @param claimState computed claim state
   * @return ExplanationOfBenefit
   */
  @Override
  public ExplanationOfBenefit toFhir(ClaimFilterOptions options, ClaimState claimState) {
    var eob = super.toFhir(options, claimState);
    getClaimItems().stream()
        .map(ClaimItemInstitutionalCmsSharedSystems::toFhir)
        .filter(Optional::isPresent)
        .forEach(adjudications -> adjudications.get().forEach(eob::addAdjudication));
    return eob;
  }

  @Override
  public Optional<ClaimPaidStatusCode> getClaimPaidStatusCode() {
    return Optional.of(claimPaidStatusCode);
  }

  @Override
  public Optional<ClaimRecordType> getClaimRecordTypeOptional() {
    return Optional.of(claimRecordType);
  }

  @Override
  protected List<ExplanationOfBenefit.SupportingInformationComponent>
      buildSubclassSupportingInfo() {
    return Stream.of(
            Stream.of(
                    claimRecordType.toFhir(supportingInfoFactory),
                    Optional.of(claimPaidStatusCode.toFhir(supportingInfoFactory)),
                    buildAuditStatusSupportingInfo())
                .flatMap(Optional::stream)
                .toList(),
            getDateSupportingInfo().toFhir(supportingInfoFactory),
            getSupportingInfo().toFhir(supportingInfoFactory))
        .flatMap(Collection::stream)
        .toList();
  }

  @Override
  public List<ClaimValue> getClaimValues() {
    return getClaimItems().stream()
        .map(ClaimItemInstitutionalCmsSharedSystems::getClaimValue)
        .toList();
  }

  /** NCH has no additional care-team members beyond the referring provider added by the base. */
  @Override
  protected void addSubclassCareTeam(
      ExplanationOfBenefit eob, SequenceGenerator sequenceGenerator) {
    // no-op for SS
  }

  @Override
  protected void addSubclassAdjudication(ExplanationOfBenefit eob) {
    super.addSubclassAdjudication(eob);

    adjudicationCharge.toFhirTotal().forEach(eob::addTotal);
    adjudicationCharge.toFhirAdjudication().forEach(eob::addAdjudication);
  }

  @Override
  public SortedSet<ClaimItemBase> getItems() {
    return new TreeSet<ClaimItemBase>(getClaimItems());
  }

  @Override
  public MetaSourceSk getMetaSourceSk() {
    return metaSourceSk;
  }

  /**
   * Returns the system type.
   *
   * @return system type
   */
  public static SystemType getSystemType() {
    return SystemType.SS;
  }

  private Optional<ExplanationOfBenefit.SupportingInformationComponent>
      buildAuditStatusSupportingInfo() {
    // since audit trail status codes can overlap between the different shared systems, we must
    // specifically handle this code to use the actual corresponding display. Claim audit trail
    // location code is only used to determine codes from VMS so it does not apply here.
    return claimAuditTrailStatusCode
        .flatMap(
            status ->
                ClaimAuditTrailStatusCode.tryFromCode(
                    getMetaSourceSk(), status, ClaimAuditTrailLocationCode.NA))
        .map(code -> code.toFhir(supportingInfoFactory));
  }
}
