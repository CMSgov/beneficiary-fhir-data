package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Comparator;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode
@NoArgsConstructor
@Getter
@AllArgsConstructor
@Embeddable
class ClaimItemId implements Serializable, Comparable<ClaimItemId> {
  @Column(name = "clm_uniq_id", insertable = false, updatable = false)
  private long claimUniqueId;

  @Column(name = "bfd_row_id", insertable = false, updatable = false)
  private int bfdRowId;

  @Override
  public int compareTo(@NotNull ClaimItemId o) {
    return Comparator.comparing((ClaimItemId id) -> id.claimUniqueId)
        .thenComparing(id -> id.bfdRowId)
        .compare(this, o);
  }
}
