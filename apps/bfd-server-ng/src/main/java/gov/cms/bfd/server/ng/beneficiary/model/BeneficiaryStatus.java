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
  @Column(name = "bene_sk")
  private Long beneSk;

  @Column(name = "mdcr_stus_bgn_dt")
  private LocalDate medicareStatusBeginDate;

  @Column(name = "mdcr_stus_end_dt")
  private LocalDate medicareStatusEndDate;

  @Column(name = "bene_mdcr_stus_cd")
  private String medicareStatusCode;

  @Column(name = "idr_trans_obslt_ts")
  private ZonedDateTime idrTransObsoleteTimestamp;

  @Column(name = "idr_updt_ts")
  private ZonedDateTime idrUpdateTimestamp;

  @Column(name = "bfd_created_ts")
  private ZonedDateTime bfdCreatedTimestamp;

  @Column(name = "bfd_updated_ts")
  private ZonedDateTime bfdUpdatedTimestamp;
}
