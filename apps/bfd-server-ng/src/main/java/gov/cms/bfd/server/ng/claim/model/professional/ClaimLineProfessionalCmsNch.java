package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.ClaimFilterOptions;
import gov.cms.bfd.server.ng.claim.model.common.AdjudicationEmbedded;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineBase;
import gov.cms.bfd.server.ng.claim.model.common.NchBenefitEnhancementSwitches;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoFactory;
import gov.cms.bfd.server.ng.converter.NonZeroDoubleConverter;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;

/** clm_line data for Professional-CMS-NCH. */
@Embeddable
@Getter
@AttributeOverride(name = "trackingNumber", column = @Column(name = "clm_line_pmd_uniq_trkng_num"))
public class ClaimLineProfessionalCmsNch extends ClaimLineProfessionalCms implements ClaimLineBase {

  @Embedded private ClaimLineProfessionalNchCore nchCore;
  @Embedded private ClaimLineAdjudicationProfessionalCmsNch adjudicationCharge;
  @Embedded private ExtensionsProfessionalCmsNch extensionsProfessionalCmsNch;
  @Embedded private RenderingProviderSsaStateCode renderingProviderSsaStateCode;

  @Embedded
  @AttributeOverride(
      name = "pbpBenefitEnhancementIndicator",
      column = @Column(name = "clm_line_ngaco_pbpmt_sw"))
  @AttributeOverride(
      name = "postDischargeHomeVisitBenefitEnhancementIndicator",
      column = @Column(name = "clm_line_ngaco_pdschrg_hcbs_sw"))
  @AttributeOverride(
      name = "snf3DayWaiverEnhancement",
      column = @Column(name = "clm_line_ngaco_snf_wvr_sw"))
  @AttributeOverride(
      name = "telehealthBenefitEnhancementIndicator",
      column = @Column(name = "clm_line_ngaco_tlhlth_sw"))
  @AttributeOverride(
      name = "aipbpBenefitEnhancementIndicator",
      column = @Column(name = "clm_line_ngaco_cptatn_sw"))
  @AttributeOverride(
      name = "careManagementHomeVisitsEnhancement",
      column = @Column(name = "clm_line_aco_care_mgmt_hcbs_sw"))
  private NchBenefitEnhancementSwitches lineNchBenefitEnhancementSwitches;

  @Column(name = "clm_line_hct_hgb_type_cd")
  private Optional<ClaimLineHCTHGBTestTypeCode> claimLineHCTHGBTestTypeCode;

  @Column(name = "clm_line_hct_hgb_rslt_num")
  @Convert(converter = NonZeroDoubleConverter.class)
  private Optional<Double> claimLineHCTHGBTestResult;

  @Override
  void populateProductAndQuantity(ExplanationOfBenefit.ItemComponent line) {
    nchCore.populateProductAndQuantity(line, getHcpcsCode(), getServiceUnitQuantity());
  }

  /**
   * Return claim observation data if available.
   *
   * @param bfdRowId Observation ID
   * @return claim Observation
   */
  @Override
  public Optional<Observation> toFhirObservation(int bfdRowId) {
    if (claimLineHCTHGBTestTypeCode.isEmpty() || claimLineHCTHGBTestResult.isEmpty()) {
      return Optional.empty();
    }

    var observation = new Observation();
    observation.setId(String.valueOf(bfdRowId));
    Quantity quantity = new Quantity();
    claimLineHCTHGBTestTypeCode.ifPresent(
        testTypeCode -> {
          observation.setCode(new CodeableConcept().addCoding(testTypeCode.toFhirCoding()));
          quantity
              .setUnit(testTypeCode.getUnit()) // or the proper UCUM unit
              .setSystem(SystemUrls.UNITS_OF_MEASURE)
              .setCode(testTypeCode.getUnit());
        });

    observation.setStatus(Observation.ObservationStatus.FINAL);
    claimLineHCTHGBTestResult.ifPresent(
        result -> {
          quantity.setValue(BigDecimal.valueOf(result));
          observation.setValue(quantity);
        });

    nchCore.addClinicalLabPerformer(observation);

    return Optional.of(observation);
  }

  @Override
  public List<ExplanationOfBenefit.SupportingInformationComponent> toFhirSupportingInfo(
      SupportingInfoFactory supportingInfoFactory) {
    return Stream.of(
            super.toFhirSupportingInfo(supportingInfoFactory),
            renderingProviderSsaStateCode.toFhir(supportingInfoFactory).stream().toList(),
            lineNchBenefitEnhancementSwitches.toFhir(supportingInfoFactory))
        .flatMap(Collection::stream)
        .toList();
  }

  @Override
  public List<Extension> getExtensions(ClaimFilterOptions options) {
    return Stream.concat(
            super.getExtensions(options).stream(), extensionsProfessionalCmsNch.toFhir().stream())
        .toList();
  }

  @Override
  Optional<AdjudicationEmbedded> getClaimLineAdjudication() {
    return Optional.of(adjudicationCharge);
  }
}
