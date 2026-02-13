package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/** Claim ANSI signature table. */
@Entity
@Table(name = "claim_ansi_signature", schema = "idr")
@Getter
public class ClaimAnsiSignature extends ClaimAnsiSignatureBase {
  @Id
  @Column(name = "clm_ansi_sgntr_sk", insertable = false, updatable = false)
  private long ansiSignatureSk;
}
