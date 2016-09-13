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
import gov.hhs.cms.bluebutton.datapipeline.rif.model.CompoundCode;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.DrugCoverageStatus;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.IcdCode;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.IcdCode.IcdVersion;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.InpatientClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.InpatientClaimGroup.InpatientClaimLine;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.OutpatientClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.OutpatientClaimGroup.OutpatientClaimLine;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.PartDEventRow;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RecordAction;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFileType;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFilesEvent;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifRecordEvent;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.SNFClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.SNFClaimGroup.SNFClaimLine;

/**
 * Contains services responsible for handling new RIF files.
 */
public final class RifFilesProcessor {
	/**
	 * The {@link BeneficiaryRow#version}, {@link CarrierClaimGroup#version},
	 * etc. value that is currently supported.
	 */
	public static final int RECORD_FORMAT_VERSION = 5;

	private static final Logger LOGGER = LoggerFactory.getLogger(RifFilesProcessor.class);

	/**
	 * FIXME The data uses a two-digit year format, which is awful. Just awful.
	 */
	private final static DateTimeFormatter RIF_DATE_FORMATTER = new DateTimeFormatterBuilder().parseCaseInsensitive()
			.appendPattern("dd-MMM-yyyy").toFormatter();

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
		Comparator<RifFile> someComparator = new Comparator<RifFile>() {
			@Override
			public int compare(RifFile o1, RifFile o2) {
				if (o1.getFileType() == RifFileType.BENEFICIARY && o2.getFileType() != RifFileType.BENEFICIARY)
					return -1;
				else if (o1.getFileType() != RifFileType.BENEFICIARY && o2.getFileType() == RifFileType.BENEFICIARY)
					return 1;
				else
					return 0;
			}
		};
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

