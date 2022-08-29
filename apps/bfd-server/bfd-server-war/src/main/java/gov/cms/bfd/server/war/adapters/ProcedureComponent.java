package gov.cms.bfd.server.war.adapters;

/**
 * Interface for creating ProcedureComponent wrapper implementations for different FHIR resource
 * implementations.
 */
public interface ProcedureComponent {

  /**
   * Gets the procedure codeable concept.
   *
   * @return the procedure codeable concept
   */
  CodeableConcept getProcedureCodeableConcept();
}
