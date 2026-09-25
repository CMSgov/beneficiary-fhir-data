package gov.cms.bfd.server.ng.claim.model.institutional.entities;

import gov.cms.bfd.server.ng.claim.model.common.ClaimItemBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimRecordType;
import gov.cms.bfd.server.ng.claim.model.common.ClaimSourceId;
import gov.cms.bfd.server.ng.claim.model.common.MetaSourceSk;
import gov.cms.bfd.server.ng.claim.model.common.NchClaim;
import gov.cms.bfd.server.ng.claim.model.institutional.AdjudicationInstitutionalRegular;
import gov.cms.bfd.server.ng.claim.model.institutional.ServiceCareTeam;
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
import javax.annotation.processing.Generated;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** The institutional claim, regular profile, sourced from nch. */
@Getter
@Entity
@Table(name = "claim_institutional_nch", schema = "idr")
@Generated("TODO - Remove after query optimization implementation")
public class ClaimInstitutionalRegularNch extends ClaimInstitutionalRegularBase
    implements NchClaim {

  @Embedded private ServiceCareTeam serviceProviderHistory;
  @Embedded private AdjudicationInstitutionalRegular adjudicationCharge;

  @AttributeOverride(name = "claimRecordTypeCode", column = @Column(name = "clm_nrln_ric_cd"))
  @Embedded
  private ClaimRecordType claimRecordType;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "clm_uniq_id")
  private SortedSet<ClaimItemInstitutionalRegularNch> claimItems;

  // region Overrides
  @Override
  public SortedSet<ClaimItemBase> getItems() {
    return new TreeSet<ClaimItemBase>(getClaimItems());
  }

  @Override
  protected void addSubclassCareTeam(
      ExplanationOfBenefit eob, SequenceGenerator sequenceGenerator) {
    serviceProviderHistory
        .toFhirCareTeamComponent(sequenceGenerator.next(), Optional.of(getClaimTypeCode()))
        .ifPresent(eob::addCareTeam);
  }

  @Override
  protected List<ExplanationOfBenefit.SupportingInformationComponent>
      buildSubclassSupportingInfo() {
    return claimRecordType.toFhir(supportingInfoFactory).stream().toList();
  }

  @Override
  public ClaimSourceId getClaimSourceId() {
    return ClaimSourceId.NATIONAL_CLAIMS_HISTORY;
  }

  @Override
  public MetaSourceSk getMetaSourceSk() {
    return MetaSourceSk.NCH;
  }
  // endregion

}
