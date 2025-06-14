package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Embeddable
public class ClaimProcedureId implements Serializable {
  @Column(name = "clm_uniq_id", insertable = false, updatable = false)
  private long claimUniqueId;

  @Column(name = "bfd_row_num")
  private int rowNumber;
}
