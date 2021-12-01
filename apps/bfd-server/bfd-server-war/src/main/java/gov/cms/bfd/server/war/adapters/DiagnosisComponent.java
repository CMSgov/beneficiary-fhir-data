package gov.cms.bfd.server.war.adapters;

/**
 * Interface for creating DiagnosisComponent wrapper implementations for different FHIR resource
 * implementations.
 */
public interface DiagnosisComponent {

  CodeableConcept getDiagnosisCodeableConcept();

  CodeableConcept getPackageCode();
}
