package gov.cms.bfd.pipeline.ccw.rif.extract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import gov.cms.bfd.model.rif.RecordAction;
import gov.cms.bfd.model.rif.RifFile;
import gov.cms.bfd.model.rif.RifFileEvent;
import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.model.rif.RifRecordEvent;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link RifFileParser}. */
@ExtendWith(MockitoExtension.class)
@Slf4j
public class RifFileParserTest {
  /** Temp file for rif data, created and deleted once per instance. */
  private static Path tempFile;

  /**
   * The file to be parsed. The event type doesn't really matter since we are only testing with
   * trivial records to verify grouping behavior.
   */
  private final RifFile rifFile = new LocalRifFile(tempFile, RifFileType.BENEFICIARY);

  /** We need this when we create a {@link RifRecordEvent} but we never call any of its methods. */
  @Mock private RifFileEvent rifFileEventMock;

  /**
   * Creates the temp file before any tests have run.
   *
   * @throws IOException pass through
   */
  @BeforeAll
  static void beforeAll() throws IOException {
    tempFile = Files.createTempFile(RifFileParser.class.getName(), ".csv");
  }

  /**
   * Deletes the temp file after all tests have run.
   *
   * @throws IOException pass through
   */
  @AfterAll
  static void afterAll() throws IOException {
    if (tempFile != null) {
      Files.deleteIfExists(tempFile);
    }
  }

  /**
   * Verifies edge conditions for {@link RifFileParser.Simple}.
   *
   * @throws IOException pass through from writing string to temp file
   */
  @Test
  void simpleAlwaysUsesOneRecordPerEvent() throws IOException {
    final var parser = new RifFileParser.Simple(this::parseSingle);
    // empty file should produce no records
    assertEquals(List.of(), parseString("", parser));

    // single record in file should work
    assertEquals(List.of("1->1-a"), parseString("id|data\n1|a\n", parser));

    // several single records should work
    assertEquals(
        List.of("1->1-a", "2->2-a", "3->3-a"), parseString("id|data\n1|a\n2|a\n3|a\n", parser));

    // several with mixed number of records should work
    assertEquals(
        List.of("1->1-a", "2->2-a", "3->2-b", "4->3-a", "5->3-b", "6->3-c"),
        parseString("id|data\n1|a\n2|a\n2|b\n3|a\n3|b\n3|c\n", parser));
  }

  /**
   * Verifies edge conditions for {@link RifFileParser.Grouping}.
   *
   * @throws IOException pass through from writing string to temp file
   */
  @Test
  void groupingHonorsIdColumn() throws IOException {
    final var parser = new RifFileParser.Grouping("id", this::parseGroup);
    // empty file should produce no records
    assertEquals(List.of(), parseString("", parser));

    // single record in file should work
    assertEquals(List.of("1->1-a"), parseString("id|data\n1|a\n", parser));

    // several single records should work
    assertEquals(
        List.of("1->1-a", "2->2-a", "3->3-a"), parseString("id|data\n1|a\n2|a\n3|a\n", parser));

    // several with mixed number of records should work
    assertEquals(
        List.of("1->1-a", "2->2-a;2-b", "3->3-a;3-b;3-c"),
        parseString("id|data\n1|a\n2|a\n2|b\n3|a\n3|b\n3|c\n", parser));

    // missing grouping column should throw an exception
    assertThrows(RuntimeException.class, () -> parseString("noid|data\n1|a\n2|a\n3|a\n", parser));
  }

  /**
   * Similar to {@link #groupingHonorsIdColumn} but uses randomly generated rif data to test wider
   * variety of scenarios {@link RifFileParser.Grouping}.
   *
   * @throws IOException pass through from writing string to temp file
   */
  @Test
  void groupingHonorsIdColumnRandomScenarios() throws IOException {
    final var parser = new RifFileParser.Grouping("id", this::parseGroup);
    final var random = new Random(42);
    for (int trial = 1; trial <= 100; ++trial) {
      // This will contain the unparsed RIF data
      final var rifFileData = new StringBuilder("id|data\n");
      // This will contain the expected output of the parsing code.
      final var expectedFileParsingResults = new ArrayList<String>();

      //
      for (int record = 1; record <= 50; ++record) {
        final int lineCount = 1 + random.nextInt(5);
        final var expectedRecordParsingResult = new StringBuilder();
        expectedRecordParsingResult.append(record + "->");
        final int recordId = random.nextInt(1_000_000);
        for (int line = 1; line <= lineCount; ++line) {
          final int recordData = random.nextInt(1_000_000);
          rifFileData.append(String.format("%s|%s\n", recordId, recordData));
          if (line > 1) {
            expectedRecordParsingResult.append(";");
          }
          expectedRecordParsingResult.append(String.format("%s-%s", recordId, recordData));
        }
        expectedFileParsingResults.add(expectedRecordParsingResult.toString());
      }
      assertEquals(expectedFileParsingResults, parseString(rifFileData.toString(), parser));
    }
  }

  /**
   * Used as a lambda for the {@link RifFileParser.Simple} tests. Simply passes through the record
   * passed to it so it can be checked for correctness.
   *
   * @param csvRecord record selected by the parser for this event
   * @return a {@link RifRecordEvent} holding the provided {@link CSVRecord}
   */
  private RifRecordEvent<Beneficiary> parseSingle(CSVRecord csvRecord) {
    return new RifRecordEvent<>(
        rifFileEventMock, List.of(csvRecord), RecordAction.INSERT, 1L, new Beneficiary());
  }

  /**
   * Used as a lambda for the {@link RifFileParser.Grouping} tests. Simply passes through the
   * records passed to it so they can be checked to see if groups are correct.
   *
   * @param csvRecords records selected by the parser for this event
   * @return a {@link RifRecordEvent} holding the provided {@link CSVRecord}s
   */
  private RifRecordEvent<Beneficiary> parseGroup(List<CSVRecord> csvRecords) {
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
  private List<String> parseString(String csvString, RifFileParser parser) throws IOException {
    Files.writeString(tempFile, csvString);
    return parser
        .parseRifFile(rifFile)
        .map(
            rifRecordEvent ->
                convertRecordsIntoStrings(
                    rifRecordEvent.getRecordNumber(), rifRecordEvent.getRawCsvRecords()))
        .collectList()
        .block();
  }

  /**
   * Convert a list of {@link CSVRecord} into a single string with fields within each record
   * separated by {@code -} and individual records separated by {@code ;}. The record number is
   * added to the beginning of the string followed by {@code ->}.
   *
   * @param csvRecords the records to convert
   * @return the resulting string
   */
  private String convertRecordsIntoStrings(long recordNumber, List<CSVRecord> csvRecords) {
    List<String> recordStrings =
        csvRecords.stream()
            .map(csvRecord -> csvRecord.stream().collect(Collectors.joining("-")))
            .toList();
    return recordNumber + "->" + String.join(";", recordStrings);
  }
}
