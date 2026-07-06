package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.ClaimFilterOptions;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineBase;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoFactory;
import gov.cms.bfd.server.ng.claim.model.professional.ClaimLineProfessionalBase;
import gov.cms.bfd.server.ng.converter.NonZeroDoubleConverter;
import gov.cms.bfd.server.ng.util.FhirUtil;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.*;

/** Claim line info. */
@Embeddable
@Getter
@SuppressWarnings("java:S2201")
@AttributeOverride(name = "trackingNumber", column = @Column(name = "clm_line_pmd_uniq_trkng_num"))
public class ClaimLineProfessionalNch extends ClaimLineProfessionalBase implements ClaimLineBase {

  @Embedded private ClaimLineAdjudicationChargeProfessionalNch adjudicationCharge;
  @Embedded private ClaimLineProfessionalNchExtensions claimLineProfessionalNchExtensions;
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

  @Column(name = "clm_line_carr_clncl_lab_num")
  private Optional<String> claimLineCarrierClinicalLabNumber;

  @Override
  void populateProductAndQuantity(ExplanationOfBenefit.ItemComponent line) {
    var productOrService = new CodeableConcept();
    getHcpcsCode().toFhir().ifPresent(productOrService::addCoding);
    line.setQuantity(getServiceUnitQuantity().toFhir());
    line.setProductOrService(FhirUtil.checkDataAbsent(productOrService));
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
    claimLineHCTHGBTestTypeCode.ifPresent(
        testTypeCode ->
            observation.setCode(new CodeableConcept().addCoding(testTypeCode.toFhirCoding())));

    observation.setStatus(Observation.ObservationStatus.FINAL);
    claimLineHCTHGBTestResult.ifPresent(
        result ->
            observation.setValue(
                new Quantity()
                    .setValue(BigDecimal.valueOf(result))
                    .setUnit("g/dL") // or the proper UCUM unit
                    .setSystem(SystemUrls.UNITS_OF_MEASURE)
                    .setCode("g/dL")));

    claimLineCarrierClinicalLabNumber.ifPresent(
        labNumber -> {
          var identifier = new Identifier().setSystem(SystemUrls.CLIA).setValue(labNumber);

          observation.addPerformer(new Reference().setIdentifier(identifier));
        });

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
  protected List<Extension> getFhirExtensions(ClaimFilterOptions options) {
    var extensions = new ArrayList<>(super.getFhirExtensions(options));
    extensions.addAll(claimLineProfessionalNchExtensions.toFhir());
    return extensions;
  }
}
