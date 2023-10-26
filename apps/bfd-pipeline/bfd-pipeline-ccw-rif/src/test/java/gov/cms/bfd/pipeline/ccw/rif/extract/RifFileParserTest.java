package gov.cms.bfd.pipeline.ccw.rif.extract;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gov.cms.bfd.model.rif.RecordAction;
import gov.cms.bfd.model.rif.RifFile;
import gov.cms.bfd.model.rif.RifFileEvent;
import gov.cms.bfd.model.rif.RifRecordEvent;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.parse.RifParsingUtils;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
public class RifFileParserTest {
  @Mock private RifFileEvent rifFileEventMock;
  @Mock private RifFile rifFileMock;

  @Test
  void groupingHonorsIdColumn() {
    final var parser = new RifFileParser.Grouping("id", this::parseRecord);
    assertEquals(
        List.of("1-a", "2-a;2-b", "3-a;3-b;3-c"),
        parseString("id|data\n1|a\n2|a\n2|b\n3|a\n3|b\n3|c\n", parser));
  }

  /**
   * Used as a lambda for the {@link RifFileParser.Grouping} tests. Simply passes through the
   * records passed to it so they can be checked to see if groups are correct.
   *
   * @param csvRecords records selected by the parser for this event
   * @return a {@link RifRecordEvent} holding the provided {@link CSVRecord}s
   */
  private RifRecordEvent<Beneficiary> parseRecord(List<CSVRecord> csvRecords) {
    return new RifRecordEvent<>(
        rifFileEventMock, csvRecords, RecordAction.INSERT, 1L, new Beneficiary());
  }

  /**
   * Creates a {@link CSVParser} for the specified CSV string (which simulates the contents of a
   * file), parses it into {@link RifRecordEvent}s using the provided parser, and converts the
   * result into a list of strings. Each string in the list contains the raw csv associated with
   * each {@link RifRecordEvent} by the parser with {@code -} between fields and {@code ;} between
   * records.
   *
   * @param csvString simulates a CSV file
   * @param parser used to parse the csv
   * @return list with one string for each {@link RifRecordEvent} produced by the parser
   */
  private List<String> parseString(String csvString, RifFileParser parser) {
    CSVParser csvParser =
        RifParsingUtils.createCsvParser(
            RifParsingUtils.CSV_FORMAT,
            new ByteArrayInputStream(csvString.getBytes(StandardCharsets.UTF_8)),
            StandardCharsets.UTF_8);
    return Flux.fromIterable(csvParser)
        .flatMap(parser::next)
        .concatWith(Flux.defer(parser::finish))
        .map(rifRecordEvent -> convertRecordsIntoStrings(rifRecordEvent.getRawCsvRecords()))
        .collectList()
        .block();
  }

  /**
   * Convert a list of {@link CSVRecord} into a single string with fields within each record
   * separated by {@code -} and individual records separated by {@code ;}.
   *
   * @param csvRecords the records to convert
   * @return the resulting string
   */
  private String convertRecordsIntoStrings(List<CSVRecord> csvRecords) {
    return csvRecords.stream()
        .map(csvRecord -> csvRecord.stream().collect(Collectors.joining("-")))
        .toList()
        .stream()
        .collect(Collectors.joining(";"));
  }
}
