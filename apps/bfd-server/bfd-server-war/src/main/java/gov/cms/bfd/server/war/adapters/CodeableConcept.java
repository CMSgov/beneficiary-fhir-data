package gov.cms.bfd.server.war.adapters;

import java.util.List;

/**
 * Interface for creating CodeableConcept wrapper implementations for different FHIR resource
 * implementations.
 */
public interface CodeableConcept {

  List<Coding> getCoding();
}
