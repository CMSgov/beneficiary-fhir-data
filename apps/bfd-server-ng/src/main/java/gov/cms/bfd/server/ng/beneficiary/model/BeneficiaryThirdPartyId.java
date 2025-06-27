package gov.cms.bfd.server.ng.beneficiary.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import lombok.Getter;

/**
 * Represents the composite primary key for the {@link BeneficiaryThirdParty} entity. It must be
 * serializable and implement equals() and hashCode().
 */
@Embeddable
@Getter
public class BeneficiaryThirdPartyId implements Serializable {

  //    private static final long serialVersionUID = 1L; // Good practice for Serializable classes

  @Column(name = "bene_sk")
  private Long beneSk;

  @Column(name = "bene_rng_bgn_dt")
  private LocalDate benefitRangeBeginDate;

  @Column(name = "bene_rng_end_dt")
  private LocalDate benefitRangeEndDate;

  @Column(name = "bene_tp_type_cd")
  private String thirdPartyTypeCode;
}
