package gov.cms.bfd.server.ng.claim.model;

import static gov.cms.bfd.server.ng.util.SystemUrls.CMS_HCPCS;

import jakarta.persistence.Column;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;

class PriceModifierCode {

  @Column(name = "price_mod1")
  private String priceModifier1;

  @Column(name = "price_mod2")
  private String priceModifier2;

  CodeableConcept toFhir() {
    var coding =
        Stream.of(priceModifier1, priceModifier2)
            .map(c -> new Coding().setSystem(CMS_HCPCS).setCode(c))
            .toList();
    return new CodeableConcept().setCoding(coding);
  }
}
