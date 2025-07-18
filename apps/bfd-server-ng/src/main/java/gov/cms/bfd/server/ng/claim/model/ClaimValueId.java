package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@EqualsAndHashCode
@NoArgsConstructor
@Getter
@AllArgsConstructor
class ClaimValueId implements Serializable {
  @Column(name = "clm_uniq_id", insertable = false, updatable = false)
  private long claimUniqueId;

  @Column(name = "clm_val_sqnc_num")
  private int sequenceNumber;
}
