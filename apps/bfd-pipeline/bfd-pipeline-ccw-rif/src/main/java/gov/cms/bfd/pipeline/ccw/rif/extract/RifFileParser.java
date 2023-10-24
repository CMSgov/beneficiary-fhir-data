package gov.cms.bfd.pipeline.ccw.rif.extract;

import gov.cms.bfd.model.rif.RifRecordEvent;
import gov.cms.bfd.sharedutils.interfaces.ThrowingFunction;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVRecord;
import reactor.core.publisher.Flux;

/**
 * Accepts a stream of {@link CSVRecord}s, one at a time, and returns a stream of {@link
 * RifRecordEvent}s. These objects are stateful and can receive records one at a time.
 */
@ThreadSafe
public abstract class RifFileParser {
  /**
   * Accepts the next record and returns a (possibly empty) {@link Flux} or parsed records.
   *
   * @param record next CSV record
   * @return resulting parsed records
   */
  public abstract Flux<RifRecordEvent<?>> next(CSVRecord record);

  /**
   * Called after all records have been processed to return a (possibly empty) {@link Flux} of
   * parsed records. Allows any previously buffered records to be combined into a final result.
   *
   * @return resulting parsed records
   */
  public abstract Flux<RifRecordEvent<?>> finish();

  /** Implementation that parses each individual record into a {@link RifRecordEvent<?>}. */
  @ThreadSafe
  @AllArgsConstructor
  public static class Simple extends RifFileParser {
    /** Lambda used to parse a single {@link CSVRecord} into a {@link RifRecordEvent<?>}. */
    private final ThrowingFunction<RifRecordEvent<?>, CSVRecord, Exception> parser;

    @Override
    public synchronized Flux<RifRecordEvent<?>> next(CSVRecord record) {
      try {
        return Flux.just(parser.apply(record));
      } catch (Exception ex) {
        return Flux.error(ex);
      }
    }

    @Override
    public synchronized Flux<RifRecordEvent<?>> finish() {
      return Flux.empty();
    }
  }

  /**
   * Implementation that parses groups of consecutive records that have the same value in a given
   * column into a {@link RifRecordEvent<?>} using a lambda function.
   */
  @ThreadSafe
  @RequiredArgsConstructor
  public static class Grouping extends RifFileParser {
    /** The name of the column to group by. */
    private final Enum<?> groupingColumn;

    /** Lambda used to parse one or more {@link CSVRecord}s into a {@link RifRecordEvent<?>}. */
    private final ThrowingFunction<RifRecordEvent<?>, List<CSVRecord>, Exception> parser;

    /** Accumulates lines between calls to {@link #next}. */
    @GuardedBy("synchronized")
    private final List<CSVRecord> recordBuffer = new ArrayList<>();

    /** Value of the grouping column for the current group or null if we have no group. */
    @GuardedBy("synchronized")
    @Nullable
    private String currentColumn;

    @Override
    public synchronized Flux<RifRecordEvent<?>> next(CSVRecord record) {
      final String newColumn = record.get(groupingColumn);
      Flux<RifRecordEvent<?>> result = Flux.empty();
      if (currentColumn == null) {
        currentColumn = newColumn;
        addRecord(record);
      } else if (currentColumn.equals(newColumn)) {
        addRecord(record);
      } else {
        currentColumn = newColumn;
        result = parse(takeRecords());
      }
      return result;
    }

    @Override
    public synchronized Flux<RifRecordEvent<?>> finish() {
      currentColumn = null;
      return parse(takeRecords());
    }

    /**
     * Calls the lambda to produce a new {@link RifRecordEvent<?>}.
     *
     * @param records non-empty group of records to parse
     * @return flux containing the resulting object
     */
    @Nonnull
    private Flux<RifRecordEvent<?>> parse(List<CSVRecord> records) {
      try {
        return records.isEmpty() ? Flux.empty() : Flux.just(parser.apply(records));
      } catch (Exception ex) {
        return Flux.error(ex);
      }
    }

    /**
     * Helper method to add a record to the buffer.
     *
     * @param record the record to add
     */
    private void addRecord(CSVRecord record) {
      recordBuffer.add(record);
    }

    /**
     * Helper method to remove all records from the buffer and return them.
     *
     * @return immutable list of all records in buffer
     */
    private List<CSVRecord> takeRecords() {
      List<CSVRecord> lines = List.copyOf(recordBuffer);
      recordBuffer.clear();
      return lines;
    }
  }
}
