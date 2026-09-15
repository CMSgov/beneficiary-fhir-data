package gov.cms.bfd.server.ng.claim.model.professional.entities;

import gov.cms.bfd.server.ng.claim.model.common.ClaimItemBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimItemId;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineHcpcsCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineRxNumber;
import gov.cms.bfd.server.ng.claim.model.common.ClaimProcedureBase;
import gov.cms.bfd.server.ng.claim.model.professional.ClaimLineHCTHGBTestTypeCode;
import gov.cms.bfd.server.ng.claim.model.professional.ClaimLineProfessionalSharedSystems;
import gov.cms.bfd.server.ng.claim.model.professional.ClaimProcedureProfessional;
import gov.cms.bfd.server.ng.converter.NonZeroDoubleConverter;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;

/** Claim item table. */
@Getter
@Entity
@EqualsAndHashCode
@Table(name = "claim_item_professional_ss", schema = "idr")
public class ClaimItemProfessionalCmsSharedSystems implements ClaimItemBase {
  @EmbeddedId private ClaimItemId claimItemId;
  @Embedded private ClaimLineProfessionalSharedSystems claimLine;
  @Embedded private ClaimProcedureProfessional claimProcedure;
  @Embedded private ClaimLineRxNumber claimLineRxNum;

  @Column(name = "clm_line_hct_lvl_num")
  @Convert(converter = NonZeroDoubleConverter.class)
  private Optional<Double> claimLineHCTTestResult;

  @Column(name = "clm_line_hgb_lvl_num")
  @Convert(converter = NonZeroDoubleConverter.class)
  private Optional<Double> claimLineHGBTestResult;

  @Column(name = "clm_line_rbndlg_crtfctn_num")
  private Optional<String> claimLineCarrierClinicalLabNumber;

  @JoinColumn(name = "clm_uniq_id")
  @ManyToOne
  private ClaimProfessionalCmsSharedSystems claim;

  @Override
  public Optional<ClaimProcedureBase> getProcedure() {
    return Optional.of(claimProcedure);
  }

  @Override
  public Optional<ClaimLineHcpcsCode> getClaimLineHcpcsCode() {
    return Optional.of(claimLine.getHcpcsCode());
  }

  /**
   * Return claim observation data if available.
   *
   * @param bfdRowId Observation ID
   * @return claim Observation
   */
  public Optional<Observation> toFhirObservationHCT(int bfdRowId) {

    if (claimLineHCTTestResult.isEmpty()) {
      return Optional.empty();
    }

    var observation = new Observation();
    observation.setId("hct-" + bfdRowId);
    observation.setCode(
        new CodeableConcept().addCoding(ClaimLineHCTHGBTestTypeCode.R2.toFhirCoding()));
    observation.setStatus(Observation.ObservationStatus.FINAL);
    observation.setValue(
        new Quantity()
            .setValue(BigDecimal.valueOf(claimLineHCTTestResult.get()))
            .setUnit("%") // or the proper UCUM unit
            .setSystem(SystemUrls.UNITS_OF_MEASURE)
            .setCode("%"));
    claimLineCarrierClinicalLabNumber.ifPresent(
        labNumber -> {
          var identifier = new Identifier().setSystem(SystemUrls.CLIA).setValue(labNumber);

          observation.addPerformer(new Reference().setIdentifier(identifier));
        });

    return Optional.of(observation);
  }

  /**
   * Return claim observation data if available.
   *
   * @param bfdRowId Observation ID
   * @return claim Observation
   */
  public Optional<Observation> toFhirObservationHGB(int bfdRowId) {
    if (claimLineHGBTestResult.isEmpty()) {
      return Optional.empty();
    }

    var observation = new Observation();
    observation.setId("hgb-" + bfdRowId);
    observation.setCode(
        new CodeableConcept().addCoding(ClaimLineHCTHGBTestTypeCode.R1.toFhirCoding()));
    observation.setStatus(Observation.ObservationStatus.FINAL);
    observation.setValue(
        new Quantity()
            .setValue(BigDecimal.valueOf(claimLineHGBTestResult.get()))
            .setUnit("g/dL") // or the proper UCUM unit
            .setSystem(SystemUrls.UNITS_OF_MEASURE)
            .setCode("g/dL"));
    claimLineCarrierClinicalLabNumber.ifPresent(
        labNumber -> {
          var identifier = new Identifier().setSystem(SystemUrls.CLIA).setValue(labNumber);

          observation.addPerformer(new Reference().setIdentifier(identifier));
        });
    return Optional.of(observation);
  }
}
