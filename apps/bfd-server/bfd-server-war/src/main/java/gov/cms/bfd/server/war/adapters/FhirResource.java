package gov.cms.bfd.server.war.adapters;

import java.util.List;

/**
 * Interface for creating FHIR base resource wrapper implementations for different FHIR resource
 * implementations.
 */
public interface FhirResource {

  List<ProcedureComponent> getProcedure();

  List<DiagnosisComponent> getDiagnosis();

  List<ItemComponent> getItem();
}
