package gov.cms.bfd.pipeline.ccw.rif.extract;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.BeneficiaryHistory;
import gov.cms.bfd.model.rif.BeneficiaryHistoryParser;
import gov.cms.bfd.model.rif.BeneficiaryParser;
import gov.cms.bfd.model.rif.CarrierClaim;
import gov.cms.bfd.model.rif.CarrierClaimParser;
import gov.cms.bfd.model.rif.DMEClaim;
import gov.cms.bfd.model.rif.DMEClaimParser;
import gov.cms.bfd.model.rif.HHAClaim;
import gov.cms.bfd.model.rif.HHAClaimParser;
import gov.cms.bfd.model.rif.HospiceClaim;
import gov.cms.bfd.model.rif.HospiceClaimParser;
import gov.cms.bfd.model.rif.InpatientClaim;
import gov.cms.bfd.model.rif.InpatientClaimParser;
import gov.cms.bfd.model.rif.MedicareBeneficiaryIdHistory;
import gov.cms.bfd.model.rif.MedicareBeneficiaryIdHistoryParser;
import gov.cms.bfd.model.rif.OutpatientClaim;
import gov.cms.bfd.model.rif.OutpatientClaimParser;
import gov.cms.bfd.model.rif.PartDEvent;
import gov.cms.bfd.model.rif.PartDEventParser;
import gov.cms.bfd.model.rif.RecordAction;
import gov.cms.bfd.model.rif.RifFile;
import gov.cms.bfd.model.rif.RifFileEvent;
import gov.cms.bfd.model.rif.RifFileRecords;
import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.model.rif.RifFilesEvent;
import gov.cms.bfd.model.rif.RifRecordEvent;
import gov.cms.bfd.model.rif.SNFClaim;
import gov.cms.bfd.model.rif.SNFClaimParser;
import gov.cms.bfd.model.rif.parse.InvalidRifValueException;
import gov.cms.bfd.model.rif.parse.RifParsingUtils;
import gov.cms.bfd.pipeline.ccw.rif.extract.CsvRecordGroupingIterator.ColumnValueCsvRecordGrouper;
import gov.cms.bfd.pipeline.ccw.rif.extract.CsvRecordGroupingIterator.CsvRecordGrouper;
import gov.cms.bfd.pipeline.ccw.rif.extract.exceptions.UnsupportedRifFileTypeException;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import gov.cms.model.dsl.codegen.library.DataTransformer;
import gov.cms.model.dsl.codegen.library.RifObjectWrapper;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Contains services responsible for handling new RIF files. */
public final class RifFilesProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(RifFilesProcessor.class);

  /**
   * Produces a {@link RifFileRecords} with the {@link RifRecordEvent}s produced from the specified
   * {@link RifFileEvent}.
   *
   * @param rifFileEvent the {@link RifFileEvent} that is being processed
   * @return the record from the rif file
   */
  public RifFileRecords produceRecords(RifFileEvent rifFileEvent) {
    RifFile file = rifFileEvent.getFile();

    /*
     * Approach used here to parse CSV as a Java 8 Stream is courtesy of
     * https://rumianom.pl/rumianom/entry/apache-commons-csv-with-java.
     */

    CSVParser csvParser = RifParsingUtils.createCsvParser(file);

    boolean isGrouped;
    BiFunction<RifFileEvent, List<CSVRecord>, RifRecordEvent<?>> recordParser;
    if (file.getFileType() == RifFileType.BENEFICIARY) {
      final var rifParser = new BeneficiaryParser();
      isGrouped = false;
      recordParser =
          (fileEvent, csvRecords) -> buildBeneficiaryEvent(fileEvent, csvRecords, rifParser);
    } else if (file.getFileType() == RifFileType.BENEFICIARY_HISTORY) {
      final var rifParser = new BeneficiaryHistoryParser();
      isGrouped = false;
      recordParser =
          (fileEvent, csvRecords) -> buildBeneficiaryHistoryEvent(fileEvent, csvRecords, rifParser);
    } else if (file.getFileType() == RifFileType.MEDICARE_BENEFICIARY_ID_HISTORY) {
      final var rifParser = new MedicareBeneficiaryIdHistoryParser();
      isGrouped = false;
      recordParser =
          (fileEvent, csvRecords) ->
              buildMedicareBeneficiaryIdHistoryEvent(fileEvent, csvRecords, rifParser);
    } else if (file.getFileType() == RifFileType.PDE) {
      final var rifParser = new PartDEventParser();
      isGrouped = false;
      recordParser = (fileEvent, csvRecords) -> buildPartDEvent(fileEvent, csvRecords, rifParser);
    } else if (file.getFileType() == RifFileType.CARRIER) {
      final var rifParser = new CarrierClaimParser();
      isGrouped = true;
      recordParser =
          (fileEvent, csvRecords) -> buildCarrierClaimEvent(fileEvent, csvRecords, rifParser);
    } else if (file.getFileType() == RifFileType.INPATIENT) {
      final var rifParser = new InpatientClaimParser();
      isGrouped = true;
      recordParser =
          (fileEvent, csvRecords) -> buildInpatientClaimEvent(fileEvent, csvRecords, rifParser);
    } else if (file.getFileType() == RifFileType.OUTPATIENT) {
      final var rifParser = new OutpatientClaimParser();
      isGrouped = true;
      recordParser =
          (fileEvent, csvRecords) -> buildOutpatientClaimEvent(fileEvent, csvRecords, rifParser);
    } else if (file.getFileType() == RifFileType.SNF) {
      final var rifParser = new SNFClaimParser();
      isGrouped = true;
      recordParser =
          (fileEvent, csvRecords) -> buildSNFClaimEvent(fileEvent, csvRecords, rifParser);
    } else if (file.getFileType() == RifFileType.HOSPICE) {
      final var rifParser = new HospiceClaimParser();
      isGrouped = true;
      recordParser =
          (fileEvent, csvRecords) -> buildHospiceClaimEvent(fileEvent, csvRecords, rifParser);
    } else if (file.getFileType() == RifFileType.HHA) {
      final var rifParser = new HHAClaimParser();
      isGrouped = true;
      recordParser =
          (fileEvent, csvRecords) -> buildHHAClaimEvent(fileEvent, csvRecords, rifParser);
    } else if (file.getFileType() == RifFileType.DME) {
      final var rifParser = new DMEClaimParser();
      isGrouped = true;
      recordParser =
          (fileEvent, csvRecords) -> buildDMEClaimEvent(fileEvent, csvRecords, rifParser);
    } else {
      throw new UnsupportedRifFileTypeException("Unsupported file type:" + file.getFileType());
    }

    /*
     * Use the CSVParser to drive a Stream of grouped CSVRecords
     * (specifically, group by claim ID/lines).
     */
    CsvRecordGrouper grouper =
        new ColumnValueCsvRecordGrouper(isGrouped ? file.getFileType().getIdColumn() : null);
    Iterator<List<CSVRecord>> csvIterator = new CsvRecordGroupingIterator(csvParser, grouper);
    Spliterator<List<CSVRecord>> spliterator =
        Spliterators.spliteratorUnknownSize(csvIterator, Spliterator.ORDERED | Spliterator.NONNULL);
    Stream<List<CSVRecord>> csvRecordStream =
        StreamSupport.stream(spliterator, false)
            .onClose(
                () -> {
                  try {
                    /*
                     * This will also close the Reader and InputStream that the
                     * CSVParser was consuming.
                     */
                    csvParser.close();
                  } catch (IOException e) {
                    LOGGER.warn("Unable to close CSVParser", e);
                  }
                });

    /* Map each record group to a single RifRecordEvent. */
    Stream<RifRecordEvent<?>> rifRecordStream =
        csvRecordStream.map(
            csvRecordGroup -> {
              Timer.Context parsingTimer =
                  rifFileEvent
                      .getEventMetrics()
                      .timer(MetricRegistry.name(getClass().getSimpleName(), "recordParsing"))
                      .time();
              RifRecordEvent<?> recordEvent =
                  parseRifRecord(rifFileEvent, csvRecordGroup, recordParser);
              // OK if an exception prevents the close because a failed parse is not a valid sample.
              parsingTimer.close();

              return recordEvent;
            });

    return new RifFileRecords(rifFileEvent, rifRecordStream);
  }

  /**
   * Wrapper to parse a CSV record group and transform it into an entity object. Any {@link
   * DataTransformer.TransformationException} is converted into a {@link InvalidRifValueException}
   * and rethrown.
   *
   * @param fileEvent the {@link RifFileEvent} being processed
   * @param csvRecords the {@link CSVRecord} to be mapped (in a single-element {@link List}), which
   *     must be from a {@link RifFileType#BENEFICIARY} {@link RifFile}
   * @param parser {@link BeneficiaryParser} used to parse the csv records
   * @return a {@link RifRecordEvent} built from the specified {@link CSVRecord}s
   * @throws InvalidRifValueException if any values within the CSV data cannot be parsed
   */
  private RifRecordEvent<?> parseRifRecord(
      RifFileEvent fileEvent,
      List<CSVRecord> csvRecords,
      BiFunction<RifFileEvent, List<CSVRecord>, RifRecordEvent<?>> parser) {
    try {
      return parser.apply(fileEvent, csvRecords);
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

  /**
   * Builds a beneficiary event record.
   *
   * @param fileEvent the {@link RifFileEvent} being processed
   * @param csvRecords the {@link CSVRecord} to be mapped (in a single-element {@link List}), which
   *     must be from a {@link RifFileType#BENEFICIARY} {@link RifFile}
   * @param parser {@link BeneficiaryParser} used to parse the csv records
   * @return a {@link RifRecordEvent} built from the specified {@link CSVRecord}s
   */
  private static RifRecordEvent<Beneficiary> buildBeneficiaryEvent(
      RifFileEvent fileEvent, List<CSVRecord> csvRecords, BeneficiaryParser parser) {
    if (csvRecords.size() != 1) throw new BadCodeMonkeyException();
    CSVRecord csvRecord = csvRecords.get(0);

    if (LOGGER.isTraceEnabled()) LOGGER.trace(csvRecord.toString());

    RecordAction recordAction = RecordAction.match(csvRecord.get("DML_IND"));
    Beneficiary beneficiaryRow = parser.transformMessage(new RifObjectWrapper(csvRecords));

    // Swap the unhashed HICN into the correct field.
    beneficiaryRow.setHicnUnhashed(Optional.ofNullable(beneficiaryRow.getHicn()));
    beneficiaryRow.setHicn(null);

    return new RifRecordEvent<Beneficiary>(
        fileEvent, csvRecords, recordAction, beneficiaryRow.getBeneficiaryId(), beneficiaryRow);
  }

  /**
   * Builds a beneficiary history event record.
   *
   * @param fileEvent the {@link RifFileEvent} being processed
   * @param csvRecords the {@link CSVRecord} to be mapped (in a single-element {@link List}), which
   *     must be from a {@link RifFileType#BENEFICIARY_HISTORY} {@link RifFile}
   * @param parser {@link BeneficiaryHistoryParser} used to parse the csv records
   * @return a {@link RifRecordEvent} built from the specified {@link CSVRecord}s
   */
  private static RifRecordEvent<BeneficiaryHistory> buildBeneficiaryHistoryEvent(
      RifFileEvent fileEvent, List<CSVRecord> csvRecords, BeneficiaryHistoryParser parser) {
    if (csvRecords.size() != 1) throw new BadCodeMonkeyException();
    CSVRecord csvRecord = csvRecords.get(0);

    if (LOGGER.isTraceEnabled()) LOGGER.trace(csvRecord.toString());

    RecordAction recordAction = RecordAction.match(csvRecord.get("DML_IND"));
    BeneficiaryHistory beneficiaryHistoryRow =
        parser.transformMessage(new RifObjectWrapper(csvRecords));
    return new RifRecordEvent<BeneficiaryHistory>(
        fileEvent,
        csvRecords,
        recordAction,
        beneficiaryHistoryRow.getBeneficiaryId(),
        beneficiaryHistoryRow);
  }

  /**
   * Builds a medicare beneficiary id history event record.
   *
   * @param fileEvent the {@link RifFileEvent} being processed
   * @param csvRecords the {@link CSVRecord} to be mapped (in a single-element {@link List}), which
   *     must be from a {@link RifFileType#MEDICARE_BENEFICIARY_ID_HISTORY} {@link RifFile}
   * @param parser {@link MedicareBeneficiaryIdHistoryParser} used to parse the csv records
   * @return a {@link RifRecordEvent} built from the specified {@link CSVRecord}s
   */
  private static RifRecordEvent<MedicareBeneficiaryIdHistory>
      buildMedicareBeneficiaryIdHistoryEvent(
          RifFileEvent fileEvent,
          List<CSVRecord> csvRecords,
          MedicareBeneficiaryIdHistoryParser parser) {
    if (csvRecords.size() != 1) throw new BadCodeMonkeyException();
    CSVRecord csvRecord = csvRecords.get(0);

    if (LOGGER.isTraceEnabled()) LOGGER.trace(csvRecord.toString());

    RecordAction recordAction = RecordAction.INSERT;
    MedicareBeneficiaryIdHistory medicareBeneficiaryIdHistoryRow =
        parser.transformMessage(new RifObjectWrapper(csvRecords));
    return new RifRecordEvent<MedicareBeneficiaryIdHistory>(
        fileEvent,
        csvRecords,
        recordAction,
        medicareBeneficiaryIdHistoryRow.getBeneficiaryId().get(),
        medicareBeneficiaryIdHistoryRow);
  }

  /**
   * Builds a part D event record.
   *
   * @param fileEvent the {@link RifFilesEvent} being processed
   * @param csvRecords the {@link CSVRecord}s to be mapped, which must be from a {@link
   *     RifFileType#PDE} {@link RifFile}
   * @param parser {@link PartDEventParser} used to parse the csv records
   * @return a {@link RifRecordEvent} built from the specified {@link CSVRecord}s
   */
  private static RifRecordEvent<PartDEvent> buildPartDEvent(
      RifFileEvent fileEvent, List<CSVRecord> csvRecords, PartDEventParser parser) {
    if (csvRecords.size() != 1) throw new BadCodeMonkeyException();
    if (LOGGER.isTraceEnabled()) LOGGER.trace(csvRecords.toString());

    CSVRecord csvRecord = csvRecords.get(0);

    RecordAction recordAction = RecordAction.match(csvRecord.get("DML_IND"));
    PartDEvent partDEvent = parser.transformMessage(new RifObjectWrapper(csvRecords));
    return new RifRecordEvent<PartDEvent>(
        fileEvent, csvRecords, recordAction, partDEvent.getBeneficiaryId(), partDEvent);
  }

  /**
   * Builds an inpatient claim event record.
   *
   * @param fileEvent the {@link RifFileEvent} being processed that is being processed
   * @param csvRecords the {@link CSVRecord}s to be mapped, which must be from a {@link
   *     RifFileType#INPATIENT} {@link RifFile}
   * @param parser {@link InpatientClaimParser} used to parse the csv records
   * @return a {@link RifRecordEvent} built from the specified {@link CSVRecord}s
   */
  private static RifRecordEvent<InpatientClaim> buildInpatientClaimEvent(
      RifFileEvent fileEvent, List<CSVRecord> csvRecords, InpatientClaimParser parser) {
    if (LOGGER.isTraceEnabled()) LOGGER.trace(csvRecords.toString());

    CSVRecord firstCsvRecord = csvRecords.get(0);

    RecordAction recordAction = RecordAction.match(firstCsvRecord.get("DML_IND"));
    InpatientClaim claim = parser.transformMessage(new RifObjectWrapper(csvRecords));
    return new RifRecordEvent<InpatientClaim>(
        fileEvent, csvRecords, recordAction, claim.getBeneficiaryId(), claim);
  }

  /**
   * Builds an outpatient claim event record.
   *
   * @param fileEvent the {@link RifFileEvent} being processed that is being processed
   * @param csvRecords the {@link CSVRecord}s to be mapped, which must be from a {@link
   *     RifFileType#OUTPATIENT} {@link RifFile}
   * @param parser {@link OutpatientClaimParser} used to parse the csv records
   * @return a {@link RifRecordEvent} built from the specified {@link CSVRecord}s
   */
  private static RifRecordEvent<OutpatientClaim> buildOutpatientClaimEvent(
      RifFileEvent fileEvent, List<CSVRecord> csvRecords, OutpatientClaimParser parser) {
    if (LOGGER.isTraceEnabled()) LOGGER.trace(csvRecords.toString());

    CSVRecord firstCsvRecord = csvRecords.get(0);

    RecordAction recordAction = RecordAction.match(firstCsvRecord.get("DML_IND"));
    OutpatientClaim claim = parser.transformMessage(new RifObjectWrapper(csvRecords));
    return new RifRecordEvent<OutpatientClaim>(
        fileEvent, csvRecords, recordAction, claim.getBeneficiaryId(), claim);
  }

  /**
   * Builds a carrier claim event record.
   *
   * @param fileEvent the {@link RifFileEvent} being processed
   * @param csvRecords the {@link CSVRecord}s to be mapped, which must be from a {@link
   *     RifFileType#CARRIER} {@link RifFile}
   * @param parser {@link CarrierClaimParser} used to parse the csv records
   * @return a {@link RifRecordEvent} built from the specified {@link CSVRecord}s
   */
  private static RifRecordEvent<CarrierClaim> buildCarrierClaimEvent(
      RifFileEvent fileEvent, List<CSVRecord> csvRecords, CarrierClaimParser parser) {
    if (LOGGER.isTraceEnabled()) LOGGER.trace(csvRecords.toString());

    CSVRecord firstCsvRecord = csvRecords.get(0);

    RecordAction recordAction = RecordAction.match(firstCsvRecord.get("DML_IND"));
    CarrierClaim claim = parser.transformMessage(new RifObjectWrapper(csvRecords));
    return new RifRecordEvent<CarrierClaim>(
        fileEvent, csvRecords, recordAction, claim.getBeneficiaryId(), claim);
  }

  /**
   * Builds an SNF event record.
   *
   * @param fileEvent the {@link RifFileEvent} being processed
   * @param csvRecords the {@link CSVRecord}s to be mapped, which must be from a {@link
   *     RifFileType#SNF} {@link RifFile}
   * @param parser {@link SNFClaimParser} used to parse the csv records
   * @return a {@link RifRecordEvent} built from the specified {@link CSVRecord}s
   */
  private static RifRecordEvent<SNFClaim> buildSNFClaimEvent(
      RifFileEvent fileEvent, List<CSVRecord> csvRecords, SNFClaimParser parser) {
    if (LOGGER.isTraceEnabled()) LOGGER.trace(csvRecords.toString());

    CSVRecord firstCsvRecord = csvRecords.get(0);

    RecordAction recordAction = RecordAction.match(firstCsvRecord.get("DML_IND"));
    SNFClaim claim = parser.transformMessage(new RifObjectWrapper(csvRecords));
    return new RifRecordEvent<SNFClaim>(
        fileEvent, csvRecords, recordAction, claim.getBeneficiaryId(), claim);
  }

  /**
   * Builds a hospice claim event record.
   *
   * @param fileEvent the {@link RifFileEvent} being processed
   * @param csvRecords the {@link CSVRecord}s to be mapped, which must be from a {@link
   *     RifFileType#HOSPICE} {@link RifFile}
   * @param parser {@link HospiceClaimParser} used to parse the csv records
   * @return a {@link RifRecordEvent} built from the specified {@link CSVRecord}s
   */
  private static RifRecordEvent<HospiceClaim> buildHospiceClaimEvent(
      RifFileEvent fileEvent, List<CSVRecord> csvRecords, HospiceClaimParser parser) {
    if (LOGGER.isTraceEnabled()) LOGGER.trace(csvRecords.toString());

    CSVRecord firstCsvRecord = csvRecords.get(0);

    RecordAction recordAction = RecordAction.match(firstCsvRecord.get("DML_IND"));
    HospiceClaim claim = parser.transformMessage(new RifObjectWrapper(csvRecords));
    return new RifRecordEvent<HospiceClaim>(
        fileEvent, csvRecords, recordAction, claim.getBeneficiaryId(), claim);
  }

  /**
   * Builds an HHA event record.
   *
   * @param fileEvent the {@link RifFileEvent} being processed
   * @param csvRecords the {@link CSVRecord}s to be mapped, which must be from a {@link
   *     RifFileType#HHA} {@link RifFile}
   * @param parser {@link HHAClaimParser} used to parse the csv records
   * @return a {@link RifRecordEvent} built from the specified {@link CSVRecord}s
   */
  private static RifRecordEvent<HHAClaim> buildHHAClaimEvent(
      RifFileEvent fileEvent, List<CSVRecord> csvRecords, HHAClaimParser parser) {
    if (LOGGER.isTraceEnabled()) LOGGER.trace(csvRecords.toString());

    CSVRecord firstCsvRecord = csvRecords.get(0);

    RecordAction recordAction = RecordAction.match(firstCsvRecord.get("DML_IND"));
    HHAClaim claim = parser.transformMessage(new RifObjectWrapper(csvRecords));
    return new RifRecordEvent<HHAClaim>(
        fileEvent, csvRecords, recordAction, claim.getBeneficiaryId(), claim);
  }

  /**
   * Builds a DME claim event record.
   *
   * @param fileEvent the {@link RifFileEvent} being processed
   * @param csvRecords the {@link CSVRecord}s to be mapped, which must be from a {@link
   *     RifFileType#DME} {@link RifFile}
   * @param parser {@link DMEClaimParser} used to parse the csv records
   * @return a {@link RifRecordEvent} built from the specified {@link CSVRecord}s
   */
  private static RifRecordEvent<DMEClaim> buildDMEClaimEvent(
      RifFileEvent fileEvent, List<CSVRecord> csvRecords, DMEClaimParser parser) {
    if (LOGGER.isTraceEnabled()) LOGGER.trace(csvRecords.toString());

    CSVRecord firstCsvRecord = csvRecords.get(0);

    RecordAction recordAction = RecordAction.match(firstCsvRecord.get("DML_IND"));
    DMEClaim claim = parser.transformMessage(new RifObjectWrapper(csvRecords));
    return new RifRecordEvent<DMEClaim>(
        fileEvent, csvRecords, recordAction, claim.getBeneficiaryId(), claim);
  }
}
