package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@EqualsAndHashCode
@NoArgsConstructor
@Getter
@AllArgsConstructor
@Embeddable
class ClaimItemId implements Serializable {
  @Column(name = "clm_uniq_id", insertable = false, updatable = false)
  private long claimUniqueId;

  @Column(name = "bfd_row_id", insertable = false, updatable = false)
  private int bfdRowId;
}
