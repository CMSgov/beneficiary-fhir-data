package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.*;
import java.util.Optional;
import lombok.Getter;

/** Contract PBP Number table. */
@Entity
@Getter
@Table(name = "contract_pbp_number", schema = "idr")
public class Contract {
  @Id
  @Column(name = "cntrct_pbp_sk", insertable = false, updatable = false)
  private long contractPbpSk;

  @Column(name = "cntrct_drug_plan_ind_cd")
  private Optional<String> contractDrugPlanCode;

  @Column(name = "cntrct_pbp_type_cd")
  private Optional<String> contractTypeCode;

  @Column(name = "cntrct_pbp_name")
  private Optional<String> contractName;

  @Column(name = "cntrct_num")
  private Optional<String> contractNumber;

  @Column(name = "cntrct_pbp_num")
  private Optional<String> contractPbpNumber;

  @OneToOne(mappedBy = "contract")
  private Claim claim;
}
