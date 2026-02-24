package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SequenceGenerator;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
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
@Table(name = "claim_institutional_nch", schema = "idr_new")
@SuppressWarnings({"java:S2293"})
public class ClaimInstitutionalNch extends ClaimInstitutionalBase {

  @Embedded private ClaimDateSupportingInfo claimDateSupportingInfo;
  @Embedded private AdjudicationChargeInstitutionalNch adjudicationCharge;
  @Embedded private ClaimNearLineRecordType claimRecordType;
  @Embedded private ClaimInstitutionalNchSupportingInfo supportingInfo;
  @Embedded private ServiceCareTeam serviceProviderHistory;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "clm_uniq_id")
  private SortedSet<ClaimItemInstitutionalNch> claimItems;

  /** NCH record-type supporting info limited to one entry. */
  @Override
  protected List<ExplanationOfBenefit.SupportingInformationComponent>
      buildRecordTypeSupportingInfo() {
    return claimRecordType.toFhir(supportingInfoFactory).limit(1).toList();
  }

  /** NCH insurance uses the near-line record variant of the insurance builder. */
  @Override
  protected void addInsurance(ExplanationOfBenefit eob) {
    var insurance = new ExplanationOfBenefit.InsuranceComponent();
    insurance.setFocal(true);
    claimRecordType.toFhirReference(getClaimTypeCode()).ifPresent(insurance::setCoverage);
    getClaimTypeCode().toFhirInsuranceNearLineRecord(claimRecordType).ifPresent(eob::addInsurance);
  }

  @Override
  protected List<ClaimValue> getClaimValues() {
    return getClaimItems().stream().map(ClaimItemInstitutionalNch::getClaimValue).toList();
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
