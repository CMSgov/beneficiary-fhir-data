package gov.cms.bfd.pipeline.ccw.rif.extract;

import gov.cms.bfd.model.rif.RifFile;
import gov.cms.bfd.model.rif.RifRecordEvent;
import gov.cms.bfd.model.rif.parse.RifParsingUtils;
import gov.cms.bfd.pipeline.sharedutils.FluxUtils;
import gov.cms.bfd.sharedutils.interfaces.ThrowingFunction;
import gov.cms.model.dsl.codegen.library.CsvRifObject;
import gov.cms.model.dsl.codegen.library.RifObject;
import java.io.IOException;
import java.util.List;
import javax.annotation.concurrent.ThreadSafe;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVRecord;
import reactor.core.publisher.Flux;

/**
 * Instances of this class provide a method that takes a {@link RifFile} and returns a {@link Flux}
 * of {@link RifRecordEvent}s.
 */
@ThreadSafe
public abstract class RifFileParser {
  /**
   * Creates a new {@link Flux} that, when subscribed to, opens the file, parses RIF data, and
   * publishes the resulting {@link RifRecordEvent} objects.
   *
   * @param rifFile the file to parse
   * @return flux that publishes parsed objects
   */
  public abstract Flux<RifRecordEvent<?>> parseRifFile(RifFile rifFile);

  /**
   * Implementation that parses each individual record into a {@link RifRecordEvent} using a lambda
   * function.
   */
  @AllArgsConstructor
  public static class Simple extends RifFileParser {
    /** Lambda used to parse a single {@link CSVRecord} into a {@link RifRecordEvent}. */
    private final ThrowingFunction<RifRecordEvent<?>, RifObject, Exception> parser;

    @Override
    public Flux<RifRecordEvent<?>> parseRifFile(RifFile rifFile) {
      return parseAutoDetectedRifFile(rifFile).map(FluxUtils.wrapFunction(parser));
    }
  }

  /**
   * Implementation that parses groups of consecutive records that have the same value in a given
   * column into a {@link RifRecordEvent} using a lambda function.
   */
  @RequiredArgsConstructor
  public static class Grouping extends RifFileParser {
    /** The name of the column to group by. */
    private final String groupingColumn;

    /** Lambda used to parse one or more {@link CSVRecord}s into a {@link RifRecordEvent}. */
    private final ThrowingFunction<RifRecordEvent<?>, List<RifObject>, Exception> parser;

    @Override
    public Flux<RifRecordEvent<?>> parseRifFile(RifFile rifFile) {
      return parseAutoDetectedRifFile(rifFile)
          // joins consecutive records with same grouping column value
          .bufferUntilChanged(csvRecord -> csvRecord.get(groupingColumn))
          // parses the list of records
          .flatMap(this::parse);
    }

    /**
     * Calls the lambda to produce a new {@link RifRecordEvent} from a list of {@link CSVRecord}s.
     *
     * @param records group of records to parse (may be empty)
     * @return flux containing the resulting object or an empty flux if the list was empty
     */
    private Flux<RifRecordEvent<?>> parse(List<RifObject> records) {
      try {
        return records.isEmpty() ? Flux.empty() : Flux.just(parser.apply(records));
      } catch (Exception ex) {
        return Flux.error(ex);
      }
    }
  }

  public static Flux<RifObject> parseAutoDetectedRifFile(RifFile rifFile) {
    try {
      if (rifFile.getFile().getPath().endsWith(".parquet")) {
        return parseParquetRifFile(rifFile);
      } else {
        return parseCsvRifFile(rifFile);
      }
    } catch (IOException ex) {
      return parseCsvRifFile(rifFile);
    }
  }

  public static Flux<RifObject> parseCsvRifFile(RifFile rifFile) {
    return FluxUtils.fromAutoCloseable(
        // creates a CSVParser for new subscriber
        () -> RifParsingUtils.createCsvParser(rifFile),
        // creates flux for subscriber to receive parsed events
        csvParser -> Flux.fromIterable(csvParser).map(CsvRifObject::new),
        // used in log message if closing the CSVParser fails
        rifFile.getDisplayName());
  }

  public static Flux<RifObject> parseParquetRifFile(RifFile rifFile) {
    return FluxUtils.fromAutoCloseable(
        // creates a ParquetFileParser for new subscriber
        () -> new ParquetFileParser(rifFile.getFile()),
        // creates flux for subscriber to receive parsed events
        parquetParser ->
            Flux.generate(
                sink -> {
                  try {
                    if (parquetParser.readNext()) {
                      sink.next(parquetParser.getCurrentObject());
                    } else {
                      sink.complete();
                    }
                  } catch (IOException ex) {
                    sink.error(ex);
                  }
                }),
        // used in log message if closing the CSVParser fails
        rifFile.getDisplayName());
  }
}
