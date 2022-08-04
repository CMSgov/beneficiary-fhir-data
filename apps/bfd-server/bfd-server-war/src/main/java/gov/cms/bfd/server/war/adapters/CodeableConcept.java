package gov.cms.bfd.server.war.adapters;

import java.util.List;

/**
 * Interface for creating CodeableConcept wrapper implementations for different FHIR resource
 * implementations.
 */
public interface CodeableConcept {

  /**
   * Gets the coding.
   *
   * @return the coding
   */
  List<Coding> getCoding();
}
