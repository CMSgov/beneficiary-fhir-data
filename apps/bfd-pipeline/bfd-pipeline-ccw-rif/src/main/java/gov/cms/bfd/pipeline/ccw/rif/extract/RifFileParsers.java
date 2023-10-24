package gov.cms.bfd.pipeline.ccw.rif.extract;

import gov.cms.bfd.model.rif.RecordAction;
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
import gov.cms.bfd.model.rif.parse.RifParsingUtils;
import gov.cms.model.dsl.codegen.library.DataTransformer;
import gov.cms.model.dsl.codegen.library.RifObjectWrapper;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

public class RifFileParsers {
  private static final Logger LOGGER = LoggerFactory.getLogger(RifFileParsers.class);
  public static final String RECORD_ACTION_COLUMN = "DML_IND";

  public static Flux<RifRecordEvent<?>> parseFile(RifFileEvent fileEvent) {
    return Flux.defer(
        () -> {
          RifFileParser rifParser = parserForFile(fileEvent);
          CSVParser csvParser = RifParsingUtils.createCsvParser(fileEvent.getFile());
          return Flux.fromIterable(csvParser)
              .flatMap(rifParser::next)
              .concatWith(Flux.defer(rifParser::finish))
              .doFinally(
                  ignored -> {
                    try {
                      csvParser.close();
                    } catch (IOException ex) {
                      LOGGER.error("unable to close RIF file {}", fileEvent.getFile(), ex);
                    }
                  });
        });
  }

  public static RifFileParser parserForFile(RifFileEvent fileEvent) {
    return switch (fileEvent.getFile().getFileType()) {
      case BENEFICIARY -> beneficiaryEventParser(fileEvent);
      case BENEFICIARY_HISTORY -> beneficiaryHistoryEventParser(fileEvent);
      case CARRIER -> carrierClaimParser(fileEvent);
      case DME -> dmeClaimParser(fileEvent);
      case HHA -> hhaClaimParser(fileEvent);
      case HOSPICE -> hospiceClaimParser(fileEvent);
      case INPATIENT -> inpatientClaimParser(fileEvent);
      case OUTPATIENT -> outpatientClaimParser(fileEvent);
      case PDE -> partDEventParser(fileEvent);
      case SNF -> snfClaimParser(fileEvent);
    };
  }

