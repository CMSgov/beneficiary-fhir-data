package gov.cms.bfd.server.ng.beneficiary.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import lombok.Getter;

/** Entity representing BeneficiaryEntitlementReason. */
@Entity
@Getter
@Table(name = "beneficiary_entitlement_reason", schema = "idr")
public class BeneficiaryEntitlementReason {

  @Id
  @Column(name = "bene_sk")
  private Long beneSk;

  @Column(name = "bene_rng_bgn_dt")
  private LocalDate benefitRangeBeginDate;

  @Column(name = "bene_rng_end_dt")
  private LocalDate benefitRangeEndDate;

  @Column(name = "bene_mdcr_entlmt_rsn_cd")
  private String medicareEntitlementReasonCode;

  @Column(name = "idr_trans_obslt_ts")
  private ZonedDateTime idrTransObsoleteTimestamp;

  @Column(name = "idr_updt_ts")
  private ZonedDateTime idrUpdateTimestamp;

  @Column(name = "bfd_created_ts")
  private ZonedDateTime bfdCreatedTimestamp;

  @Column(name = "bfd_updated_ts")
  private ZonedDateTime bfdUpdatedTimestamp;
}
