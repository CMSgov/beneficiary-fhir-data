package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** Claim item table. */
@Getter
@Entity
@EqualsAndHashCode
@Table(name = "claim_item_institutional_nch", schema = "idr_new")
public class ClaimItemInstitutionalNch implements ClaimItemBase {
  @EmbeddedId private ClaimItemId claimItemId;
  @Embedded private ClaimLineInstitutionalNch claimLine;
  @Embedded private ClaimProcedure claimProcedure;
  @Embedded private ClaimValue claimValue;
  @Embedded private ClaimRelatedCondition claimRelatedCondition;

  @JoinColumn(name = "clm_uniq_id")
  @ManyToOne
  private ClaimInstitutionalNch claim;

  @Override
  public Optional<ClaimProcedureBase> getProcedure() {
    return Optional.of(claimProcedure);
  }

  @Override
  public Optional<ClaimLineHcpcsCode> getClaimLineHcpcsCode() {
    return Optional.of(claimLine.getHcpcsCode());
  }
}
