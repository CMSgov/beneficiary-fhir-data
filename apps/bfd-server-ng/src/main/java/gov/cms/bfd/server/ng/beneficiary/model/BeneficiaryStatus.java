package gov.cms.bfd.server.ng.beneficiary.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import lombok.Getter;

/** Entity representing Beneficiary Status. */
@Entity
@Getter
@Table(name = "beneficiary_status", schema = "idr")
public class BeneficiaryStatus {

  @Id
  @Column(name = "bene_sk", nullable = false)
  private Long beneSk;

  @Column(name = "mdcr_stus_bgn_dt", nullable = false)
  private LocalDate medicareStatusBeginDate;

  @Column(name = "mdcr_stus_end_dt", nullable = false)
  private LocalDate medicareStatusEndDate;

  @Column(name = "idr_trans_efctv_ts", nullable = false)
  private ZonedDateTime idrTransEffectiveTimestamp;

  @Column(name = "bene_mdcr_stus_cd", nullable = false, length = 2)
  private String medicareStatusCode;

  @Column(name = "idr_trans_obslt_ts", nullable = false)
  private ZonedDateTime idrTransObsoleteTimestamp;

  @Column(name = "idr_updt_ts")
  private ZonedDateTime idrUpdateTimestamp;

  @Column(name = "bfd_created_ts", nullable = false)
  private ZonedDateTime bfdCreatedTimestamp;

  @Column(name = "bfd_updated_ts", nullable = false)
  private ZonedDateTime bfdUpdatedTimestamp;
}
