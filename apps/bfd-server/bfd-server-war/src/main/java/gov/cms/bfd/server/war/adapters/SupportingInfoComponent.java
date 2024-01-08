package gov.cms.bfd.server.war.adapters;

/**
 * Interface for creating SupportingInfoComponent wrapper implementations for different FHIR
 * resource implementations.
 */
public interface SupportingInfoComponent {

  /**
   * Gets the supporting info codeable concept.
   *
   * @return the supporting info codeable concept
   */
  CodeableConcept getSupportingInfoCodeableConcept();
}