					String claimId1 = o1.get(CarrierClaimGroup.Column.CLM_ID);
					String claimId2 = o2.get(CarrierClaimGroup.Column.CLM_ID);

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
		} else if (file.getFileType() == RifFileType.INPATIENT) {
			CsvRecordGrouper grouper = new CsvRecordGrouper() {
				@Override
				public int compare(CSVRecord o1, CSVRecord o2) {
					if (o1 == null)
						throw new IllegalArgumentException();
					if (o2 == null)
						throw new IllegalArgumentException();

					String claimId1 = o1.get(InpatientClaimGroup.Column.CLM_ID);
					String claimId2 = o2.get(InpatientClaimGroup.Column.CLM_ID);

					return claimId1.compareTo(claimId2);
				}
			};

			Iterator<List<CSVRecord>> csvIterator = new CsvRecordGroupingIterator(parser, grouper);
			Spliterator<List<CSVRecord>> spliterator = Spliterators.spliteratorUnknownSize(csvIterator, 0);
			Stream<List<CSVRecord>> csvRecordStream = StreamSupport.stream(spliterator, false);

			rifRecordStream = csvRecordStream.map(csvRecordGroup -> {
				RifRecordEvent<InpatientClaimGroup> recordEvent = new RifRecordEvent<InpatientClaimGroup>(rifFilesEvent,
						file, buildInpatientClaimEvent(csvRecordGroup));
				closeParserIfDone(parser, csvIterator);
				return recordEvent;
			});
		} else if (file.getFileType() == RifFileType.OUTPATIENT) {
			CsvRecordGrouper grouper = new CsvRecordGrouper() {
				@Override
				public int compare(CSVRecord o1, CSVRecord o2) {
					if (o1 == null)
						throw new IllegalArgumentException();
					if (o2 == null)
						throw new IllegalArgumentException();

					String claimId1 = o1.get(OutpatientClaimGroup.Column.CLM_ID);
					String claimId2 = o2.get(OutpatientClaimGroup.Column.CLM_ID);

					return claimId1.compareTo(claimId2);
				}
			};

			Iterator<List<CSVRecord>> csvIterator = new CsvRecordGroupingIterator(parser, grouper);
			Spliterator<List<CSVRecord>> spliterator = Spliterators.spliteratorUnknownSize(csvIterator, 0);
			Stream<List<CSVRecord>> csvRecordStream = StreamSupport.stream(spliterator, false);

			rifRecordStream = csvRecordStream.map(csvRecordGroup -> {
				RifRecordEvent<OutpatientClaimGroup> recordEvent = new RifRecordEvent<OutpatientClaimGroup>(
						rifFilesEvent, file, buildOutpatientClaimEvent(csvRecordGroup));
				closeParserIfDone(parser, csvIterator);
				return recordEvent;
			});
		} else if (file.getFileType() == RifFileType.SNF) {
			CsvRecordGrouper grouper = new CsvRecordGrouper() {
				@Override
				public int compare(CSVRecord o1, CSVRecord o2) {
					if (o1 == null)
						throw new IllegalArgumentException();
					if (o2 == null)
						throw new IllegalArgumentException();

					String claimId1 = o1.get(SNFClaimGroup.Column.CLM_ID);
					String claimId2 = o2.get(SNFClaimGroup.Column.CLM_ID);

					return claimId1.compareTo(claimId2);
				}
			};

			Iterator<List<CSVRecord>> csvIterator = new CsvRecordGroupingIterator(parser, grouper);
			Spliterator<List<CSVRecord>> spliterator = Spliterators.spliteratorUnknownSize(csvIterator, 0);
			Stream<List<CSVRecord>> csvRecordStream = StreamSupport.stream(spliterator, false);

			rifRecordStream = csvRecordStream.map(csvRecordGroup -> {
				RifRecordEvent<SNFClaimGroup> recordEvent = new RifRecordEvent<SNFClaimGroup>(rifFilesEvent, file,
						buildSNFClaimEvent(csvRecordGroup));
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
		CSVFormat csvFormat = CSVFormat.EXCEL.withHeader().withDelimiter('|').withEscape('\\');

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
		beneficiaryRow.version = Integer.parseInt(csvRecord.get(BeneficiaryRow.Column.VERSION));
		beneficiaryRow.recordAction = RecordAction.match(csvRecord.get(BeneficiaryRow.Column.DML_IND));
		beneficiaryRow.beneficiaryId = csvRecord.get(BeneficiaryRow.Column.BENE_ID);
		beneficiaryRow.stateCode = csvRecord.get(BeneficiaryRow.Column.STATE_CODE);
		beneficiaryRow.countyCode = csvRecord.get(BeneficiaryRow.Column.BENE_COUNTY_CD);
		beneficiaryRow.postalCode = csvRecord.get(BeneficiaryRow.Column.BENE_ZIP_CD);
		beneficiaryRow.birthDate = parseDate(csvRecord.get(BeneficiaryRow.Column.BENE_BIRTH_DT));
		beneficiaryRow.sex = parseCharacter(csvRecord.get(BeneficiaryRow.Column.BENE_SEX_IDENT_CD));
		beneficiaryRow.race = parseCharacter(csvRecord.get(BeneficiaryRow.Column.BENE_RACE_CD));
		beneficiaryRow.entitlementCodeOriginal = parseOptCharacter(
				csvRecord.get(BeneficiaryRow.Column.BENE_ENTLMT_RSN_ORIG));
		beneficiaryRow.entitlementCodeCurrent = parseOptCharacter(
				csvRecord.get(BeneficiaryRow.Column.BENE_ENTLMT_RSN_CURR));
		beneficiaryRow.endStageRenalDiseaseCode = parseOptCharacter(
				csvRecord.get(BeneficiaryRow.Column.BENE_ESRD_IND));
		beneficiaryRow.medicareEnrollmentStatusCode = parseOptString(
				csvRecord.get(BeneficiaryRow.Column.BENE_MDCR_STATUS_CD));
		beneficiaryRow.partATerminationCode = parseOptCharacter(
				csvRecord.get(BeneficiaryRow.Column.BENE_PTA_TRMNTN_CD));
		beneficiaryRow.partBTerminationCode = parseOptCharacter(
				csvRecord.get(BeneficiaryRow.Column.BENE_PTB_TRMNTN_CD));
		beneficiaryRow.hicn = csvRecord.get(BeneficiaryRow.Column.BENE_CRNT_HIC_NUM);
		beneficiaryRow.nameSurname = csvRecord.get(BeneficiaryRow.Column.BENE_SRNM_NAME);
		beneficiaryRow.nameGiven = csvRecord.get(BeneficiaryRow.Column.BENE_GVN_NAME);
		beneficiaryRow.nameMiddleInitial = parseOptCharacter(
				csvRecord.get(BeneficiaryRow.Column.BENE_MDL_NAME));

		// Sanity check:
		if (RECORD_FORMAT_VERSION != beneficiaryRow.version)
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
		pdeRow.version = Integer.parseInt(csvRecord.get(PartDEventRow.Column.VERSION));
		// Sanity check:
		if (RECORD_FORMAT_VERSION != pdeRow.version)
			throw new IllegalArgumentException("Unsupported record version: " + pdeRow.version);

		pdeRow.recordAction = RecordAction.match(csvRecord.get(PartDEventRow.Column.DML_IND));
		pdeRow.partDEventId = csvRecord.get(PartDEventRow.Column.PDE_ID);
		pdeRow.beneficiaryId = csvRecord.get(PartDEventRow.Column.BENE_ID);
		pdeRow.prescriptionFillDate = LocalDate.parse(csvRecord.get(PartDEventRow.Column.SRVC_DT),
				RIF_DATE_FORMATTER);
		pdeRow.paymentDate = parseOptDate(csvRecord.get(PartDEventRow.Column.PD_DT));
		pdeRow.serviceProviderIdQualiferCode = csvRecord.get(PartDEventRow.Column.SRVC_PRVDR_ID_QLFYR_CD);
		pdeRow.serviceProviderId = csvRecord.get(PartDEventRow.Column.SRVC_PRVDR_ID);
		pdeRow.prescriberIdQualifierCode = csvRecord.get(PartDEventRow.Column.PRSCRBR_ID_QLFYR_CD);
		pdeRow.prescriberId = csvRecord.get(PartDEventRow.Column.PRSCRBR_ID);
		pdeRow.prescriptionReferenceNumber = Long
				.parseLong(csvRecord.get(PartDEventRow.Column.RX_SRVC_RFRNC_NUM));
		pdeRow.nationalDrugCode = csvRecord.get(PartDEventRow.Column.PROD_SRVC_ID);
		pdeRow.planContractId = csvRecord.get(PartDEventRow.Column.PLAN_CNTRCT_REC_ID);
		pdeRow.planBenefitPackageId = csvRecord.get(PartDEventRow.Column.PLAN_PBP_REC_NUM);
		pdeRow.compoundCode = CompoundCode
				.parseRifValue(Integer.parseInt(csvRecord.get(PartDEventRow.Column.CMPND_CD)));
		pdeRow.dispenseAsWrittenProductSelectionCode = csvRecord.get(PartDEventRow.Column.DAW_PROD_SLCTN_CD);
		pdeRow.quantityDispensed = new BigDecimal(csvRecord.get(PartDEventRow.Column.QTY_DSPNSD_NUM));
		pdeRow.daysSupply = Integer.parseInt(csvRecord.get(PartDEventRow.Column.DAYS_SUPLY_NUM));
		pdeRow.fillNumber = Integer.parseInt(csvRecord.get(PartDEventRow.Column.FILL_NUM));
		pdeRow.dispensingStatuscode = parseOptCharacter(csvRecord.get(PartDEventRow.Column.DSPNSNG_STUS_CD));
		pdeRow.drugCoverageStatusCode = DrugCoverageStatus
				.parseRifValue(csvRecord.get(PartDEventRow.Column.DRUG_CVRG_STUS_CD));
		pdeRow.adjustmentDeletionCode = parseOptCharacter(
				csvRecord.get(PartDEventRow.Column.ADJSTMT_DLTN_CD));
		pdeRow.nonstandardFormatCode = parseOptCharacter(csvRecord.get(PartDEventRow.Column.NSTD_FRMT_CD));
		pdeRow.pricingExceptionCode = parseOptCharacter(csvRecord.get(PartDEventRow.Column.PRCNG_EXCPTN_CD));
		pdeRow.catastrophicCoverageCode = parseOptCharacter(
				csvRecord.get(PartDEventRow.Column.CTSTRPHC_CVRG_CD));
		pdeRow.grossCostBelowOutOfPocketThreshold = new BigDecimal(
				csvRecord.get(PartDEventRow.Column.GDC_BLW_OOPT_AMT));
		pdeRow.grossCostAboveOutOfPocketThreshold = new BigDecimal(
				csvRecord.get(PartDEventRow.Column.GDC_ABV_OOPT_AMT));
		pdeRow.patientPaidAmount = new BigDecimal(csvRecord.get(PartDEventRow.Column.PTNT_PAY_AMT));
		pdeRow.otherTrueOutOfPocketPaidAmount = new BigDecimal(
				csvRecord.get(PartDEventRow.Column.OTHR_TROOP_AMT));
		pdeRow.lowIncomeSubsidyPaidAmount = new BigDecimal(csvRecord.get(PartDEventRow.Column.LICS_AMT));
		pdeRow.patientLiabilityReductionOtherPaidAmount = new BigDecimal(
				csvRecord.get(PartDEventRow.Column.PLRO_AMT));
		pdeRow.partDPlanCoveredPaidAmount = new BigDecimal(
				csvRecord.get(PartDEventRow.Column.CVRD_D_PLAN_PD_AMT));
		pdeRow.partDPlanNonCoveredPaidAmount = new BigDecimal(
				csvRecord.get(PartDEventRow.Column.NCVRD_PLAN_PD_AMT));
		pdeRow.totalPrescriptionCost = new BigDecimal(csvRecord.get(PartDEventRow.Column.TOT_RX_CST_AMT));
		pdeRow.prescriptionOriginationCode = parseOptCharacter(
				csvRecord.get(PartDEventRow.Column.RX_ORGN_CD));
		pdeRow.gapDiscountAmount = new BigDecimal(csvRecord.get(PartDEventRow.Column.RPTD_GAP_DSCNT_NUM));
		/*
		 * TODO Re-enable this mapping once it is determined for sure if this is
		 * optional or not.
		 */
		// pdeRow.brandGenericCode =
		// csvRecord.get(PartDEventRow.Column.BRND_GNRC_CD).charAt(0);
		pdeRow.pharmacyTypeCode = csvRecord.get(PartDEventRow.Column.PHRMCY_SRVC_TYPE_CD);
		pdeRow.patientResidenceCode = csvRecord.get(PartDEventRow.Column.PTNT_RSDNC_CD);
		pdeRow.submissionClarificationCode = parseOptString(
				csvRecord.get(PartDEventRow.Column.SUBMSN_CLR_CD));

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
		claimGroup.version = parseInt(firstClaimLine.get(CarrierClaimGroup.Column.VERSION));
		claimGroup.recordAction = RecordAction.match(firstClaimLine.get(CarrierClaimGroup.Column.DML_IND));
		claimGroup.beneficiaryId = firstClaimLine.get(CarrierClaimGroup.Column.BENE_ID);
		claimGroup.claimId = firstClaimLine.get(CarrierClaimGroup.Column.CLM_ID);
		claimGroup.nearLineRecordIdCode = parseCharacter(
				firstClaimLine.get(CarrierClaimGroup.Column.NCH_NEAR_LINE_REC_IDENT_CD));
		claimGroup.claimTypeCode = firstClaimLine.get(CarrierClaimGroup.Column.NCH_CLM_TYPE_CD);
		claimGroup.dateFrom = parseDate(firstClaimLine.get(CarrierClaimGroup.Column.CLM_FROM_DT));
		claimGroup.dateThrough = parseDate(firstClaimLine.get(CarrierClaimGroup.Column.CLM_THRU_DT));
		claimGroup.weeklyProcessDate = parseDate(firstClaimLine.get(CarrierClaimGroup.Column.NCH_WKLY_PROC_DT));
		claimGroup.claimEntryCode = parseCharacter(
				firstClaimLine.get(CarrierClaimGroup.Column.CARR_CLM_ENTRY_CD));
		claimGroup.claimDispositionCode = firstClaimLine.get(CarrierClaimGroup.Column.CLM_DISP_CD);
		claimGroup.carrierNumber = firstClaimLine.get(CarrierClaimGroup.Column.CARR_NUM);
		claimGroup.paymentDenialCode = firstClaimLine.get(CarrierClaimGroup.Column.CARR_CLM_PMT_DNL_CD);
		claimGroup.paymentAmount = parseDecimal(
				firstClaimLine.get(CarrierClaimGroup.Column.CLM_PMT_AMT));
		claimGroup.primaryPayerPaidAmount = parseDecimal(
				firstClaimLine.get(CarrierClaimGroup.Column.CARR_CLM_PRMRY_PYR_PD_AMT));
		claimGroup.referringPhysicianUpin = firstClaimLine.get(CarrierClaimGroup.Column.RFR_PHYSN_UPIN);
		claimGroup.referringPhysicianNpi = firstClaimLine.get(CarrierClaimGroup.Column.RFR_PHYSN_NPI);
		claimGroup.providerAssignmentIndicator = parseCharacter(
				firstClaimLine.get(CarrierClaimGroup.Column.CARR_CLM_PRVDR_ASGNMT_IND_SW));
		claimGroup.providerPaymentAmount = parseDecimal(
				firstClaimLine.get(CarrierClaimGroup.Column.NCH_CLM_PRVDR_PMT_AMT));
		claimGroup.beneficiaryPaymentAmount = parseDecimal(
				firstClaimLine.get(CarrierClaimGroup.Column.NCH_CLM_BENE_PMT_AMT));
		claimGroup.submittedChargeAmount = parseDecimal(
				firstClaimLine.get(CarrierClaimGroup.Column.NCH_CARR_CLM_SBMTD_CHRG_AMT));
		claimGroup.allowedChargeAmount = parseDecimal(
				firstClaimLine.get(CarrierClaimGroup.Column.NCH_CARR_CLM_ALOWD_AMT));
		claimGroup.beneficiaryPartBDeductAmount = parseDecimal(
				firstClaimLine.get(CarrierClaimGroup.Column.CARR_CLM_CASH_DDCTBL_APLD_AMT));
		claimGroup.hcpcsYearCode = parseCharacter(firstClaimLine.get(CarrierClaimGroup.Column.CARR_CLM_HCPCS_YR_CD));
		claimGroup.referringProviderIdNumber = firstClaimLine.get(CarrierClaimGroup.Column.CARR_CLM_RFRNG_PIN_NUM);
		claimGroup.diagnosisPrincipal = parseIcdCode(
				firstClaimLine.get(CarrierClaimGroup.Column.PRNCPAL_DGNS_CD),
				firstClaimLine.get(CarrierClaimGroup.Column.PRNCPAL_DGNS_VRSN_CD));
		claimGroup.diagnosesAdditional = parseIcdCodes(firstClaimLine, CarrierClaimGroup.Column.ICD_DGNS_CD1.ordinal(),
				CarrierClaimGroup.Column.ICD_DGNS_VRSN_CD12.ordinal());
		/*
		 * Parse the claim lines.
		 */
		for (CSVRecord claimLineRecord : csvRecords) {
			CarrierClaimLine claimLine = new CarrierClaimLine();

			claimLine.clinicalTrialNumber = claimLineRecord.get(CarrierClaimGroup.Column.CLM_CLNCL_TRIL_NUM);
			claimLine.number = parseInt(claimLineRecord.get(CarrierClaimGroup.Column.LINE_NUM));
			claimLine.performingProviderIdNumber = claimLineRecord.get(CarrierClaimGroup.Column.CARR_PRFRNG_PIN_NUM);
			claimLine.performingPhysicianUpin = parseOptString(
					claimLineRecord.get(CarrierClaimGroup.Column.PRF_PHYSN_UPIN));
			claimLine.performingPhysicianNpi = claimLineRecord.get(CarrierClaimGroup.Column.PRF_PHYSN_NPI);
			claimLine.organizationNpi = parseOptString(
					claimLineRecord.get(CarrierClaimGroup.Column.ORG_NPI_NUM));
			claimLine.providerTypeCode = parseCharacter(
					claimLineRecord.get(CarrierClaimGroup.Column.CARR_LINE_PRVDR_TYPE_CD));
			claimLine.providerTaxNumber = claimLineRecord.get(CarrierClaimGroup.Column.TAX_NUM);
			claimLine.providerStateCode = claimLineRecord.get(CarrierClaimGroup.Column.PRVDR_STATE_CD);
			claimLine.providerZipCode = claimLineRecord.get(CarrierClaimGroup.Column.PRVDR_ZIP);
			claimLine.providerSpecialityCode = claimLineRecord.get(CarrierClaimGroup.Column.PRVDR_SPCLTY);
			claimLine.providerParticipatingIndCode = parseCharacter(
					claimLineRecord.get(CarrierClaimGroup.Column.PRTCPTNG_IND_CD));
			claimLine.reducedPaymentPhysicianAsstCode = parseCharacter(
					claimLineRecord.get(CarrierClaimGroup.Column.CARR_LINE_RDCD_PMT_PHYS_ASTN_C));
			claimLine.serviceCount = parseDecimal(claimLineRecord.get(CarrierClaimGroup.Column.LINE_SRVC_CNT));
			claimLine.cmsServiceTypeCode = claimLineRecord
					.get(CarrierClaimGroup.Column.LINE_CMS_TYPE_SRVC_CD);
			claimLine.placeOfServiceCode = claimLineRecord.get(CarrierClaimGroup.Column.LINE_PLACE_OF_SRVC_CD);
			claimLine.linePricingLocalityCode = claimLineRecord.get(CarrierClaimGroup.Column.CARR_LINE_PRCNG_LCLTY_CD);
			claimLine.firstExpenseDate = parseDate(claimLineRecord.get(CarrierClaimGroup.Column.LINE_1ST_EXPNS_DT));
			claimLine.lastExpenseDate = parseDate(claimLineRecord.get(CarrierClaimGroup.Column.LINE_LAST_EXPNS_DT));
			claimLine.hcpcsCode = claimLineRecord.get(CarrierClaimGroup.Column.HCPCS_CD);
			claimLine.hcpcsInitialModifierCode = parseOptString(
					claimLineRecord.get(CarrierClaimGroup.Column.HCPCS_1ST_MDFR_CD));
			claimLine.hcpcsSecondModifierCode = parseOptString(
					claimLineRecord.get(CarrierClaimGroup.Column.HCPCS_2ND_MDFR_CD));
			claimLine.betosCode = claimLineRecord.get(CarrierClaimGroup.Column.BETOS_CD);
			claimLine.paymentAmount = parseDecimal(claimLineRecord.get(CarrierClaimGroup.Column.LINE_NCH_PMT_AMT));
			claimLine.beneficiaryPaymentAmount = parseDecimal(
					claimLineRecord.get(CarrierClaimGroup.Column.LINE_BENE_PMT_AMT));
			claimLine.providerPaymentAmount = parseDecimal(
					claimLineRecord.get(CarrierClaimGroup.Column.LINE_PRVDR_PMT_AMT));
			claimLine.beneficiaryPartBDeductAmount = parseDecimal(
					claimLineRecord.get(CarrierClaimGroup.Column.LINE_BENE_PTB_DDCTBL_AMT));
			claimLine.primaryPayerCode = parseOptCharacter(
					claimLineRecord.get(CarrierClaimGroup.Column.LINE_BENE_PRMRY_PYR_CD));
			claimLine.primaryPayerPaidAmount = parseDecimal(
					claimLineRecord.get(CarrierClaimGroup.Column.LINE_BENE_PRMRY_PYR_PD_AMT));
			claimLine.coinsuranceAmount = parseDecimal(claimLineRecord.get(CarrierClaimGroup.Column.LINE_COINSRNC_AMT));
			claimLine.submittedChargeAmount = parseDecimal(
					claimLineRecord.get(CarrierClaimGroup.Column.LINE_SBMTD_CHRG_AMT));
			claimLine.allowedChargeAmount = parseDecimal(
					claimLineRecord.get(CarrierClaimGroup.Column.LINE_ALOWD_CHRG_AMT));
			claimLine.processingIndicatorCode = claimLineRecord.get(CarrierClaimGroup.Column.LINE_PRCSG_IND_CD);
			claimLine.paymentCode = parseCharacter(claimLineRecord.get(CarrierClaimGroup.Column.LINE_PMT_80_100_CD));
			claimLine.serviceDeductibleCode = parseCharacter(
					claimLineRecord.get(CarrierClaimGroup.Column.LINE_SERVICE_DEDUCTIBLE));
			claimLine.mtusCount = parseDecimal(claimLineRecord.get(CarrierClaimGroup.Column.CARR_LINE_MTUS_CNT));
			claimLine.mtusCode = parseCharacter(claimLineRecord.get(CarrierClaimGroup.Column.CARR_LINE_MTUS_CD));
			claimLine.diagnosis = parseIcdCode(claimLineRecord.get(CarrierClaimGroup.Column.LINE_ICD_DGNS_CD),
					claimLineRecord.get(CarrierClaimGroup.Column.LINE_ICD_DGNS_VRSN_CD));
			claimLine.hpsaScarcityCode = parseOptCharacter(
					claimLineRecord.get(CarrierClaimGroup.Column.HPSA_SCRCTY_IND_CD));
			claimLine.rxNumber = parseOptString(claimLineRecord.get(CarrierClaimGroup.Column.CARR_LINE_RX_NUM));
			claimLine.hctHgbTestResult = parseDecimal(
					claimLineRecord.get(CarrierClaimGroup.Column.LINE_HCT_HGB_RSLT_NUM));
			claimLine.hctHgbTestTypeCode = parseOptString(
					claimLineRecord.get(CarrierClaimGroup.Column.LINE_HCT_HGB_TYPE_CD));
			claimLine.nationalDrugCode = parseOptString(claimLineRecord.get(CarrierClaimGroup.Column.LINE_NDC_CD));
			claimLine.cliaLabNumber = parseOptString(
					claimLineRecord.get(CarrierClaimGroup.Column.CARR_LINE_CLIA_LAB_NUM));
			claimLine.anesthesiaUnitCount = parseInt(
					claimLineRecord.get(CarrierClaimGroup.Column.CARR_LINE_ANSTHSA_UNIT_CNT));
			claimGroup.lines.add(claimLine);
		}

		// Sanity check:
		if (RECORD_FORMAT_VERSION != claimGroup.version)
			throw new IllegalArgumentException("Unsupported record version: " + claimGroup);

		return claimGroup;
	}

	private static InpatientClaimGroup buildInpatientClaimEvent(List<CSVRecord> csvRecords) {
		if (LOGGER.isTraceEnabled())
			LOGGER.trace(csvRecords.toString());

		CSVRecord firstClaimLine = csvRecords.get(0);

		InpatientClaimGroup claimGroup = new InpatientClaimGroup();

		/*
		 * Parse the claim header fields.
		 */
		claimGroup.version = parseInt(firstClaimLine.get(InpatientClaimGroup.Column.VERSION));
		claimGroup.recordAction = RecordAction.match(firstClaimLine.get(InpatientClaimGroup.Column.DML_IND));
		claimGroup.beneficiaryId = firstClaimLine.get(InpatientClaimGroup.Column.BENE_ID);
		claimGroup.claimId = firstClaimLine.get(InpatientClaimGroup.Column.CLM_ID);
		claimGroup.nearLineRecordIdCode = parseCharacter(
				firstClaimLine.get(InpatientClaimGroup.Column.NCH_NEAR_LINE_REC_IDENT_CD));
		claimGroup.claimTypeCode = firstClaimLine.get(InpatientClaimGroup.Column.NCH_CLM_TYPE_CD);
		claimGroup.dateFrom = parseDate(firstClaimLine.get(InpatientClaimGroup.Column.CLM_FROM_DT));
		claimGroup.dateThrough = parseDate(firstClaimLine.get(InpatientClaimGroup.Column.CLM_THRU_DT));
		claimGroup.weeklyProcessDate = parseDate(firstClaimLine.get(InpatientClaimGroup.Column.NCH_WKLY_PROC_DT));
		claimGroup.providerNumber = firstClaimLine.get(InpatientClaimGroup.Column.PRVDR_NUM);
		claimGroup.claimFacilityTypeCode = parseCharacter(
				firstClaimLine.get(InpatientClaimGroup.Column.CLM_FAC_TYPE_CD));
		claimGroup.claimServiceClassificationTypeCode = parseCharacter(
				firstClaimLine.get(InpatientClaimGroup.Column.CLM_SRVC_CLSFCTN_TYPE_CD));
		claimGroup.claimNonPaymentReasonCode = parseOptString(
				firstClaimLine.get(InpatientClaimGroup.Column.CLM_MDCR_NON_PMT_RSN_CD));
		claimGroup.paymentAmount = parseDecimal(firstClaimLine.get(InpatientClaimGroup.Column.CLM_PMT_AMT));
		claimGroup.primaryPayerPaidAmount = parseDecimal(
				firstClaimLine.get(InpatientClaimGroup.Column.NCH_PRMRY_PYR_CLM_PD_AMT));
		claimGroup.providerStateCode = firstClaimLine.get(InpatientClaimGroup.Column.PRVDR_STATE_CD);
		claimGroup.organizationNpi = firstClaimLine.get(InpatientClaimGroup.Column.ORG_NPI_NUM);
		claimGroup.attendingPhysicianNpi = firstClaimLine.get(InpatientClaimGroup.Column.AT_PHYSN_NPI);
		claimGroup.operatingPhysicianNpi = firstClaimLine.get(InpatientClaimGroup.Column.OP_PHYSN_NPI);
		claimGroup.otherPhysicianNpi = firstClaimLine.get(InpatientClaimGroup.Column.OT_PHYSN_NPI);
		claimGroup.patientDischargeStatusCode = firstClaimLine.get(InpatientClaimGroup.Column.PTNT_DSCHRG_STUS_CD);
		claimGroup.totalChargeAmount = parseDecimal(firstClaimLine.get(InpatientClaimGroup.Column.CLM_TOT_CHRG_AMT));
		claimGroup.passThruPerDiemAmount = parseDecimal(
				firstClaimLine.get(InpatientClaimGroup.Column.CLM_PASS_THRU_PER_DIEM_AMT));
		claimGroup.deductibleAmount = parseDecimal(
				firstClaimLine.get(InpatientClaimGroup.Column.NCH_BENE_IP_DDCTBL_AMT));
		claimGroup.partACoinsuranceLiabilityAmount = parseDecimal(
				firstClaimLine.get(InpatientClaimGroup.Column.NCH_BENE_PTA_COINSRNC_LBLTY_AM));
		claimGroup.bloodDeductibleLiabilityAmount = parseDecimal(
				firstClaimLine.get(InpatientClaimGroup.Column.NCH_BENE_BLOOD_DDCTBL_LBLTY_AM));
		claimGroup.professionalComponentCharge = parseDecimal(
				firstClaimLine.get(InpatientClaimGroup.Column.NCH_PROFNL_CMPNT_CHRG_AMT));
		claimGroup.noncoveredCharge = parseDecimal(
				firstClaimLine.get(InpatientClaimGroup.Column.NCH_IP_NCVRD_CHRG_AMT));
		claimGroup.totalDeductionAmount = parseDecimal(
				firstClaimLine.get(InpatientClaimGroup.Column.NCH_IP_TOT_DDCTN_AMT));
		claimGroup.diagnosisAdmitting = parseIcdCode(firstClaimLine.get(InpatientClaimGroup.Column.ADMTG_DGNS_CD),
				firstClaimLine.get(InpatientClaimGroup.Column.ADMTG_DGNS_VRSN_CD));
		claimGroup.diagnosisPrincipal = parseIcdCode(firstClaimLine.get(InpatientClaimGroup.Column.PRNCPAL_DGNS_CD),
				firstClaimLine.get(InpatientClaimGroup.Column.PRNCPAL_DGNS_VRSN_CD));
		claimGroup.diagnosesAdditional = parseIcdCodesWithPOA(firstClaimLine,
				InpatientClaimGroup.Column.ICD_DGNS_CD1.ordinal(),
				InpatientClaimGroup.Column.ICD_DGNS_VRSN_CD25.ordinal());
		claimGroup.diagnosisFirstClaimExternal = parseOptIcdCode(
				firstClaimLine.get(InpatientClaimGroup.Column.FST_DGNS_E_CD),
				firstClaimLine.get(InpatientClaimGroup.Column.FST_DGNS_E_VRSN_CD));
		claimGroup.diagnosesExternal = parseIcdCodesWithPOA(firstClaimLine,
				InpatientClaimGroup.Column.ICD_DGNS_E_CD1.ordinal(),
				InpatientClaimGroup.Column.ICD_DGNS_E_VRSN_CD12.ordinal());

		/*
		 * TODO Need to parse procedure codes once STU3 is available
		 * 
		 */
		/*
		 * Parse the claim lines.
		 */
		for (CSVRecord claimLineRecord : csvRecords) {
			InpatientClaimLine claimLine = new InpatientClaimLine();

			claimLine.lineNumber = parseInt(claimLineRecord.get(InpatientClaimGroup.Column.CLM_LINE_NUM));
			claimLine.hcpcsCode = claimLineRecord.get(InpatientClaimGroup.Column.HCPCS_CD);

			claimGroup.lines.add(claimLine);
		}

		// Sanity check:
		if (RECORD_FORMAT_VERSION != claimGroup.version)
			throw new IllegalArgumentException("Unsupported record version: " + claimGroup);

		return claimGroup;
	}

	private static OutpatientClaimGroup buildOutpatientClaimEvent(List<CSVRecord> csvRecords) {
		if (LOGGER.isTraceEnabled())
			LOGGER.trace(csvRecords.toString());

		CSVRecord firstClaimLine = csvRecords.get(0);

		OutpatientClaimGroup claimGroup = new OutpatientClaimGroup();

		/*
		 * Parse the claim header fields.
		 */
		claimGroup.version = parseInt(firstClaimLine.get(OutpatientClaimGroup.Column.VERSION));
		claimGroup.recordAction = RecordAction.match(firstClaimLine.get(OutpatientClaimGroup.Column.DML_IND));
		claimGroup.beneficiaryId = firstClaimLine.get(OutpatientClaimGroup.Column.BENE_ID);
		claimGroup.claimId = firstClaimLine.get(OutpatientClaimGroup.Column.CLM_ID);
		claimGroup.nearLineRecordIdCode = parseCharacter(
				firstClaimLine.get(OutpatientClaimGroup.Column.NCH_NEAR_LINE_REC_IDENT_CD));
		claimGroup.claimTypeCode = firstClaimLine.get(OutpatientClaimGroup.Column.NCH_CLM_TYPE_CD);
		claimGroup.dateFrom = parseDate(firstClaimLine.get(OutpatientClaimGroup.Column.CLM_FROM_DT));
		claimGroup.dateThrough = parseDate(firstClaimLine.get(OutpatientClaimGroup.Column.CLM_THRU_DT));
		claimGroup.weeklyProcessDate = parseDate(firstClaimLine.get(OutpatientClaimGroup.Column.NCH_WKLY_PROC_DT));
		claimGroup.providerNumber = firstClaimLine.get(OutpatientClaimGroup.Column.PRVDR_NUM);
		claimGroup.claimFacilityTypeCode = parseCharacter(
				firstClaimLine.get(OutpatientClaimGroup.Column.CLM_FAC_TYPE_CD));
		claimGroup.claimServiceClassificationTypeCode = parseCharacter(
				firstClaimLine.get(OutpatientClaimGroup.Column.CLM_SRVC_CLSFCTN_TYPE_CD));
		claimGroup.claimNonPaymentReasonCode = parseOptString(
				firstClaimLine.get(OutpatientClaimGroup.Column.CLM_MDCR_NON_PMT_RSN_CD));
		claimGroup.paymentAmount = parseDecimal(firstClaimLine.get(OutpatientClaimGroup.Column.CLM_PMT_AMT));
		claimGroup.primaryPayerPaidAmount = parseDecimal(
				firstClaimLine.get(OutpatientClaimGroup.Column.NCH_PRMRY_PYR_CLM_PD_AMT));
		claimGroup.providerStateCode = firstClaimLine.get(OutpatientClaimGroup.Column.PRVDR_STATE_CD);
		claimGroup.organizationNpi = firstClaimLine.get(OutpatientClaimGroup.Column.ORG_NPI_NUM);
		claimGroup.attendingPhysicianNpi = firstClaimLine.get(OutpatientClaimGroup.Column.AT_PHYSN_NPI);
		claimGroup.operatingPhysicianNpi = firstClaimLine.get(OutpatientClaimGroup.Column.OP_PHYSN_NPI);
		claimGroup.otherPhysicianNpi = parseOptString(firstClaimLine.get(OutpatientClaimGroup.Column.OT_PHYSN_NPI));
		claimGroup.patientDischargeStatusCode = firstClaimLine.get(OutpatientClaimGroup.Column.PTNT_DSCHRG_STUS_CD);
		claimGroup.totalChargeAmount = parseDecimal(firstClaimLine.get(OutpatientClaimGroup.Column.CLM_TOT_CHRG_AMT));
		claimGroup.bloodDeductibleLiabilityAmount = parseDecimal(
				firstClaimLine.get(OutpatientClaimGroup.Column.NCH_BENE_BLOOD_DDCTBL_LBLTY_AM));
		claimGroup.professionalComponentCharge = parseDecimal(
				firstClaimLine.get(OutpatientClaimGroup.Column.NCH_PROFNL_CMPNT_CHRG_AMT));
		claimGroup.diagnosisPrincipal = parseIcdCode(firstClaimLine.get(OutpatientClaimGroup.Column.PRNCPAL_DGNS_CD),
				firstClaimLine.get(OutpatientClaimGroup.Column.PRNCPAL_DGNS_VRSN_CD));
		claimGroup.diagnosesAdditional = parseIcdCodes(firstClaimLine,
				OutpatientClaimGroup.Column.ICD_DGNS_CD1.ordinal(),
				OutpatientClaimGroup.Column.ICD_DGNS_VRSN_CD25.ordinal());
		claimGroup.diagnosisFirstClaimExternal = parseOptIcdCode(
				firstClaimLine.get(OutpatientClaimGroup.Column.FST_DGNS_E_CD),
				firstClaimLine.get(OutpatientClaimGroup.Column.FST_DGNS_E_VRSN_CD));
		claimGroup.diagnosesExternal = parseIcdCodes(firstClaimLine,
				OutpatientClaimGroup.Column.ICD_DGNS_E_CD1.ordinal(),
				OutpatientClaimGroup.Column.ICD_DGNS_E_VRSN_CD12.ordinal());

		/*
		 * TODO Need to parse procedure codes once STU3 is available
		 * 
		 */

		claimGroup.diagnosesReasonForVisit = parseIcdCodes(firstClaimLine,
				OutpatientClaimGroup.Column.RSN_VISIT_CD1.ordinal(),
				OutpatientClaimGroup.Column.RSN_VISIT_VRSN_CD3.ordinal());
		claimGroup.deductibleAmount = parseDecimal(
				firstClaimLine.get(OutpatientClaimGroup.Column.NCH_BENE_PTB_DDCTBL_AMT));
		claimGroup.coninsuranceAmount = parseDecimal(
				firstClaimLine.get(OutpatientClaimGroup.Column.NCH_BENE_PTB_COINSRNC_AMT));
		claimGroup.providerPaymentAmount = parseDecimal(
				firstClaimLine.get(OutpatientClaimGroup.Column.CLM_OP_PRVDR_PMT_AMT));
		claimGroup.beneficiaryPaymentAmount = parseDecimal(
				firstClaimLine.get(OutpatientClaimGroup.Column.CLM_OP_BENE_PMT_AMT));
		/*
		 * Parse the claim lines.
		 */
		for (CSVRecord claimLineRecord : csvRecords) {
			OutpatientClaimLine claimLine = new OutpatientClaimLine();

			claimLine.lineNumber = parseInt(claimLineRecord.get(OutpatientClaimGroup.Column.CLM_LINE_NUM));
			claimLine.hcpcsCode = claimLineRecord.get(OutpatientClaimGroup.Column.HCPCS_CD);
			claimLine.bloodDeductibleAmount = parseDecimal(claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR_BLOOD_DDCTBL_AMT)); 
			claimLine.cashDeductibleAmount = parseDecimal( claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR_CASH_DDCTBL_AMT));
			claimLine.wageAdjustedCoinsuranceAmount = parseDecimal(claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR_COINSRNC_WGE_ADJSTD_C));
			claimLine.reducedCoinsuranceAmount = parseDecimal(
					claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR_RDCD_COINSRNC_AMT));
			claimLine.providerPaymentAmount = parseDecimal(
					claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR_PRVDR_PMT_AMT));
			claimLine.benficiaryPaymentAmount = parseDecimal(
					claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR_BENE_PMT_AMT));
			claimLine.patientResponsibilityAmount = parseDecimal(
					claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR_PTNT_RSPNSBLTY_PMT));
			claimLine.paymentAmount = parseDecimal(
					claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR_PMT_AMT_AMT));
			claimLine.totalChargeAmount = parseDecimal(
					claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR_TOT_CHRG_AMT));
			claimLine.nonCoveredChargeAmount = parseDecimal(
					claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR_NCVRD_CHRG_AMT));
			 
			claimGroup.lines.add(claimLine);
		}

		// Sanity check:
		if (RECORD_FORMAT_VERSION != claimGroup.version)
			throw new IllegalArgumentException("Unsupported record version: " + claimGroup);

		return claimGroup;
	}

	private static SNFClaimGroup buildSNFClaimEvent(List<CSVRecord> csvRecords) {
		if (LOGGER.isTraceEnabled())
			LOGGER.trace(csvRecords.toString());

		CSVRecord firstClaimLine = csvRecords.get(0);

		SNFClaimGroup claimGroup = new SNFClaimGroup();

		/*
		 * Parse the claim header fields.
		 */
		claimGroup.version = parseInt(firstClaimLine.get(SNFClaimGroup.Column.VERSION));
		claimGroup.recordAction = RecordAction.match(firstClaimLine.get(SNFClaimGroup.Column.DML_IND));
		claimGroup.beneficiaryId = firstClaimLine.get(SNFClaimGroup.Column.BENE_ID);
		claimGroup.claimId = firstClaimLine.get(SNFClaimGroup.Column.CLM_ID);
		claimGroup.nearLineRecordIdCode = parseCharacter(
				firstClaimLine.get(SNFClaimGroup.Column.NCH_NEAR_LINE_REC_IDENT_CD));
		claimGroup.claimTypeCode = firstClaimLine.get(SNFClaimGroup.Column.NCH_CLM_TYPE_CD);
		claimGroup.dateFrom = parseDate(firstClaimLine.get(SNFClaimGroup.Column.CLM_FROM_DT));
		claimGroup.dateThrough = parseDate(firstClaimLine.get(SNFClaimGroup.Column.CLM_THRU_DT));
		claimGroup.weeklyProcessDate = parseDate(firstClaimLine.get(SNFClaimGroup.Column.NCH_WKLY_PROC_DT));
		claimGroup.providerNumber = firstClaimLine.get(SNFClaimGroup.Column.PRVDR_NUM);
		claimGroup.claimFacilityTypeCode = parseCharacter(firstClaimLine.get(SNFClaimGroup.Column.CLM_FAC_TYPE_CD));
		claimGroup.claimServiceClassificationTypeCode = parseCharacter(
				firstClaimLine.get(SNFClaimGroup.Column.CLM_SRVC_CLSFCTN_TYPE_CD));
		claimGroup.claimNonPaymentReasonCode = parseOptString(
				firstClaimLine.get(SNFClaimGroup.Column.CLM_MDCR_NON_PMT_RSN_CD));
		claimGroup.paymentAmount = parseDecimal(firstClaimLine.get(SNFClaimGroup.Column.CLM_PMT_AMT));
		claimGroup.primaryPayerPaidAmount = parseDecimal(
				firstClaimLine.get(SNFClaimGroup.Column.NCH_PRMRY_PYR_CLM_PD_AMT));
		claimGroup.providerStateCode = firstClaimLine.get(SNFClaimGroup.Column.PRVDR_STATE_CD);
		claimGroup.organizationNpi = firstClaimLine.get(SNFClaimGroup.Column.ORG_NPI_NUM);
		claimGroup.attendingPhysicianNpi = firstClaimLine.get(SNFClaimGroup.Column.AT_PHYSN_NPI);
		claimGroup.operatingPhysicianNpi = firstClaimLine.get(SNFClaimGroup.Column.OP_PHYSN_NPI);
		claimGroup.otherPhysicianNpi = firstClaimLine.get(SNFClaimGroup.Column.OT_PHYSN_NPI);
		claimGroup.patientDischargeStatusCode = firstClaimLine.get(SNFClaimGroup.Column.PTNT_DSCHRG_STUS_CD);
		claimGroup.totalChargeAmount = parseDecimal(firstClaimLine.get(SNFClaimGroup.Column.CLM_TOT_CHRG_AMT));
		claimGroup.deductibleAmount = parseDecimal(firstClaimLine.get(SNFClaimGroup.Column.NCH_BENE_IP_DDCTBL_AMT));
		claimGroup.partACoinsuranceLiabilityAmount = parseDecimal(
				firstClaimLine.get(SNFClaimGroup.Column.NCH_BENE_PTA_COINSRNC_LBLTY_AM));
		claimGroup.bloodDeductibleLiabilityAmount = parseDecimal(
				firstClaimLine.get(SNFClaimGroup.Column.NCH_BENE_BLOOD_DDCTBL_LBLTY_AM));
		claimGroup.noncoveredCharge = parseDecimal(firstClaimLine.get(SNFClaimGroup.Column.NCH_IP_NCVRD_CHRG_AMT));
		claimGroup.totalDeductionAmount = parseDecimal(firstClaimLine.get(SNFClaimGroup.Column.NCH_IP_TOT_DDCTN_AMT));
		claimGroup.diagnosisAdmitting = parseIcdCode(firstClaimLine.get(SNFClaimGroup.Column.ADMTG_DGNS_CD),
				firstClaimLine.get(SNFClaimGroup.Column.ADMTG_DGNS_VRSN_CD));
		claimGroup.diagnosisPrincipal = parseIcdCode(firstClaimLine.get(SNFClaimGroup.Column.PRNCPAL_DGNS_CD),
				firstClaimLine.get(SNFClaimGroup.Column.PRNCPAL_DGNS_VRSN_CD));
		claimGroup.diagnosesAdditional = parseIcdCodes(firstClaimLine,
				SNFClaimGroup.Column.ICD_DGNS_CD1.ordinal(), SNFClaimGroup.Column.ICD_DGNS_VRSN_CD25.ordinal());
		claimGroup.diagnosisFirstClaimExternal = parseOptIcdCode(firstClaimLine.get(SNFClaimGroup.Column.FST_DGNS_E_CD),
				firstClaimLine.get(SNFClaimGroup.Column.FST_DGNS_E_VRSN_CD));
		claimGroup.diagnosesExternal = parseIcdCodes(firstClaimLine,
				SNFClaimGroup.Column.ICD_DGNS_E_CD1.ordinal(), SNFClaimGroup.Column.ICD_DGNS_E_VRSN_CD12.ordinal());

		/*
		 * TODO Need to parse procedure codes once STU3 is available
		 * 
		 */
		/*
		 * Parse the claim lines.
		 */
		for (CSVRecord claimLineRecord : csvRecords) {
			SNFClaimLine claimLine = new SNFClaimLine();
			claimLine.lineNumber = parseInt(claimLineRecord.get(SNFClaimGroup.Column.CLM_LINE_NUM));
			claimLine.hcpcsCode = claimLineRecord.get(SNFClaimGroup.Column.HCPCS_CD);
			claimLine.totalChargeAmount = parseDecimal(claimLineRecord.get(SNFClaimGroup.Column.REV_CNTR_TOT_CHRG_AMT));
			claimLine.nonCoveredChargeAmount = parseDecimal(
					claimLineRecord.get(SNFClaimGroup.Column.REV_CNTR_NCVRD_CHRG_AMT));
			claimGroup.lines.add(claimLine);
		}

		// Sanity check:
		if (RECORD_FORMAT_VERSION != claimGroup.version)
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
	 * @param dateText
	 *            the date string to parse
	 * @return an {@link Optional} populated with a {@link LocalDate} if the
	 *         input has data, or an empty Optional if not
	 */
	private static Optional<LocalDate> parseOptDate(String dateText) {
		if (dateText.isEmpty()) {
			return Optional.empty();
		} else {
			return Optional.of(parseDate(dateText));
		}
	}

	/**
	 * @param charText
	 *            the char string to parse
	 * @return the specified text as a {@link Character} (first character only),
	 *         parsed using {@link #RIF_DATE_FORMATTER}
	 */
	private static Character parseCharacter(String charText) {
		/*
		 * Might seem silly to pull this out, but it makes the code a bit easier
		 * to read, and ensures that this parsing is standardized.
		 */

		return charText.charAt(0);
	}

	/**
	 * @param dateText
	 *            the date string to parse
	 * @return an {@link Optional} populated with a {@link Character} if the
	 *         input has data, or an empty Optional if not
	 */
	private static Optional<Character> parseOptCharacter(String charText) {
		if (charText.isEmpty()) {
			return Optional.empty();
		} else {
			return Optional.of(parseCharacter(charText));
		}
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
	 * @param icdCode
	 *            the value to use for {@link IcdCode#getCode()}
	 * @param icdVersion
	 *            the value to parse and use for {@link IcdCode#getVersion()}
	 * @return an {@link Optional} populated with a {@link IcdCode} if the input
	 *         has data, or an empty Optional if not
	 */
	private static Optional<IcdCode> parseOptIcdCode(String icdCode, String icdVersion) {
		if (icdCode.isEmpty()) {
			return Optional.empty();
		} else {
			return Optional.of(new IcdCode(IcdVersion.parse(icdVersion), icdCode));
		}
	}

	/**
	 * @param icdCode
	 *            the value to use for {@link IcdCode#getCode()}
	 * @param icdVersion
	 *            the value to parse and use for {@link IcdCode#getVersion()}
	 * @param icdPresentOnAdmission
	 *            the value to parse and use for
	 *            {@link IcdCode#getPresentOnAdmission()}
	 * @return an {@link IcdCode} instance built from the specified values
	 */
	private static IcdCode parseIcdCode(String icdCode, String icdVersion, String icdPresentOnAdmission) {
		return new IcdCode(IcdVersion.parse(icdVersion), icdCode, icdPresentOnAdmission);
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

	/**
	 * Parses {@link IcdCode}s out of the specified columns of the specified
	 * {@link CSVRecord}. The columns must be arranged in order of code, version
	 * and present on admission; the first column specified must represent an
	 * ICD code, the next column must represent an ICD version (as can be parsed
	 * by {@link IcdVersion#parse(String)}), next column is present on admission
	 * and this sequence must repeat for all of the specified columns.
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
	private static List<IcdCode> parseIcdCodesWithPOA(CSVRecord csvRecord, int icdColumnFirst, int icdColumnLast) {
		if ((icdColumnLast - icdColumnFirst) < 1)
			throw new BadCodeMonkeyException();
		if ((icdColumnLast - icdColumnFirst + 2) % 3 != 0)
			throw new BadCodeMonkeyException();

		List<IcdCode> icdCodes = new LinkedList<>();
		for (int i = icdColumnFirst; i < icdColumnLast; i += 3) {
			String icdCodeText = csvRecord.get(i);
			String icdVersionText = csvRecord.get(i + 1);
			String icdPresentOnAdmissionCode = csvRecord.get(i + 2);
			if (icdCodeText.isEmpty() && icdVersionText.isEmpty() && icdPresentOnAdmissionCode.isEmpty())
				continue;
			else if (!icdCodeText.isEmpty() && !icdVersionText.isEmpty()
					&& !icdPresentOnAdmissionCode.toString().isEmpty())
				icdCodes.add(parseIcdCode(icdCodeText, icdVersionText, icdPresentOnAdmissionCode));
			else
				throw new IllegalArgumentException(
						String.format("Unexpected ICD code/ver/poa : '%s' and '%s' and '%s'.", icdCodeText,
								icdVersionText, icdPresentOnAdmissionCode));
		}

		return icdCodes;
	}
}
