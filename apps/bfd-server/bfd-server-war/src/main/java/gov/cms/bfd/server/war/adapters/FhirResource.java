package gov.cms.bfd.server.war.adapters;

import java.util.List;

/**
 * Interface for creating FHIR base resource wrapper implementations for different FHIR resource
 * implementations.
 */
public interface FhirResource {

  /**
   * Gets the procedure.
   *
   * @return the procedure
   */
  List<ProcedureComponent> getProcedure();

  /**
   * Gets the diagnosis.
   *
   * @return the diagnosis
   */
  List<DiagnosisComponent> getDiagnosis();

  /**
   * Gets the item.
   *
   * @return the item
   */
  List<ItemComponent> getItem();
}
