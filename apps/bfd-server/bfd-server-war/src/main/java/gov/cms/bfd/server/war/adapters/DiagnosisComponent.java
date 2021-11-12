package gov.cms.bfd.server.war.adapters;

public interface DiagnosisComponent {

  CodeableConcept getDiagnosisCodeableConcept();

  CodeableConcept getPackageCode();
}
