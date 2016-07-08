package gov.hhs.cms.bluebutton.datapipeline.rif.extract;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link CsvRecordGroupingIterator}.
 */
public class CsvRecordGroupingIteratorTest {
	/**
	 * Tests {@link CsvRecordGroupingIterator} in a scenario that should result
	 * in single-record groups.
	 * 
	 * @throws IOException
	 *             (indicates a test failure)
	 */
	@Test
	public void singleRowGroups() throws IOException {
		// Create some mock data and the iterator to test against it.
		CSVParser parser = CSVFormat.EXCEL.parse(new StringReader("a,b\na,b\na,b\n"));
		CsvRecordGroupingIterator groupingIter = new CsvRecordGroupingIterator(parser, (record1, record2) -> 1);

		// Run the iterator, collecting its results into a List for analysis.
		Stream<List<CSVRecord>> groupedRecordsStream = StreamSupport
				.stream(Spliterators.spliteratorUnknownSize(groupingIter, 0), false);
		List<List<CSVRecord>> groupedRecordsList = groupedRecordsStream.collect(Collectors.toList());

		// Verify the results.
		Assert.assertEquals(3, groupedRecordsList.size());
		Assert.assertEquals(1, groupedRecordsList.get(0).size());
		Assert.assertEquals(1, groupedRecordsList.get(1).size());
		Assert.assertEquals(1, groupedRecordsList.get(2).size());
	}

	/**
	 * Tests {@link CsvRecordGroupingIterator} in a scenario that should result
	 * in two-record groups.
	 * 
	 * @throws IOException
	 *             (indicates a test failure)
	 */
	@Test
	public void twoRowGroups() throws IOException {
		// Create some mock data and the iterator to test against it.
		CSVParser parser = CSVFormat.EXCEL.parse(new StringReader("a,b\na,b\nc,d\nc,d"));
		CsvRecordGroupingIterator groupingIter = new CsvRecordGroupingIterator(parser,
				(record1, record2) -> record1.get(0).compareTo(record2.get(0)));

		// Run the iterator, collecting its results into a List for analysis.
		Stream<List<CSVRecord>> groupedRecordsStream = StreamSupport
				.stream(Spliterators.spliteratorUnknownSize(groupingIter, 0), false);
		List<List<CSVRecord>> groupedRecordsList = groupedRecordsStream.collect(Collectors.toList());

		// Verify the results.
		Assert.assertEquals(2, groupedRecordsList.size());
		Assert.assertEquals(2, groupedRecordsList.get(0).size());
		Assert.assertEquals(2, groupedRecordsList.get(1).size());
	}
}
