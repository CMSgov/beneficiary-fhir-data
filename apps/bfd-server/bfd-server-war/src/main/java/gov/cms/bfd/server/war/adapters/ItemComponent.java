package gov.cms.bfd.server.war.adapters;

/**
 * Interface for creating ItemComponent wrapper implementations for different FHIR resource
 * implementations.
 */
public interface ItemComponent {

  CodeableConcept getProductOrService();
}
