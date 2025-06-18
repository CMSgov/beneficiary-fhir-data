package gov.cms.bfd.server.ng.claim.model;

import org.hl7.fhir.r4.model.Reference;

/** Creates a reference to a patient resource. */
public class PatientReferenceFactory {
  private PatientReferenceFactory() {}

  /**
   * Creates a patient reference.
   *
   * @param beneSk bene surrogate key
   * @return reference
   */
  public static Reference toFhir(long beneSk) {
    return new Reference().setReference("Patient/" + beneSk);
  }
}
