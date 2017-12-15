package gov.hhs.cms.bluebutton.fhirstress.utils;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.hl7.fhir.dstu3.model.Patient;
import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.model.primitive.UriDt;

/**
 * Unit test for simple FhirClient.
 */
public class FhirClientTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public FhirClientTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( FhirClientTest.class );
    }

    /**
     * Test FHIR server connectivity
     */
    public void testFhirClient()
    {
        IGenericClient client = 
          FhirClient.create("https://fhir.backend.bluebutton.hhsdevcloud.us/baseDstu3","./dev/ssl-stores");
        Patient patient =
        client.read().resource(Patient.class).withId("20140000005499").execute();
        //System.out.println("Patient Family = " + patient.getName().get(0).getFamily()); 
        assertEquals("Doe", patient.getName().get(0).getFamily());
    }
}
