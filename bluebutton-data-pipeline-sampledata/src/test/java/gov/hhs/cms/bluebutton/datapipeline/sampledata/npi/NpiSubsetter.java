package gov.hhs.cms.bluebutton.datapipeline.sampledata.npi;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides a development utility for producing more manageable
 * sample subsets of the
 * <a href="http://download.cms.gov/nppes/NPI_Files.html">NPI/NPPES File</a>
 * data. Without this, it's a 5GB file, which is way more data than we need,
 * since we're just taking random values out of it.
 */
public final class NpiSubsetter {
	private static final Logger LOGGER = LoggerFactory.getLogger(NpiSubsetter.class);

	/**
	 * How many records to try and end up with. Note that, due to the sampling
	 * method used, the end result may not be exactly this number; just close to
	 * it.
	 */
	private static final int TARGET_RECORD_COUNT = 10000;

	/**
	 * The {@link Path} to the source file to select a subset of. Yeah, this is
	 * just a hardcoded, non-portable path.
	 */
	private static final Path SOURCE_PATH = Paths
			.get("/home/karl/workspaces/cms/NPPES_Data_Dissemination_Mar_2016/npidata_20050523-20160313.csv");

	/**
	 * The number of records in the 2016-03-13 source file.
	 */
	private static final int SOURCE_RECORD_COUNT = 4829134;

	/**
	 * The main driver/launcher for this utility.
	 * 
	 * @param args
	 *            (not used)
	 * @throws IOException
	 *             An {@link IOException} will be thrown if errors occur reading
	 *             in the original file or writing out the subsetted data.
	 */
	public static void main(String[] args) throws IOException {
		double targetPercentage = ((double) TARGET_RECORD_COUNT) / ((double) SOURCE_RECORD_COUNT);
		int targetPercentageInverse = (int) Math.round(1 / targetPercentage);
		LOGGER.info("Targets: record count={}, percentage={}, percentage-inverse={}", TARGET_RECORD_COUNT,
				targetPercentage, targetPercentageInverse);

		Path inputPath = SOURCE_PATH;
		Path outputPath = Paths.get(".", "src", "main", "resources", "npi",
				String.format("npidata-20050523-20160313-subset-%d.csv", TARGET_RECORD_COUNT));
		LOGGER.info("Output path: {}", outputPath);
		Files.createDirectories(outputPath.getParent());
		try (Reader in = new FileReader(inputPath.toFile()); Writer out = new FileWriter(outputPath.toFile());) {
			// Read through every row of the input file.
			CSVFormat csvFormat = CSVFormat.EXCEL;
			CSVPrinter csvPrinter = new CSVPrinter(out, csvFormat);
			for (CSVRecord inputRecord : csvFormat.parse(in)) {
				boolean copyRecord = false;
				if (inputRecord.getRecordNumber() == 1) {
					copyRecord = true;
				} else if (inputRecord.getRecordNumber() % targetPercentageInverse == 0) {
					copyRecord = true;
				}

				if (copyRecord)
					csvPrinter.printRecord(inputRecord);
			}
			csvPrinter.close();
		}
	}
}
