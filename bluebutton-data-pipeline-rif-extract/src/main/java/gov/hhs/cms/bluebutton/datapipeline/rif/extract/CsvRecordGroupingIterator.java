package gov.hhs.cms.bluebutton.datapipeline.rif.extract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import gov.hhs.cms.bluebutton.datapipeline.rif.model.CarrierClaimGroup;

/**
 * <p>
 * This {@link Iterator} can group together related {@link CSVRecord}s from a
 * {@link CSVParser}. The grouping/relationships will be defined by the
 * {@link CsvRecordGrouper} implementation that is provided to it at
 * construction. This was designed to allow the streaming of related
 * {@link CSVRecord}s, as Java 8's {@link Stream}s do not provide any
 * non-terminal (i.e. lazy) batching/grouping operations. Instead, the
 * batching/grouping has to be performed upstream. So.
 * </p>
 * <p>
 * This class is laughably non-thread-safe. That said, it shouldn't need to be,
 * as even Java 8's parallel {@link Stream}s use only a single thread for record
 * production. TODO verify this assertion!!!!
 * </p>
 */
public final class CsvRecordGroupingIterator implements Iterator<List<CSVRecord>> {
	private final Iterator<CSVRecord> singleRecordIter;
	private final CsvRecordGrouper grouper;

	/**
	 * During the processing of {@link #next()}, this iterator has to
	 * "look ahead" at the next {@link CSVRecord} (if any) to see if it is part
	 * of the current group. If not, that record shouldn't be returned right
	 * away, but will instead be the first item in the {@link List} that will be
	 * returned by the <em>next</em> call to {@link #next()}. When that happens,
	 * we store the record here until it's needed.
	 */
	private Optional<CSVRecord> recordFromNextGroup = Optional.empty();

	/**
	 * Constructs a new {@link CsvRecordGroupingIterator} instance.
	 * 
	 * @param parser
	 *            the {@link CSVParser} to iterate over
	 * @param grouper
	 *            the {@link CsvRecordGrouper} to use
	 */
	public CsvRecordGroupingIterator(CSVParser parser, CsvRecordGrouper grouper) {
		this.singleRecordIter = groupingBugWorkaround(parser.iterator());
		this.grouper = grouper;
	}

	/**
	 * @param iterator
	 *            the {@link CSVRecord} {@link Iterator} to fix claim grouping
	 *            issues in
	 * @return a new {@link CSVRecord} {@link Iterator}, with records properly
	 *         grouped
	 */
	private static Iterator<CSVRecord> groupingBugWorkaround(Iterator<CSVRecord> iterator) {
		/*
		 * FIXME This is a workaround for
		 * http://issues.hhsdevcloud.us/browse/CBBD-92. This workaround adds
		 * unacceptable memory usage requirements to the system, and MUST be
		 * removed before the system enters production.
		 */
		List<CSVRecord> records = new ArrayList<>();
		while (iterator.hasNext())
			records.add(iterator.next());

		records.sort(new Comparator<CSVRecord>() {
			@Override
			public int compare(CSVRecord o1, CSVRecord o2) {
				String claimId1 = o1.get(CarrierClaimGroup.Column.CLM_ID.ordinal());
				String claimId2 = o2.get(CarrierClaimGroup.Column.CLM_ID.ordinal());
				return claimId1.compareTo(claimId2);
			}
		});

		return records.iterator();
	}

	/**
	 * @see java.util.Iterator#hasNext()
	 */
	@Override
	public boolean hasNext() {
		return recordFromNextGroup.isPresent() || singleRecordIter.hasNext();
	}

	/**
	 * @see java.util.Iterator#next()
	 */
	@Override
	public List<CSVRecord> next() {
		if (!hasNext())
			throw new NoSuchElementException();

		List<CSVRecord> recordGroup = new LinkedList<>();
		CSVRecord firstRecordInGroup;
		if (recordFromNextGroup.isPresent()) {
			firstRecordInGroup = recordFromNextGroup.get();
			recordFromNextGroup = Optional.empty();
		} else {
			firstRecordInGroup = singleRecordIter.next();
		}
		recordGroup.add(firstRecordInGroup);

		while (hasNext()) {
			CSVRecord previousRecord = recordGroup.get(recordGroup.size() - 1);
			CSVRecord nextRecord = singleRecordIter.next();

			if (grouper.areSameGroup(previousRecord, nextRecord)) {
				recordGroup.add(nextRecord);
			} else {
				recordFromNextGroup = Optional.of(nextRecord);
				break;
			}
		}

		return Collections.unmodifiableList(recordGroup);
	}

	/**
	 * Implementations of this interface can be used by
	 * {@link CsvRecordGroupingIterator} to determine which {@link CSVRecord}s
	 * should be included in same/different groups. Note that this operates in a
	 * sequential fashion: {@link CSVRecord}s that should be grouped together
	 * must be adjacent to each other.
	 */
	public interface CsvRecordGrouper {
		/**
		 * @param record1
		 *            the first {@link CSVRecord} to compare
		 * @param record2
		 *            the second {@link CSVRecord} to compare
		 * @return <code>true</code> if the specified {@link CSVRecord}s should
		 *         be part of the same group, <code>false</code> if they should
		 *         not
		 */
		boolean areSameGroup(CSVRecord record1, CSVRecord record2);
	}
}
