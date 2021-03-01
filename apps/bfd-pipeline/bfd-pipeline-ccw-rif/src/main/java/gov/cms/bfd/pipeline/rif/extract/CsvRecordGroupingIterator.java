package gov.cms.bfd.pipeline.rif.extract;

import gov.cms.bfd.model.rif.parse.InvalidRifFileFormatException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * This {@link Iterator} can group together related {@link CSVRecord}s from a {@link CSVParser}. The
 * grouping/relationships will be defined by the {@link CsvRecordGrouper} implementation that is
 * provided to it at construction. This was designed to allow the streaming of related {@link
 * CSVRecord}s, as Java 8's {@link Stream}s do not provide any non-terminal (i.e. lazy)
 * batching/grouping operations. Instead, the batching/grouping has to be performed upstream. So.
 *
 * <p>This class is laughably non-thread-safe. But I'm not aware of any way to ensure that {@link
 * Iterator}s in general are ever thread-safe. Anyways: <strong>this may not be used with parallel
 * {@link Stream}s</strong>, only sequential ones.
 */
public final class CsvRecordGroupingIterator implements Iterator<List<CSVRecord>> {
  private final Iterator<CSVRecord> singleRecordIter;
  private final CsvRecordGrouper grouper;

  /**
   * During the processing of {@link #next()}, this iterator has to "look ahead" at the next {@link
   * CSVRecord} (if any) to see if it is part of the current group. If not, that record shouldn't be
   * returned right away, but will instead be the first item in the {@link List} that will be
   * returned by the <em>next</em> call to {@link #next()}. When that happens, we store the record
   * here until it's needed.
   */
  private Optional<CSVRecord> recordFromNextGroup = Optional.empty();

  /**
   * Constructs a new {@link CsvRecordGroupingIterator} instance.
   *
   * @param parser the {@link CSVParser} to iterate over
   * @param grouper the {@link CsvRecordGrouper} to use
   */
  public CsvRecordGroupingIterator(CSVParser parser, CsvRecordGrouper grouper) {
    this.singleRecordIter = parser.iterator();
    this.grouper = grouper;
  }

  /** @see java.util.Iterator#hasNext() */
  @Override
  public boolean hasNext() {
    return recordFromNextGroup.isPresent() || singleRecordIter.hasNext();
  }

  /** @see java.util.Iterator#next() */
  @Override
  public List<CSVRecord> next() {
    if (!hasNext()) throw new NoSuchElementException();

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
   * Implementations of this interface can be used by {@link CsvRecordGroupingIterator} to determine
   * which {@link CSVRecord}s should be included in same/different groups. Note that this operates
   * in a sequential fashion: {@link CSVRecord}s that should be grouped together must be adjacent to
   * each other.
   */
  public interface CsvRecordGrouper {
    /**
     * @param record1 the first {@link CSVRecord} to compare
     * @param record2 the second {@link CSVRecord} to compare
     * @return <code>true</code> if the specified {@link CSVRecord}s should be part of the same
     *     group, <code>false</code> if they should not
     */
    boolean areSameGroup(CSVRecord record1, CSVRecord record2);
  }

  /**
   * The "standard" {@link CsvRecordGrouper} implementation, that simply groups rows by the value of
   * a single shared column (typically the claim ID).
   */
  public static final class ColumnValueCsvRecordGrouper implements CsvRecordGrouper {
    private final Enum<?> groupingColumn;

    /**
     * Constructs a new {@link ColumnValueCsvRecordGrouper} instance.
     *
     * @param groupingColumn the name of the column to group by, or <code>null</code> if no rows
     *     should be grouped
     */
    public ColumnValueCsvRecordGrouper(Enum<?> groupingColumn) {
      this.groupingColumn = groupingColumn;
    }

    /**
     * @see
     *     gov.cms.bfd.pipeline.rif.extract.CsvRecordGroupingIterator.CsvRecordGrouper#areSameGroup(org.apache.commons.csv.CSVRecord,
     *     org.apache.commons.csv.CSVRecord)
     */
    @Override
    public boolean areSameGroup(CSVRecord record1, CSVRecord record2) {
      if (record1 == null) throw new InvalidRifFileFormatException("Carrier record 1 is null");
      if (record2 == null) throw new InvalidRifFileFormatException("Carrier record 2 is null");

      if (groupingColumn == null) return false;

      String record1ComparisonValue = record1.get(groupingColumn);
      String record2ComparisonValue = record2.get(groupingColumn);
      return record1ComparisonValue.equals(record2ComparisonValue);
    }
  }
}
