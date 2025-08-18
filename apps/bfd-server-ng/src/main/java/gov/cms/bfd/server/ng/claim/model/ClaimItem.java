package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.Optional;
import lombok.Getter;

/** Claim item table. */
@Getter
@Entity
@Table(name = "claim_item", schema = "idr")
public class ClaimItem {
  @EmbeddedId private ClaimItemId claimItemId;
  @Embedded private ClaimLine claimLine;
  @Embedded private ClaimProcedure claimProcedure;
  @Embedded private ClaimValue claimValue;

  @JoinColumn(name = "clm_uniq_id")
  @ManyToOne
  private Claim claim;

  @JoinColumns({
    @JoinColumn(
        name = "clm_uniq_id",
        insertable = false,
        updatable = false,
        referencedColumnName = "clm_uniq_id"),
    @JoinColumn(
        name = "clm_line_num",
        insertable = false,
        updatable = false,
        referencedColumnName = "clm_line_num")
  })
  @OneToOne
  private ClaimLineInstitutional claimLineInstitutional;

  Optional<ClaimLineInstitutional> getClaimLineInstitutional() {
    return Optional.ofNullable(claimLineInstitutional);
  }
}
