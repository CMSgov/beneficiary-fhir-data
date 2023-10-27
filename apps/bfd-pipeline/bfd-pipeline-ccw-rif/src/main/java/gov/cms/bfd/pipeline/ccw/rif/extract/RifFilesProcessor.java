package gov.cms.bfd.pipeline.ccw.rif.extract;

import gov.cms.bfd.model.rif.RecordAction;
import gov.cms.bfd.model.rif.RifFile;
import gov.cms.bfd.model.rif.RifFileEvent;
import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.model.rif.RifRecordEvent;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.entities.BeneficiaryHistory;
import gov.cms.bfd.model.rif.entities.BeneficiaryHistoryParser;
import gov.cms.bfd.model.rif.entities.BeneficiaryParser;
import gov.cms.bfd.model.rif.entities.CarrierClaim;
import gov.cms.bfd.model.rif.entities.CarrierClaimParser;
import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.model.rif.entities.DMEClaimParser;
import gov.cms.bfd.model.rif.entities.HHAClaim;
import gov.cms.bfd.model.rif.entities.HHAClaimParser;
import gov.cms.bfd.model.rif.entities.HospiceClaim;
import gov.cms.bfd.model.rif.entities.HospiceClaimParser;
import gov.cms.bfd.model.rif.entities.InpatientClaim;
import gov.cms.bfd.model.rif.entities.InpatientClaimParser;
import gov.cms.bfd.model.rif.entities.OutpatientClaim;
import gov.cms.bfd.model.rif.entities.OutpatientClaimParser;
import gov.cms.bfd.model.rif.entities.PartDEvent;
import gov.cms.bfd.model.rif.entities.PartDEventParser;
import gov.cms.bfd.model.rif.entities.SNFClaim;
import gov.cms.bfd.model.rif.entities.SNFClaimParser;
import gov.cms.bfd.model.rif.parse.InvalidRifValueException;
import gov.cms.model.dsl.codegen.library.DataTransformer.TransformationException;
import gov.cms.model.dsl.codegen.library.RifObjectWrapper;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;
import reactor.core.publisher.Flux;

/** Contains services responsible for handling new RIF files. */
@Slf4j
public class RifFilesProcessor {
  /** Column ID for the column that contains the action value in each CSV record. */
  private static final String RECORD_ACTION_COLUMN = "DML_IND";

  /**
   * Produces a {@link RifFileRecords} with the {@link RifRecordEvent}s produced from the specified
   * {@link RifFileEvent}.
   *
   * @param rifFileEvent the {@link RifFileEvent} that is being processed
   * @return the record from the rif file
   */
  public RifFileRecords produceRecords(RifFileEvent rifFileEvent) {
    RifFile rifFile = rifFileEvent.getFile();
    RifFileParser rifFileParser =
        switch (rifFile.getFileType()) {
          case BENEFICIARY -> beneficiaryEventParser(rifFileEvent);
          case BENEFICIARY_HISTORY -> beneficiaryHistoryEventParser(rifFileEvent);
          case CARRIER -> carrierClaimParser(rifFileEvent);
          case DME -> dmeClaimParser(rifFileEvent);
          case HHA -> hhaClaimParser(rifFileEvent);
          case HOSPICE -> hospiceClaimParser(rifFileEvent);
          case INPATIENT -> inpatientClaimParser(rifFileEvent);
          case OUTPATIENT -> outpatientClaimParser(rifFileEvent);
          case PDE -> partDEventParser(rifFileEvent);
          case SNF -> snfClaimParser(rifFileEvent);
        };
    Flux<RifRecordEvent<?>> records = rifFileParser.parseRifFile(rifFile);
    return new RifFileRecords(rifFileEvent, records);
  }

  /**
   * Creates a {@link RifFileParser} that creates {@link Beneficiary}s.
   *
   * @param fileEvent the {@link RifFileEvent} being processed
   * @return the parser
   */
  private static RifFileParser beneficiaryEventParser(RifFileEvent fileEvent) {
    final var parser = new BeneficiaryParser();
    return new RifFileParser.Simple(
        csvRecord -> {
          trace(csvRecord);
          final List<CSVRecord> csvRecords = List.of(csvRecord);
          final RecordAction recordAction = parseRecordAction(csvRecord);
          final Beneficiary beneficiaryRow = parse(csvRecords, parser::transformMessage);

          // Swap the unhashed HICN into the correct field.
          beneficiaryRow.setHicnUnhashed(Optional.ofNullable(beneficiaryRow.getHicn()));
          beneficiaryRow.setHicn(null);

          return new RifRecordEvent<>(
              fileEvent,
              csvRecords,
              recordAction,
              beneficiaryRow.getBeneficiaryId(),
              beneficiaryRow);
        });
  }

