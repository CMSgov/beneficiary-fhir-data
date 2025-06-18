package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hl7.fhir.r4.model.SimpleQuantity;

@Embeddable
class ClaimLineServiceUnitQuantity {
  @Column(name = "clm_line_srvc_unit_qty")
  private double serviceUnitQuantity;

  SimpleQuantity toFhir() {
    var quantity = new SimpleQuantity();
    quantity.setValue(serviceUnitQuantity);
    return quantity;
  }
}
