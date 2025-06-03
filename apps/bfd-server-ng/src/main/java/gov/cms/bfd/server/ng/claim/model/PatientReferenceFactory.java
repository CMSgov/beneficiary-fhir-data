package gov.cms.bfd.server.ng.claim.model;

import org.hl7.fhir.r4.model.Reference;

public class PatientReferenceFactory {
  private PatientReferenceFactory() {}

  public static Reference toFhir(long beneXrefSk) {
    return new Reference().setReference("Patient/" + beneXrefSk);
  }
}
