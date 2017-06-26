
package gov.hhs.cms.bluebutton.datapipeline.rif.extract;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;
import gov.hhs.cms.bluebutton.data.model.rif.BeneficiaryParser;
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaim;
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaimParser;
import gov.hhs.cms.bluebutton.data.model.rif.DMEClaim;
import gov.hhs.cms.bluebutton.data.model.rif.DMEClaimParser;
import gov.hhs.cms.bluebutton.data.model.rif.HHAClaim;
import gov.hhs.cms.bluebutton.data.model.rif.HHAClaimParser;
import gov.hhs.cms.bluebutton.data.model.rif.HospiceClaim;
import gov.hhs.cms.bluebutton.data.model.rif.HospiceClaimParser;
import gov.hhs.cms.bluebutton.data.model.rif.InpatientClaim;
import gov.hhs.cms.bluebutton.data.model.rif.InpatientClaimParser;
import gov.hhs.cms.bluebutton.data.model.rif.OutpatientClaim;
import gov.hhs.cms.bluebutton.data.model.rif.OutpatientClaimParser;
import gov.hhs.cms.bluebutton.data.model.rif.PartDEvent;
import gov.hhs.cms.bluebutton.data.model.rif.PartDEventParser;
import gov.hhs.cms.bluebutton.data.model.rif.RecordAction;
import gov.hhs.cms.bluebutton.data.model.rif.RifFile;
import gov.hhs.cms.bluebutton.data.model.rif.RifFileRecords;
import gov.hhs.cms.bluebutton.data.model.rif.RifFileType;
import gov.hhs.cms.bluebutton.data.model.rif.RifFilesEvent;
import gov.hhs.cms.bluebutton.data.model.rif.RifRecordEvent;
import gov.hhs.cms.bluebutton.data.model.rif.SNFClaim;
import gov.hhs.cms.bluebutton.data.model.rif.SNFClaimParser;
import gov.hhs.cms.bluebutton.data.model.rif.parse.InvalidRifValueException;
import gov.hhs.cms.bluebutton.data.model.rif.parse.RifParsingUtils;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.CsvRecordGroupingIterator.ColumnValueCsvRecordGrouper;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.CsvRecordGroupingIterator.CsvRecordGrouper;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.exceptions.UnsupportedRifFileTypeException;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.exceptions.UnsupportedRifVersionException;

/**
 * Contains services responsible for handling new RIF files.
 */
public final class RifFilesProcessor {
	/**
	 * The RIF schema version that is currently supported.
	 */
	public static final int RECORD_FORMAT_VERSION = 5;

	private static final Logger LOGGER = LoggerFactory.getLogger(RifFilesProcessor.class);

	/**
	 * <p>
	 * Processes the specified {@link RifFilesEvent}, reading in the data found
	 * in it and converting that data into {@link RifRecordEvent}s that can be
	 * handled downstream in the ETL pipeline.
	 * </p>
	 * <h3>Design Notes</h3>
	 * <p>
	 * This method must accept {@link RifFilesEvent} instances, rather than
	 * individual {@link RifFile} instances. This is due to processing order
	 * constraints that would otherwise be impossible to implement:
	 * </p>
	 * <ol>
	 * <li>{@link RifFileType#BENEFICIARY} files must always be processed first
	 * (if more than one {@link RifFile} is available). This is necessary in
	 * order to ensure that FHIR <code>Patient</code> resources are always
	 * created before (and can be referenced by)
	 * <code>ExplanationOfBenefit</code> resources.</li>
	 * <li>While still honoring the previous rule, {@link RifFile}s generated
	 * earlier must be processed before those with later values. This is
	 * necessary in order to ensure that, if a backlog of {@link RifFile}s
	 * occurs, FHIR updates are not pushed out of order, which would result in
	 * newer data being overwritten by older data.</li>
	 * </ol>
	 * <p>
	 * Please note that, assuming the extraction process that produces
	 * {@link RifFile}s is functioning correctly (i.e. it's producing and
	 * pushing files to S3 in the correct order), it is always safe to process a
	 * single {@link RifFile}, if it's the only one found/available. There is no
	 * need to wait for a "full" complement of {@link RifFilesEvent}s to be
	 * present. The above constraints only impact this class' behavior when
	 * multiple RIF files are found/available at the same time.
	 * </p>
	 * 
	 * @param event
	 *            the {@link RifFilesEvent} to be processed
	 * @return a {@link List} of {@link RifFileRecords}, one per {@link RifFile}
	 */
	public List<RifFileRecords> process(RifFilesEvent event) {
		List<RifFile> filesOrderedSafely = new ArrayList<>(event.getFiles());
		Comparator<RifFile> someComparator = new Comparator<RifFile>() {
			@Override
			public int compare(RifFile o1, RifFile o2) {
				if (o1.getFileType() == RifFileType.BENEFICIARY && o2.getFileType() != RifFileType.BENEFICIARY)
					return -1;
				else if (o1.getFileType() != RifFileType.BENEFICIARY && o2.getFileType() == RifFileType.BENEFICIARY)
					return 1;
				else
					return Integer.compare(o1.getFileType().ordinal(), o2.getFileType().ordinal());
			}
		};
		Collections.sort(filesOrderedSafely, someComparator);

		List<RifFileRecords> recordsBundles = filesOrderedSafely.stream().map(file -> produceRecords(event, file))
				.collect(Collectors.toList());
		return recordsBundles;
	}

