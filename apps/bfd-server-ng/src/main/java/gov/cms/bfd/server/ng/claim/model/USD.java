package gov.cms.bfd.server.ng.claim.model;

import org.hl7.fhir.r4.model.Money;

public class USD extends Money {
  public USD() {
    super();
    this.setCurrency("USD");
  }
}
