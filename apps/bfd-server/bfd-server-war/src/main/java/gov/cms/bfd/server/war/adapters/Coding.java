package gov.cms.bfd.server.war.adapters;

/**
 * Interface for creating Coding wrapper implementations for different FHIR resource
 * implementations.
 */
public interface Coding {

  String getSystem();

  String getCode();
}