  public static RifFileParser beneficiaryEventParser(RifFileEvent fileEvent) {
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

  public static RifFileParser beneficiaryHistoryEventParser(RifFileEvent fileEvent) {
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

  public static RifFileParser partDEventParser(RifFileEvent fileEvent) {
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

  public static RifFileParser inpatientClaimParser(RifFileEvent fileEvent) {
    final var parser = new InpatientClaimParser();
    return new RifFileParser.Grouping(
        RifFileType.INPATIENT.getIdColumn(),
        csvRecords -> {
          trace(csvRecords);
          final RecordAction recordAction = parseRecordAction(csvRecords);
          final InpatientClaim claim = parse(csvRecords, parser::transformMessage);
          return new RifRecordEvent<>(
              fileEvent, csvRecords, recordAction, claim.getBeneficiaryId(), claim);
        });
  }

  public static RifFileParser outpatientClaimParser(RifFileEvent fileEvent) {
    final var parser = new OutpatientClaimParser();
    return new RifFileParser.Grouping(
        RifFileType.OUTPATIENT.getIdColumn(),
        csvRecords -> {
          trace(csvRecords);
          final RecordAction recordAction = parseRecordAction(csvRecords);
          final OutpatientClaim claim = parse(csvRecords, parser::transformMessage);
          return new RifRecordEvent<>(
              fileEvent, csvRecords, recordAction, claim.getBeneficiaryId(), claim);
        });
  }

  public static RifFileParser carrierClaimParser(RifFileEvent fileEvent) {
    final var parser = new CarrierClaimParser();
    return new RifFileParser.Grouping(
        RifFileType.CARRIER.getIdColumn(),
        csvRecords -> {
          trace(csvRecords);
          final RecordAction recordAction = parseRecordAction(csvRecords);
          final CarrierClaim claim = parse(csvRecords, parser::transformMessage);
          return new RifRecordEvent<>(
              fileEvent, csvRecords, recordAction, claim.getBeneficiaryId(), claim);
        });
  }

  public static RifFileParser snfClaimParser(RifFileEvent fileEvent) {
    final var parser = new SNFClaimParser();
    return new RifFileParser.Grouping(
        RifFileType.SNF.getIdColumn(),
        csvRecords -> {
          trace(csvRecords);
          final RecordAction recordAction = parseRecordAction(csvRecords);
          final SNFClaim claim = parse(csvRecords, parser::transformMessage);
          return new RifRecordEvent<>(
              fileEvent, csvRecords, recordAction, claim.getBeneficiaryId(), claim);
        });
  }

  public static RifFileParser hospiceClaimParser(RifFileEvent fileEvent) {
    final var parser = new HospiceClaimParser();
    return new RifFileParser.Grouping(
        RifFileType.HOSPICE.getIdColumn(),
        csvRecords -> {
          trace(csvRecords);
          final RecordAction recordAction = parseRecordAction(csvRecords);
          final HospiceClaim claim = parse(csvRecords, parser::transformMessage);
          return new RifRecordEvent<>(
              fileEvent, csvRecords, recordAction, claim.getBeneficiaryId(), claim);
        });
  }

  public static RifFileParser hhaClaimParser(RifFileEvent fileEvent) {
    final var parser = new HHAClaimParser();
    return new RifFileParser.Grouping(
        RifFileType.HHA.getIdColumn(),
        csvRecords -> {
          trace(csvRecords);
          final RecordAction recordAction = parseRecordAction(csvRecords);
          final HHAClaim claim = parse(csvRecords, parser::transformMessage);
          return new RifRecordEvent<>(
              fileEvent, csvRecords, recordAction, claim.getBeneficiaryId(), claim);
        });
  }

  public static RifFileParser dmeClaimParser(RifFileEvent fileEvent) {
    final var parser = new DMEClaimParser();
    return new RifFileParser.Grouping(
        RifFileType.DME.getIdColumn(),
        csvRecords -> {
          trace(csvRecords);
          final RecordAction recordAction = parseRecordAction(csvRecords);
          final DMEClaim claim = parse(csvRecords, parser::transformMessage);
          return new RifRecordEvent<>(
              fileEvent, csvRecords, recordAction, claim.getBeneficiaryId(), claim);
        });
  }

  private static <T> T parse(List<CSVRecord> csvRecords, Function<RifObjectWrapper, T> parser) {
    try {
      return parser.apply(new RifObjectWrapper(csvRecords));
    } catch (DataTransformer.TransformationException error) {
      String message =
          String.format(
              "Parse error: lineNumber: %d message: %s errors: %s",
              csvRecords.get(0).getRecordNumber(), error.getMessage(), error.getErrors());
      LOGGER.warn(
          "Parse error encountered near line number '{}'.", csvRecords.get(0).getRecordNumber());
      throw new InvalidRifValueException(message, error);
    }
  }

  @Nonnull
  private static RecordAction parseRecordAction(CSVRecord csvRecord) {
    return RecordAction.match(csvRecord.get(RECORD_ACTION_COLUMN));
  }

  @Nonnull
  private static RecordAction parseRecordAction(List<CSVRecord> csvRecords) {
    return parseRecordAction(csvRecords.get(0));
  }

  private static void trace(List<CSVRecord> csvRecords) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace(csvRecords.toString());
    }
  }

  private static void trace(CSVRecord csvRecord) {
    if (LOGGER.isTraceEnabled()) LOGGER.trace(csvRecord.toString());
  }
}
