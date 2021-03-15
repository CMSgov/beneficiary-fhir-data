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
   * @param rifFileEvent the {@link RifFileEvent} that is being processed
   * @return a {@link RifFileRecords} with the {@link RifRecordEvent}s produced from the specified
   *     {@link RifFileEvent}
   */
  public RifFileRecords produceRecords(RifFileEvent rifFileEvent) {
    RifFile file = rifFileEvent.getFile();

    /*
     * Approach used here to parse CSV as a Java 8 Stream is courtesy of
     * https://rumianom.pl/rumianom/entry/apache-commons-csv-with-java.
     */

    CSVParser parser = RifParsingUtils.createCsvParser(file);

    boolean isGrouped;
    BiFunction<RifFileEvent, List<CSVRecord>, RifRecordEvent<?>> recordParser;
    if (file.getFileType() == RifFileType.BENEFICIARY) {
      isGrouped = false;
      recordParser = RifFilesProcessor::buildBeneficiaryEvent;
    } else if (file.getFileType() == RifFileType.BENEFICIARY_HISTORY) {
      isGrouped = false;
      recordParser = RifFilesProcessor::buildBeneficiaryHistoryEvent;
    } else if (file.getFileType() == RifFileType.MEDICARE_BENEFICIARY_ID_HISTORY) {
      isGrouped = false;
      recordParser = RifFilesProcessor::buildMedicareBeneficiaryIdHistoryEvent;
    } else if (file.getFileType() == RifFileType.PDE) {
      isGrouped = false;
      recordParser = RifFilesProcessor::buildPartDEvent;
    } else if (file.getFileType() == RifFileType.CARRIER) {
      isGrouped = true;
      recordParser = RifFilesProcessor::buildCarrierClaimEvent;
    } else if (file.getFileType() == RifFileType.INPATIENT) {
      isGrouped = true;
      recordParser = RifFilesProcessor::buildInpatientClaimEvent;
    } else if (file.getFileType() == RifFileType.OUTPATIENT) {
      isGrouped = true;
      recordParser = RifFilesProcessor::buildOutpatientClaimEvent;
    } else if (file.getFileType() == RifFileType.SNF) {
      isGrouped = true;
      recordParser = RifFilesProcessor::buildSNFClaimEvent;
    } else if (file.getFileType() == RifFileType.HOSPICE) {
      isGrouped = true;
      recordParser = RifFilesProcessor::buildHospiceClaimEvent;
    } else if (file.getFileType() == RifFileType.HHA) {
      isGrouped = true;
      recordParser = RifFilesProcessor::buildHHAClaimEvent;
    } else if (file.getFileType() == RifFileType.DME) {
      isGrouped = true;
      recordParser = RifFilesProcessor::buildDMEClaimEvent;
    } else {
      throw new UnsupportedRifFileTypeException("Unsupported file type:" + file.getFileType());
    }

    /*
     * Use the CSVParser to drive a Stream of grouped CSVRecords
     * (specifically, group by claim ID/lines).
     */
    CsvRecordGrouper grouper =
        new ColumnValueCsvRecordGrouper(isGrouped ? file.getFileType().getIdColumn() : null);
    Iterator<List<CSVRecord>> csvIterator = new CsvRecordGroupingIterator(parser, grouper);
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
                    parser.close();
                  } catch (IOException e) {
                    LOGGER.warn("Unable to close CSVParser", e);
                  }
                });

    /* Map each record group to a single RifRecordEvent. */
    Stream<RifRecordEvent<?>> rifRecordStream =
        csvRecordStream.map(
            csvRecordGroup -> {
              try {
                Timer.Context parsingTimer =
                    rifFileEvent
                        .getEventMetrics()
                        .timer(MetricRegistry.name(getClass().getSimpleName(), "recordParsing"))
                        .time();
                RifRecordEvent<?> recordEvent = recordParser.apply(rifFileEvent, csvRecordGroup);
                parsingTimer.close();

                return recordEvent;
              } catch (InvalidRifValueException e) {
                LOGGER.warn(
                    "Parse error encountered near line number '{}'.",
                    csvRecordGroup.get(0).getRecordNumber());
                throw new InvalidRifValueException(e);
              }
            });

    return new RifFileRecords(rifFileEvent, rifRecordStream);
  }

  /**
   * @param fileEvent the {@link RifFileEvent} being processed
   * @param csvRecords the {@link CSVRecord} to be mapped (in a single-element {@link List}), which
   *     must be from a {@link RifFileType#BENEFICIARY} {@link RifFile}
   * @return a {@link RifRecordEvent} built from the specified {@link CSVRecord}s
   */
  private static RifRecordEvent<Beneficiary> buildBeneficiaryEvent(
      RifFileEvent fileEvent, List<CSVRecord> csvRecords) {
    if (csvRecords.size() != 1) throw new BadCodeMonkeyException();
    CSVRecord csvRecord = csvRecords.get(0);

    if (LOGGER.isTraceEnabled()) LOGGER.trace(csvRecord.toString());

    RecordAction recordAction = RecordAction.match(csvRecord.get("DML_IND"));
    Beneficiary beneficiaryRow = BeneficiaryParser.parseRif(csvRecords);

    // Swap the unhashed HICN into the correct field.
    beneficiaryRow.setHicnUnhashed(Optional.ofNullable(beneficiaryRow.getHicn()));
    beneficiaryRow.setHicn(null);

    return new RifRecordEvent<Beneficiary>(
        fileEvent, recordAction, beneficiaryRow.getBeneficiaryId(), beneficiaryRow);
  }

  /**
   * @param fileEvent the {@link RifFileEvent} being processed
   * @param csvRecords the {@link CSVRecord} to be mapped (in a single-element {@link List}), which
   *     must be from a {@link RifFileType#BENEFICIARY_HISTORY} {@link RifFile}
   * @return a {@link RifRecordEvent} built from the specified {@link CSVRecord}s
   */
  private static RifRecordEvent<BeneficiaryHistory> buildBeneficiaryHistoryEvent(
      RifFileEvent fileEvent, List<CSVRecord> csvRecords) {
    if (csvRecords.size() != 1) throw new BadCodeMonkeyException();
    CSVRecord csvRecord = csvRecords.get(0);

    if (LOGGER.isTraceEnabled()) LOGGER.trace(csvRecord.toString());

    RecordAction recordAction = RecordAction.match(csvRecord.get("DML_IND"));
    BeneficiaryHistory beneficiaryHistoryRow = BeneficiaryHistoryParser.parseRif(csvRecords);
    return new RifRecordEvent<BeneficiaryHistory>(
        fileEvent, recordAction, beneficiaryHistoryRow.getBeneficiaryId(), beneficiaryHistoryRow);
  }

  /**
   * @param fileEvent the {@link RifFileEvent} being processed
   * @param csvRecords the {@link CSVRecord} to be mapped (in a single-element {@link List}), which
   *     must be from a {@link RifFileType#Medicare_Beneficiary_Id_History} {@link RifFile}
   * @return a {@link RifRecordEvent} built from the specified {@link CSVRecord}s
   */
  private static RifRecordEvent<MedicareBeneficiaryIdHistory>
      buildMedicareBeneficiaryIdHistoryEvent(RifFileEvent fileEvent, List<CSVRecord> csvRecords) {
    if (csvRecords.size() != 1) throw new BadCodeMonkeyException();
    CSVRecord csvRecord = csvRecords.get(0);

    if (LOGGER.isTraceEnabled()) LOGGER.trace(csvRecord.toString());

    RecordAction recordAction = RecordAction.INSERT;
    MedicareBeneficiaryIdHistory medicareBeneficiaryIdHistoryRow =
        MedicareBeneficiaryIdHistoryParser.parseRif(csvRecords);
    return new RifRecordEvent<MedicareBeneficiaryIdHistory>(
        fileEvent,
        recordAction,
        medicareBeneficiaryIdHistoryRow.getBeneficiaryId().get(),
        medicareBeneficiaryIdHistoryRow);
  }

  /**
   * @param fileEvent the {@link RifFilesEvent} being processed
   * @param csvRecords the {@link CSVRecord}s to be mapped, which must be from a {@link
   *     RifFileType#PDE} {@link RifFile}
   * @return a {@link RifRecordEvent} built from the specified {@link CSVRecord}s
   */
  private static RifRecordEvent<PartDEvent> buildPartDEvent(
      RifFileEvent fileEvent, List<CSVRecord> csvRecords) {
    if (csvRecords.size() != 1) throw new BadCodeMonkeyException();
    if (LOGGER.isTraceEnabled()) LOGGER.trace(csvRecords.toString());

    CSVRecord csvRecord = csvRecords.get(0);

    RecordAction recordAction = RecordAction.match(csvRecord.get("DML_IND"));
    PartDEvent partDEvent = PartDEventParser.parseRif(csvRecords);
    return new RifRecordEvent<PartDEvent>(
        fileEvent, recordAction, partDEvent.getBeneficiaryId(), partDEvent);
  }

  /**
   * @param fileEvent the {@link RifFileEvent} being processed that is being processed
   * @param csvRecords the {@link CSVRecord}s to be mapped, which must be from a {@link
   *     RifFileType#INPATIENT} {@link RifFile}
   * @return a {@link RifRecordEvent} built from the specified {@link CSVRecord}s
   */
  private static RifRecordEvent<InpatientClaim> buildInpatientClaimEvent(
      RifFileEvent fileEvent, List<CSVRecord> csvRecords) {
    if (LOGGER.isTraceEnabled()) LOGGER.trace(csvRecords.toString());

    CSVRecord firstCsvRecord = csvRecords.get(0);

    RecordAction recordAction = RecordAction.match(firstCsvRecord.get("DML_IND"));
    InpatientClaim claim = InpatientClaimParser.parseRif(csvRecords);
    return new RifRecordEvent<InpatientClaim>(
        fileEvent, recordAction, claim.getBeneficiaryId(), claim);
  }

  /**
   * @param fileEvent the {@link RifFileEvent} being processed that is being processed
   * @param csvRecords the {@link CSVRecord}s to be mapped, which must be from a {@link
   *     RifFileType#OUTPATIENT} {@link RifFile}
   * @return a {@link RifRecordEvent} built from the specified {@link CSVRecord}s
   */
  private static RifRecordEvent<OutpatientClaim> buildOutpatientClaimEvent(
      RifFileEvent fileEvent, List<CSVRecord> csvRecords) {
    if (LOGGER.isTraceEnabled()) LOGGER.trace(csvRecords.toString());

    CSVRecord firstCsvRecord = csvRecords.get(0);

    RecordAction recordAction = RecordAction.match(firstCsvRecord.get("DML_IND"));
    OutpatientClaim claim = OutpatientClaimParser.parseRif(csvRecords);
    return new RifRecordEvent<OutpatientClaim>(
        fileEvent, recordAction, claim.getBeneficiaryId(), claim);
  }

  /**
   * @param fileEvent the {@link RifFileEvent} being processed
   * @param csvRecords the {@link CSVRecord}s to be mapped, which must be from a {@link
   *     RifFileType#CARRIER} {@link RifFile}
   * @return a {@link RifRecordEvent} built from the specified {@link CSVRecord}s
   */
  private static RifRecordEvent<CarrierClaim> buildCarrierClaimEvent(
      RifFileEvent fileEvent, List<CSVRecord> csvRecords) {
    if (LOGGER.isTraceEnabled()) LOGGER.trace(csvRecords.toString());

    CSVRecord firstCsvRecord = csvRecords.get(0);

    RecordAction recordAction = RecordAction.match(firstCsvRecord.get("DML_IND"));
    CarrierClaim claim = CarrierClaimParser.parseRif(csvRecords);
    return new RifRecordEvent<CarrierClaim>(
        fileEvent, recordAction, claim.getBeneficiaryId(), claim);
  }

  /**
   * @param fileEvent the {@link RifFileEvent} being processed
   * @param csvRecords the {@link CSVRecord}s to be mapped, which must be from a {@link
   *     RifFileType#SNF} {@link RifFile}
   * @return a {@link RifRecordEvent} built from the specified {@link CSVRecord}s
   */
  private static RifRecordEvent<SNFClaim> buildSNFClaimEvent(
      RifFileEvent fileEvent, List<CSVRecord> csvRecords) {
    if (LOGGER.isTraceEnabled()) LOGGER.trace(csvRecords.toString());

    CSVRecord firstCsvRecord = csvRecords.get(0);

    RecordAction recordAction = RecordAction.match(firstCsvRecord.get("DML_IND"));
    SNFClaim claim = SNFClaimParser.parseRif(csvRecords);
    return new RifRecordEvent<SNFClaim>(fileEvent, recordAction, claim.getBeneficiaryId(), claim);
  }

  /**
   * @param fileEvent the {@link RifFileEvent} being processed
   * @param csvRecords the {@link CSVRecord}s to be mapped, which must be from a {@link
   *     RifFileType#HOSPICE} {@link RifFile}
   * @return a {@link RifRecordEvent} built from the specified {@link CSVRecord}s
   */
  private static RifRecordEvent<HospiceClaim> buildHospiceClaimEvent(
      RifFileEvent fileEvent, List<CSVRecord> csvRecords) {
    if (LOGGER.isTraceEnabled()) LOGGER.trace(csvRecords.toString());

    CSVRecord firstCsvRecord = csvRecords.get(0);

    RecordAction recordAction = RecordAction.match(firstCsvRecord.get("DML_IND"));
    HospiceClaim claim = HospiceClaimParser.parseRif(csvRecords);
    return new RifRecordEvent<HospiceClaim>(
        fileEvent, recordAction, claim.getBeneficiaryId(), claim);
  }

  /**
   * @param fileEvent the {@link RifFileEvent} being processed
   * @param csvRecords the {@link CSVRecord}s to be mapped, which must be from a {@link
   *     RifFileType#HHA} {@link RifFile}
   * @return a {@link RifRecordEvent} built from the specified {@link CSVRecord}s
   */
  private static RifRecordEvent<HHAClaim> buildHHAClaimEvent(
      RifFileEvent fileEvent, List<CSVRecord> csvRecords) {
    if (LOGGER.isTraceEnabled()) LOGGER.trace(csvRecords.toString());

    CSVRecord firstCsvRecord = csvRecords.get(0);

    RecordAction recordAction = RecordAction.match(firstCsvRecord.get("DML_IND"));
    HHAClaim claim = HHAClaimParser.parseRif(csvRecords);
    return new RifRecordEvent<HHAClaim>(fileEvent, recordAction, claim.getBeneficiaryId(), claim);
  }

  /**
   * @param fileEvent the {@link RifFileEvent} being processed
   * @param csvRecords the {@link CSVRecord}s to be mapped, which must be from a {@link
   *     RifFileType#DME} {@link RifFile}
   * @return a {@link RifRecordEvent} built from the specified {@link CSVRecord}s
   */
  private static RifRecordEvent<DMEClaim> buildDMEClaimEvent(
      RifFileEvent fileEvent, List<CSVRecord> csvRecords) {
    if (LOGGER.isTraceEnabled()) LOGGER.trace(csvRecords.toString());

    CSVRecord firstCsvRecord = csvRecords.get(0);

    RecordAction recordAction = RecordAction.match(firstCsvRecord.get("DML_IND"));
    DMEClaim claim = DMEClaimParser.parseRif(csvRecords);
    return new RifRecordEvent<DMEClaim>(fileEvent, recordAction, claim.getBeneficiaryId(), claim);
  }
}
