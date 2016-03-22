package gov.hhs.cms.bluebutton.datapipeline.sampledata.pharmacies;

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
 * Provides methods for generating {@link SamplePharmacy}s.
 */
public final class SamplePharmacyGenerator {
	/**
	 * Used to select a random {@link SamplePharmacy} entry from
	 * {@link #pharmacies}. Note that the seed is fixed so that the sequence
	 * produced is stable. This should make the sample data more predictable,
	 * which is a good thing for our use cases.
	 */
	private final Random rng = new Random(42L);
	private final List<SamplePharmacy> pharmacies;

	/**
	 * Constructs a new {@link SamplePharmacyGenerator} instance. This operation
	 * will be a bit slow, as it will load and parse a large set of data from
	 * the classpath.
	 * 
	 * @throws SampleDataException
	 *             A {@link SampleDataException} will be thrown if any errors
	 *             occur reading in or processing the address data resource from
	 *             the classpath.
	 * 
	 */
	public SamplePharmacyGenerator() throws SampleDataException {
		List<SamplePharmacy> pharmacies = new ArrayList<>(10000);
		InputStream csvStream = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("pharmacy-names.csv");
		CSVFormat csvFormat = CSVFormat.EXCEL.withSkipHeaderRecord();
		try (Reader csvReader = new InputStreamReader(csvStream);) {
			CSVParser csvParser = csvFormat.parse(csvReader);
			for (CSVRecord record : csvParser) {
				if (record.getRecordNumber() == 1)
					continue;

				// Pull the fields out of the CSV, as-is, skipping incomplete
				// records.
				String npiText = record.get(SamplePharmacyColumn.NPI.getColumnIndex());
				if (npiText.trim().isEmpty())
					continue;
				int npi = Integer.parseInt(npiText);

				// Build and store the new SampleAddress.
				SamplePharmacy provider = new SamplePharmacy(npi);
				pharmacies.add(provider);
			}
			csvParser.close();
		} catch (IOException e) {
			throw new SampleDataException(e);
		}

		this.pharmacies = pharmacies;
	}

	/**
	 * @return a generated {@link SamplePharmacy} instance
	 */
	public SamplePharmacy generateProvider() {
		return pharmacies.get(rng.nextInt(pharmacies.size()));
	}
}
