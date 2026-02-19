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
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/**
 * Claim table. Suppress SonarQube Monster Class warning that dependencies to other class should be
 * reduced from 21 to the max 20. Ignore. Class itself is relatively short in lines of code.
 */
@Getter
@Entity
@Table(name = "claim_institutional_nch", schema = "idr_new")
public class ClaimInstitutionalNch extends ClaimInstitutionalBase {

  @Column(name = "clm_cntrctr_num")
  private Optional<ClaimContractorNumber> claimContractorNumber;

  @Column(name = "clm_disp_cd")
  private Optional<ClaimDispositionCode> claimDispositionCode;

  @Embedded private ClaimDateSupportingInfo claimDateSupportingInfo;
  @Embedded private BloodPints bloodPints;
  @Embedded private TypeOfBillCode typeOfBillCode;
  @Embedded private AdjudicationChargeInstitutionalNch adjudicationCharge;
  @Embedded private ClaimPaymentAmount claimPaymentAmount;
  @Embedded private ClaimNearLineRecordType claimRecordType;
  @Embedded private ClaimInstitutionalNchSupportingInfo supportingInfo;
  @Embedded private AdjudicationChargeInstitutional adjudicationChargeInstitutional;
  @Embedded private ServiceCareTeam serviceProviderHistory;
  @Embedded private BillingProviderInstitutional billingProviderHistory;
  @Embedded private OtherInstitutionalCareTeam otherProviderHistory;
  @Embedded private OperatingCareTeam operatingProviderHistory;
  @Embedded private AttendingCareTeam attendingProviderHistory;
  @Embedded private RenderingCareTeam renderingProviderHistory;
  @Embedded private ReferringInstitutionalCareTeam referringProviderHistory;
  @Embedded private DiagnosisDrgCode diagnosisDrgCode;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "clm_uniq_id")
  private SortedSet<ClaimItemInstitutionalNch> claimItems;

  /**
   * NCH-specific initial supporting info: contractor number and disposition code. Blood pints and
   * type-of-bill are in the shared bucket handled by the base class.
   */
  @Override
  protected List<ExplanationOfBenefit.SupportingInformationComponent>
      buildSubclassInitialSupportingInfo() {
    return Stream.of(
            claimContractorNumber.map(c -> c.toFhir(supportingInfoFactory)),
            claimDispositionCode.map(c -> c.toFhir(supportingInfoFactory)))
        .flatMap(Optional::stream)
        .toList();
  }

  /** NCH record-type supporting info limited to one entry. */
  @Override
  protected List<ExplanationOfBenefit.SupportingInformationComponent>
      buildRecordTypeSupportingInfo() {
    return claimRecordType.toFhir(supportingInfoFactory).limit(1).toList();
  }

  /** NCH insurance uses the near-line record variant of the insurance builder. */
  @Override
  protected void addInsurance(ExplanationOfBenefit eob) {
    getClaimTypeCode().toFhirInsuranceNearLineRecord(claimRecordType).ifPresent(eob::addInsurance);
  }

  @Override
  protected List<ClaimValue> getClaimValues() {
    return claimItems.stream().map(ClaimItemInstitutionalNch::getClaimValue).toList();
  }

  @Override
  public ClaimSourceId getClaimSourceId() {
    return ClaimSourceId.NATIONAL_CLAIMS_HISTORY;
  }

  @Override
  protected void addSubclassCareTeam(
      ExplanationOfBenefit eob, SequenceGenerator sequenceGenerator) {
    serviceProviderHistory
        .toFhirCareTeamComponent(sequenceGenerator.next())
        .ifPresent(eob::addCareTeam);
  }
}