  /**
   * Creates a {@link RifFileParser} that creates {@link BeneficiaryHistory}s.
   *
   * @param fileEvent the {@link RifFileEvent} being processed
   * @return the parser
   */
  private static RifFileParser beneficiaryHistoryEventParser(RifFileEvent fileEvent) {
    final var parser = new BeneficiaryHistoryParser();
    return new RifFileParser.Simple(
        csvRecord -> {
          trace(csvRecord);
          final List<CSVRecord> csvRecords = List.of(csvRecord);
          final RecordAction recordAction = parseRecordAction(csvRecord);
          final BeneficiaryHistory beneHistoryRow = parse(csvRecords, parser::transformMessage);
          return new RifRecordEvent<>(
              fileEvent,
              csvRecords,
              recordAction,
              beneHistoryRow.getBeneficiaryId(),
              beneHistoryRow);
        });
  }

  /**
   * Creates a {@link RifFileParser} that creates {@link PartDEvent}s.
   *
   * @param fileEvent the {@link RifFileEvent} being processed
   * @return the parser
   */
  private static RifFileParser partDEventParser(RifFileEvent fileEvent) {
    final var parser = new PartDEventParser();
    return new RifFileParser.Simple(
        csvRecord -> {
          trace(csvRecord);
          final List<CSVRecord> csvRecords = List.of(csvRecord);
          final RecordAction recordAction = parseRecordAction(csvRecord);
          final PartDEvent partDEvent = parse(csvRecords, parser::transformMessage);
          return new RifRecordEvent<>(
              fileEvent, csvRecords, recordAction, partDEvent.getBeneficiaryId(), partDEvent);
        });
  }

  /**
   * Creates a {@link RifFileParser} that creates {@link InpatientClaim}s.
   *
   * @param fileEvent the {@link RifFileEvent} being processed
   * @return the parser
   */
  private static RifFileParser inpatientClaimParser(RifFileEvent fileEvent) {
    final var parser = new InpatientClaimParser();
    return new RifFileParser.Grouping(
        RifFileType.INPATIENT.getIdColumn().name(),
        csvRecords -> {
          trace(csvRecords);
          final RecordAction recordAction = parseRecordAction(csvRecords);
          final InpatientClaim claim = parse(csvRecords, parser::transformMessage);
          return new RifRecordEvent<>(
              fileEvent, csvRecords, recordAction, claim.getBeneficiaryId(), claim);
        });
  }

  /**
   * Creates a {@link RifFileParser} that creates {@link OutpatientClaim}s.
   *
   * @param fileEvent the {@link RifFileEvent} being processed
   * @return the parser
   */
  private static RifFileParser outpatientClaimParser(RifFileEvent fileEvent) {
    final var parser = new OutpatientClaimParser();
    return new RifFileParser.Grouping(
        RifFileType.OUTPATIENT.getIdColumn().name(),
        csvRecords -> {
          trace(csvRecords);
          final RecordAction recordAction = parseRecordAction(csvRecords);
          final OutpatientClaim claim = parse(csvRecords, parser::transformMessage);
          return new RifRecordEvent<>(
              fileEvent, csvRecords, recordAction, claim.getBeneficiaryId(), claim);
        });
  }

  /**
   * Creates a {@link RifFileParser} that creates {@link CarrierClaim}s.
   *
   * @param fileEvent the {@link RifFileEvent} being processed
   * @return the parser
   */
  private static RifFileParser carrierClaimParser(RifFileEvent fileEvent) {
    final var parser = new CarrierClaimParser();
    return new RifFileParser.Grouping(
        RifFileType.CARRIER.getIdColumn().name(),
        csvRecords -> {
          trace(csvRecords);
          final RecordAction recordAction = parseRecordAction(csvRecords);
          final CarrierClaim claim = parse(csvRecords, parser::transformMessage);
          return new RifRecordEvent<>(
              fileEvent, csvRecords, recordAction, claim.getBeneficiaryId(), claim);
        });
  }

  /**
   * Creates a {@link RifFileParser} that creates {@link SNFClaim}s.
   *
   * @param fileEvent the {@link RifFileEvent} being processed
   * @return the parser
   */
  private static RifFileParser snfClaimParser(RifFileEvent fileEvent) {
    final var parser = new SNFClaimParser();
    return new RifFileParser.Grouping(
        RifFileType.SNF.getIdColumn().name(),
        csvRecords -> {
          trace(csvRecords);
          final RecordAction recordAction = parseRecordAction(csvRecords);
          final SNFClaim claim = parse(csvRecords, parser::transformMessage);
          return new RifRecordEvent<>(
              fileEvent, csvRecords, recordAction, claim.getBeneficiaryId(), claim);
        });
  }

