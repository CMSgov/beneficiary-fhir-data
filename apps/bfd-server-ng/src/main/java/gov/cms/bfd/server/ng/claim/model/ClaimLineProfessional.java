package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;

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
  private double claimLineHCTHGBTestResult;

  @Column(name = "clm_line_carr_clncl_lab_num")
  private Optional<String> claimLineCarrierClinicalLabNumber;

  @OneToOne(mappedBy = "claimLineProfessional")
  private ClaimItem claimLine;

  /**
   * Return claim observation data if available.
   *
   * @param bfdRowId Observation ID
   * @return claim Observation
   */
  public Optional<Observation> toFhirObservation(int bfdRowId) {
    if (claimLineHCTHGBTestTypeCode.isEmpty() || claimLineHCTHGBTestResult <= 0) {
      return Optional.empty();
    }

    var observation = new Observation();
    observation.setId(String.valueOf(bfdRowId));
    claimLineHCTHGBTestTypeCode.ifPresent(
        testTypeCode ->
            observation.setCode(new CodeableConcept().addCoding(testTypeCode.toFhirCoding())));

    observation.setStatus(Observation.ObservationStatus.FINAL);
    observation.setValue(
        new Quantity()
            .setValue(BigDecimal.valueOf(claimLineHCTHGBTestResult))
            .setUnit("g/dL") // or the proper UCUM unit
            .setSystem("http://unitsofmeasure.org")
            .setCode("g/dL"));

    claimLineCarrierClinicalLabNumber.ifPresent(
        labNumber -> {
          var identifier = new Identifier().setSystem(SystemUrls.CLIA).setValue(labNumber);

          observation.addPerformer(new Reference().setIdentifier(identifier));
        });

    return Optional.of(observation);
  }
}
