package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.StringType;

/** Claim header-level NCH benefit enhancement switches. */
@Embeddable
@Getter
public class NchBenefitEnhancementSwitches {

  @Column(name = "clm_ngaco_pbpmt_sw")
  private Optional<String> pbpBenefitEnhancementIndicator;

  @Column(name = "clm_ngaco_pdschrg_hcbs_sw")
  private Optional<String> postDischargeHomeVisitBenefitEnhancementIndicator;

  @Column(name = "clm_ngaco_snf_wvr_sw")
  private Optional<String> snf3DayWaiverEnhancement;

  @Column(name = "clm_ngaco_tlhlth_sw")
  private Optional<String> telehealthBenefitEnhancementIndicator;

  @Column(name = "clm_ngaco_cptatn_sw")
  private Optional<String> aipbpBenefitEnhancementIndicator;

  @Column(name = "clm_aco_care_mgmt_hcbs_sw")
  private Optional<String> careManagementHomeVisitsEnhancement;

  /**
   * Converts the switch values to a list of FHIR SupportingInformationComponent.
   *
   * @param supportingInfoFactory the supportingInfoFactory containing the other mappings.
   * @return SupportingInfoComponent list
   */
  public List<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {

    return Stream.of(
            toSupportingInfo(
                pbpBenefitEnhancementIndicator,
                BlueButtonSupportingInfoCategory.CLM_NGACO_PBPMT_SW,
                supportingInfoFactory),
            toSupportingInfo(
                postDischargeHomeVisitBenefitEnhancementIndicator,
                BlueButtonSupportingInfoCategory.CLM_NGACO_PDSCHRG_HCBS_SW,
                supportingInfoFactory),
            toSupportingInfo(
                snf3DayWaiverEnhancement,
                BlueButtonSupportingInfoCategory.CLM_NGACO_SNF_WVR_SW,
                supportingInfoFactory),
            toSupportingInfo(
                telehealthBenefitEnhancementIndicator,
                BlueButtonSupportingInfoCategory.CLM_NGACO_TLHLTH_SW,
                supportingInfoFactory),
            toSupportingInfo(
                aipbpBenefitEnhancementIndicator,
                BlueButtonSupportingInfoCategory.CLM_NGACO_CPTATN_SW,
                supportingInfoFactory),
            toSupportingInfo(
                careManagementHomeVisitsEnhancement,
                BlueButtonSupportingInfoCategory.CLM_ACO_CARE_MGMT_HCBS_SW,
                supportingInfoFactory))
        .flatMap(Optional::stream)
        .toList();
  }

  private Optional<ExplanationOfBenefit.SupportingInformationComponent> toSupportingInfo(
      Optional<String> value,
      BlueButtonSupportingInfoCategory category,
      SupportingInfoFactory supportingInfoFactory) {
    return value.map(
        v ->
            supportingInfoFactory
                .createSupportingInfo()
                .setCategory(category.toFhir())
                .setValue(new StringType(v)));
  }
}
