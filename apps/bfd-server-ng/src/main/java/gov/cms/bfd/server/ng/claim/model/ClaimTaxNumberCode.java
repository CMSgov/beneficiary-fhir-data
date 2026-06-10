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
    return taxNumber.map(this::createTaxNumberExtension);
  }

  private Extension createTaxNumberExtension(String number) {
    return new Extension()
        .setUrl(SystemUrls.EXT_CLM_RNDRG_PRVDR_TAX_NUM_URL)
        .setValue(createTaxNumberIdentifier(number));
  }

  private Identifier createTaxNumberIdentifier(String number) {
    return new Identifier()
        .setType(createTaxNumberType())
        .setSystem(SystemUrls.US_EIN)
        .setValue(number);
  }

  private CodeableConcept createTaxNumberType() {
    return new CodeableConcept().setCoding(List.of(createTaxNumberCoding()));
  }

  private Coding createTaxNumberCoding() {
    return new Coding().setSystem(SystemUrls.HL7_IDENTIFIER).setCode("TAX");
  }
}
