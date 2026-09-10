package gov.cms.bfd.server.ng.claim.model.institutional.entities;

import gov.cms.bfd.server.ng.claim.model.common.ClaimItemBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimPaidStatusCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimRecordType;
import gov.cms.bfd.server.ng.claim.model.common.ClaimSourceId;
import gov.cms.bfd.server.ng.claim.model.common.MetaSourceSk;
import gov.cms.bfd.server.ng.claim.model.common.SharedSystemsClaim;
import gov.cms.bfd.server.ng.claim.model.institutional.AdjudicationChargeInstitutionalRegularSharedSystems;
import gov.cms.bfd.server.ng.converter.ClaimPaidStatusCodeConverter;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.annotation.processing.Generated;
import lombok.Getter;

/** The institutional claim, regular profile, sourced from shared systems. */
@Getter
@Entity
@Table(name = "claim_institutional_ss", schema = "idr")
@Generated("TODO - Remove after query optimization implementation")
public class ClaimInstitutionalRegularSharedSystems extends ClaimInstitutionalRegularBase
    implements SharedSystemsClaim {

  @Column(name = "clm_pd_stus_cd")
  @Convert(converter = ClaimPaidStatusCodeConverter.class)
  private ClaimPaidStatusCode claimPaidStatusCode;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "clm_uniq_id")
  private SortedSet<ClaimItemRegularSharedSystems> claimItems;

  @Column(name = "clm_src_id")
  private ClaimSourceId claimSourceId;

  @Column(name = "meta_src_sk") /**/
  private MetaSourceSk metaSourceSk;

  @AttributeOverride(name = "claimRecordTypeCode", column = @Column(name = "clm_ric_cd"))
  @Embedded
  private ClaimRecordType claimRecordType;

  @Embedded private AdjudicationChargeInstitutionalRegularSharedSystems adjudicationCharge;

  @Override
  public Optional<ClaimRecordType> getClaimRecordTypeOptional() {
    return Optional.of(claimRecordType);
  }

  @Override
  public SortedSet<ClaimItemBase> getItems() {
    return new TreeSet<ClaimItemBase>(getClaimItems());
  }

  @Override
  public Optional<ClaimPaidStatusCode> getClaimPaidStatusCode() {
    return Optional.of(claimPaidStatusCode);
  }
}
