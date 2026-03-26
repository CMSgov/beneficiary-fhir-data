package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.*;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.ParametersUtil;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;

class PatientMatchIT extends IntegrationTestBase {
  private Bundle searchBundle(Patient patient) {
    FhirContext ctx = FhirContext.forR4();
    var params = ParametersUtil.newInstance(ctx);
    var param = ParametersUtil.addParameterToParameters(ctx, params, "IDIPatient");
    ParametersUtil.addPartResource(ctx, param, "patient", patient);
    var res =
        getFhirClient()
            .operation()
            .onType(Patient.class)
            .named("idi-match")
            .withParameters(params)
            .returnResourceType(Bundle.class)
            .execute();
    // Organization should always be the first entry in the bundle
    assertEquals(
        ResourceType.Organization, res.getEntry().getFirst().getResource().getResourceType());

    return res;
  }

  @Test
  void emptyRequestReturnsEmptyBundle() {
    var res = searchBundle(new Patient());
    assertEquals(1, res.getEntry().size());
  }
}
