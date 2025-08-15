package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hl7.fhir.r4.model.Period;

/** Represents the composite primary key for the {@link BeneficiaryEntitlement} entity. */
@EqualsAndHashCode
@NoArgsConstructor
@Getter
@AllArgsConstructor
@Embeddable
public class BeneficiaryEntitlementId implements Serializable {

  @Column(name = "bene_sk")
  private Long beneSk;

  @Column(name = "bene_rng_bgn_dt")
  private LocalDate benefitRangeBeginDate;

  @Column(name = "bene_rng_end_dt")
  private LocalDate benefitRangeEndDate;

  @Column(name = "bene_mdcr_entlmt_type_cd")
  private String medicareEntitlementTypeCode;

  Period toFhirPeriod() {

    Period period = new Period();
    period.setStartElement(DateUtil.toFhirDate(benefitRangeBeginDate));
    period.setEndElement(DateUtil.toFhirDate(benefitRangeEndDate));

    return period;
  }
}
