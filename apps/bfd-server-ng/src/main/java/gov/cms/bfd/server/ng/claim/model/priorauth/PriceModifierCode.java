package gov.cms.bfd.server.ng.claim.model.priorauth;

import static gov.cms.bfd.server.ng.util.SystemUrls.CMS_HCPCS;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;

/** LineComponent CodeableConcept related to price modifiers. */
@Embeddable
public class PriceModifierCode {

  @Column(name = "price_mod1")
  private Optional<String> priceModifier1;

  @Column(name = "price_mod2")
  private Optional<String> priceModifier2;

  CodeableConcept toFhir() {
    var codeableConcept = new CodeableConcept();
    priceModifier1.ifPresent(
        p -> codeableConcept.addCoding(new Coding().setSystem(CMS_HCPCS).setCode(p)));
    priceModifier2.ifPresent(
        p -> codeableConcept.addCoding(new Coding().setSystem(CMS_HCPCS).setCode(p)));
    return codeableConcept;
  }
}
