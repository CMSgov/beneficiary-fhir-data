package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@Getter
@AllArgsConstructor
@Embeddable
public class ClaimLineInstitutionalId implements Serializable {
  @Column(name = "clm_uniq_id", insertable = false, updatable = false)
  private long claimUniqueId;

  @Column(name = "clm_line_num")
  private int claimLineNumber;
}
