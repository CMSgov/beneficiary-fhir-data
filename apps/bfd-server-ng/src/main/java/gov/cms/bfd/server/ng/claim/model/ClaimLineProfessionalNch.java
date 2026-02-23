package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.converter.NonZeroDoubleConverter;
import gov.cms.bfd.server.ng.util.FhirUtil;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;

/** Claim line info. */
@Embeddable
@Getter
@SuppressWarnings("java:S2201")
@AttributeOverride(name = "trackingNumber", column = @Column(name = "clm_line_pmd_uniq_trkng_num"))
public class ClaimLineProfessionalNch extends ClaimLineProfessionalBase implements ClaimLineBase {

  @Embedded private ClaimLineAdjudicationChargeProfessionalNch adjudicationCharge;

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
}
