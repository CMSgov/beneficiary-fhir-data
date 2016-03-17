package gov.hhs.cms.bluebutton.datapipeline.sampledata.addresses;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import gov.hhs.cms.bluebutton.datapipeline.sampledata.SampleDataException;

/**
 * Provides methods for generating {@link SampleAddress}s.
 */
public final class SampleAddressGenerator {
	/**
	 * Used to select a random {@link SampleAddress} entry from XXX. Note that
	 * the seed is fixed so that the sequence produced is stable. This should
	 * make the sample data more predictable, which is a good thing for our use
	 * cases.
	 */
	private final Random rng = new Random(42L);
	private final List<SampleAddress> addresses;

	/**
	 * Constructs a new {@link SampleAddressGenerator} instance. This operation
	 * will be a bit slow, as it will load and parse a large set of data from
	 * the classpath.
	 * 
	 * @throws SampleDataException
	 *             A {@link SampleDataException} will be thrown if any errors
	 *             occur reading in or processing the address data resource from
	 *             the classpath.
	 * 
	 */
	public SampleAddressGenerator() throws SampleDataException {
		List<SampleAddress> addresses = new ArrayList<>(503730);
		InputStream addessesCsvStream = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("prescriber-addresses.csv");
		CSVFormat csvFormat = CSVFormat.EXCEL.withHeader(SampleAddressColumn.getAllColumnNames())
				.withSkipHeaderRecord();
		try (Reader addressesCsvReader = new InputStreamReader(addessesCsvStream);) {
			CSVParser addressCsvParser = csvFormat.parse(addressesCsvReader);
			for (CSVRecord record : addressCsvParser) {
				// Pull the fields out of the CSV, as-is.
				String street1 = record.get(SampleAddressColumn.STREET_1.getColumnName());
				String street2 = record.get(SampleAddressColumn.STREET_2.getColumnName());
				String city = record.get(SampleAddressColumn.CITY.getColumnName());
				String state = record.get(SampleAddressColumn.STATE.getColumnName());
				String zip = record.get(SampleAddressColumn.ZIP.getColumnName());

				/*
				 * This code is surprisingly complex for a simple result: it
				 * combines the address components into a single address string,
				 * with no blanks, and with each component separated by ", ".
				 */
				List<String> addressExceptZipComponents = new ArrayList<>(4);
				for (String addressExceptZipComponent : new String[] { street1, street2, city, state })
					if (addressExceptZipComponent != null && addressExceptZipComponent.trim().length() > 0)
						addressExceptZipComponents.add(addressExceptZipComponent.trim());
				StringBuilder addressExceptZip = new StringBuilder();
				Iterator<String> addressExceptZipComponentsIter = addressExceptZipComponents.iterator();
				while (addressExceptZipComponentsIter.hasNext()) {
					addressExceptZip.append(addressExceptZipComponentsIter.next());
					if (addressExceptZipComponentsIter.hasNext())
						addressExceptZip.append(", ");
				}

				// Build and store the new SampleAddress.
				SampleAddress address = new SampleAddress(addressExceptZip.toString(), zip.trim());
				addresses.add(address);
			}
			addressCsvParser.close();
		} catch (IOException e) {
			throw new SampleDataException(e);
		}

		this.addresses = addresses;
	}

	/**
	 * @return a generated {@link SampleAddress} instance
	 */
	public SampleAddress generateAddress() {
		return addresses.get(rng.nextInt(addresses.size()));
	}
}