  /**
   * Creates a {@link RifFileParser} that creates {@link HospiceClaim}s.
   *
   * @param fileEvent the {@link RifFileEvent} being processed
   * @return the parser
   */
  private static RifFileParser hospiceClaimParser(RifFileEvent fileEvent) {
    final var parser = new HospiceClaimParser();
    return new RifFileParser.Grouping(
        RifFileType.HOSPICE.getIdColumn().name(),
        csvRecords -> {
          trace(csvRecords);
          final RecordAction recordAction = parseRecordAction(csvRecords);
          final HospiceClaim claim = parse(csvRecords, parser::transformMessage);
          return new RifRecordEvent<>(
              fileEvent, csvRecords, recordAction, claim.getBeneficiaryId(), claim);
        });
  }

  /**
   * Creates a {@link RifFileParser} that creates {@link HHAClaim}s.
   *
   * @param fileEvent the {@link RifFileEvent} being processed
   * @return the parser
   */
  private static RifFileParser hhaClaimParser(RifFileEvent fileEvent) {
    final var parser = new HHAClaimParser();
    return new RifFileParser.Grouping(
        RifFileType.HHA.getIdColumn().name(),
        csvRecords -> {
          trace(csvRecords);
          final RecordAction recordAction = parseRecordAction(csvRecords);
          final HHAClaim claim = parse(csvRecords, parser::transformMessage);
          return new RifRecordEvent<>(
              fileEvent, csvRecords, recordAction, claim.getBeneficiaryId(), claim);
        });
  }

  /**
   * Creates a {@link RifFileParser} that creates {@link DMEClaim}s.
   *
   * @param fileEvent the {@link RifFileEvent} being processed
   * @return the parser
   */
  private static RifFileParser dmeClaimParser(RifFileEvent fileEvent) {
    final var parser = new DMEClaimParser();
    return new RifFileParser.Grouping(
        RifFileType.DME.getIdColumn().name(),
        csvRecords -> {
          trace(csvRecords);
          final RecordAction recordAction = parseRecordAction(csvRecords);
          final DMEClaim claim = parse(csvRecords, parser::transformMessage);
          return new RifRecordEvent<>(
              fileEvent, csvRecords, recordAction, claim.getBeneficiaryId(), claim);
        });
  }

  /**
   * Calls the provided parser lambda function with the given list of {@link CSVRecord}s to produce
   * an object. {@link TransformationException}s are converted into {@link
   * InvalidRifValueException}s.
   *
   * @param csvRecords records to pass to the lambda function
   * @param parser the lambda function that does the parsing
   * @return the object returned by the lambda function
   * @param <T> the type of object returned by the lambda function
   */
  private static <T> T parse(List<CSVRecord> csvRecords, Function<RifObjectWrapper, T> parser) {
    try {
      return parser.apply(new RifObjectWrapper(csvRecords));
    } catch (TransformationException error) {
      String message =
          String.format(
              "Parse error: lineNumber: %d message: %s errors: %s",
              csvRecords.get(0).getRecordNumber(), error.getMessage(), error.getErrors());
      log.warn(
          "Parse error encountered near line number '{}'.", csvRecords.get(0).getRecordNumber());
      throw new InvalidRifValueException(message, error);
    }
  }

  /**
   * Extracts the appropriate {@link RecordAction} from the given record.
   *
   * @param csvRecord the record
   * @return the action
   */
  @Nonnull
  private static RecordAction parseRecordAction(CSVRecord csvRecord) {
    return RecordAction.match(csvRecord.get(RECORD_ACTION_COLUMN));
  }

  /**
   * Extracts the appropriate {@link RecordAction} from the first record.
   *
   * @param csvRecords the records
   * @return the action
   */
  @Nonnull
  private static RecordAction parseRecordAction(List<CSVRecord> csvRecords) {
    return parseRecordAction(csvRecords.get(0));
  }

  /**
   * Logs all of the records if trace logging is enabled.
   *
   * @param csvRecords the records
   */
  private static void trace(List<CSVRecord> csvRecords) {
    if (log.isTraceEnabled()) {
      log.trace(csvRecords.toString());
    }
  }

  /**
   * Logs the record if trace logging is enabled.
   *
   * @param csvRecord the record
   */
  private static void trace(CSVRecord csvRecord) {
    if (log.isTraceEnabled()) {
      log.trace(csvRecord.toString());
    }
  }
}
