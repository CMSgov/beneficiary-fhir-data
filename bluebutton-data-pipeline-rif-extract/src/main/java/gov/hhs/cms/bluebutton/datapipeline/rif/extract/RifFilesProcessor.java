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
import java.util.LinkedList;
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

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.datapipeline.rif.extract.CsvRecordGroupingIterator.CsvRecordGrouper;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.BeneficiaryRow;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.CarrierClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.CarrierClaimGroup.CarrierClaimLine;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.IcdCode;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.IcdCode.IcdVersion;
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

	/**
	 * FIXME The data uses a two-digit year format, which is awful. Just awful.
	 */
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

		Stream<RifRecordEvent<?>> rifRecordStream;
		if (file.getFileType() == RifFileType.BENEFICIARY) {
			Iterator<CSVRecord> csvIterator = parser.iterator();
			Spliterator<CSVRecord> spliterator = Spliterators.spliteratorUnknownSize(csvIterator, 0);
			Stream<CSVRecord> csvRecordStream = StreamSupport.stream(spliterator, false);

			rifRecordStream = csvRecordStream.map(csvRecord -> {
				RifRecordEvent<BeneficiaryRow> recordEvent = new RifRecordEvent<BeneficiaryRow>(rifFilesEvent, file,
						buildBeneficiaryEvent(csvRecord));
				closeParserIfDone(parser, csvIterator);
				return recordEvent;
			});
		} else if (file.getFileType() == RifFileType.PDE) {
			Iterator<CSVRecord> csvIterator = parser.iterator();
			Spliterator<CSVRecord> spliterator = Spliterators.spliteratorUnknownSize(csvIterator, 0);
			Stream<CSVRecord> csvRecordStream = StreamSupport.stream(spliterator, false);

			rifRecordStream = csvRecordStream.map(csvRecord -> {
				RifRecordEvent<PartDEventRow> recordEvent = new RifRecordEvent<>(rifFilesEvent, file,
						buildPartDEvent(csvRecord));
				closeParserIfDone(parser, csvIterator);
				return recordEvent;
			});
		} else if (file.getFileType() == RifFileType.CARRIER) {
			CsvRecordGrouper grouper = new CsvRecordGrouper() {
				@Override
				public int compare(CSVRecord o1, CSVRecord o2) {
					if (o1 == null)
						throw new IllegalArgumentException();
					if (o2 == null)
						throw new IllegalArgumentException();

					String claimId1 = o1.get(CarrierClaimGroup.Column.CLM_ID.ordinal());
					String claimId2 = o2.get(CarrierClaimGroup.Column.CLM_ID.ordinal());

					return claimId1.compareTo(claimId2);
				}
			};

			Iterator<List<CSVRecord>> csvIterator = new CsvRecordGroupingIterator(parser, grouper);
			Spliterator<List<CSVRecord>> spliterator = Spliterators.spliteratorUnknownSize(csvIterator, 0);
			Stream<List<CSVRecord>> csvRecordStream = StreamSupport.stream(spliterator, false);

			rifRecordStream = csvRecordStream.map(csvRecordGroup -> {
				RifRecordEvent<CarrierClaimGroup> recordEvent = new RifRecordEvent<CarrierClaimGroup>(rifFilesEvent,
						file, buildCarrierClaimEvent(csvRecordGroup));
				closeParserIfDone(parser, csvIterator);
				return recordEvent;
			});
		} else {
			throw new BadCodeMonkeyException();
		}

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
	private static void closeParserIfDone(CSVParser parser, Iterator<?> csvIterator) {
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
		// pdeRow.paymentDate =
		// LocalDate.parse(csvRecord.get(PartDEventRow.Column.PD_DT.ordinal()),
		// RIF_DATE_FORMATTER);
		// pdeRow.serviceProviderIdQualiferCode =
		// csvRecord.get(PartDEventRow.Column.SRVC_PRVDR_ID_QLFYR_CD.ordinal());
		// pdeRow.serviceProviderId =
		// csvRecord.get(PartDEventRow.Column.SRVC_PRVDR_ID.ordinal());
		// pdeRow.prescriberIdQualifierCode =
		// csvRecord.get(PartDEventRow.Column.PRSCRBR_ID_QLFYR_CD.ordinal());
		// pdeRow.prescriberId =
		// csvRecord.get(PartDEventRow.Column.PRSCRBR_ID.ordinal());
		// pdeRow.prescriptionReferenceNumber = Integer
		// .parseInt(csvRecord.get(PartDEventRow.Column.RX_SRVC_RFRNC_NUM.ordinal()));
		// pdeRow.nationalDrugCode =
		// csvRecord.get(PartDEventRow.Column.PROD_SRVC_ID.ordinal());
		// pdeRow.planContractId =
		// csvRecord.get(PartDEventRow.Column.PLAN_CNTRCT_REC_ID.ordinal());
		// pdeRow.planBenefitPackageId =
		// csvRecord.get(PartDEventRow.Column.PLAN_PBP_REC_NUM.ordinal());
		// pdeRow.compoundCode =
		// Integer.parseInt(csvRecord.get(PartDEventRow.Column.CMPND_CD.ordinal()));
		// pdeRow.dispenseAsWrittenProductSelectionCode =
		// csvRecord.get(PartDEventRow.Column.DAW_PROD_SLCTN_CD.ordinal());
		// pdeRow.quantityDispensed =
		// Integer.parseInt(csvRecord.get(PartDEventRow.Column.QTY_DPSNSD_NUM.ordinal()));
		// pdeRow.daysSupply =
		// Integer.parseInt(csvRecord.get(PartDEventRow.Column.DAYS_SUPLY_NUM.ordinal()));
		// pdeRow.fillNumber =
		// Integer.parseInt(csvRecord.get(PartDEventRow.Column.FILL_NUM.ordinal()));
		// pdeRow.dispensingStatuscode =
		// csvRecord.get(PartDEventRow.Column.DSPNSNG_STUS_CD.ordinal()).charAt(0);
		// TODO handle null/empty?
		// TODO finish mapping the rest of the columns

		// Sanity check:
		if (1 != pdeRow.version)
			throw new IllegalArgumentException("Unsupported record version: " + pdeRow.version);

		return pdeRow;
	}

	/**
	 * @param csvRecords
	 *            the {@link CSVRecord}s to be mapped, which must be from a
	 *            {@link RifFileType#CARRIER} {@link RifFile}, and must
	 *            represent all of the claim lines from a single claim
	 * @return a {@link BeneficiaryRow} built from the specified
	 *         {@link CSVRecord}
	 */
	private static CarrierClaimGroup buildCarrierClaimEvent(List<CSVRecord> csvRecords) {
		if (LOGGER.isTraceEnabled())
			LOGGER.trace(csvRecords.toString());

		CSVRecord firstClaimLine = csvRecords.get(0);

		CarrierClaimGroup claimGroup = new CarrierClaimGroup();

		/*
		 * Parse the claim header fields.
		 */
		claimGroup.version = parseInt(firstClaimLine.get(CarrierClaimGroup.Column.VERSION.ordinal()));
		claimGroup.recordAction = RecordAction.match(firstClaimLine.get(CarrierClaimGroup.Column.DML_IND.ordinal()));
		claimGroup.beneficiaryId = firstClaimLine.get(CarrierClaimGroup.Column.BENE_ID.ordinal());
		claimGroup.claimId = firstClaimLine.get(CarrierClaimGroup.Column.CLM_ID.ordinal());
		claimGroup.dateFrom = parseDate(firstClaimLine.get(CarrierClaimGroup.Column.CLM_FROM_DT.ordinal()));
		claimGroup.dateThrough = parseDate(firstClaimLine.get(CarrierClaimGroup.Column.CLM_THRU_DT.ordinal()));
		claimGroup.referringPhysicianNpi = firstClaimLine.get(CarrierClaimGroup.Column.RFR_PHYSN_NPI.ordinal());
		claimGroup.providerPaymentAmount = parseDecimal(
				firstClaimLine.get(CarrierClaimGroup.Column.NCH_CLM_PRVDR_PMT_AMT.ordinal()));
		claimGroup.diagnosisPrincipal = parseIcdCode(
				firstClaimLine.get(CarrierClaimGroup.Column.PRNCPAL_DGNS_CD.ordinal()),
				firstClaimLine.get(CarrierClaimGroup.Column.PRNCPAL_DGNS_VRSN_CD.ordinal()));
		claimGroup.diagnosesAdditional = parseIcdCodes(firstClaimLine, CarrierClaimGroup.Column.ICD_DGNS_CD1.ordinal(),
				CarrierClaimGroup.Column.ICD_DGNS_VRSN_CD12.ordinal());
		// TODO finish mapping the rest of the columns

		/*
		 * Parse the claim lines.
		 */
		for (CSVRecord claimLineRecord : csvRecords) {
			CarrierClaimLine claimLine = new CarrierClaimLine();

			claimLine.number = parseInt(claimLineRecord.get(CarrierClaimGroup.Column.LINE_NUM.ordinal()));
			claimLine.organizationNpi = parseOptString(
					claimLineRecord.get(CarrierClaimGroup.Column.ORG_NPI_NUM.ordinal()));
			claimLine.cmsServiceTypeCode = claimLineRecord
					.get(CarrierClaimGroup.Column.LINE_CMS_TYPE_SRVC_CD.ordinal());
			claimLine.hcpcsCode = claimLineRecord.get(CarrierClaimGroup.Column.HCPCS_CD.ordinal());
			claimLine.providerPaymentAmount = parseDecimal(
					claimLineRecord.get(CarrierClaimGroup.Column.LINE_PRVDR_PMT_AMT.ordinal()));
			claimLine.diagnosis = parseIcdCode(claimLineRecord.get(CarrierClaimGroup.Column.LINE_ICD_DGNS_CD.ordinal()),
					claimLineRecord.get(CarrierClaimGroup.Column.LINE_ICD_DGNS_VRSN_CD.ordinal()));
			// TODO finish mapping the rest of the columns

			claimGroup.lines.add(claimLine);
		}

		// Sanity check:
		if (1 != claimGroup.version)
			throw new IllegalArgumentException("Unsupported record version: " + claimGroup);

		return claimGroup;
	}

	/**
	 * @param string
	 *            the value to parse
	 * @return an {@link Optional} {@link String}, where
	 *         {@link Optional#isPresent()} will be <code>false</code> if the
	 *         specified value was empty, and will otherwise contain the
	 *         specified value
	 */
	private static Optional<String> parseOptString(String string) {
		return string.isEmpty() ? Optional.empty() : Optional.of(string);
	}

	/**
	 * @param intText
	 *            the number string to parse
	 * @return the specified text parsed into an {@link Integer}
	 */
	private static Integer parseInt(String intText) {
		/*
		 * Might seem silly to pull this out, but it makes the code a bit easier
		 * to read, and ensures that this parsing is standardized.
		 */

		return Integer.parseInt(intText);
	}

	/**
	 * @param decimalText
	 *            the decimal string to parse
	 * @return the specified text parsed into a {@link BigDecimal}
	 */
	private static BigDecimal parseDecimal(String decimalText) {
		/*
		 * Might seem silly to pull this out, but it makes the code a bit easier
		 * to read, and ensures that this parsing is standardized.
		 */

		return new BigDecimal(decimalText);
	}

	/**
	 * @param dateText
	 *            the date string to parse
	 * @return the specified text as a {@link LocalDate}, parsed using
	 *         {@link #RIF_DATE_FORMATTER}
	 */
	private static LocalDate parseDate(String dateText) {
		/*
		 * Might seem silly to pull this out, but it makes the code a bit easier
		 * to read, and ensures that this parsing is standardized.
		 */

		LocalDate dateFrom = LocalDate.parse(dateText, RIF_DATE_FORMATTER);
		return dateFrom;
	}

	/**
	 * @param icdCode
	 *            the value to use for {@link IcdCode#getCode()}
	 * @param icdVersion
	 *            the value to parse and use for {@link IcdCode#getVersion()}
	 * @return an {@link IcdCode} instance built from the specified values
	 */
	private static IcdCode parseIcdCode(String icdCode, String icdVersion) {
		return new IcdCode(IcdVersion.parse(icdVersion), icdCode);
	}

	/**
	 * Parses {@link IcdCode}s out of the specified columns of the specified
	 * {@link CSVRecord}. The columns must be arranged in code and version
	 * pairs; the first column specified must represent an ICD code, the next
	 * column must represent an ICD version (as can be parsed by
	 * {@link IcdVersion#parse(String)}), and this sequence must repeat for all
	 * of the specified columns.
	 * 
	 * @param csvRecord
	 *            the {@link CSVRecord} to parse the {@link IcdCode}s from
	 * @param icdColumnFirst
	 *            the first column ordinal to parse from, which must contain an
	 *            {@link IcdCode#getCode()} value
	 * @param icdColumnLast
	 *            the last column ordinal to parse from, which must contain a
	 *            value that could be parsed by {@link IcdVersion#parse(String)}
	 * @return the {@link IcdCode}s contained in the specified columns of the
	 *         specified {@link CSVRecord}
	 */
	private static List<IcdCode> parseIcdCodes(CSVRecord csvRecord, int icdColumnFirst, int icdColumnLast) {
		if ((icdColumnLast - icdColumnFirst) < 1)
			throw new BadCodeMonkeyException();
		if ((icdColumnLast - icdColumnFirst + 1) % 2 != 0)
			throw new BadCodeMonkeyException();

		List<IcdCode> icdCodes = new LinkedList<>();
		for (int i = icdColumnFirst; i < icdColumnLast; i += 2) {
			String icdCodeText = csvRecord.get(i);
			String icdVersionText = csvRecord.get(i + 1);

			if (icdCodeText.isEmpty() && icdVersionText.isEmpty())
				continue;
			else if (!icdCodeText.isEmpty() && !icdVersionText.isEmpty())
				icdCodes.add(parseIcdCode(icdCodeText, icdVersionText));
			else
				throw new IllegalArgumentException(
						String.format("Unexpected ICD code pair: '%s' and '%s'.", icdCodeText, icdVersionText));
		}

		return icdCodes;
	}
}
