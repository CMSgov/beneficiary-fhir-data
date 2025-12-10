package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.coverage.converter.StringToDoubleConverter;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Extension;
import org.jetbrains.annotations.NotNull;

/** Entity representing BeneficiaryLowIncomeSubsidy. */
@Entity
@Getter
@EqualsAndHashCode
@Table(name = "beneficiary_low_income_subsidy", schema = "idr")
public class BeneficiaryLowIncomeSubsidy implements Comparable<BeneficiaryLowIncomeSubsidy> {

  @EmbeddedId private BeneficiaryLowIncomeSubsidyId id;

  @Column(name = "bene_rng_end_dt")
  private LocalDate benefitRangeEndDate;

  @Column(name = "bene_lis_copmt_lvl_cd")
  private Optional<BeneficiaryLISCopaymentLevelCode> copayLevelCode;

  @Column(name = "bene_lis_ptd_prm_pct")
  @Convert(converter = StringToDoubleConverter.class)
  private double partDPremiumPercentage;

  @Column(name = "bfd_updated_ts")
  private ZonedDateTime bfdUpdatedTimestamp;

  /**
   * Create copay level code and part D premium percentage extensions.
   *
   * @return optional extension
   */
  public List<Extension> toFhirExtensions() {

    var extPartDPremiumPercentage =
        new Extension(SystemUrls.EXT_BENE_LIS_PTD_PRM_PCT_URL)
            .setValue(new DecimalType(partDPremiumPercentage));

    return Stream.of(
            Optional.of(extPartDPremiumPercentage),
            copayLevelCode.map(BeneficiaryLISCopaymentLevelCode::toFhir))
        .flatMap(Optional::stream)
        .toList();
  }

  @Override
  public int compareTo(@NotNull BeneficiaryLowIncomeSubsidy o) {
    return this.id.compareTo(o.id);
  }
}
