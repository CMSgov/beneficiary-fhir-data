package gov.hhs.cms.bluebutton.datapipeline.sampledata.npi;

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
 * Provides methods for generating {@link SampleProvider}s.
 */
public final class SampleProviderGenerator {
	/**
	 * Used to select a random {@link SampleProvider} entry from
	 * {@link #providers}. Note that the seed is fixed so that the sequence
	 * produced is stable. This should make the sample data more predictable,
	 * which is a good thing for our use cases.
	 */
	private final Random rng = new Random(42L);
	private final List<SampleProvider> providers;

	/**
	 * Constructs a new {@link SampleProviderGenerator} instance. This operation
	 * will be a bit slow, as it will load and parse a large set of data from
	 * the classpath.
	 * 
	 * @throws SampleDataException
	 *             A {@link SampleDataException} will be thrown if any errors
	 *             occur reading in or processing the address data resource from
	 *             the classpath.
	 * 
	 */
	public SampleProviderGenerator() throws SampleDataException {
		List<SampleProvider> providers = new ArrayList<>(10000);
		InputStream npiCsvStream = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("npi/npidata-20050523-20160313-subset-10000.csv");
		CSVFormat csvFormat = CSVFormat.EXCEL.withSkipHeaderRecord();
		try (Reader npiCsvReader = new InputStreamReader(npiCsvStream);) {
			CSVParser npiCsvParser = csvFormat.parse(npiCsvReader);
			for (CSVRecord record : npiCsvParser) {
				if (record.getRecordNumber() == 1)
					continue;

				// Pull the fields out of the CSV, as-is.
				String npiText = record.get(SampleProviderColumn.NPI.getColumnIndex());
				int npi = Integer.parseInt(npiText);

				// Build and store the new SampleAddress.
				SampleProvider provider = new SampleProvider(npi);
				providers.add(provider);
			}
			npiCsvParser.close();
		} catch (IOException e) {
			throw new SampleDataException(e);
		}

		this.providers = providers;
	}

	/**
	 * @return a generated {@link SampleProvider} instance
	 */
	public SampleProvider generateProvider() {
		return providers.get(rng.nextInt(providers.size()));
	}
}
