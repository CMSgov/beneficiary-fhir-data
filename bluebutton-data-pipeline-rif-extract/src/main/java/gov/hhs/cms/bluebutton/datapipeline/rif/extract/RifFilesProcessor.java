package gov.hhs.cms.bluebutton.datapipeline.rif.extract;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.datapipeline.rif.model.BeneficiaryRow;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.PartDEventRow;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RecordAction;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFileType;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFilesEvent;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifRecordEvent;

/**
 * Contains services responsible for handling new RIF files.
 */
public final class RifFilesProcessor {
	private static final Logger LOGGER = LoggerFactory.getLogger(RifFilesProcessor.class);
	private final static DateTimeFormatter RIF_DATE_FORMATTER = new DateTimeFormatterBuilder().parseCaseInsensitive()
			.appendPattern("dd-MMM-yy").toFormatter();

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
	 * <li>While still honoring the previous rule, {@link RifFile}s with earlier
	 * {@link RifFile#getLastModifiedTimestamp()} values must be processed
	 * before those with later values. This is necessary in order to ensure
	 * that, if a backlog of {@link RifFile}s occurs, FHIR updates are not
	 * pushed out of order, which would result in newer data being overwritten
	 * by older data.</li>
	 * </ol>
	 * <p>
	 * Please note that, assuming the extraction process that produces
	 * {@link RifFile}s is functioning correctly (i.e. it's producing and
	 * pushing files to S3 in the correct order), it is always safe to process a
	 * single {@link RifFile}, if it's the only one found/available. There is no
	 * need to wait for a "full" set of {@link RifFile}s to be present. The
	 * above constraints only impact this class' behavior when multiple RIF
	 * files are found/available at the same time.
	 * </p>
	 * 
	 * @param event
	 *            the {@link RifFilesEvent} to be processed
	 * @return the {@link RifRecordEvent}s that are produced from the specified
	 *         {@link RifFilesEvent}
	 */
	public Stream<RifRecordEvent<?>> process(RifFilesEvent event) {
		/*
		 * Given that the bottleneck in our ETL processing is the Load phase
		 * (and likely always will be, due to network overhead and the FHIR
		 * server's performance), the Extract and Transform phases are
		 * single-threaded and likely to remain so. This allows the system to
		 * prevent resource over-consumption by blocking in the Load phase: the
		 * Load phase should block the Extract and Load phases' thread if too
		 * many records are in-flight at once. This is effectively backpressure,
		 * which will keep the Extract phase from over-producing and blowing the
		 * heap.
		 */
		// TODO test the above assertions, to ensure I'm not a liar

		List<RifFile> filesOrderedSafely = new ArrayList<>(event.getFiles());
		Comparator<RifFile> someComparator = null; // TODO
		Collections.sort(filesOrderedSafely, someComparator);

		/*
		 * The flatMap(...) call is used here instead map(...), to merge the
		 * Streams produced by produceRecords(...) into a single, flat/combined
		 * Stream.
		 */

		/*
		 * FIXME I've got a resource ownership problem: no way to tell when the
		 * stream is fully mapped, such that it's safe to close the parser. I be
		 * fucked.
		 */

		Stream<RifRecordEvent<?>> recordProducer = filesOrderedSafely.stream()
				.flatMap(file -> produceRecords(event, file));
		return recordProducer;
	}

	/**
	 * @param rifFilesEvent
	 *            the {@link RifFilesEvent} that is being processed
	 * @param file
	 *            the {@link RifFile} to produce {@link RifRecordEvent}s from
	 * @return a {@link Stream} that produces the {@link RifRecordEvent}s
	 *         represented in the specified {@link RifFile}
	 */
	private Stream<RifRecordEvent<?>> produceRecords(RifFilesEvent rifFilesEvent, RifFile file) {
		/*
		 * Approach used here to parse CSV as a Java 8 Stream is courtesy of
		 * https://rumianom.pl/rumianom/entry/apache-commons-csv-with-java.
		 * However, we have to screw around directly with the Iterator in order
		 * to ensure that the CSVParser (and its Reader and InputStream) are
		 * properly closed after the last record is read. Unfortunately,
		 * try-with-resources won't work here, due to the lazy nature of Stream
		 * processing (the CSVParser would be closed before it's even used).
		 */

		CSVParser parser = createCsvParser(file);
		Iterator<CSVRecord> csvIterator = parser.iterator();
		Spliterator<CSVRecord> spliterator = Spliterators.spliteratorUnknownSize(csvIterator, 0);
		Stream<CSVRecord> csvRecordStream = StreamSupport.stream(spliterator, false);

		Stream<RifRecordEvent<?>> rifRecordStream;
		if (file.getFileType() == RifFileType.BENEFICIARY)
			rifRecordStream = csvRecordStream.map(csvRecord -> {
				RifRecordEvent<BeneficiaryRow> recordEvent = new RifRecordEvent<BeneficiaryRow>(rifFilesEvent, file,
						buildBeneficiaryEvent(csvRecord));
				closeParserIfDone(parser, csvIterator);
				return recordEvent;
			});
		else if (file.getFileType() == RifFileType.PDE)
			rifRecordStream = csvRecordStream.map(csvRecord -> {
				RifRecordEvent<PartDEventRow> recordEvent = new RifRecordEvent<>(rifFilesEvent, file,
						buildPartDEvent(csvRecord));
				closeParserIfDone(parser, csvIterator);
				return recordEvent;
			});
		else
			throw new BadCodeMonkeyException();

		return rifRecordStream;
	}

	/**
	 * @param file
	 *            the {@link RifFile} to parse
	 * @return a {@link CSVParser} for the specified {@link RifFile}
	 */
	private static CSVParser createCsvParser(RifFile file) {
		CSVFormat csvFormat = CSVFormat.EXCEL.withHeader((String[]) null).withDelimiter('|').withEscape('\\');

		InputStream fileStream = file.open();
		BOMInputStream fileStreamWithoutBom = new BOMInputStream(fileStream, false);
		InputStreamReader reader = new InputStreamReader(fileStreamWithoutBom, file.getCharset());

		try {
			CSVParser parser = new CSVParser(reader, csvFormat);
			return parser;
		} catch (IOException e) {
			/*
			 * Per the docs, this should only be thrown if there's an issue with
			 * the header record. We don't use header records, so this shouldn't
			 * ever occur.
			 */
			throw new BadCodeMonkeyException(e);
		}
	}

	/**
	 * Closes the specified {@link CSVParser} if {@link Iterator#hasNext()} is
	 * <code>false</code> for the specified {@link Iterator}.
	 * 
	 * @param parser
	 *            the {@link CSVParser} to close
	 * @param csvIterator
	 *            the {@link Iterator} that is being used, from the specified
	 *            {@link CSVParser}
	 */
	private static void closeParserIfDone(CSVParser parser, Iterator<CSVRecord> csvIterator) {
		if (!csvIterator.hasNext()) {
			try {
				/*
				 * This will also close the Reader and InputStream that the
				 * CSVParser was consuming.
				 */
				parser.close();
			} catch (IOException e) {
				LOGGER.warn("Unable to close CSVParser", e);
			}
		}
	}

	/**
	 * @param csvRecord
	 *            the {@link CSVRecord} to be mapped, which must be from a
	 *            {@link RifFileType#BENEFICIARY} {@link RifFile}
	 * @return a {@link BeneficiaryRow} built from the specified
	 *         {@link CSVRecord}
	 */
	private static BeneficiaryRow buildBeneficiaryEvent(CSVRecord csvRecord) {
		if (LOGGER.isTraceEnabled())
			LOGGER.trace(csvRecord.toString());

		BeneficiaryRow beneficiaryRow = new BeneficiaryRow();
		beneficiaryRow.version = Integer.parseInt(csvRecord.get(BeneficiaryRow.Column.VERSION.ordinal()));
		beneficiaryRow.recordAction = RecordAction.match(csvRecord.get(BeneficiaryRow.Column.DML_IND.ordinal()));
		beneficiaryRow.beneficiaryId = csvRecord.get(BeneficiaryRow.Column.BENE_ID.ordinal());
		beneficiaryRow.stateCode = csvRecord.get(BeneficiaryRow.Column.STATE_CODE.ordinal());
		beneficiaryRow.countyCode = csvRecord.get(BeneficiaryRow.Column.BENE_COUNTY_CD.ordinal());
		beneficiaryRow.postalCode = csvRecord.get(BeneficiaryRow.Column.BENE_ZIP_CD.ordinal());
		// TODO finish mapping the rest of the columns

		// Sanity check:
		if (1 != beneficiaryRow.version)
			throw new IllegalArgumentException("Unsupported record version: " + beneficiaryRow);

		return beneficiaryRow;
	}

	/**
	 * @param csvRecord
	 *            the {@link CSVRecord} to be mapped, which must be from a
	 *            {@link RifFileType#PDE} {@link RifFile}
	 * @return a {@link PartDEventRow} built from the specified
	 *         {@link CSVRecord}
	 */
	private static PartDEventRow buildPartDEvent(CSVRecord csvRecord) {
		if (LOGGER.isTraceEnabled())
			LOGGER.trace(csvRecord.toString());

		PartDEventRow pdeRow = new PartDEventRow();
		pdeRow.version = Integer.parseInt(csvRecord.get(PartDEventRow.Column.VERSION.ordinal()));
		pdeRow.recordAction = RecordAction.match(csvRecord.get(PartDEventRow.Column.DML_IND.ordinal()));
		pdeRow.partDEventId = csvRecord.get(PartDEventRow.Column.PDE_ID.ordinal());
		pdeRow.beneficiaryId = csvRecord.get(PartDEventRow.Column.BENE_ID.ordinal());
		pdeRow.prescriptionFillDate = LocalDate.parse(csvRecord.get(PartDEventRow.Column.SRVC_DT.ordinal()),
				RIF_DATE_FORMATTER);
		pdeRow.paymentDate = getOptionalLocalDate(csvRecord.get(PartDEventRow.Column.PD_DT.ordinal()));
		pdeRow.serviceProviderIdQualiferCode = csvRecord.get(PartDEventRow.Column.SRVC_PRVDR_ID_QLFYR_CD.ordinal());
		pdeRow.serviceProviderId = csvRecord.get(PartDEventRow.Column.SRVC_PRVDR_ID.ordinal());
		pdeRow.prescriberIdQualifierCode = csvRecord.get(PartDEventRow.Column.PRSCRBR_ID_QLFYR_CD.ordinal());
		pdeRow.prescriberId = csvRecord.get(PartDEventRow.Column.PRSCRBR_ID.ordinal());
		pdeRow.prescriptionReferenceNumber = Long
				.parseLong(csvRecord.get(PartDEventRow.Column.RX_SRVC_RFRNC_NUM.ordinal()));
		pdeRow.nationalDrugCode = csvRecord.get(PartDEventRow.Column.PROD_SRVC_ID.ordinal());
		pdeRow.planContractId = csvRecord.get(PartDEventRow.Column.PLAN_CNTRCT_REC_ID.ordinal());
		pdeRow.planBenefitPackageId = csvRecord.get(PartDEventRow.Column.PLAN_PBP_REC_NUM.ordinal());
		pdeRow.compoundCode = Integer.parseInt(csvRecord.get(PartDEventRow.Column.CMPND_CD.ordinal()));
		pdeRow.dispenseAsWrittenProductSelectionCode = csvRecord.get(PartDEventRow.Column.DAW_PROD_SLCTN_CD.ordinal());
		pdeRow.quantityDispensed = new BigDecimal(csvRecord.get(PartDEventRow.Column.QTY_DPSNSD_NUM.ordinal()));
		pdeRow.daysSupply = Integer.parseInt(csvRecord.get(PartDEventRow.Column.DAYS_SUPLY_NUM.ordinal()));
		pdeRow.fillNumber = Integer.parseInt(csvRecord.get(PartDEventRow.Column.FILL_NUM.ordinal()));
		pdeRow.dispensingStatuscode = getOptionalCharacter(
				csvRecord.get(PartDEventRow.Column.DSPNSNG_STUS_CD.ordinal()));
		// TODO finish mapping the rest of the columns

		// Sanity check:
		if (1 != pdeRow.version)
			throw new IllegalArgumentException("Unsupported record version: " + pdeRow.version);

		return pdeRow;
	}

	/**
	 * Utility method to return either a populated {@link LocalDate} or an empty
	 * Optional if a null or empty {@link String} was passed.
	 * 
	 * @param record
	 *            the parsed String from the input file.
	 * @return a populated {@link Optional} if the record has data, or an empty
	 *         Optional if not
	 */
	private static Optional<LocalDate> getOptionalLocalDate(String record) {
		if (StringUtils.isEmpty(record)) {
			return Optional.empty();
		} else {
			return Optional.of(LocalDate.parse(record, RIF_DATE_FORMATTER));
		}
	}

	/**
	 * Utility method to return either a populated {@link Integer} or an empty
	 * Optional if a null or empty {@link String} was passed.
	 * 
	 * @param record
	 *            the parsed String from the input file.
	 * @return a populated {@link Optional} if the record has data, or an empty
	 *         Optional if not
	 */
	private static Optional<Integer> getOptionalInteger(String record) {
		if (StringUtils.isEmpty(record)) {
			return Optional.empty();
		} else {
			return Optional.of(Integer.parseInt(record));
		}
	}

	/**
	 * Utility method to return either a populated {@link Character} or an empty
	 * Optional if a null or empty {@link String} was passed.
	 * 
	 * @param record
	 *            the parsed String from the input file.
	 * @return a populated {@link Optional} if the record has data, or an empty
	 *         Optional if not
	 */
	private static Optional<Character> getOptionalCharacter(String record) {
		if (StringUtils.isEmpty(record)) {
			return Optional.empty();
		} else {
			return Optional.of(record.charAt(0));
		}
	}
}
