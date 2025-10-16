package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;

import java.util.Map;
import java.util.Optional;

@Entity
@Getter
@Table(name = "claim_line_professional", schema = "idr")
@SuppressWarnings("java:S6539")
public class ClaimLineProfessional {
    @EmbeddedId ClaimLineProfessionalId claimLineProfessionalId;

    @Column(name = "clm_line_hct_hgb_type_cd")
    private Optional<ClaimLineHCTHGBTestTypeCode> claimLineHCTHGBTestTypeCode;

    @Column(name = "clm_line_hct_hgb_rslt_num")
    private Optional<Integer> claimLineHCTHGBTestResult;

    @Column(name = "clm_line_carr_clncl_lab_num")
    private Optional<String> claimLineCarrierClinicalLabNumber;

    @OneToOne(mappedBy = "claimLineProfessional")
    private ClaimItem claimLine;

    public Optional<Observation> toFhirObservation(int bfdRowId) {
        if(claimLineHCTHGBTestTypeCode.isEmpty()) {
            return Optional.empty();
        }

        var observation = new Observation();
        observation.setId(String.valueOf(bfdRowId));
        observation.setCode(new CodeableConcept().addCoding(claimLineHCTHGBTestTypeCode.get().toFhirCoding()));
        observation.setValue(new IntegerType(String.valueOf(claimLineHCTHGBTestResult)));

        if (claimLineCarrierClinicalLabNumber.isPresent()) {
            var identifier = new Identifier()
                    .setSystem(SystemUrls.CLIA)
                    .setValue(String.valueOf(claimLineCarrierClinicalLabNumber));
            observation.addPerformer(new Reference().setIdentifier(identifier));
        }

        return Optional.of(observation);
    }
}
