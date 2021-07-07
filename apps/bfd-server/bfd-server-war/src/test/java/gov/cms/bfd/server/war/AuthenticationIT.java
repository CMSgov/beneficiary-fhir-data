package gov.cms.bfd.server.war;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.junit.Assert;
import org.junit.Test;

/** Verifies that authentication works as expected. */
public final class AuthenticationIT {
  /**
   * Verifies that authentication works for an SSL client certificate in the server's trust store.
   */
  @Test
  public void authenticationWorksForTrustedClient() {
    // Construct a FHIR client using a trusted client identity certificate.
    IGenericClient fhirClient =
        ServerTestUtils.get().createFhirClient(Optional.of(ClientSslIdentity.TRUSTED));

    /*
     * Just check an arbitrary endpoint (all trusted clients have access to
     * everything).
     */
    CapabilityStatement capabilities =
        fhirClient.capabilities().ofType(CapabilityStatement.class).execute();
    Assert.assertNotNull(capabilities);
  }

  /**
   * Verifies that clients that don't present a client certificate receive an access denied error.
   */
  @Test(expected = FhirClientConnectionException.class)
  public void accessDeniedForNoClientCert() {
    // Construct a FHIR client using no client identity certificate.
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient(Optional.empty());

    /*
     * Just check an arbitrary endpoint (all trusted clients have access to
     * everything).
     */
    fhirClient.capabilities().ofType(CapabilityStatement.class).execute();
  }

  /**
   * Verifies that clients that present a client certificate that is not in the server's trust store
   * receive an access denied error.
   */
  @Test(expected = FhirClientConnectionException.class)
  public void accessDeniedForClientCertThatIsNotTrusted() {
    /*
     * Construct a FHIR client using a not-trusted client identity
     * certificate.
     */
    IGenericClient fhirClient =
        ServerTestUtils.get().createFhirClient(Optional.of(ClientSslIdentity.UNTRUSTED));

    /*
     * Just check an arbitrary endpoint (all trusted clients have access to
     * everything).
     */
    fhirClient.capabilities().ofType(CapabilityStatement.class).execute();
  }
}
