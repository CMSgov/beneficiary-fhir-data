package gov.cms.bfd.server.ng.beneficiary.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * A simplified beneficiary model for use with claims data. Excess data is omitted to avoid
 * over-fetching.
 */
@Entity
@Getter
@Table(name = "beneficiary", schema = "idr")
public class BeneficiarySimple {
  @Id
  @Column(name = "bene_sk")
  private long beneSk;

  @Column(name = "bene_xref_efctv_sk_computed")
  private long xrefSk;
}
