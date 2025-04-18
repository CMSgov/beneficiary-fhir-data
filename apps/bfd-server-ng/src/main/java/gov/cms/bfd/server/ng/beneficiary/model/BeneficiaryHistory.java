package gov.cms.bfd.server.ng.beneficiary.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Optional;

@Entity
@Table(name = "beneficiary_history", schema = "idr")
public class BeneficiaryHistory {
  @Id
  @Column(name = "bene_sk")
  private long beneSk;

  @Column(name = "bene_xref_efctv_sk_computed")
  private long xrefSk;

  @Column(name = "bene_mbi_id")
  private Optional<String> mbi;
}
