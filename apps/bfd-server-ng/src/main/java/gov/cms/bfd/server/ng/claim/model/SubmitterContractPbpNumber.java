package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Extension;

import java.util.Optional;

@Embeddable
public class SubmitterContractPbpNumber {
    @Column(name = "clm_sbmtr_cntrct_pbp_num")
    private Optional<String> submitterContractPbpNumber;

    Optional<Extension> toFhir() {
        return submitterContractPbpNumber.map(
                c ->
                        new Extension()
                                .setUrl(SystemUrls.BLUE_BUTTON_STRUCTURE_DEFINITION_CLAIM_SUBMITTER_CONTRACT_PBP_NUMBER)
                                .setValue(new CodeType().setValue(c)));
    }
}
