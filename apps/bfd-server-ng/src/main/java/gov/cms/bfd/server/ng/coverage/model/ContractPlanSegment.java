package gov.cms.bfd.server.ng.coverage.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Optional;
import lombok.Getter;

/** Contract PBP Segment table. */
@Entity
@Getter
@Table(name = "contract_pbp_segment", schema = "idr")
public class ContractPlanSegment {

  @Id
  @Column(name = "cntrct_pbp_sk", insertable = false, updatable = false)
  private long contractPbpSk;

  @Column(name = "cntrct_pbp_sgmt_num")
  private Optional<String> contractPbpSegmentNumber;
}
