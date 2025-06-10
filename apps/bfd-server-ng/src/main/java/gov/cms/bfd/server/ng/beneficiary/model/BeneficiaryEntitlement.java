package gov.cms.bfd.server.ng.beneficiary.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import lombok.Getter;

/** Entity representing BeneficiaryEntitlement. */
@Entity
@Getter
@Table(name = "beneficiary_entitlement", schema = "idr")
public class BeneficiaryEntitlement {

  @Id
  @Column(name = "bene_sk", nullable = false)
  private Long beneSk;

  @Column(name = "bene_rng_bgn_dt", nullable = false)
  private LocalDate benefitRangeBeginDate;

  @Column(name = "bene_rng_end_dt", nullable = false)
  private LocalDate benefitRangeEndDate;

  @Column(name = "bene_mdcr_entlmt_type_cd", length = 1)
  private String medicareEntitlementTypeCode;

  @Column(name = "bene_mdcr_enrlmt_rsn_cd", length = 1)
  private String medicareEnrollmentReasonCode;

  @Column(name = "bene_mdcr_entlmt_stus_cd", length = 1)
  private String medicareEntitlementStatusCode;

  @Column(name = "idr_trans_efctv_ts", nullable = false)
  private ZonedDateTime idrTransEffectiveTimestamp;

  @Column(name = "idr_trans_obslt_ts", nullable = false)
  private ZonedDateTime idrTransObsoleteTimestamp;

  @Column(name = "idr_updt_ts", nullable = false)
  private ZonedDateTime idrUpdateTimestamp;

  @Column(name = "bfd_created_ts", nullable = false)
  private ZonedDateTime bfdCreatedTimestamp;

  @Column(name = "bfd_updated_ts", nullable = false)
  private ZonedDateTime bfdUpdatedTimestamp;
}
