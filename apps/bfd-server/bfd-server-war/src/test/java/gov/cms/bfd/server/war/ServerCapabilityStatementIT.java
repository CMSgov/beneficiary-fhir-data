package gov.cms.bfd.server.war;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.util.VersionUtil;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.hl7.fhir.dstu3.model.CapabilityStatement.CapabilityStatementRestComponent;
import org.hl7.fhir.dstu3.model.CapabilityStatement.CapabilityStatementRestResourceComponent;
import org.hl7.fhir.dstu3.model.CapabilityStatement.RestfulCapabilityMode;
import org.hl7.fhir.dstu3.model.CapabilityStatement.TypeRestfulInteraction;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.DiagnosticReport;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.Assert;
import org.junit.Test;

/**
 * Integration tests to verify that our FHIR server supports {@link CapabilityStatement}s via the
 * <code>GET [base]/_metadata</code> endpoint. This is required by the FHIR STU3 specification, as
 * detailed here: <a href="https://www.hl7.org/fhir/http.html#capabilities">FHIR RESTful API:
 * capabilities</a>.
 *
 * <p>Note that our application code doesn't directly provide this functionality. Instead, it comes
 * "for free" with our use of the HAPI framework. These tests are just here to verify that it works
 * as expected, since it's so critical for clients.
 */
public final class ServerCapabilityStatementIT {
  /**
   * Verifies that the server responds as expected to the <code>GET [base]/_metadata</code>
   * endpoint.
   */
  @Test
  public void getCapabilities() {
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    CapabilityStatement capabilities =
        fhirClient.capabilities().ofType(CapabilityStatement.class).execute();
    Assert.assertNotNull(capabilities);

    // Verify that our custom server metadata is correct.
    Assert.assertEquals(V1Server.CAPABILITIES_PUBLISHER, capabilities.getPublisher());
    Assert.assertEquals(V1Server.CAPABILITIES_SERVER_NAME, capabilities.getSoftware().getName());
    Assert.assertEquals(
        "gov.cms.bfd:bfd-server-war", capabilities.getImplementation().getDescription());
    Assert.assertNotEquals(null, capabilities.getSoftware().getVersion());
    Assert.assertNotEquals("", capabilities.getSoftware().getVersion());

    // The default for this field is HAPI's version but we don't use that.
    Assert.assertNotEquals(VersionUtil.getVersion(), capabilities.getSoftware().getVersion());

    Assert.assertEquals(1, capabilities.getRest().size());
    CapabilityStatementRestComponent restCapabilities = capabilities.getRestFirstRep();
    Assert.assertEquals(RestfulCapabilityMode.SERVER, restCapabilities.getMode());

    // Verify that Patient resource support looks like expected.
    CapabilityStatementRestResourceComponent patientCapabilities =
        restCapabilities.getResource().stream()
            .filter(r -> r.getType().equals(Patient.class.getSimpleName()))
            .findAny()
            .get();
    Assert.assertTrue(
        patientCapabilities.getInteraction().stream()
            .filter(i -> i.getCode() == TypeRestfulInteraction.READ)
            .findAny()
            .isPresent());
    Assert.assertTrue(
        patientCapabilities.getInteraction().stream()
            .filter(i -> i.getCode() == TypeRestfulInteraction.SEARCHTYPE)
            .findAny()
            .isPresent());
    Assert.assertFalse(
        patientCapabilities.getInteraction().stream()
            .filter(i -> i.getCode() == TypeRestfulInteraction.CREATE)
            .findAny()
            .isPresent());

    // Verify that Coverage resource support looks like expected.
    CapabilityStatementRestResourceComponent coverageCapabilities =
        restCapabilities.getResource().stream()
            .filter(r -> r.getType().equals(Coverage.class.getSimpleName()))
            .findAny()
            .get();
    Assert.assertTrue(
        coverageCapabilities.getInteraction().stream()
            .filter(i -> i.getCode() == TypeRestfulInteraction.READ)
            .findAny()
            .isPresent());
    Assert.assertTrue(
        coverageCapabilities.getInteraction().stream()
            .filter(i -> i.getCode() == TypeRestfulInteraction.SEARCHTYPE)
            .findAny()
            .isPresent());

    // Verify that EOB resource support looks like expected.
    CapabilityStatementRestResourceComponent eobCapabilities =
        restCapabilities.getResource().stream()
            .filter(r -> r.getType().equals(ExplanationOfBenefit.class.getSimpleName()))
            .findAny()
            .get();
    Assert.assertTrue(
        eobCapabilities.getInteraction().stream()
            .filter(i -> i.getCode() == TypeRestfulInteraction.READ)
            .findAny()
            .isPresent());
    Assert.assertTrue(
        eobCapabilities.getInteraction().stream()
            .filter(i -> i.getCode() == TypeRestfulInteraction.SEARCHTYPE)
            .findAny()
            .isPresent());

    // Spot check that an arbitrary unsupported resource isn't listed.
    Assert.assertFalse(
        restCapabilities.getResource().stream()
            .filter(r -> r.getType().equals(DiagnosticReport.class.getSimpleName()))
            .findAny()
            .isPresent());
  }
}
