package gov.hhs.cms.bluebutton.texttofhir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import org.hl7.fhir.dstu21.model.Bundle;
import org.hl7.fhir.dstu21.model.Patient;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.Assert;
import org.junit.Test;

import ca.uhn.fhir.rest.client.GenericClient;
import ca.uhn.fhir.rest.gclient.ITransaction;
import ca.uhn.fhir.rest.gclient.ITransactionTyped;
import gov.hhs.cms.bluebutton.texttofhir.parsing.TextFile;
import gov.hhs.cms.bluebutton.texttofhir.parsing.TextFileParseException;
import gov.hhs.cms.bluebutton.texttofhir.parsing.TextFileProcessor;
import gov.hhs.cms.bluebutton.texttofhir.transform.TextFileTransformer;
import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;

/**
 * Unit tests for {@link TextToFhirApp}.
 */
public final class TextToFhirAppTest {
	// TODO start as part of test
	private static final String FHIR_URL = "http://localhost:8080/hapi-fhir/baseDstu2";

	@Test
	public void quickRun() throws TextFileParseException, IOException {
		// Create the mock data that we'll flow through.
		TextFile parseResult = new TextFile(ZonedDateTime.now());
		List<IBaseResource> transformResult = Arrays.asList(new Patient());

		/*
		 * Specify all of the mock objects to use. Creating these accomplishes
		 * two things: 1) JMockit will register each of these as they're created
		 * and then verify that they're all used at some point in this test
		 * case, and 2) JMockit will intercept any calls to matching "real"
		 * objects in this test case and replace those calls with these mocks.
		 */
		new MockUp<TextFileProcessor>() {
			@Mock
			TextFile parse(InputStream textFileStream) {
				return parseResult;
			}
		};
		new MockUp<TextFileTransformer>() {
			@Mock
			List<IBaseResource> transform(TextFile inputData) {
				/*
				 * Verify that the TextFile passed in is the one produced by the
				 * (mock) processor.
				 */
				Assert.assertSame(parseResult, inputData);

				return transformResult;
			}
		};
		MockUp<ITransactionTyped<Bundle>> fhirTransactionTyped = new MockUp<ITransactionTyped<Bundle>>() {
			@Mock
			Bundle execute(Invocation invocation) {
				return new Bundle();
			}
		};
		MockUp<ITransaction> fhirTransaction = new MockUp<ITransaction>() {
			@SuppressWarnings("unchecked")
			@Mock
			<T extends IBaseBundle> ITransactionTyped<T> withBundle(Invocation invocation, T theBundle) {
				/*
				 * Verify that the Bundle contains the resource(s) that were
				 * produced by the (mock) transformer.
				 */
				Bundle bundleTyped = (Bundle) theBundle;
				Assert.assertNotNull(bundleTyped);
				Assert.assertEquals(transformResult.size(), bundleTyped.getEntry().size());
				Assert.assertSame(transformResult.get(0), bundleTyped.getEntry().get(0).getResource());

				return (ITransactionTyped<T>) fhirTransactionTyped.getMockInstance();
			}
		};
		new MockUp<GenericClient>() {
			@Mock
			ITransaction transaction(Invocation invocation) {
				return fhirTransaction.getMockInstance();
			}
		};

		// Create the fake (empty) file to run against.
		Path inputTextFile = Files.createTempFile(null, ".txt");
		inputTextFile.toFile().deleteOnExit();

		// Run the application (will use all mocks defined above).
		TextToFhirApp.main(new String[] { "--server", FHIR_URL, inputTextFile.toAbsolutePath().toString() });

		/*
		 * If we got here, the test case passed. Yay! Most of the verification
		 * is handled by JMockit just ensuring that the mocks were all used.
		 * There's some additional assertions inside those mocks, too.
		 */
	}
}
