package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.converter.DefaultFalseBooleanConverter;
import gov.cms.bfd.server.ng.coverage.converter.StringToDoubleConverter;
import gov.cms.bfd.server.ng.util.IdrConstants;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.jetbrains.annotations.NotNull;

/** Entity representing BeneficiaryLowIncomeSubsidy. */
@Entity
@Getter
@EqualsAndHashCode
@Table(name = "beneficiary_low_income_subsidy_cmbnd", schema = "idr")
@SQLRestriction(value = "idr_trans_obslt_ts >= DATE '" + IdrConstants.DEFAULT_DATE_STRING + "'")
public class BeneficiaryLowIncomeSubsidy implements Comparable<BeneficiaryLowIncomeSubsidy> {

  @EmbeddedId private BeneficiaryLowIncomeSubsidyId id;

  @Column(name = "bene_cmbnd_deemd_trmntn_dt")
  private LocalDate benefitRangeEndDate;

  @Column(name = "bene_cmbnd_deemd_copmt_lvl_id")
  private Optional<BeneficiaryLISCopaymentLevelCode> copayLevelCode;

  @Column(name = "bene_cmbnd_deemd_prm_pct")
  @Convert(converter = StringToDoubleConverter.class)
  private double partDPremiumPercentage;

  @Column(name = "bene_cmbnd_deemd_ind")
  @Convert(converter = DefaultFalseBooleanConverter.class)
  private boolean beneDeemedInd;

  /**
   * Create copay level code and part D premium percentage extensions.
   *
   * @return optional extension
   */
  public List<Extension> toFhirExtensions() {

    var premiumPercentage = partDPremiumPercentage;
    var extPartDPremiumPercentage =
        new Extension(SystemUrls.EXT_BENE_CMBND_DEEMD_PRM_PCT_URL)
            .setValue(new DecimalType(premiumPercentage));

    var stream =
        new ArrayList<>(
            List.of(
                Optional.of(extPartDPremiumPercentage),
                copayLevelCode.map(BeneficiaryLISCopaymentLevelCode::toFhir)));

    if (beneDeemedInd) {
      stream.add(
          Optional.of(
              new Extension(SystemUrls.EXT_BENE_CMBND_DEEMD_IND).setValue(new StringType("Y"))));
    }

    return stream.stream().flatMap(Optional::stream).toList();
  }

  @Override
  public int compareTo(@NotNull BeneficiaryLowIncomeSubsidy o) {
    return this.id.compareTo(o.id);
  }
}
