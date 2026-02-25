package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/** Claim item table. */
@Getter
@Entity
@EqualsAndHashCode
@Table(name = "claim_item", schema = "idr")
public class ClaimItem implements Comparable<ClaimItem> {
  @EmbeddedId private ClaimItemId claimItemId;
  @Embedded private ClaimLine claimLine;
  @Embedded private ClaimProcedureInstitutional claimProcedureInstitutional;
  @Embedded private ClaimValue claimValue;
  @Embedded private ClaimLineRxNumber claimLineRxNum;
  @Embedded private ClaimRelatedCondition claimRelatedCondition;
  @Embedded private ClaimItemOptional claimItemOptional;

  @JoinColumn(name = "clm_uniq_id")
  @ManyToOne
  private Claim claim;

  @Override
  public int compareTo(@NotNull ClaimItem o) {
    return claimItemId.compareTo(o.claimItemId);
  }
}
