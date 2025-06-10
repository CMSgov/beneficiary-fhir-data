package gov.cms.bfd.server.ng.beneficiary.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import lombok.Getter;

/** Entity representing beneficiary_third_party. */
@Entity
@Getter
@Table(name = "beneficiary_third_party", schema = "idr")
public class BeneficiaryThirdParty {

  @Id
  @Column(name = "bene_sk", nullable = false)
  private Long beneSk;

  @Column(name = "bene_rng_bgn_dt", nullable = false)
  private LocalDate benefitRangeBeginDate;

  @Column(name = "bene_rng_end_dt", nullable = false)
  private LocalDate benefitRangeEndDate;

  @Column(name = "bene_tp_type_cd", nullable = false, length = 1)
  private String thirdPartyTypeCode;

  @Column(name = "idr_trans_efctv_ts", nullable = false)
  private ZonedDateTime idrTransEffectiveTimestamp;

  @Column(name = "bene_buyin_cd", nullable = false, length = 2)
  private String buyInCode;

  @Column(name = "idr_trans_obslt_ts", nullable = false)
  private ZonedDateTime idrTransObsoleteTimestamp;

  @Column(name = "idr_updt_ts")
  private ZonedDateTime idrUpdateTimestamp;

  @Column(name = "bfd_created_ts", nullable = false)
  private ZonedDateTime bfdCreatedTimestamp;

  @Column(name = "bfd_updated_ts", nullable = false)
  private ZonedDateTime bfdUpdatedTimestamp;
}
