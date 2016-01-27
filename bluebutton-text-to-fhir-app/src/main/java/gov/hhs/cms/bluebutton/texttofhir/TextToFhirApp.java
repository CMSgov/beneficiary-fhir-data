package gov.hhs.cms.bluebutton.texttofhir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.dstu21.model.Bundle;
import org.hl7.fhir.dstu21.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu21.model.Bundle.HTTPVerb;
import org.hl7.fhir.dstu21.model.Resource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.kohsuke.args4j.CmdLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import gov.hhs.cms.bluebutton.texttofhir.parsing.TextFile;
import gov.hhs.cms.bluebutton.texttofhir.parsing.TextFileParseException;
import gov.hhs.cms.bluebutton.texttofhir.parsing.TextFileProcessor;
import gov.hhs.cms.bluebutton.texttofhir.transform.TextFileTransformer;

/**
 * The main application/driver/entry point. See {@link #main(String[])}.
 */
public final class TextToFhirApp {
	private static final Logger LOGGER = LoggerFactory.getLogger(TextToFhirApp.class);

	/**
	 * This {@link System#exit(int)} value should be used when everything works
	 * as expected.
	 */
	private static final int EXIT_CODE_COPACETIC = 0;

	/**
	 * This {@link System#exit(int)} value should be used when the command line
	 * arguments are invalid.
	 */
	private static final int EXIT_CODE_CMD_SYNTAX = 1;

	/**
	 * This {@link System#exit(int)} value should be used when one of the
	 * specified input files cannot be read/parsed properly.
	 */
	private static final int EXIT_CODE_INPUT_FILE = 2;

	/**
	 * This method is the one that will get called when users launch the
	 * application from the command line.
	 * 
	 * @param args
	 *            the space-separated command line arguments specified by the
	 *            user will be passed in this array
	 */
	public static void main(String[] args) {
		OptionsParser optionsParser = new OptionsParser();
		Options options = null;
		try {
			options = optionsParser.parse(args);
		} catch (CmdLineException e) {
			System.err.println("Invalid syntax.");
			bailWithUsage(optionsParser);
		}

		if (options.isHelpRequested()) {
			System.err.println(optionsParser.getUsageText());
			System.exit(EXIT_CODE_COPACETIC);
		}

		for (String inputFilePathText : options.getInputFilePaths()) {
			Path inputFilePath = Paths.get(inputFilePathText);
			try (InputStream inputFileStream = Files.newInputStream(inputFilePath, StandardOpenOption.READ);) {
				TextFile parsedInputFile = TextFileProcessor.parse(inputFileStream);
				List<IBaseResource> fhirResources = TextFileTransformer.transform(parsedInputFile);
				FhirContext ctx = FhirContext.forDstu2_1();
				IGenericClient client = ctx.newRestfulGenericClient(options.getServer().toString());
				LoggingInterceptor fhirClientLogging = new LoggingInterceptor();
				fhirClientLogging.setLogRequestBody(true);
				client.registerInterceptor(fhirClientLogging);

				Bundle bundle = new Bundle();
				for (IBaseResource resource : fhirResources) {
					Resource typedResource = (Resource) resource;
					bundle.addEntry().setFullUrl(typedResource.getId()).setResource(typedResource).getRequest()
							.setMethod(HTTPVerb.POST);
				}

				client.transaction().withBundle(bundle).execute();
			} catch (IOException | TextFileParseException e) {
				System.err.println("Unable to read file: " + inputFilePathText);
				LOGGER.error("Error processing file.", e);
				System.exit(EXIT_CODE_INPUT_FILE);
			}
		}
	}

	/**
	 * Prints the usage text out and calls {@link System#exit(int)}.
	 * 
	 * @param optionsParser
	 *            the {@link OptionsParser} to get the usage text from
	 */
	private static void bailWithUsage(OptionsParser optionsParser) {
		System.err.println(optionsParser.getUsageText());
		System.exit(EXIT_CODE_CMD_SYNTAX);
	}
}
