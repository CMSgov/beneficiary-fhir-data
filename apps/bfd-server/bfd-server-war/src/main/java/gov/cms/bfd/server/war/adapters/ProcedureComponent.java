package gov.cms.bfd.server.war.adapters;

/**
 * Interface for creating ProcedureComponent wrapper implementations for different FHIR resource
 * implementations.
 */
public interface ProcedureComponent {

  CodeableConcept getProcedureCodeableConcept();
}
