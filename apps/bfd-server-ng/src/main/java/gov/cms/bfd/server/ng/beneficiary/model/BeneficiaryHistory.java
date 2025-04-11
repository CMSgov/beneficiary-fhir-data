package gov.cms.bfd.server.ng.beneficiary.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "beneficiary_history", schema = "idr")
public class BeneficiaryHistory {
  @Id
  @Column(name = "bene_sk", nullable = false)
  private long beneSk;

  @Column(name = "bene_xref_efctv_sk_computed", nullable = false)
  private long xrefSk;

  @Column(name = "bene_mbi_id", nullable = false)
  private String mbi;
}
