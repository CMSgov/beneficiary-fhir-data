package gov.cms.bfd.server.ng.claim.model;

import org.hl7.fhir.r4.model.Money;

class USD extends Money {
  private USD() {}

  static Money toFhir(double value) {
    return new Money().setCurrency("USD").setValue(value);
  }
}
