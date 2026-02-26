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
@Table(name = "claim_institutional_nch", schema = "idr_new")
@SuppressWarnings({"java:S2293"})
public class ClaimInstitutionalNch extends ClaimInstitutionalBase {

  @Embedded private ClaimDateSupportingInfo claimDateSupportingInfo;
  @Embedded private AdjudicationChargeInstitutionalNch adjudicationCharge;

  @AttributeOverride(name = "claimRecordTypeCode", column = @Column(name = "clm_nrln_ric_cd"))
  @Embedded
  private ClaimRecordType claimRecordType;

  @Embedded private ClaimInstitutionalNchSupportingInfo supportingInfo;
  @Embedded private ServiceCareTeam serviceProviderHistory;
  @Embedded private BloodPints bloodPints;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "clm_uniq_id")
  private SortedSet<ClaimItemInstitutionalNch> claimItems;

  /** NCH record-type supporting info limited to one entry. */
  @Override
  protected List<ExplanationOfBenefit.SupportingInformationComponent>
      buildRecordTypeSupportingInfo() {
    return claimRecordType.toFhir(supportingInfoFactory).stream().toList();
  }

  @Override
  protected List<ExplanationOfBenefit.SupportingInformationComponent>
      buildSubclassSupportingInfo() {
    return Stream.of(bloodPints.toFhir(supportingInfoFactory)).flatMap(Optional::stream).toList();
  }

  @Override
  ClaimSourceId getClaimSourceId() {
    return ClaimSourceId.NATIONAL_CLAIMS_HISTORY;
  }

  @Override
  MetaSourceSk getMetaSourceSk() {
    return MetaSourceSk.NCH;
  }

  @Override
  Optional<ClaimRecordType> getClaimRecordTypeOptional() {
    return Optional.of(claimRecordType);
  }

  @Override
  protected List<ClaimValue> getClaimValues() {
    return getClaimItems().stream().map(ClaimItemInstitutionalNch::getClaimValue).toList();
  }

  /**
   * Returns the system type.
   *
   * @return system type
   */
  public static SystemType getSystemType() {
    return SystemType.NCH;
  }

  @Override
  protected void addSubclassCareTeam(
      ExplanationOfBenefit eob, SequenceGenerator sequenceGenerator) {
    serviceProviderHistory
        .toFhirCareTeamComponent(sequenceGenerator.next())
        .ifPresent(eob::addCareTeam);
  }

  @Override
  public SortedSet<ClaimItemBase> getItems() {
    return new TreeSet<ClaimItemBase>(getClaimItems());
  }
}
