package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;

/** Represents the claim tax number code as an extension on an EOB.line. */
public class ClaimTaxNumberCode {
  @Column(name = "clm_rndrg_prvdr_tax_num")
  private Optional<String> taxNumber;

  Optional<Extension> toFhir() {
    return taxNumber.map(
        number ->
            new Extension()
                .setUrl(SystemUrls.EXT_CLM_RNDRG_PRVDR_TAX_NUM_URL)
                .setValue(
                    new Identifier()
                        .setType(
                            new CodeableConcept()
                                .setCoding(
                                    List.of(
                                        new Coding()
                                            .setSystem(SystemUrls.HL7_IDENTIFIER)
                                            .setCode("TAX"))))
                        .setSystem(SystemUrls.US_EIN)
                        .setValue(number)));
  }
}
