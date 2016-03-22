package gov.hhs.cms.bluebutton.datapipeline.sampledata.prescribers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import gov.hhs.cms.bluebutton.datapipeline.sampledata.SampleDataException;

/**
 * Provides methods for generating {@link SamplePresciber}s.
 */
public final class SamplePrescriberGenerator {
	/**
	 * Used to select a random {@link SamplePresciber} entry from
	 * {@link #prescribers}. Note that the seed is fixed so that the sequence
	 * produced is stable. This should make the sample data more predictable,
	 * which is a good thing for our use cases.
	 */
	private final Random rng = new Random(42L);
	private final List<SamplePresciber> prescribers;

	/**
	 * Constructs a new {@link SamplePrescriberGenerator} instance. This
	 * operation will be a bit slow, as it will load and parse a large set of
	 * data from the classpath.
	 * 
	 * @throws SampleDataException
	 *             A {@link SampleDataException} will be thrown if any errors
	 *             occur reading in or processing the address data resource from
	 *             the classpath.
	 * 
	 */
	public SamplePrescriberGenerator() throws SampleDataException {
		List<SamplePresciber> prescribers = new ArrayList<>(1000000);
		InputStream csvStream = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("prescriber-names.csv");
		CSVFormat csvFormat = CSVFormat.EXCEL.withSkipHeaderRecord();
		try (Reader csvReader = new InputStreamReader(csvStream);) {
			CSVParser csvParser = csvFormat.parse(csvReader);
			for (CSVRecord record : csvParser) {
				if (record.getRecordNumber() == 1)
					continue;

				// Pull the fields out of the CSV, as-is, skipping incomplete
				// records.
				String npiText = record.get(SamplePrescriberColumn.NPI.getColumnIndex());
				if (npiText.trim().isEmpty())
					continue;
				int npi = Integer.parseInt(npiText);

				// Build and store the new SampleAddress.
				SamplePresciber provider = new SamplePresciber(npi);
				prescribers.add(provider);
			}
			csvParser.close();
		} catch (IOException e) {
			throw new SampleDataException(e);
		}

		this.prescribers = prescribers;
	}

	/**
	 * @return a generated {@link SamplePresciber} instance
	 */
	public SamplePresciber generatePrescriber() {
		return prescribers.get(rng.nextInt(prescribers.size()));
	}
}
