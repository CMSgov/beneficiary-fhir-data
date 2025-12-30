package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.converter.NonZeroDoubleConverter;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.*;

/** Professional claim line table. */
@Entity
@Getter
@Table(name = "claim_line_professional", schema = "idr")
@SuppressWarnings("java:S6539")
public class ClaimLineProfessional {
  @EmbeddedId ClaimLineProfessionalId claimLineProfessionalId;

  @Column(name = "clm_line_hct_hgb_type_cd")
  private Optional<ClaimLineHCTHGBTestTypeCode> claimLineHCTHGBTestTypeCode;

  @Column(name = "clm_line_hct_hgb_rslt_num")
  @Convert(converter = NonZeroDoubleConverter.class)
  private Optional<Double> claimLineHCTHGBTestResult;

  @Column(name = "clm_line_carr_clncl_lab_num")
  private Optional<String> claimLineCarrierClinicalLabNumber;

  @OneToOne(mappedBy = "claimLineProfessional")
  private ClaimItem claimLine;

  @Column(name = "clm_line_carr_clncl_chrg_amt")
  private double carrierClinicalChargeAmount;

  @Column(name = "clm_line_carr_psych_ot_lmt_amt")
  private double therapyAmountAppliedToLimit;

  @Column(name = "clm_line_prfnl_intrst_amt")
  private double professionalInterestAmount;

  @Column(name = "clm_mdcr_prmry_pyr_alowd_amt")
  private double primaryPayerAllowedAmount;

  /**
   * Return claim observation data if available.
   *
   * @param bfdRowId Observation ID
   * @return claim Observation
   */
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

  List<ExplanationOfBenefit.AdjudicationComponent> toFhirAdjudication() {
    return List.of(
        AdjudicationChargeType.LINE_PROFESSIONAL_CARRIER_CLINICAL_CHARGE_AMOUNT.toFhirAdjudication(
            carrierClinicalChargeAmount),
        AdjudicationChargeType.LINE_PROFESSIONAL_THERAPY_LMT_AMOUNT.toFhirAdjudication(
            therapyAmountAppliedToLimit),
        AdjudicationChargeType.LINE_PROFESSIONAL_INTEREST_AMOUNT.toFhirAdjudication(
            professionalInterestAmount),
        AdjudicationChargeType.LINE_PROFESSIONAL_PRIMARY_PAYER_ALLOWED_AMOUNT.toFhirAdjudication(
            primaryPayerAllowedAmount));
  }
}