	/**
	 * @param rifFilesEvent
	 *            the {@link RifFilesEvent} that is being processed
	 * @param file
	 *            the {@link RifFile} to produce {@link RifRecordEvent}s from
	 * @return a {@link RifFileRecords} with the {@link RifRecordEvent}s
	 *         produced from the specified {@link RifFile}
	 */
	private RifFileRecords produceRecords(RifFilesEvent rifFilesEvent, RifFile file) {
		/*
		 * Approach used here to parse CSV as a Java 8 Stream is courtesy of
		 * https://rumianom.pl/rumianom/entry/apache-commons-csv-with-java.
		 */

		LOGGER.info("Processing RIF file: " + file);
		CSVParser parser = RifParsingUtils.createCsvParser(file);

		boolean isGrouped;
		TriFunction<RifFilesEvent, RifFile, List<CSVRecord>, RifRecordEvent<?>> recordParser;
		if (file.getFileType() == RifFileType.BENEFICIARY) {
			isGrouped = false;
			recordParser = RifFilesProcessor::buildBeneficiaryEvent;
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
		CsvRecordGrouper grouper = new ColumnValueCsvRecordGrouper(isGrouped ? file.getFileType().getIdColumn() : null);
		Iterator<List<CSVRecord>> csvIterator = new CsvRecordGroupingIterator(parser, grouper);
		Spliterator<List<CSVRecord>> spliterator = Spliterators.spliteratorUnknownSize(csvIterator,
				Spliterator.ORDERED | Spliterator.NONNULL);
		Stream<List<CSVRecord>> csvRecordStream = StreamSupport.stream(spliterator, false).onClose(() -> {
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
		Stream<RifRecordEvent<?>> rifRecordStream = csvRecordStream.map(csvRecordGroup -> {
			try {
				RifRecordEvent<?> recordEvent = recordParser.apply(rifFilesEvent, file, csvRecordGroup);
				return recordEvent;
			} catch (InvalidRifValueException e) {
				LOGGER.warn("Parse error encountered near line number '{}'.", csvRecordGroup.get(0).getRecordNumber());
				throw new InvalidRifValueException(e);
			}
		});

		return new RifFileRecords(rifFilesEvent, file, rifRecordStream);
	}

	/**
	 * @param filesEvent
	 *            the {@link RifFilesEvent} being processed
	 * @param file
	 *            the specific {@link RifFile} in that {@link RifFilesEvent}
	 *            that is being processed
	 * @param csvRecords
	 *            the {@link CSVRecord} to be mapped (in a single-element
	 *            {@link List}), which must be from a
	 *            {@link RifFileType#BENEFICIARY} {@link RifFile}
	 * @return a {@link RifRecordEvent} built from the specified
	 *         {@link CSVRecord}s
	 */
	private static RifRecordEvent<Beneficiary> buildBeneficiaryEvent(RifFilesEvent filesEvent, RifFile file,
			List<CSVRecord> csvRecords) {
		if (csvRecords.size() != 1)
			throw new BadCodeMonkeyException();
		CSVRecord csvRecord = csvRecords.get(0);

		if (LOGGER.isTraceEnabled())
			LOGGER.trace(csvRecord.toString());

		int schemaVersion = RifParsingUtils.parseInteger(csvRecord.get("VERSION"));
		if (RECORD_FORMAT_VERSION != schemaVersion)
			throw new UnsupportedRifVersionException(schemaVersion);
		RecordAction recordAction = RecordAction.match(csvRecord.get("DML_IND"));

		Beneficiary beneficiaryRow = BeneficiaryParser.parseRif(csvRecords);
		return new RifRecordEvent<Beneficiary>(filesEvent, file, recordAction, beneficiaryRow);
	}

	/**
	 * @param filesEvent
	 *            the {@link RifFilesEvent} being processed
	 * @param file
	 *            the specific {@link RifFile} in that {@link RifFilesEvent}
	 *            that is being processed
	 * @param csvRecords
	 *            the {@link CSVRecord}s to be mapped, which must be from a
	 *            {@link RifFileType#PDE} {@link RifFile}
	 * @return a {@link RifRecordEvent} built from the specified
	 *         {@link CSVRecord}s
	 */
	private static RifRecordEvent<PartDEvent> buildPartDEvent(RifFilesEvent filesEvent, RifFile file,
			List<CSVRecord> csvRecords) {
		if (csvRecords.size() != 1)
			throw new BadCodeMonkeyException();
		if (LOGGER.isTraceEnabled())
			LOGGER.trace(csvRecords.toString());

		CSVRecord csvRecord = csvRecords.get(0);

		int schemaVersion = RifParsingUtils.parseInteger(csvRecord.get("VERSION"));
		if (RECORD_FORMAT_VERSION != schemaVersion)
			throw new UnsupportedRifVersionException(schemaVersion);
		RecordAction recordAction = RecordAction.match(csvRecord.get("DML_IND"));

		PartDEvent partDEvent = PartDEventParser.parseRif(csvRecords);
		return new RifRecordEvent<PartDEvent>(filesEvent, file, recordAction, partDEvent);
	}

	/**
	 * @param filesEvent
	 *            the {@link RifFilesEvent} being processed
	 * @param file
	 *            the specific {@link RifFile} in that {@link RifFilesEvent}
	 *            that is being processed
	 * @param csvRecords
	 *            the {@link CSVRecord}s to be mapped, which must be from a
	 *            {@link RifFileType#INPATIENT} {@link RifFile}
	 * @return a {@link RifRecordEvent} built from the specified
	 *         {@link CSVRecord}s
	 */
	private static RifRecordEvent<InpatientClaim> buildInpatientClaimEvent(RifFilesEvent filesEvent, RifFile file,
			List<CSVRecord> csvRecords) {
		if (LOGGER.isTraceEnabled())
			LOGGER.trace(csvRecords.toString());

		CSVRecord firstCsvRecord = csvRecords.get(0);

		int schemaVersion = RifParsingUtils.parseInteger(firstCsvRecord.get("VERSION"));
		if (RECORD_FORMAT_VERSION != schemaVersion)
			throw new UnsupportedRifVersionException(schemaVersion);
		RecordAction recordAction = RecordAction.match(firstCsvRecord.get("DML_IND"));

		InpatientClaim claim = InpatientClaimParser.parseRif(csvRecords);
		/*
		 * FIXME Can't use real line numbers because random/dummy test data has
		 * dupes.
		 */
		for (int fakeLineNumber = 1; fakeLineNumber <= claim.getLines().size(); fakeLineNumber++)
			claim.getLines().get(fakeLineNumber - 1).setLineNumber(new BigDecimal(fakeLineNumber));
		return new RifRecordEvent<InpatientClaim>(filesEvent, file, recordAction, claim);
	}

	/**
	 * @param filesEvent
	 *            the {@link RifFilesEvent} being processed
	 * @param file
	 *            the specific {@link RifFile} in that {@link RifFilesEvent}
	 *            that is being processed
	 * @param csvRecords
	 *            the {@link CSVRecord}s to be mapped, which must be from a
	 *            {@link RifFileType#OUTPATIENT} {@link RifFile}
	 * @return a {@link RifRecordEvent} built from the specified
	 *         {@link CSVRecord}s
	 */
	private static RifRecordEvent<OutpatientClaim> buildOutpatientClaimEvent(RifFilesEvent filesEvent, RifFile file,
			List<CSVRecord> csvRecords) {
		if (LOGGER.isTraceEnabled())
			LOGGER.trace(csvRecords.toString());

		CSVRecord firstCsvRecord = csvRecords.get(0);

		int schemaVersion = RifParsingUtils.parseInteger(firstCsvRecord.get("VERSION"));
		if (RECORD_FORMAT_VERSION != schemaVersion)
			throw new UnsupportedRifVersionException(schemaVersion);
		RecordAction recordAction = RecordAction.match(firstCsvRecord.get("DML_IND"));

		OutpatientClaim claim = OutpatientClaimParser.parseRif(csvRecords);
		/*
		 * FIXME Can't use real line numbers because random/dummy test data has
		 * dupes.
		 */
		for (int fakeLineNumber = 1; fakeLineNumber <= claim.getLines().size(); fakeLineNumber++)
			claim.getLines().get(fakeLineNumber - 1).setLineNumber(new BigDecimal(fakeLineNumber));
		return new RifRecordEvent<OutpatientClaim>(filesEvent, file, recordAction, claim);
	}

	/**
	 * @param filesEvent
	 *            the {@link RifFilesEvent} being processed
	 * @param file
	 *            the specific {@link RifFile} in that {@link RifFilesEvent}
	 *            that is being processed
	 * @param csvRecords
	 *            the {@link CSVRecord}s to be mapped, which must be from a
	 *            {@link RifFileType#CARRIER} {@link RifFile}
	 * @return a {@link RifRecordEvent} built from the specified
	 *         {@link CSVRecord}s
	 */
	private static RifRecordEvent<CarrierClaim> buildCarrierClaimEvent(RifFilesEvent filesEvent, RifFile file,
			List<CSVRecord> csvRecords) {
		if (LOGGER.isTraceEnabled())
			LOGGER.trace(csvRecords.toString());

		CSVRecord firstCsvRecord = csvRecords.get(0);

		int schemaVersion = RifParsingUtils.parseInteger(firstCsvRecord.get("VERSION"));
		if (RECORD_FORMAT_VERSION != schemaVersion)
			throw new UnsupportedRifVersionException(schemaVersion);
		RecordAction recordAction = RecordAction.match(firstCsvRecord.get("DML_IND"));

		CarrierClaim claim = CarrierClaimParser.parseRif(csvRecords);
		/*
		 * FIXME Can't use real line numbers because random/dummy test data has
		 * dupes.
		 */
		for (int fakeLineNumber = 1; fakeLineNumber <= claim.getLines().size(); fakeLineNumber++)
			claim.getLines().get(fakeLineNumber - 1).setLineNumber(new BigDecimal(fakeLineNumber));
		return new RifRecordEvent<CarrierClaim>(filesEvent, file, recordAction, claim);
	}

	/**
	 * @param filesEvent
	 *            the {@link RifFilesEvent} being processed
	 * @param file
	 *            the specific {@link RifFile} in that {@link RifFilesEvent}
	 *            that is being processed
	 * @param csvRecords
	 *            the {@link CSVRecord}s to be mapped, which must be from a
	 *            {@link RifFileType#SNF} {@link RifFile}
	 * @return a {@link RifRecordEvent} built from the specified
	 *         {@link CSVRecord}s
	 */
	private static RifRecordEvent<SNFClaim> buildSNFClaimEvent(RifFilesEvent filesEvent, RifFile file,
			List<CSVRecord> csvRecords) {
		if (LOGGER.isTraceEnabled())
			LOGGER.trace(csvRecords.toString());

		CSVRecord firstCsvRecord = csvRecords.get(0);

		int schemaVersion = RifParsingUtils.parseInteger(firstCsvRecord.get("VERSION"));
		if (RECORD_FORMAT_VERSION != schemaVersion)
			throw new UnsupportedRifVersionException(schemaVersion);
		RecordAction recordAction = RecordAction.match(firstCsvRecord.get("DML_IND"));

		SNFClaim claim = SNFClaimParser.parseRif(csvRecords);
		/*
		 * FIXME Can't use real line numbers because random/dummy test data has
		 * dupes.
		 */
		for (int fakeLineNumber = 1; fakeLineNumber <= claim.getLines().size(); fakeLineNumber++)
			claim.getLines().get(fakeLineNumber - 1).setLineNumber(new BigDecimal(fakeLineNumber));
		return new RifRecordEvent<SNFClaim>(filesEvent, file, recordAction, claim);
	}

	/**
	 * @param filesEvent
	 *            the {@link RifFilesEvent} being processed
	 * @param file
	 *            the specific {@link RifFile} in that {@link RifFilesEvent}
	 *            that is being processed
	 * @param csvRecords
	 *            the {@link CSVRecord}s to be mapped, which must be from a
	 *            {@link RifFileType#HOSPICE} {@link RifFile}
	 * @return a {@link RifRecordEvent} built from the specified
	 *         {@link CSVRecord}s
	 */
	private static RifRecordEvent<HospiceClaim> buildHospiceClaimEvent(RifFilesEvent filesEvent, RifFile file,
			List<CSVRecord> csvRecords) {
		if (LOGGER.isTraceEnabled())
			LOGGER.trace(csvRecords.toString());

		CSVRecord firstCsvRecord = csvRecords.get(0);

		int schemaVersion = RifParsingUtils.parseInteger(firstCsvRecord.get("VERSION"));
		if (RECORD_FORMAT_VERSION != schemaVersion)
			throw new UnsupportedRifVersionException(schemaVersion);
		RecordAction recordAction = RecordAction.match(firstCsvRecord.get("DML_IND"));

		HospiceClaim claim = HospiceClaimParser.parseRif(csvRecords);
		/*
		 * FIXME Can't use real line numbers because random/dummy test data has
		 * dupes.
		 */
		for (int fakeLineNumber = 1; fakeLineNumber <= claim.getLines().size(); fakeLineNumber++)
			claim.getLines().get(fakeLineNumber - 1).setLineNumber(new BigDecimal(fakeLineNumber));
		return new RifRecordEvent<HospiceClaim>(filesEvent, file, recordAction, claim);
	}

	/**
	 * @param filesEvent
	 *            the {@link RifFilesEvent} being processed
	 * @param file
	 *            the specific {@link RifFile} in that {@link RifFilesEvent}
	 *            that is being processed
	 * @param csvRecords
	 *            the {@link CSVRecord}s to be mapped, which must be from a
	 *            {@link RifFileType#HHA} {@link RifFile}
	 * @return a {@link RifRecordEvent} built from the specified
	 *         {@link CSVRecord}s
	 */
	private static RifRecordEvent<HHAClaim> buildHHAClaimEvent(RifFilesEvent filesEvent, RifFile file,
			List<CSVRecord> csvRecords) {
		if (LOGGER.isTraceEnabled())
			LOGGER.trace(csvRecords.toString());

		CSVRecord firstCsvRecord = csvRecords.get(0);

		int schemaVersion = RifParsingUtils.parseInteger(firstCsvRecord.get("VERSION"));
		if (RECORD_FORMAT_VERSION != schemaVersion)
			throw new UnsupportedRifVersionException(schemaVersion);
		RecordAction recordAction = RecordAction.match(firstCsvRecord.get("DML_IND"));

		HHAClaim claim = HHAClaimParser.parseRif(csvRecords);
		/*
		 * FIXME Can't use real line numbers because random/dummy test data has
		 * dupes.
		 */
		for (int fakeLineNumber = 1; fakeLineNumber <= claim.getLines().size(); fakeLineNumber++)
			claim.getLines().get(fakeLineNumber - 1).setLineNumber(new BigDecimal(fakeLineNumber));
		return new RifRecordEvent<HHAClaim>(filesEvent, file, recordAction, claim);
	}

	/**
	 * @param filesEvent
	 *            the {@link RifFilesEvent} being processed
	 * @param file
	 *            the specific {@link RifFile} in that {@link RifFilesEvent}
	 *            that is being processed
	 * @param csvRecords
	 *            the {@link CSVRecord}s to be mapped, which must be from a
	 *            {@link RifFileType#DME} {@link RifFile}
	 * @return a {@link RifRecordEvent} built from the specified
	 *         {@link CSVRecord}s
	 */
	private static RifRecordEvent<DMEClaim> buildDMEClaimEvent(RifFilesEvent filesEvent, RifFile file,
			List<CSVRecord> csvRecords) {
		if (LOGGER.isTraceEnabled())
			LOGGER.trace(csvRecords.toString());

		CSVRecord firstCsvRecord = csvRecords.get(0);

		int schemaVersion = RifParsingUtils.parseInteger(firstCsvRecord.get("VERSION"));
		if (RECORD_FORMAT_VERSION != schemaVersion)
			throw new UnsupportedRifVersionException(schemaVersion);
		RecordAction recordAction = RecordAction.match(firstCsvRecord.get("DML_IND"));

		DMEClaim claim = DMEClaimParser.parseRif(csvRecords);
		/*
		 * FIXME Can't use real line numbers because random/dummy test data has
		 * dupes.
		 */
		for (int fakeLineNumber = 1; fakeLineNumber <= claim.getLines().size(); fakeLineNumber++)
			claim.getLines().get(fakeLineNumber - 1).setLineNumber(new BigDecimal(fakeLineNumber));
		return new RifRecordEvent<DMEClaim>(filesEvent, file, recordAction, claim);
	}
}
