package gov.cms.bfd.server.ng.beneficiary.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/** Entity representing the beneficiary XREF table. */
@Entity
@Getter
@Table(name = "beneficiary_xref", schema = "idr")
public class BeneficiaryXref {

  @Id
  @Column(name = "bene_sk")
  private long beneSk;

  @Column(name = "bene_xref_sk")
  private long beneXrefSk;

  @Column(name = "bene_kill_cred_cd")
  private String beneKillCred;
}
