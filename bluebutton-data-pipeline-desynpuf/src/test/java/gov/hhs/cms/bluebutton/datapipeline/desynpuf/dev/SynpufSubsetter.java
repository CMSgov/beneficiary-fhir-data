package gov.hhs.cms.bluebutton.datapipeline.desynpuf.dev;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.hhs.cms.bluebutton.datapipeline.desynpuf.SynpufArchive;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.SynpufFile;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.SynpufSample;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.SynpufSampleLoader;

/**
 * This is not a test. Instead, it's a dev utility that can be used to create
 * "subset" {@link SynpufArchive}s: when run against an existing
 * {@link SynpufArchive} it will select a fixed number of beneficiaries from
 * that {@link SynpufArchive} and output them and all of their associated claims
 * data into a new set of sample data files. Note that the original
 * {@link SynpufArchive} and the number of beneficiaries to select from it can
 * be selected by editing the class' constants.
 */
public class SynpufSubsetter {
	private final static Logger LOGGER = LoggerFactory.getLogger(SynpufSubsetter.class);

	/**
	 * The original {@link SynpufArchive} to select a subset of records from.
	 */
	private static final SynpufArchive ARCHIVE_TO_SUBSET = SynpufArchive.SAMPLE_1;

	/**
	 * The {@link SynpufArchive} that will be created note: you'll have to
	 * create the constant first).
	 */
	private static SynpufArchive ARCHIVE_TO_CREATE = SynpufArchive.SAMPLE_TEST_B;

	/**
	 * The number of beneficiaries from the input sample to include in the
	 * output sample.
	 */
	private static final long NUM_BENES = ARCHIVE_TO_CREATE.getBeneficiaryCount();

	/**
	 * The driver/entry point for this dev utility.
	 * 
	 * @param args
	 *            (not used)
	 * @throws IOException
	 *             An {@link IOException} will be thrown if errors occur reading
	 *             in the original {@link SynpufArchive} or writing out the
	 *             subsetted data.
	 */
	public static void main(String[] args) throws IOException {
		SynpufSample originalSample = SynpufSampleLoader.extractSynpufFile(Paths.get(".", "target"), ARCHIVE_TO_SUBSET);

		Set<String> synpufBeneIdsToKeep = new HashSet<>();

		SynpufSample sampleToCreate = new SynpufSample(
				Paths.get(".", "src", "main", "resources", "de-synpuf", ARCHIVE_TO_CREATE.name()), ARCHIVE_TO_CREATE);

		for (SynpufFile synpufFile : SynpufFile.values()) {
			LOGGER.info("Processing DE-SynPUF file '{}'...", synpufFile);

			// Open the file to read from and the file to write to
			Path inputPath = originalSample.resolve(synpufFile);
			Path outputPath = sampleToCreate.resolve(synpufFile);
			Files.createDirectories(outputPath.getParent());
			try (Reader in = new FileReader(inputPath.toFile()); Writer out = new FileWriter(outputPath.toFile());) {
				// Read through every row of the input file.
				CSVFormat csvFormat = CSVFormat.EXCEL;
				CSVPrinter csvPrinter = new CSVPrinter(out, csvFormat);
				for (CSVRecord inputRecord : csvFormat.parse(in)) {
					/*
					 * Every file has a "DESYNPUF_ID" bene ID as the first
					 * column. Grab the value for this row.
					 */
					String beneIdForRow = inputRecord.get(0);
					boolean copyRecord = false;
					if (inputRecord.getRecordNumber() == 1) {
						copyRecord = true;
					} else if (synpufBeneIdsToKeep.contains(beneIdForRow)) {
						copyRecord = true;
					} else if (synpufBeneIdsToKeep.size() < NUM_BENES) {
						synpufBeneIdsToKeep.add(beneIdForRow);
						copyRecord = true;
					}

					if (copyRecord)
						csvPrinter.printRecord(inputRecord);
				}
				csvPrinter.close();
			}
		}
	}
}
