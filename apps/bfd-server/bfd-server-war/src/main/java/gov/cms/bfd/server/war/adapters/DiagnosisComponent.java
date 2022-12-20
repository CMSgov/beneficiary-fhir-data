package gov.cms.bfd.server.war.adapters;

/**
 * Interface for creating DiagnosisComponent wrapper implementations for different FHIR resource
 * implementations.
 */
public interface DiagnosisComponent {

  /**
   * Gets the diagnosis codeable concept.
   *
   * @return the diagnosis codeable concept
   */
  CodeableConcept getDiagnosisCodeableConcept();

  /**
   * Gets the package code.
   *
   * @return the package code
   */
  CodeableConcept getPackageCode();
}
