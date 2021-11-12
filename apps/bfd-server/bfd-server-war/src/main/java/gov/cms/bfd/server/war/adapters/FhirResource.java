package gov.cms.bfd.server.war.adapters;

import java.util.List;

public interface FhirResource {

  List<ProcedureComponent> getProcedure();

  List<DiagnosisComponent> getDiagnosis();

  List<ItemComponent> getItem();
}
