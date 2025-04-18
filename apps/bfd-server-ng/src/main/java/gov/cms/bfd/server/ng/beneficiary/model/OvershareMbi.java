package gov.cms.bfd.server.ng.beneficiary.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "overshare_mbis", schema = "idr")
public class OvershareMbi {
  @Column(name = "bene_mbi_id")
  @Id
  private String mbi;
}
