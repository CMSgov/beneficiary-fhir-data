package gov.cms.bfd.server.ng.coverage.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Represents the composite primary key for the {@link BeneficiaryThirdParty} entity. */
@EqualsAndHashCode
@NoArgsConstructor
@Getter
@AllArgsConstructor
@Embeddable
public class BeneficiaryThirdPartyId implements Serializable {

  @Column(name = "bene_sk")
  private Long beneSk;

  @Column(name = "bene_rng_bgn_dt")
  private LocalDate benefitRangeBeginDate;

  @Column(name = "bene_rng_end_dt")
  private LocalDate benefitRangeEndDate;

  @Column(name = "bene_tp_type_cd")
  private String thirdPartyTypeCode;
}
