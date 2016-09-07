package gov.hhs.cms.bluebutton.datapipeline.fhir.sampledata;

import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.kohsuke.args4j.CmdLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;

import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.CurrentBeneficiary;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.SynpufArchive;
import gov.hhs.cms.bluebutton.datapipeline.fhir.LoadAppOptions;
import gov.hhs.cms.bluebutton.datapipeline.fhir.load.FhirLoader;
import gov.hhs.cms.bluebutton.datapipeline.fhir.transform.BeneficiaryBundle;
import gov.hhs.cms.bluebutton.datapipeline.fhir.transform.DataTransformer;
import gov.hhs.cms.bluebutton.datapipeline.sampledata.SampleDataLoader;

/**
 * The main application/driver/entry point. See {@link #main(String[])}.
 */
public final class FhirSampleDataApp {
	private static final Logger LOGGER = LoggerFactory.getLogger(FhirSampleDataApp.class);

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

		MetricRegistry metricsSampleData = new MetricRegistry();
		SampleDataLoader sampleDataLoader = new SampleDataLoader(metricsSampleData);
		SynpufArchive synpufArchive = SynpufArchive.SAMPLE_1;
		LOGGER.info("Loading sample data from {}...", synpufArchive);
		// FIXME Should cleanup after itself and use a temp directory
		List<CurrentBeneficiary> sampleBeneficiaries = sampleDataLoader.loadSampleData(Paths.get("."), synpufArchive);
		LOGGER.info("Loaded sample data from {}.", synpufArchive);
		Slf4jReporter.forRegistry(metricsSampleData).outputTo(LOGGER).build().report();

		MetricRegistry metricsFhir = new MetricRegistry();
		metricsFhir.registerAll(new MemoryUsageGaugeSet());
		metricsFhir.registerAll(new GarbageCollectorMetricSet());
		Slf4jReporter fhirMetricsReporter = Slf4jReporter.forRegistry(metricsFhir).outputTo(LOGGER).build();
		fhirMetricsReporter.start(300, TimeUnit.SECONDS);

		LOGGER.info("Pushing sample data to FHIR...");
		/*
		 * FIXME The LoadAppOptions used here are invalid and will cause
		 * IllegalArgumentExceptions.
		 */
		FhirLoader fhirLoader = new FhirLoader(metricsFhir,
				new LoadAppOptions(options.getServer(), null, null, null, null));
		Stream<BeneficiaryBundle> fhirStream = new DataTransformer().transformSourceData(sampleBeneficiaries.stream());
		fhirLoader.insertFhirRecords(fhirStream);

		fhirMetricsReporter.stop();
		LOGGER.info("Pushed sample data to FHIR.");
		fhirMetricsReporter.report();
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
