
package gov.hhs.cms.bluebutton.datapipeline.rif.extract;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
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
import gov.hhs.cms.bluebutton.data.model.rif.CompoundCode;
import gov.hhs.cms.bluebutton.data.model.rif.DMEClaimGroup;
import gov.hhs.cms.bluebutton.data.model.rif.DMEClaimGroup.DMEClaimLine;
import gov.hhs.cms.bluebutton.data.model.rif.DrugCoverageStatus;
import gov.hhs.cms.bluebutton.data.model.rif.HHAClaimGroup;
import gov.hhs.cms.bluebutton.data.model.rif.HHAClaimGroup.HHAClaimLine;
import gov.hhs.cms.bluebutton.data.model.rif.HospiceClaimGroup;
import gov.hhs.cms.bluebutton.data.model.rif.HospiceClaimGroup.HospiceClaimLine;
import gov.hhs.cms.bluebutton.data.model.rif.IcdCode;
import gov.hhs.cms.bluebutton.data.model.rif.IcdCode.IcdVersion;
import gov.hhs.cms.bluebutton.data.model.rif.InpatientClaimGroup;
import gov.hhs.cms.bluebutton.data.model.rif.InpatientClaimGroup.InpatientClaimLine;
import gov.hhs.cms.bluebutton.data.model.rif.OutpatientClaimGroup;
import gov.hhs.cms.bluebutton.data.model.rif.OutpatientClaimGroup.OutpatientClaimLine;
import gov.hhs.cms.bluebutton.data.model.rif.PartDEventRow;
import gov.hhs.cms.bluebutton.data.model.rif.RecordAction;
import gov.hhs.cms.bluebutton.data.model.rif.RifFile;
import gov.hhs.cms.bluebutton.data.model.rif.RifFileType;
import gov.hhs.cms.bluebutton.data.model.rif.RifFilesEvent;
import gov.hhs.cms.bluebutton.data.model.rif.RifRecordEvent;
import gov.hhs.cms.bluebutton.data.model.rif.SNFClaimGroup;
import gov.hhs.cms.bluebutton.data.model.rif.SNFClaimGroup.SNFClaimLine;
import gov.hhs.cms.bluebutton.data.model.rif.parse.InvalidRifFileFormatException;
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

	private static DateTimeFormatter formatter = new DateTimeFormatterBuilder().parseCaseInsensitive()
			.appendPattern("dd-MMM-yyyy").toFormatter();

	private static final Logger LOGGER = LoggerFactory.getLogger(RifFilesProcessor.class);

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
	 * need to wait for a "full" set of {@link RifFile}s to be present. The
	 * above constraints only impact this class' behavior when multiple RIF
	 * files are found/available at the same time.
	 * </p>
	 * 
	 * @param event
	 *            the {@link RifFilesEvent} to be processed
	 * @return a {@link List} of {@link RifRecordEvent} {@link Stream}s, one
	 *         {@link Stream} per {@link RifFile}, where each {@link Stream}
	 *         must be processed serially in a single thread, to avoid data race
	 *         conditions
	 */
	public List<Stream<RifRecordEvent<?>>> process(RifFilesEvent event) {
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
		 * FIXME I've got a resource ownership problem: no way to tell when the
		 * stream is fully mapped, such that it's safe to close the parser.
		 */

		List<Stream<RifRecordEvent<?>>> recordProducer = filesOrderedSafely.stream()
				.map(file -> produceRecords(event, file)).collect(Collectors.toList());
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
				LOGGER.warn("Parse error encountered near line number '{}'.",
						csvRecordGroup.get(0).getRecordNumber());
				throw new InvalidRifValueException(e);
			}
		});

		return rifRecordStream;
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

		int schemaVersion = parseInt(csvRecord.get("VERSION"));
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
	private static RifRecordEvent<PartDEventRow> buildPartDEvent(RifFilesEvent filesEvent, RifFile file,
			List<CSVRecord> csvRecords) {
		if (csvRecords.size() != 1)
			throw new BadCodeMonkeyException();
		CSVRecord csvRecord = csvRecords.get(0);

		if (LOGGER.isTraceEnabled())
			LOGGER.trace(csvRecord.toString());

		int schemaVersion = parseInt(csvRecord.get(PartDEventRow.Column.VERSION));
		if (RECORD_FORMAT_VERSION != schemaVersion)
			throw new UnsupportedRifVersionException(schemaVersion);
		RecordAction recordAction = RecordAction.match(csvRecord.get(PartDEventRow.Column.DML_IND));

		PartDEventRow pdeRow = new PartDEventRow();
		pdeRow.partDEventId = csvRecord.get(PartDEventRow.Column.PDE_ID);
		pdeRow.beneficiaryId = csvRecord.get(PartDEventRow.Column.BENE_ID);
		pdeRow.prescriptionFillDate = LocalDate.parse(csvRecord.get(PartDEventRow.Column.SRVC_DT), RIF_DATE_FORMATTER);
		pdeRow.paymentDate = parseOptDate(csvRecord.get(PartDEventRow.Column.PD_DT));
		pdeRow.serviceProviderIdQualiferCode = csvRecord.get(PartDEventRow.Column.SRVC_PRVDR_ID_QLFYR_CD);
		pdeRow.serviceProviderId = csvRecord.get(PartDEventRow.Column.SRVC_PRVDR_ID);
		pdeRow.prescriberIdQualifierCode = csvRecord.get(PartDEventRow.Column.PRSCRBR_ID_QLFYR_CD);
		pdeRow.prescriberId = csvRecord.get(PartDEventRow.Column.PRSCRBR_ID);
		pdeRow.prescriptionReferenceNumber = Long.parseLong(csvRecord.get(PartDEventRow.Column.RX_SRVC_RFRNC_NUM));
		pdeRow.nationalDrugCode = csvRecord.get(PartDEventRow.Column.PROD_SRVC_ID);
		pdeRow.planContractId = csvRecord.get(PartDEventRow.Column.PLAN_CNTRCT_REC_ID);
		pdeRow.planBenefitPackageId = csvRecord.get(PartDEventRow.Column.PLAN_PBP_REC_NUM);
		pdeRow.compoundCode = CompoundCode.parseRifValue(parseOptInteger(csvRecord.get(PartDEventRow.Column.CMPND_CD)));
		pdeRow.dispenseAsWrittenProductSelectionCode = csvRecord.get(PartDEventRow.Column.DAW_PROD_SLCTN_CD);
		pdeRow.quantityDispensed = new BigDecimal(csvRecord.get(PartDEventRow.Column.QTY_DSPNSD_NUM));
		pdeRow.daysSupply = Integer.parseInt(csvRecord.get(PartDEventRow.Column.DAYS_SUPLY_NUM));
		pdeRow.fillNumber = Integer.parseInt(csvRecord.get(PartDEventRow.Column.FILL_NUM));
		pdeRow.dispensingStatusCode = parseOptCharacter(csvRecord.get(PartDEventRow.Column.DSPNSNG_STUS_CD));
		pdeRow.drugCoverageStatusCode = DrugCoverageStatus
				.parseRifValue(csvRecord.get(PartDEventRow.Column.DRUG_CVRG_STUS_CD));
		pdeRow.adjustmentDeletionCode = parseOptCharacter(csvRecord.get(PartDEventRow.Column.ADJSTMT_DLTN_CD));
		pdeRow.nonstandardFormatCode = parseOptCharacter(csvRecord.get(PartDEventRow.Column.NSTD_FRMT_CD));
		pdeRow.pricingExceptionCode = parseOptCharacter(csvRecord.get(PartDEventRow.Column.PRCNG_EXCPTN_CD));
		pdeRow.catastrophicCoverageCode = parseOptCharacter(csvRecord.get(PartDEventRow.Column.CTSTRPHC_CVRG_CD));
		pdeRow.grossCostBelowOutOfPocketThreshold = new BigDecimal(
				csvRecord.get(PartDEventRow.Column.GDC_BLW_OOPT_AMT));
		pdeRow.grossCostAboveOutOfPocketThreshold = new BigDecimal(
				csvRecord.get(PartDEventRow.Column.GDC_ABV_OOPT_AMT));
		pdeRow.patientPaidAmount = new BigDecimal(csvRecord.get(PartDEventRow.Column.PTNT_PAY_AMT));
		pdeRow.otherTrueOutOfPocketPaidAmount = new BigDecimal(csvRecord.get(PartDEventRow.Column.OTHR_TROOP_AMT));
		pdeRow.lowIncomeSubsidyPaidAmount = new BigDecimal(csvRecord.get(PartDEventRow.Column.LICS_AMT));
		pdeRow.patientLiabilityReductionOtherPaidAmount = new BigDecimal(csvRecord.get(PartDEventRow.Column.PLRO_AMT));
		pdeRow.partDPlanCoveredPaidAmount = new BigDecimal(csvRecord.get(PartDEventRow.Column.CVRD_D_PLAN_PD_AMT));
		pdeRow.partDPlanNonCoveredPaidAmount = new BigDecimal(csvRecord.get(PartDEventRow.Column.NCVRD_PLAN_PD_AMT));
		pdeRow.totalPrescriptionCost = new BigDecimal(csvRecord.get(PartDEventRow.Column.TOT_RX_CST_AMT));
		pdeRow.prescriptionOriginationCode = parseOptCharacter(csvRecord.get(PartDEventRow.Column.RX_ORGN_CD));
		pdeRow.gapDiscountAmount = new BigDecimal(csvRecord.get(PartDEventRow.Column.RPTD_GAP_DSCNT_NUM));
		pdeRow.brandGenericCode = parseOptCharacter(csvRecord.get(PartDEventRow.Column.BRND_GNRC_CD));
		pdeRow.pharmacyTypeCode = csvRecord.get(PartDEventRow.Column.PHRMCY_SRVC_TYPE_CD);
		pdeRow.patientResidenceCode = csvRecord.get(PartDEventRow.Column.PTNT_RSDNC_CD);
		pdeRow.submissionClarificationCode = parseOptString(csvRecord.get(PartDEventRow.Column.SUBMSN_CLR_CD));

		return new RifRecordEvent<PartDEventRow>(filesEvent, file, recordAction, pdeRow);
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
	private static RifRecordEvent<InpatientClaimGroup> buildInpatientClaimEvent(RifFilesEvent filesEvent, RifFile file,
			List<CSVRecord> csvRecords) {
		if (LOGGER.isTraceEnabled())
			LOGGER.trace(csvRecords.toString());

		CSVRecord firstClaimLine = csvRecords.get(0);

		int schemaVersion = parseInt(firstClaimLine.get(InpatientClaimGroup.Column.VERSION));
		if (RECORD_FORMAT_VERSION != schemaVersion)
			throw new UnsupportedRifVersionException(schemaVersion);
		RecordAction recordAction = RecordAction.match(firstClaimLine.get(InpatientClaimGroup.Column.DML_IND));

		InpatientClaimGroup claimGroup = new InpatientClaimGroup();

		/*
		 * Parse the claim header fields.
		 */
		claimGroup.beneficiaryId = firstClaimLine.get(InpatientClaimGroup.Column.BENE_ID);
		claimGroup.claimId = firstClaimLine.get(InpatientClaimGroup.Column.CLM_ID);
		claimGroup.nearLineRecordIdCode = parseCharacter(
				firstClaimLine.get(InpatientClaimGroup.Column.NCH_NEAR_LINE_REC_IDENT_CD));
		claimGroup.claimTypeCode = firstClaimLine.get(InpatientClaimGroup.Column.NCH_CLM_TYPE_CD);
		claimGroup.dateFrom = parseDate(firstClaimLine.get(InpatientClaimGroup.Column.CLM_FROM_DT));
		claimGroup.dateThrough = parseDate(firstClaimLine.get(InpatientClaimGroup.Column.CLM_THRU_DT));
		claimGroup.weeklyProcessDate = parseDate(firstClaimLine.get(InpatientClaimGroup.Column.NCH_WKLY_PROC_DT));
		claimGroup.claimQueryCode = parseCharacter(firstClaimLine.get(InpatientClaimGroup.Column.CLAIM_QUERY_CODE));
		claimGroup.providerNumber = firstClaimLine.get(InpatientClaimGroup.Column.PRVDR_NUM);
		claimGroup.claimFacilityTypeCode = parseCharacter(
				firstClaimLine.get(InpatientClaimGroup.Column.CLM_FAC_TYPE_CD));
		claimGroup.claimServiceClassificationTypeCode = parseCharacter(
				firstClaimLine.get(InpatientClaimGroup.Column.CLM_SRVC_CLSFCTN_TYPE_CD));
		claimGroup.claimFrequencyCode = parseCharacter(firstClaimLine.get(InpatientClaimGroup.Column.CLM_FREQ_CD));
		claimGroup.claimNonPaymentReasonCode = parseOptString(
				firstClaimLine.get(InpatientClaimGroup.Column.CLM_MDCR_NON_PMT_RSN_CD));
		claimGroup.paymentAmount = parseDecimal(firstClaimLine.get(InpatientClaimGroup.Column.CLM_PMT_AMT));
		claimGroup.primaryPayerPaidAmount = parseDecimal(
				firstClaimLine.get(InpatientClaimGroup.Column.NCH_PRMRY_PYR_CLM_PD_AMT));
		claimGroup.claimPrimaryPayerCode = parseOptCharacter(
				firstClaimLine.get(InpatientClaimGroup.Column.NCH_PRMRY_PYR_CD));
		claimGroup.providerStateCode = firstClaimLine.get(InpatientClaimGroup.Column.PRVDR_STATE_CD);
		claimGroup.organizationNpi = parseOptString(firstClaimLine.get(InpatientClaimGroup.Column.ORG_NPI_NUM));
		claimGroup.attendingPhysicianNpi = parseOptString(firstClaimLine.get(InpatientClaimGroup.Column.AT_PHYSN_NPI));
		claimGroup.operatingPhysicianNpi = parseOptString(firstClaimLine.get(InpatientClaimGroup.Column.OP_PHYSN_NPI));
		claimGroup.otherPhysicianNpi = parseOptString(firstClaimLine.get(InpatientClaimGroup.Column.OT_PHYSN_NPI));
		claimGroup.mcoPaidSw = parseOptCharacter(firstClaimLine.get(InpatientClaimGroup.Column.CLM_MCO_PD_SW));
		claimGroup.patientDischargeStatusCode = firstClaimLine.get(InpatientClaimGroup.Column.PTNT_DSCHRG_STUS_CD);
		claimGroup.totalChargeAmount = parseDecimal(firstClaimLine.get(InpatientClaimGroup.Column.CLM_TOT_CHRG_AMT));
		claimGroup.claimAdmissionDate = parseOptDate(firstClaimLine.get(InpatientClaimGroup.Column.CLM_ADMSN_DT));
		claimGroup.admissionTypeCd = parseCharacter(
				firstClaimLine.get(InpatientClaimGroup.Column.CLM_IP_ADMSN_TYPE_CD));
		claimGroup.sourceAdmissionCd = parseOptCharacter(
				firstClaimLine.get(InpatientClaimGroup.Column.CLM_SRC_IP_ADMSN_CD));
		claimGroup.patientStatusCd = parseOptCharacter(
				firstClaimLine.get(InpatientClaimGroup.Column.NCH_PTNT_STATUS_IND_CD));
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

		claimGroup.claimTotalPPSCapitalAmount = Optional
				.of(parseDecimal(firstClaimLine.get(InpatientClaimGroup.Column.CLM_TOT_PPS_CPTL_AMT)));
		claimGroup.claimPPSCapitalFSPAmount = Optional
				.of(parseDecimal(firstClaimLine.get(InpatientClaimGroup.Column.CLM_PPS_CPTL_FSP_AMT)));
		claimGroup.claimPPSCapitalOutlierAmount = Optional
				.of(parseDecimal(firstClaimLine.get(InpatientClaimGroup.Column.CLM_PPS_CPTL_OUTLIER_AMT)));
		claimGroup.claimPPSCapitalDisproportionateShareAmt = Optional
				.of(parseDecimal(firstClaimLine.get(InpatientClaimGroup.Column.CLM_PPS_CPTL_DSPRPRTNT_SHR_AMT)));
		claimGroup.claimPPSCapitalIMEAmount = Optional
				.of(parseDecimal(firstClaimLine.get(InpatientClaimGroup.Column.CLM_PPS_CPTL_IME_AMT)));
		claimGroup.claimPPSCapitalExceptionAmount = Optional
				.of(parseDecimal(firstClaimLine.get(InpatientClaimGroup.Column.CLM_PPS_CPTL_EXCPTN_AMT)));
		claimGroup.claimPPSOldCapitalHoldHarmlessAmount = Optional
				.of(parseDecimal(firstClaimLine.get(InpatientClaimGroup.Column.CLM_PPS_OLD_CPTL_HLD_HRMLS_AMT)));
		claimGroup.nchDrugOutlierApprovedPaymentAmount = Optional
				.of(parseDecimal(firstClaimLine.get(InpatientClaimGroup.Column.NCH_DRG_OUTLIER_APRVD_PMT_AMT)));

		claimGroup.utilizationDayCount = parseInt(firstClaimLine.get(InpatientClaimGroup.Column.CLM_UTLZTN_DAY_CNT));
		claimGroup.coinsuranceDayCount = parseInt(
				firstClaimLine.get(InpatientClaimGroup.Column.BENE_TOT_COINSRNC_DAYS_CNT));
		claimGroup.nonUtilizationDayCount = parseInt(
				firstClaimLine.get(InpatientClaimGroup.Column.CLM_NON_UTLZTN_DAYS_CNT));
		claimGroup.bloodPintsFurnishedQty = parseInt(
				firstClaimLine.get(InpatientClaimGroup.Column.NCH_BLOOD_PNTS_FRNSHD_QTY));
		claimGroup.noncoveredStayFromDate = parseOptDate(
				firstClaimLine.get(InpatientClaimGroup.Column.NCH_VRFD_NCVRD_STAY_FROM_DT));
		claimGroup.noncoveredStayThroughDate = parseOptDate(
				firstClaimLine.get(InpatientClaimGroup.Column.NCH_VRFD_NCVRD_STAY_THRU_DT));
		claimGroup.coveredCareThoughDate = parseOptDate(
				firstClaimLine.get(InpatientClaimGroup.Column.NCH_ACTV_OR_CVRD_LVL_CARE_THRU));
		claimGroup.medicareBenefitsExhaustedDate = parseOptDate(
				firstClaimLine.get(InpatientClaimGroup.Column.NCH_BENE_MDCR_BNFTS_EXHTD_DT_I));
		claimGroup.beneficiaryDischargeDate = parseOptDate(
				firstClaimLine.get(InpatientClaimGroup.Column.NCH_BENE_DSCHRG_DT));

		claimGroup.diagnosisRelatedGroupCd = parseOptString(firstClaimLine.get(InpatientClaimGroup.Column.CLM_DRG_CD));

		claimGroup.diagnosisAdmitting = parseOptIcdCode(firstClaimLine.get(InpatientClaimGroup.Column.ADMTG_DGNS_CD),
				firstClaimLine.get(InpatientClaimGroup.Column.ADMTG_DGNS_VRSN_CD));
		claimGroup.diagnosisPrincipal = parseOptIcdCode(firstClaimLine.get(InpatientClaimGroup.Column.PRNCPAL_DGNS_CD),
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
		claimGroup.procedureCodes = parseIcdCodesProcedure(firstClaimLine,
				InpatientClaimGroup.Column.ICD_PRCDR_CD1.ordinal(),
				InpatientClaimGroup.Column.ICD_PRCDR_VRSN_CD25.ordinal());

		/*
		 * Parse the claim lines.
		 */
		for (CSVRecord claimLineRecord : csvRecords) {
			InpatientClaimLine claimLine = new InpatientClaimLine();

			claimLine.lineNumber = parseInt(claimLineRecord.get(InpatientClaimGroup.Column.CLM_LINE_NUM));
			claimLine.revenueCenter = claimLineRecord.get(InpatientClaimGroup.Column.REV_CNTR);
			claimLine.hcpcsCode = parseOptString(claimLineRecord.get(InpatientClaimGroup.Column.HCPCS_CD));
			claimLine.unitCount = parseDecimal(claimLineRecord.get(InpatientClaimGroup.Column.REV_CNTR_UNIT_CNT));

			claimLine.rateAmount = parseDecimal(claimLineRecord.get(InpatientClaimGroup.Column.REV_CNTR_RATE_AMT));
			claimLine.totalChargeAmount = parseDecimal(
					claimLineRecord.get(InpatientClaimGroup.Column.REV_CNTR_TOT_CHRG_AMT));
			claimLine.nonCoveredChargeAmount = parseDecimal(
					claimLineRecord.get(InpatientClaimGroup.Column.REV_CNTR_NCVRD_CHRG_AMT));

			claimLine.deductibleCoinsuranceCd = parseOptCharacter(
					claimLineRecord.get(InpatientClaimGroup.Column.REV_CNTR_DDCTBL_COINSRNC_CD));

			claimLine.nationalDrugCodeQuantity = parseOptDecimal(
					claimLineRecord.get(InpatientClaimGroup.Column.REV_CNTR_NDC_QTY));
			claimLine.nationalDrugCodeQualifierCode = parseOptString(
					claimLineRecord.get(InpatientClaimGroup.Column.REV_CNTR_NDC_QTY_QLFR_CD));
			claimLine.revenueCenterRenderingPhysicianNPI = parseOptString(
					claimLineRecord.get(InpatientClaimGroup.Column.RNDRNG_PHYSN_NPI));

			claimGroup.lines.add(claimLine);
		}

		return new RifRecordEvent<InpatientClaimGroup>(filesEvent, file, recordAction, claimGroup);
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
	private static RifRecordEvent<OutpatientClaimGroup> buildOutpatientClaimEvent(RifFilesEvent filesEvent,
			RifFile file, List<CSVRecord> csvRecords) {
		if (LOGGER.isTraceEnabled())
			LOGGER.trace(csvRecords.toString());

		CSVRecord firstClaimLine = csvRecords.get(0);

		int schemaVersion = parseInt(firstClaimLine.get(OutpatientClaimGroup.Column.VERSION));
		if (RECORD_FORMAT_VERSION != schemaVersion)
			throw new UnsupportedRifVersionException(schemaVersion);
		RecordAction recordAction = RecordAction.match(firstClaimLine.get(OutpatientClaimGroup.Column.DML_IND));

		OutpatientClaimGroup claimGroup = new OutpatientClaimGroup();

		/*
		 * Parse the claim header fields.
		 */
		claimGroup.beneficiaryId = firstClaimLine.get(OutpatientClaimGroup.Column.BENE_ID);
		claimGroup.claimId = firstClaimLine.get(OutpatientClaimGroup.Column.CLM_ID);
		claimGroup.nearLineRecordIdCode = parseCharacter(
				firstClaimLine.get(OutpatientClaimGroup.Column.NCH_NEAR_LINE_REC_IDENT_CD));
		claimGroup.claimTypeCode = firstClaimLine.get(OutpatientClaimGroup.Column.NCH_CLM_TYPE_CD);
		claimGroup.dateFrom = parseDate(firstClaimLine.get(OutpatientClaimGroup.Column.CLM_FROM_DT));
		claimGroup.dateThrough = parseDate(firstClaimLine.get(OutpatientClaimGroup.Column.CLM_THRU_DT));
		claimGroup.weeklyProcessDate = parseDate(firstClaimLine.get(OutpatientClaimGroup.Column.NCH_WKLY_PROC_DT));
		claimGroup.claimQueryCode = parseCharacter(firstClaimLine.get(OutpatientClaimGroup.Column.CLAIM_QUERY_CODE));
		claimGroup.providerNumber = firstClaimLine.get(OutpatientClaimGroup.Column.PRVDR_NUM);
		claimGroup.claimFacilityTypeCode = parseCharacter(
				firstClaimLine.get(OutpatientClaimGroup.Column.CLM_FAC_TYPE_CD));
		claimGroup.claimServiceClassificationTypeCode = parseCharacter(
				firstClaimLine.get(OutpatientClaimGroup.Column.CLM_SRVC_CLSFCTN_TYPE_CD));
		claimGroup.claimFrequencyCode = parseCharacter(firstClaimLine.get(OutpatientClaimGroup.Column.CLM_FREQ_CD));
		claimGroup.claimNonPaymentReasonCode = parseOptString(
				firstClaimLine.get(OutpatientClaimGroup.Column.CLM_MDCR_NON_PMT_RSN_CD));
		claimGroup.paymentAmount = parseDecimal(firstClaimLine.get(OutpatientClaimGroup.Column.CLM_PMT_AMT));
		claimGroup.primaryPayerPaidAmount = parseDecimal(
				firstClaimLine.get(OutpatientClaimGroup.Column.NCH_PRMRY_PYR_CLM_PD_AMT));
		claimGroup.claimPrimaryPayerCode = parseOptCharacter(
				firstClaimLine.get(OutpatientClaimGroup.Column.NCH_PRMRY_PYR_CD));
		claimGroup.providerStateCode = firstClaimLine.get(OutpatientClaimGroup.Column.PRVDR_STATE_CD);
		claimGroup.organizationNpi = parseOptString(firstClaimLine.get(OutpatientClaimGroup.Column.ORG_NPI_NUM));
		claimGroup.attendingPhysicianNpi = parseOptString(firstClaimLine.get(OutpatientClaimGroup.Column.AT_PHYSN_NPI));
		claimGroup.operatingPhysicianNpi = parseOptString(firstClaimLine.get(OutpatientClaimGroup.Column.OP_PHYSN_NPI));
		claimGroup.otherPhysicianNpi = parseOptString(firstClaimLine.get(OutpatientClaimGroup.Column.OT_PHYSN_NPI));
		claimGroup.mcoPaidSw = parseOptCharacter(firstClaimLine.get(OutpatientClaimGroup.Column.CLM_MCO_PD_SW));
		claimGroup.patientDischargeStatusCode = parseOptString(
				firstClaimLine.get(OutpatientClaimGroup.Column.PTNT_DSCHRG_STUS_CD));
		claimGroup.totalChargeAmount = parseDecimal(firstClaimLine.get(OutpatientClaimGroup.Column.CLM_TOT_CHRG_AMT));
		claimGroup.bloodDeductibleLiabilityAmount = parseDecimal(
				firstClaimLine.get(OutpatientClaimGroup.Column.NCH_BENE_BLOOD_DDCTBL_LBLTY_AM));
		claimGroup.professionalComponentCharge = parseDecimal(
				firstClaimLine.get(OutpatientClaimGroup.Column.NCH_PROFNL_CMPNT_CHRG_AMT));
		claimGroup.diagnosisPrincipal = parseOptIcdCode(firstClaimLine.get(OutpatientClaimGroup.Column.PRNCPAL_DGNS_CD),
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

		claimGroup.procedureCodes = parseIcdCodesProcedure(firstClaimLine,
				OutpatientClaimGroup.Column.ICD_PRCDR_CD1.ordinal(),
				OutpatientClaimGroup.Column.ICD_PRCDR_VRSN_CD25.ordinal());

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
			claimLine.revenueCenter = claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR);
			claimLine.revCntr1stAnsiCd = parseOptString(
					claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR_1ST_ANSI_CD));
			claimLine.revCntr2ndAnsiCd = parseOptString(
					claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR_2ND_ANSI_CD));
			claimLine.revCntr3rdAnsiCd = parseOptString(
					claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR_3RD_ANSI_CD));
			claimLine.revCntr4thAnsiCd = parseOptString(
					claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR_4TH_ANSI_CD));
			claimLine.hcpcsCode = parseOptString(claimLineRecord.get(OutpatientClaimGroup.Column.HCPCS_CD));
			claimLine.hcpcsInitialModifierCode = parseOptString(
					claimLineRecord.get(OutpatientClaimGroup.Column.HCPCS_1ST_MDFR_CD));
			claimLine.hcpcsSecondModifierCode = parseOptString(
					claimLineRecord.get(OutpatientClaimGroup.Column.HCPCS_2ND_MDFR_CD));
			claimLine.nationalDrugCode = parseOptString(
					claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR_IDE_NDC_UPC_NUM));
			claimLine.unitCount = parseDecimal(claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR_UNIT_CNT));
			claimLine.rateAmount = parseDecimal(claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR_RATE_AMT));
			claimLine.bloodDeductibleAmount = parseDecimal(
					claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR_BLOOD_DDCTBL_AMT));
			claimLine.cashDeductibleAmount = parseDecimal(
					claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR_CASH_DDCTBL_AMT));
			claimLine.wageAdjustedCoinsuranceAmount = parseDecimal(
					claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR_COINSRNC_WGE_ADJSTD_C));
			claimLine.reducedCoinsuranceAmount = parseDecimal(
					claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR_RDCD_COINSRNC_AMT));
			claimLine.firstMspPaidAmount = parseDecimal(
					claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR_1ST_MSP_PD_AMT));
			claimLine.secondMspPaidAmount = parseDecimal(
					claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR_2ND_MSP_PD_AMT));
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

			claimLine.nationalDrugCodeQuantity = parseOptDecimal(
					claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR_NDC_QTY));
			claimLine.nationalDrugCodeQualifierCode = parseOptString(
					claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR_NDC_QTY_QLFR_CD));
			claimLine.revenueCenterRenderingPhysicianNPI = parseOptString(
					claimLineRecord.get(OutpatientClaimGroup.Column.RNDRNG_PHYSN_NPI));

			claimGroup.lines.add(claimLine);
		}

		return new RifRecordEvent<OutpatientClaimGroup>(filesEvent, file, recordAction, claimGroup);
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

		CSVRecord firstClaimLine = csvRecords.get(0);

		int schemaVersion = parseInt(firstClaimLine.get("VERSION"));
		if (RECORD_FORMAT_VERSION != schemaVersion)
			throw new UnsupportedRifVersionException(schemaVersion);
		RecordAction recordAction = RecordAction.match(firstClaimLine.get("DML_IND"));

		CarrierClaim claimGroup = CarrierClaimParser.parseRif(csvRecords);
		/*
		 * FIXME Can't use real line numbers because random/dummy test data has
		 * dupes.
		 */
		for (int fakeLineNumber = 1; fakeLineNumber <= claimGroup.getLines().size(); fakeLineNumber++)
			claimGroup.getLines().get(fakeLineNumber - 1).setLineNumber(new BigDecimal(fakeLineNumber));
		return new RifRecordEvent<CarrierClaim>(filesEvent, file, recordAction, claimGroup);
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
	private static RifRecordEvent<SNFClaimGroup> buildSNFClaimEvent(RifFilesEvent filesEvent, RifFile file,
			List<CSVRecord> csvRecords) {
		if (LOGGER.isTraceEnabled())
			LOGGER.trace(csvRecords.toString());

		CSVRecord firstClaimLine = csvRecords.get(0);

		int schemaVersion = parseInt(firstClaimLine.get(SNFClaimGroup.Column.VERSION));
		if (RECORD_FORMAT_VERSION != schemaVersion)
			throw new UnsupportedRifVersionException(schemaVersion);
		RecordAction recordAction = RecordAction.match(firstClaimLine.get(SNFClaimGroup.Column.DML_IND));

		SNFClaimGroup claimGroup = new SNFClaimGroup();

		/*
		 * Parse the claim header fields.
		 */
		claimGroup.beneficiaryId = firstClaimLine.get(SNFClaimGroup.Column.BENE_ID);
		claimGroup.claimId = firstClaimLine.get(SNFClaimGroup.Column.CLM_ID);
		claimGroup.nearLineRecordIdCode = parseCharacter(
				firstClaimLine.get(SNFClaimGroup.Column.NCH_NEAR_LINE_REC_IDENT_CD));
		claimGroup.claimTypeCode = firstClaimLine.get(SNFClaimGroup.Column.NCH_CLM_TYPE_CD);
		claimGroup.dateFrom = parseDate(firstClaimLine.get(SNFClaimGroup.Column.CLM_FROM_DT));
		claimGroup.dateThrough = parseDate(firstClaimLine.get(SNFClaimGroup.Column.CLM_THRU_DT));
		claimGroup.claimQueryCode = parseCharacter(firstClaimLine.get(SNFClaimGroup.Column.CLAIM_QUERY_CODE));
		claimGroup.weeklyProcessDate = parseDate(firstClaimLine.get(SNFClaimGroup.Column.NCH_WKLY_PROC_DT));
		claimGroup.providerNumber = firstClaimLine.get(SNFClaimGroup.Column.PRVDR_NUM);
		claimGroup.claimFacilityTypeCode = parseCharacter(firstClaimLine.get(SNFClaimGroup.Column.CLM_FAC_TYPE_CD));
		claimGroup.claimServiceClassificationTypeCode = parseCharacter(
				firstClaimLine.get(SNFClaimGroup.Column.CLM_SRVC_CLSFCTN_TYPE_CD));
		claimGroup.claimFrequencyCode = parseCharacter(firstClaimLine.get(SNFClaimGroup.Column.CLM_FREQ_CD));
		claimGroup.claimNonPaymentReasonCode = parseOptString(
				firstClaimLine.get(SNFClaimGroup.Column.CLM_MDCR_NON_PMT_RSN_CD));
		claimGroup.paymentAmount = parseDecimal(firstClaimLine.get(SNFClaimGroup.Column.CLM_PMT_AMT));
		claimGroup.primaryPayerPaidAmount = parseDecimal(
				firstClaimLine.get(SNFClaimGroup.Column.NCH_PRMRY_PYR_CLM_PD_AMT));
		claimGroup.claimPrimaryPayerCode = parseOptCharacter(firstClaimLine.get(SNFClaimGroup.Column.NCH_PRMRY_PYR_CD));
		claimGroup.providerStateCode = firstClaimLine.get(SNFClaimGroup.Column.PRVDR_STATE_CD);
		claimGroup.organizationNpi = parseOptString(firstClaimLine.get(SNFClaimGroup.Column.ORG_NPI_NUM));
		claimGroup.attendingPhysicianNpi = parseOptString(firstClaimLine.get(SNFClaimGroup.Column.AT_PHYSN_NPI));
		claimGroup.operatingPhysicianNpi = parseOptString(firstClaimLine.get(SNFClaimGroup.Column.OP_PHYSN_NPI));
		claimGroup.otherPhysicianNpi = parseOptString(firstClaimLine.get(SNFClaimGroup.Column.OT_PHYSN_NPI));
		claimGroup.mcoPaidSw = parseOptCharacter(firstClaimLine.get(SNFClaimGroup.Column.CLM_MCO_PD_SW));
		claimGroup.patientDischargeStatusCode = firstClaimLine.get(SNFClaimGroup.Column.PTNT_DSCHRG_STUS_CD);
		claimGroup.totalChargeAmount = parseDecimal(firstClaimLine.get(SNFClaimGroup.Column.CLM_TOT_CHRG_AMT));
		claimGroup.claimAdmissionDate = parseOptDate(firstClaimLine.get(SNFClaimGroup.Column.CLM_ADMSN_DT));
		claimGroup.admissionTypeCd = parseCharacter(firstClaimLine.get(SNFClaimGroup.Column.CLM_IP_ADMSN_TYPE_CD));
		claimGroup.sourceAdmissionCd = parseOptCharacter(firstClaimLine.get(SNFClaimGroup.Column.CLM_SRC_IP_ADMSN_CD));
		claimGroup.patientStatusCd = parseOptCharacter(firstClaimLine.get(SNFClaimGroup.Column.NCH_PTNT_STATUS_IND_CD));
		claimGroup.deductibleAmount = parseDecimal(firstClaimLine.get(SNFClaimGroup.Column.NCH_BENE_IP_DDCTBL_AMT));
		claimGroup.partACoinsuranceLiabilityAmount = parseDecimal(
				firstClaimLine.get(SNFClaimGroup.Column.NCH_BENE_PTA_COINSRNC_LBLTY_AM));
		claimGroup.bloodDeductibleLiabilityAmount = parseDecimal(
				firstClaimLine.get(SNFClaimGroup.Column.NCH_BENE_BLOOD_DDCTBL_LBLTY_AM));
		claimGroup.noncoveredCharge = parseDecimal(firstClaimLine.get(SNFClaimGroup.Column.NCH_IP_NCVRD_CHRG_AMT));
		claimGroup.totalDeductionAmount = parseDecimal(firstClaimLine.get(SNFClaimGroup.Column.NCH_IP_TOT_DDCTN_AMT));
		claimGroup.claimPPSCapitalFSPAmount = Optional
				.of(parseDecimal(firstClaimLine.get(SNFClaimGroup.Column.CLM_PPS_CPTL_FSP_AMT)));
		claimGroup.claimPPSCapitalOutlierAmount = Optional
				.of(parseDecimal(firstClaimLine.get(SNFClaimGroup.Column.CLM_PPS_CPTL_OUTLIER_AMT)));
		claimGroup.claimPPSCapitalDisproportionateShareAmt = Optional
				.of(parseDecimal(firstClaimLine.get(SNFClaimGroup.Column.CLM_PPS_CPTL_DSPRPRTNT_SHR_AMT)));
		claimGroup.claimPPSCapitalIMEAmount = Optional
				.of(parseDecimal(firstClaimLine.get(SNFClaimGroup.Column.CLM_PPS_CPTL_IME_AMT)));
		claimGroup.claimPPSCapitalExceptionAmount = Optional
				.of(parseDecimal(firstClaimLine.get(SNFClaimGroup.Column.CLM_PPS_CPTL_EXCPTN_AMT)));
		claimGroup.claimPPSOldCapitalHoldHarmlessAmount = Optional
				.of(parseDecimal(firstClaimLine.get(SNFClaimGroup.Column.CLM_PPS_OLD_CPTL_HLD_HRMLS_AMT)));

		claimGroup.utilizationDayCount = parseInt(firstClaimLine.get(SNFClaimGroup.Column.CLM_UTLZTN_DAY_CNT));
		claimGroup.coinsuranceDayCount = parseInt(firstClaimLine.get(SNFClaimGroup.Column.BENE_TOT_COINSRNC_DAYS_CNT));
		claimGroup.nonUtilizationDayCount = parseInt(firstClaimLine.get(SNFClaimGroup.Column.CLM_NON_UTLZTN_DAYS_CNT));
		claimGroup.bloodPintsFurnishedQty = parseInt(
				firstClaimLine.get(SNFClaimGroup.Column.NCH_BLOOD_PNTS_FRNSHD_QTY));
		claimGroup.qualifiedStayFromDate = parseOptDate(
				firstClaimLine.get(SNFClaimGroup.Column.NCH_QLFYD_STAY_FROM_DT));
		claimGroup.qualifiedStayThroughDate = parseOptDate(
				firstClaimLine.get(SNFClaimGroup.Column.NCH_QLFYD_STAY_THRU_DT));
		claimGroup.noncoveredStayFromDate = parseOptDate(
				firstClaimLine.get(SNFClaimGroup.Column.NCH_VRFD_NCVRD_STAY_FROM_DT));
		claimGroup.noncoveredStayThroughDate = parseOptDate(
				firstClaimLine.get(SNFClaimGroup.Column.NCH_VRFD_NCVRD_STAY_THRU_DT));
		claimGroup.coveredCareThoughDate = parseOptDate(
				firstClaimLine.get(SNFClaimGroup.Column.NCH_ACTV_OR_CVRD_LVL_CARE_THRU));
		claimGroup.medicareBenefitsExhaustedDate = parseOptDate(
				firstClaimLine.get(SNFClaimGroup.Column.NCH_BENE_MDCR_BNFTS_EXHTD_DT_I));
		claimGroup.beneficiaryDischargeDate = parseOptDate(firstClaimLine.get(SNFClaimGroup.Column.NCH_BENE_DSCHRG_DT));

		claimGroup.diagnosisRelatedGroupCd = parseOptString(firstClaimLine.get(SNFClaimGroup.Column.CLM_DRG_CD));

		claimGroup.diagnosisAdmitting = parseOptIcdCode(firstClaimLine.get(SNFClaimGroup.Column.ADMTG_DGNS_CD),
				firstClaimLine.get(SNFClaimGroup.Column.ADMTG_DGNS_VRSN_CD));
		claimGroup.diagnosisPrincipal = parseIcdCode(firstClaimLine.get(SNFClaimGroup.Column.PRNCPAL_DGNS_CD),
				firstClaimLine.get(SNFClaimGroup.Column.PRNCPAL_DGNS_VRSN_CD));
		claimGroup.diagnosesAdditional = parseIcdCodes(firstClaimLine, SNFClaimGroup.Column.ICD_DGNS_CD1.ordinal(),
				SNFClaimGroup.Column.ICD_DGNS_VRSN_CD25.ordinal());
		claimGroup.diagnosisFirstClaimExternal = parseOptIcdCode(firstClaimLine.get(SNFClaimGroup.Column.FST_DGNS_E_CD),
				firstClaimLine.get(SNFClaimGroup.Column.FST_DGNS_E_VRSN_CD));
		claimGroup.diagnosesExternal = parseIcdCodes(firstClaimLine, SNFClaimGroup.Column.ICD_DGNS_E_CD1.ordinal(),
				SNFClaimGroup.Column.ICD_DGNS_E_VRSN_CD12.ordinal());
		claimGroup.procedureCodes = parseIcdCodesProcedure(firstClaimLine, SNFClaimGroup.Column.ICD_PRCDR_CD1.ordinal(),
				SNFClaimGroup.Column.ICD_PRCDR_VRSN_CD25.ordinal());
		/*
		 * Parse the claim lines.
		 */
		for (CSVRecord claimLineRecord : csvRecords) {
			SNFClaimLine claimLine = new SNFClaimLine();
			claimLine.lineNumber = parseInt(claimLineRecord.get(SNFClaimGroup.Column.CLM_LINE_NUM));
			claimLine.revenueCenter = claimLineRecord.get(SNFClaimGroup.Column.REV_CNTR);
			claimLine.hcpcsCode = parseOptString(claimLineRecord.get(SNFClaimGroup.Column.HCPCS_CD));
			claimLine.unitCount = parseDecimal(claimLineRecord.get(SNFClaimGroup.Column.REV_CNTR_UNIT_CNT));
			claimLine.rateAmount = parseDecimal(claimLineRecord.get(SNFClaimGroup.Column.REV_CNTR_RATE_AMT));
			claimLine.totalChargeAmount = parseDecimal(claimLineRecord.get(SNFClaimGroup.Column.REV_CNTR_TOT_CHRG_AMT));
			claimLine.nonCoveredChargeAmount = parseDecimal(
					claimLineRecord.get(SNFClaimGroup.Column.REV_CNTR_NCVRD_CHRG_AMT));
			claimLine.deductibleCoinsuranceCd = parseOptCharacter(
					claimLineRecord.get(SNFClaimGroup.Column.REV_CNTR_DDCTBL_COINSRNC_CD));
			claimLine.nationalDrugCodeQuantity = parseOptDecimal(
					claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR_NDC_QTY));
			claimLine.nationalDrugCodeQualifierCode = parseOptString(
					claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR_NDC_QTY_QLFR_CD));
			claimLine.revenueCenterRenderingPhysicianNPI = parseOptString(
					claimLineRecord.get(SNFClaimGroup.Column.RNDRNG_PHYSN_NPI));

			claimGroup.lines.add(claimLine);
		}

		return new RifRecordEvent<SNFClaimGroup>(filesEvent, file, recordAction, claimGroup);
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
	private static RifRecordEvent<HospiceClaimGroup> buildHospiceClaimEvent(RifFilesEvent filesEvent, RifFile file,
			List<CSVRecord> csvRecords) {
		if (LOGGER.isTraceEnabled())
			LOGGER.trace(csvRecords.toString());

		CSVRecord firstClaimLine = csvRecords.get(0);

		int schemaVersion = parseInt(firstClaimLine.get(HospiceClaimGroup.Column.VERSION));
		if (RECORD_FORMAT_VERSION != schemaVersion)
			throw new UnsupportedRifVersionException(schemaVersion);
		RecordAction recordAction = RecordAction.match(firstClaimLine.get(HospiceClaimGroup.Column.DML_IND));

		HospiceClaimGroup claimGroup = new HospiceClaimGroup();

		/*
		 * Parse the claim header fields.
		 */
		claimGroup.beneficiaryId = firstClaimLine.get(HospiceClaimGroup.Column.BENE_ID);
		claimGroup.claimId = firstClaimLine.get(HospiceClaimGroup.Column.CLM_ID);
		claimGroup.nearLineRecordIdCode = parseCharacter(
				firstClaimLine.get(HospiceClaimGroup.Column.NCH_NEAR_LINE_REC_IDENT_CD));
		claimGroup.claimTypeCode = firstClaimLine.get(HospiceClaimGroup.Column.NCH_CLM_TYPE_CD);
		claimGroup.dateFrom = parseDate(firstClaimLine.get(HospiceClaimGroup.Column.CLM_FROM_DT));
		claimGroup.dateThrough = parseDate(firstClaimLine.get(HospiceClaimGroup.Column.CLM_THRU_DT));
		claimGroup.weeklyProcessDate = parseDate(firstClaimLine.get(HospiceClaimGroup.Column.NCH_WKLY_PROC_DT));
		claimGroup.providerNumber = firstClaimLine.get(HospiceClaimGroup.Column.PRVDR_NUM);
		claimGroup.claimFacilityTypeCode = parseCharacter(firstClaimLine.get(HospiceClaimGroup.Column.CLM_FAC_TYPE_CD));
		claimGroup.claimServiceClassificationTypeCode = parseCharacter(
				firstClaimLine.get(HospiceClaimGroup.Column.CLM_SRVC_CLSFCTN_TYPE_CD));
		claimGroup.claimFrequencyCode = parseCharacter(firstClaimLine.get(HospiceClaimGroup.Column.CLM_FREQ_CD));
		claimGroup.claimNonPaymentReasonCode = parseOptString(
				firstClaimLine.get(HospiceClaimGroup.Column.CLM_MDCR_NON_PMT_RSN_CD));
		claimGroup.paymentAmount = parseDecimal(firstClaimLine.get(HospiceClaimGroup.Column.CLM_PMT_AMT));
		claimGroup.primaryPayerPaidAmount = parseDecimal(
				firstClaimLine.get(HospiceClaimGroup.Column.NCH_PRMRY_PYR_CLM_PD_AMT));
		claimGroup.claimPrimaryPayerCode = parseOptCharacter(
				firstClaimLine.get(HospiceClaimGroup.Column.NCH_PRMRY_PYR_CD));
		claimGroup.providerStateCode = firstClaimLine.get(HospiceClaimGroup.Column.PRVDR_STATE_CD);
		claimGroup.organizationNpi = parseOptString(firstClaimLine.get(HospiceClaimGroup.Column.ORG_NPI_NUM));
		claimGroup.attendingPhysicianNpi = parseOptString(firstClaimLine.get(HospiceClaimGroup.Column.AT_PHYSN_NPI));
		claimGroup.patientDischargeStatusCode = firstClaimLine.get(HospiceClaimGroup.Column.PTNT_DSCHRG_STUS_CD);
		claimGroup.totalChargeAmount = parseDecimal(firstClaimLine.get(HospiceClaimGroup.Column.CLM_TOT_CHRG_AMT));
		claimGroup.patientStatusCd = parseOptCharacter(
				firstClaimLine.get(HospiceClaimGroup.Column.NCH_PTNT_STATUS_IND_CD));
		claimGroup.utilizationDayCount = parseInt(firstClaimLine.get(HospiceClaimGroup.Column.CLM_UTLZTN_DAY_CNT));
		claimGroup.beneficiaryDischargeDate = parseOptDate(
				firstClaimLine.get(HospiceClaimGroup.Column.NCH_BENE_DSCHRG_DT));
		claimGroup.diagnosisPrincipal = parseOptIcdCode(firstClaimLine.get(HospiceClaimGroup.Column.PRNCPAL_DGNS_CD),
				firstClaimLine.get(HospiceClaimGroup.Column.PRNCPAL_DGNS_VRSN_CD));
		claimGroup.diagnosesAdditional = parseIcdCodes(firstClaimLine, HospiceClaimGroup.Column.ICD_DGNS_CD1.ordinal(),
				HospiceClaimGroup.Column.ICD_DGNS_VRSN_CD25.ordinal());
		claimGroup.diagnosisFirstClaimExternal = parseOptIcdCode(
				firstClaimLine.get(HospiceClaimGroup.Column.FST_DGNS_E_CD),
				firstClaimLine.get(HospiceClaimGroup.Column.FST_DGNS_E_VRSN_CD));
		claimGroup.diagnosesExternal = parseIcdCodes(firstClaimLine, HospiceClaimGroup.Column.ICD_DGNS_E_CD1.ordinal(),
				HospiceClaimGroup.Column.ICD_DGNS_E_VRSN_CD12.ordinal());
		claimGroup.claimHospiceStartDate = parseOptDate(
				firstClaimLine.get(HospiceClaimGroup.Column.CLM_HOSPC_START_DT_ID));

		/*
		 * Parse the claim lines.
		 */
		for (CSVRecord claimLineRecord : csvRecords) {
			HospiceClaimLine claimLine = new HospiceClaimLine();

			claimLine.lineNumber = parseInt(claimLineRecord.get(HospiceClaimGroup.Column.CLM_LINE_NUM));
			claimLine.revenueCenter = claimLineRecord.get(HospiceClaimGroup.Column.REV_CNTR);
			claimLine.hcpcsCode = parseOptString(claimLineRecord.get(HospiceClaimGroup.Column.HCPCS_CD));
			claimLine.hcpcsInitialModifierCode = parseOptString(
					claimLineRecord.get(HospiceClaimGroup.Column.HCPCS_1ST_MDFR_CD));
			claimLine.hcpcsSecondModifierCode = parseOptString(
					claimLineRecord.get(HospiceClaimGroup.Column.HCPCS_2ND_MDFR_CD));
			claimLine.unitCount = parseDecimal(claimLineRecord.get(HospiceClaimGroup.Column.REV_CNTR_UNIT_CNT));
			claimLine.rateAmount = parseDecimal(claimLineRecord.get(HospiceClaimGroup.Column.REV_CNTR_RATE_AMT));
			claimLine.providerPaymentAmount = parseDecimal(
					claimLineRecord.get(HospiceClaimGroup.Column.REV_CNTR_PRVDR_PMT_AMT));
			claimLine.benficiaryPaymentAmount = parseDecimal(
					claimLineRecord.get(HospiceClaimGroup.Column.REV_CNTR_BENE_PMT_AMT));
			claimLine.paymentAmount = parseDecimal(claimLineRecord.get(HospiceClaimGroup.Column.REV_CNTR_PMT_AMT_AMT));
			claimLine.totalChargeAmount = parseDecimal(
					claimLineRecord.get(HospiceClaimGroup.Column.REV_CNTR_TOT_CHRG_AMT));
			claimLine.deductibleCoinsuranceCd = parseOptCharacter(
					claimLineRecord.get(HospiceClaimGroup.Column.REV_CNTR_DDCTBL_COINSRNC_CD));
			claimLine.nonCoveredChargeAmount = parseDecimal(
					claimLineRecord.get(HospiceClaimGroup.Column.REV_CNTR_NCVRD_CHRG_AMT));
			claimLine.nationalDrugCodeQuantity = parseOptDecimal(
					claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR_NDC_QTY));
			claimLine.nationalDrugCodeQualifierCode = parseOptString(
					claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR_NDC_QTY_QLFR_CD));
			claimLine.revenueCenterRenderingPhysicianNPI = parseOptString(
					claimLineRecord.get(HospiceClaimGroup.Column.RNDRNG_PHYSN_NPI));

			claimGroup.lines.add(claimLine);
		}

		return new RifRecordEvent<HospiceClaimGroup>(filesEvent, file, recordAction, claimGroup);
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
	private static RifRecordEvent<HHAClaimGroup> buildHHAClaimEvent(RifFilesEvent filesEvent, RifFile file,
			List<CSVRecord> csvRecords) {
		if (LOGGER.isTraceEnabled())
			LOGGER.trace(csvRecords.toString());

		CSVRecord firstClaimLine = csvRecords.get(0);

		int schemaVersion = parseInt(firstClaimLine.get(HHAClaimGroup.Column.VERSION));
		if (RECORD_FORMAT_VERSION != schemaVersion)
			throw new UnsupportedRifVersionException(schemaVersion);
		RecordAction recordAction = RecordAction.match(firstClaimLine.get(HHAClaimGroup.Column.DML_IND));

		HHAClaimGroup claimGroup = new HHAClaimGroup();

		/*
		 * Parse the claim header fields.
		 */
		claimGroup.beneficiaryId = firstClaimLine.get(HHAClaimGroup.Column.BENE_ID);
		claimGroup.claimId = firstClaimLine.get(HHAClaimGroup.Column.CLM_ID);
		claimGroup.nearLineRecordIdCode = parseCharacter(
				firstClaimLine.get(HHAClaimGroup.Column.NCH_NEAR_LINE_REC_IDENT_CD));
		claimGroup.claimTypeCode = firstClaimLine.get(HHAClaimGroup.Column.NCH_CLM_TYPE_CD);
		claimGroup.dateFrom = parseDate(firstClaimLine.get(HHAClaimGroup.Column.CLM_FROM_DT));
		claimGroup.dateThrough = parseDate(firstClaimLine.get(HHAClaimGroup.Column.CLM_THRU_DT));
		claimGroup.weeklyProcessDate = parseDate(firstClaimLine.get(HHAClaimGroup.Column.NCH_WKLY_PROC_DT));
		claimGroup.providerNumber = firstClaimLine.get(HHAClaimGroup.Column.PRVDR_NUM);
		claimGroup.claimFacilityTypeCode = parseCharacter(firstClaimLine.get(HHAClaimGroup.Column.CLM_FAC_TYPE_CD));
		claimGroup.claimServiceClassificationTypeCode = parseCharacter(
				firstClaimLine.get(HHAClaimGroup.Column.CLM_SRVC_CLSFCTN_TYPE_CD));
		claimGroup.claimFrequencyCode = parseCharacter(firstClaimLine.get(HHAClaimGroup.Column.CLM_FREQ_CD));
		claimGroup.claimNonPaymentReasonCode = parseOptString(
				firstClaimLine.get(HHAClaimGroup.Column.CLM_MDCR_NON_PMT_RSN_CD));
		claimGroup.paymentAmount = parseDecimal(firstClaimLine.get(HHAClaimGroup.Column.CLM_PMT_AMT));
		claimGroup.primaryPayerPaidAmount = parseDecimal(
				firstClaimLine.get(HHAClaimGroup.Column.NCH_PRMRY_PYR_CLM_PD_AMT));
		claimGroup.claimPrimaryPayerCode = parseOptCharacter(firstClaimLine.get(HHAClaimGroup.Column.NCH_PRMRY_PYR_CD));
		claimGroup.providerStateCode = firstClaimLine.get(HHAClaimGroup.Column.PRVDR_STATE_CD);
		claimGroup.organizationNpi = parseOptString(firstClaimLine.get(HHAClaimGroup.Column.ORG_NPI_NUM));
		claimGroup.attendingPhysicianNpi = parseOptString(firstClaimLine.get(HHAClaimGroup.Column.AT_PHYSN_NPI));
		claimGroup.patientDischargeStatusCode = firstClaimLine.get(HHAClaimGroup.Column.PTNT_DSCHRG_STUS_CD);
		claimGroup.totalChargeAmount = parseDecimal(firstClaimLine.get(HHAClaimGroup.Column.CLM_TOT_CHRG_AMT));
		claimGroup.diagnosisPrincipal = parseIcdCode(firstClaimLine.get(HHAClaimGroup.Column.PRNCPAL_DGNS_CD),
				firstClaimLine.get(HHAClaimGroup.Column.PRNCPAL_DGNS_VRSN_CD));
		claimGroup.diagnosesAdditional = parseIcdCodes(firstClaimLine, HHAClaimGroup.Column.ICD_DGNS_CD1.ordinal(),
				HHAClaimGroup.Column.ICD_DGNS_VRSN_CD25.ordinal());
		claimGroup.diagnosisFirstClaimExternal = parseOptIcdCode(firstClaimLine.get(HHAClaimGroup.Column.FST_DGNS_E_CD),
				firstClaimLine.get(HHAClaimGroup.Column.FST_DGNS_E_VRSN_CD));
		claimGroup.diagnosesExternal = parseIcdCodes(firstClaimLine, HHAClaimGroup.Column.ICD_DGNS_E_CD1.ordinal(),
				HHAClaimGroup.Column.ICD_DGNS_E_VRSN_CD12.ordinal());
		claimGroup.claimLUPACode = parseOptCharacter(firstClaimLine.get(HHAClaimGroup.Column.CLM_HHA_LUPA_IND_CD));
		claimGroup.claimReferralCode = parseOptCharacter(firstClaimLine.get(HHAClaimGroup.Column.CLM_HHA_RFRL_CD));
		claimGroup.totalVisitCount = parseInt(firstClaimLine.get(HHAClaimGroup.Column.CLM_HHA_TOT_VISIT_CNT));
		claimGroup.careStartDate = parseOptDate(firstClaimLine.get(HHAClaimGroup.Column.CLM_ADMSN_DT));

		/*
		 * Parse the claim lines.
		 */
		for (CSVRecord claimLineRecord : csvRecords) {
			HHAClaimLine claimLine = new HHAClaimLine();

			claimLine.lineNumber = parseInt(claimLineRecord.get(HHAClaimGroup.Column.CLM_LINE_NUM));
			claimLine.revenueCenter = claimLineRecord.get(HHAClaimGroup.Column.REV_CNTR);
			claimLine.revCntr1stAnsiCd = parseOptString(claimLineRecord.get(HHAClaimGroup.Column.REV_CNTR_1ST_ANSI_CD));
			claimLine.hcpcsCode = parseOptString(claimLineRecord.get(HHAClaimGroup.Column.HCPCS_CD));
			claimLine.hcpcsInitialModifierCode = parseOptString(
					claimLineRecord.get(HHAClaimGroup.Column.HCPCS_1ST_MDFR_CD));
			claimLine.hcpcsSecondModifierCode = parseOptString(
					claimLineRecord.get(HHAClaimGroup.Column.HCPCS_2ND_MDFR_CD));
			claimLine.unitCount = parseDecimal(claimLineRecord.get(HHAClaimGroup.Column.REV_CNTR_UNIT_CNT));
			claimLine.rateAmount = parseDecimal(claimLineRecord.get(HHAClaimGroup.Column.REV_CNTR_RATE_AMT));
			claimLine.paymentAmount = parseDecimal(claimLineRecord.get(HHAClaimGroup.Column.REV_CNTR_PMT_AMT_AMT));
			claimLine.totalChargeAmount = parseDecimal(claimLineRecord.get(HHAClaimGroup.Column.REV_CNTR_TOT_CHRG_AMT));
			claimLine.nonCoveredChargeAmount = parseDecimal(
					claimLineRecord.get(HHAClaimGroup.Column.REV_CNTR_NCVRD_CHRG_AMT));
			claimLine.deductibleCoinsuranceCd = parseOptCharacter(
					claimLineRecord.get(HHAClaimGroup.Column.REV_CNTR_DDCTBL_COINSRNC_CD));
			claimLine.nationalDrugCodeQuantity = parseOptInteger(
					claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR_NDC_QTY));
			claimLine.nationalDrugCodeQualifierCode = parseOptString(
					claimLineRecord.get(OutpatientClaimGroup.Column.REV_CNTR_NDC_QTY_QLFR_CD));
			claimLine.revenueCenterRenderingPhysicianNPI = parseOptString(
					claimLineRecord.get(HHAClaimGroup.Column.RNDRNG_PHYSN_NPI));

			claimGroup.lines.add(claimLine);
		}

		return new RifRecordEvent<HHAClaimGroup>(filesEvent, file, recordAction, claimGroup);
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
	private static RifRecordEvent<DMEClaimGroup> buildDMEClaimEvent(RifFilesEvent filesEvent, RifFile file,
			List<CSVRecord> csvRecords) {
		if (LOGGER.isTraceEnabled())
			LOGGER.trace(csvRecords.toString());

		CSVRecord firstClaimLine = csvRecords.get(0);

		int schemaVersion = parseInt(firstClaimLine.get(DMEClaimGroup.Column.VERSION));
		if (RECORD_FORMAT_VERSION != schemaVersion)
			throw new UnsupportedRifVersionException(schemaVersion);
		RecordAction recordAction = RecordAction.match(firstClaimLine.get(DMEClaimGroup.Column.DML_IND));

		DMEClaimGroup claimGroup = new DMEClaimGroup();

		/*
		 * Parse the claim header fields.
		 */
		claimGroup.beneficiaryId = firstClaimLine.get(DMEClaimGroup.Column.BENE_ID);
		claimGroup.claimId = firstClaimLine.get(DMEClaimGroup.Column.CLM_ID);
		claimGroup.nearLineRecordIdCode = parseCharacter(
				firstClaimLine.get(DMEClaimGroup.Column.NCH_NEAR_LINE_REC_IDENT_CD));
		claimGroup.claimTypeCode = firstClaimLine.get(DMEClaimGroup.Column.NCH_CLM_TYPE_CD);
		claimGroup.dateFrom = parseDate(firstClaimLine.get(DMEClaimGroup.Column.CLM_FROM_DT));
		claimGroup.dateThrough = parseDate(firstClaimLine.get(DMEClaimGroup.Column.CLM_THRU_DT));
		claimGroup.weeklyProcessDate = parseDate(firstClaimLine.get(DMEClaimGroup.Column.NCH_WKLY_PROC_DT));
		claimGroup.claimEntryCode = parseCharacter(firstClaimLine.get(DMEClaimGroup.Column.CARR_CLM_ENTRY_CD));
		claimGroup.claimDispositionCode = firstClaimLine.get(DMEClaimGroup.Column.CLM_DISP_CD);
		claimGroup.carrierNumber = firstClaimLine.get(DMEClaimGroup.Column.CARR_NUM);
		claimGroup.paymentDenialCode = firstClaimLine.get(DMEClaimGroup.Column.CARR_CLM_PMT_DNL_CD);
		claimGroup.paymentAmount = parseDecimal(firstClaimLine.get(DMEClaimGroup.Column.CLM_PMT_AMT));
		claimGroup.primaryPayerPaidAmount = parseDecimal(
				firstClaimLine.get(DMEClaimGroup.Column.CARR_CLM_PRMRY_PYR_PD_AMT));
		claimGroup.providerAssignmentIndicator = parseCharacter(
				firstClaimLine.get(DMEClaimGroup.Column.CARR_CLM_PRVDR_ASGNMT_IND_SW));
		claimGroup.providerPaymentAmount = parseDecimal(firstClaimLine.get(DMEClaimGroup.Column.NCH_CLM_PRVDR_PMT_AMT));
		claimGroup.beneficiaryPaymentAmount = parseDecimal(
				firstClaimLine.get(DMEClaimGroup.Column.NCH_CLM_BENE_PMT_AMT));
		claimGroup.submittedChargeAmount = parseDecimal(
				firstClaimLine.get(DMEClaimGroup.Column.NCH_CARR_CLM_SBMTD_CHRG_AMT));
		claimGroup.allowedChargeAmount = parseDecimal(firstClaimLine.get(DMEClaimGroup.Column.NCH_CARR_CLM_ALOWD_AMT));
		claimGroup.beneficiaryPartBDeductAmount = parseDecimal(
				firstClaimLine.get(DMEClaimGroup.Column.CARR_CLM_CASH_DDCTBL_APLD_AMT));
		claimGroup.hcpcsYearCode = parseOptCharacter(firstClaimLine.get(DMEClaimGroup.Column.CARR_CLM_HCPCS_YR_CD));
		claimGroup.diagnosisPrincipal = parseOptIcdCode(firstClaimLine.get(DMEClaimGroup.Column.PRNCPAL_DGNS_CD),
				firstClaimLine.get(DMEClaimGroup.Column.PRNCPAL_DGNS_VRSN_CD));
		claimGroup.diagnosesAdditional = parseIcdCodes(firstClaimLine, DMEClaimGroup.Column.ICD_DGNS_CD1.ordinal(),
				DMEClaimGroup.Column.ICD_DGNS_VRSN_CD12.ordinal());
		claimGroup.referringPhysicianNpi = parseOptString(firstClaimLine.get(DMEClaimGroup.Column.RFR_PHYSN_NPI));
		claimGroup.clinicalTrialNumber = parseOptString(firstClaimLine.get(DMEClaimGroup.Column.CLM_CLNCL_TRIL_NUM));

		/*
		 * Parse the claim lines.
		 */
		for (CSVRecord claimLineRecord : csvRecords) {
			DMEClaimLine claimLine = new DMEClaimLine();
			claimLine.number = parseInt(claimLineRecord.get(DMEClaimGroup.Column.LINE_NUM));
			claimLine.providerTaxNumber = claimLineRecord.get(DMEClaimGroup.Column.TAX_NUM);
			claimLine.providerSpecialityCode = parseOptString(claimLineRecord.get(DMEClaimGroup.Column.PRVDR_SPCLTY));
			claimLine.providerParticipatingIndCode = parseOptCharacter(
					claimLineRecord.get(DMEClaimGroup.Column.PRTCPTNG_IND_CD));
			claimLine.serviceCount = parseDecimal(claimLineRecord.get(DMEClaimGroup.Column.LINE_SRVC_CNT));
			claimLine.cmsServiceTypeCode = claimLineRecord.get(DMEClaimGroup.Column.LINE_CMS_TYPE_SRVC_CD);
			claimLine.placeOfServiceCode = claimLineRecord.get(DMEClaimGroup.Column.LINE_PLACE_OF_SRVC_CD);
			claimLine.firstExpenseDate = parseOptDate(claimLineRecord.get(DMEClaimGroup.Column.LINE_1ST_EXPNS_DT));
			claimLine.lastExpenseDate = parseOptDate(claimLineRecord.get(DMEClaimGroup.Column.LINE_LAST_EXPNS_DT));
			claimLine.hcpcsCode = parseOptString(claimLineRecord.get(DMEClaimGroup.Column.HCPCS_CD));
			claimLine.hcpcsInitialModifierCode = parseOptString(
					claimLineRecord.get(DMEClaimGroup.Column.HCPCS_1ST_MDFR_CD));
			claimLine.hcpcsSecondModifierCode = parseOptString(
					claimLineRecord.get(DMEClaimGroup.Column.HCPCS_2ND_MDFR_CD));
			claimLine.hcpcsThirdModifierCode = parseOptString(
					claimLineRecord.get(DMEClaimGroup.Column.HCPCS_3RD_MDFR_CD));
			claimLine.hcpcsFourthModifierCode = parseOptString(
					claimLineRecord.get(DMEClaimGroup.Column.HCPCS_4TH_MDFR_CD));
			claimLine.betosCode = parseOptString(claimLineRecord.get(DMEClaimGroup.Column.BETOS_CD));
			claimLine.paymentAmount = parseDecimal(claimLineRecord.get(DMEClaimGroup.Column.LINE_NCH_PMT_AMT));
			claimLine.beneficiaryPaymentAmount = parseDecimal(
					claimLineRecord.get(DMEClaimGroup.Column.LINE_BENE_PMT_AMT));
			claimLine.providerPaymentAmount = parseDecimal(
					claimLineRecord.get(DMEClaimGroup.Column.LINE_PRVDR_PMT_AMT));
			claimLine.beneficiaryPartBDeductAmount = parseDecimal(
					claimLineRecord.get(DMEClaimGroup.Column.LINE_BENE_PTB_DDCTBL_AMT));
			claimLine.primaryPayerCode = parseOptCharacter(
					claimLineRecord.get(DMEClaimGroup.Column.LINE_BENE_PRMRY_PYR_CD));
			claimLine.primaryPayerPaidAmount = parseDecimal(
					claimLineRecord.get(DMEClaimGroup.Column.LINE_BENE_PRMRY_PYR_PD_AMT));
			claimLine.coinsuranceAmount = parseDecimal(claimLineRecord.get(DMEClaimGroup.Column.LINE_COINSRNC_AMT));
			claimLine.primaryPayerAllowedChargeAmount = parseDecimal(
					claimLineRecord.get(DMEClaimGroup.Column.LINE_PRMRY_ALOWD_CHRG_AMT));
			claimLine.submittedChargeAmount = parseDecimal(
					claimLineRecord.get(DMEClaimGroup.Column.LINE_SBMTD_CHRG_AMT));
			claimLine.allowedChargeAmount = parseDecimal(claimLineRecord.get(DMEClaimGroup.Column.LINE_ALOWD_CHRG_AMT));
			claimLine.processingIndicatorCode = parseOptString(
					claimLineRecord.get(DMEClaimGroup.Column.LINE_PRCSG_IND_CD));
			claimLine.paymentCode = parseOptCharacter(claimLineRecord.get(DMEClaimGroup.Column.LINE_PMT_80_100_CD));
			claimLine.serviceDeductibleCode = parseOptCharacter(
					claimLineRecord.get(DMEClaimGroup.Column.LINE_SERVICE_DEDUCTIBLE));
			claimLine.diagnosis = parseOptIcdCode(claimLineRecord.get(DMEClaimGroup.Column.LINE_ICD_DGNS_CD),
					claimLineRecord.get(DMEClaimGroup.Column.LINE_ICD_DGNS_VRSN_CD));
			claimLine.purchasePriceAmount = parseDecimal(
					claimLineRecord.get(DMEClaimGroup.Column.LINE_DME_PRCHS_PRICE_AMT));
			claimLine.providerNPI = claimLineRecord.get(DMEClaimGroup.Column.PRVDR_NPI);
			claimLine.pricingStateCode = parseOptString(
					claimLineRecord.get(DMEClaimGroup.Column.DMERC_LINE_PRCNG_STATE_CD));
			claimLine.providerStateCode = claimLineRecord.get(DMEClaimGroup.Column.PRVDR_STATE_CD);
			claimLine.supplierTypeCode = parseOptCharacter(
					claimLineRecord.get(DMEClaimGroup.Column.DMERC_LINE_SUPPLR_TYPE_CD));
			claimLine.screenSavingsAmount = parseDecimal(
					claimLineRecord.get(DMEClaimGroup.Column.DMERC_LINE_SCRN_SVGS_AMT));
			claimLine.mtusCount = parseDecimal(claimLineRecord.get(DMEClaimGroup.Column.DMERC_LINE_MTUS_CNT));
			claimLine.mtusCode = parseOptCharacter(claimLineRecord.get(DMEClaimGroup.Column.DMERC_LINE_MTUS_CD));
			claimLine.hctHgbTestResult = parseDecimal(claimLineRecord.get(DMEClaimGroup.Column.LINE_HCT_HGB_RSLT_NUM));
			claimLine.hctHgbTestTypeCode = parseOptString(
					claimLineRecord.get(DMEClaimGroup.Column.LINE_HCT_HGB_TYPE_CD));
			claimLine.nationalDrugCode = parseOptString(claimLineRecord.get(DMEClaimGroup.Column.LINE_NDC_CD));

			claimGroup.lines.add(claimLine);
		}

		return new RifRecordEvent<DMEClaimGroup>(filesEvent, file, recordAction, claimGroup);
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
		try {
			return Integer.parseInt(intText);
		} catch (NumberFormatException e) {
			throw new InvalidRifValueException(String.format("Unable to parse integer value: '%s'.", intText), e);
		}
	}

	/**
	 * @param intText
	 *            the number string to parse
	 * @return an {@link Optional} populated with an {@link Integer} if the
	 *         input has data, or an empty Optional if not
	 */
	private static Optional<Integer> parseOptInteger(String intText) {
		if (intText.isEmpty()) {
			return Optional.empty();
		} else {
			return Optional.of(parseInt(intText));
		}
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
		if (decimalText.isEmpty()) {
			return new BigDecimal(0);
		} else {
			try {
				return new BigDecimal(decimalText);
			} catch (NumberFormatException e) {
				throw new InvalidRifValueException(String.format("Unable to parse decimal value: '%s'.", decimalText),
						e);
			}
		}
	}

	/**
	 * @param decimalText
	 *            the decimal string to parse
	 * @return the result of {@link #parseDecimal(String)} if the specified text
	 *         isn't empty, or an empty Optional if it is empty
	 */
	private static Optional<BigDecimal> parseOptDecimal(String decimalText) {
		if (decimalText.isEmpty()) {
			return Optional.empty();
		} else {
			return Optional.of(parseDecimal(decimalText));
		}
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

		try {
			LocalDate dateFrom = LocalDate.parse(dateText, RIF_DATE_FORMATTER);
			return dateFrom;
		} catch (DateTimeParseException e) {
			throw new InvalidRifValueException(String.format("Unable to parse date value: '%s'.", dateText), e);
		}
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

		if (charText.length() != 1)
			throw new InvalidRifValueException(String.format("Unable to parse character value: '%s'.", charText));

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
	 * @param icdCode
	 *            the value to use for {@link IcdCode#getCode()}
	 * @param icdVersion
	 *            the value to parse and use for {@link IcdCode#getVersion()}
	 * @param icdProcedureDate
	 *            the value to parse and use for
	 *            {@link IcdCode#getProcedureDate()}
	 * @return an {@link IcdCode} instance built from the specified values
	 */
	private static IcdCode parseIcdCode(String icdCode, String icdVersion, LocalDate icdProcedureDate) {
		return new IcdCode(IcdVersion.parse(icdVersion), icdCode, icdProcedureDate);
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
			throw new BadCodeMonkeyException(
					String.format("ICD column last value ( '%s' )  is before first ICD column id ( '%s' )",
							icdColumnLast, icdColumnFirst));
		if ((icdColumnLast - icdColumnFirst + 1) % 2 != 0)
			throw new BadCodeMonkeyException(String.format(
					"ICD column last value ( '%s' ) and first ICD column id ( '%s' ) are not divisible by 2",
					icdColumnLast, icdColumnFirst));

		List<IcdCode> icdCodes = new LinkedList<>();
		for (int i = icdColumnFirst; i < icdColumnLast; i += 2) {
			String icdCodeText = csvRecord.get(i);
			String icdVersionText = csvRecord.get(i + 1);

			if (icdCodeText.isEmpty() && icdVersionText.isEmpty())
				continue;
			else if (!icdCodeText.isEmpty() && !icdVersionText.isEmpty())
				icdCodes.add(parseIcdCode(icdCodeText, icdVersionText));
			else
				throw new InvalidRifFileFormatException(
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
			throw new BadCodeMonkeyException(
					String.format("ICD column last value ( '%s' )  is before first ICD column id ( '%s' )",
							icdColumnLast, icdColumnFirst));
		if ((icdColumnLast - icdColumnFirst + 2) % 3 != 0)
			throw new BadCodeMonkeyException(String.format(
					"ICD column last value ( '%s' ) and first ICD column id ( '%s' ) are not divisible by 3",
					icdColumnLast, icdColumnFirst));

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
				throw new InvalidRifFileFormatException(
						String.format("Unexpected ICD code/ver/poa : '%s' and '%s' and '%s'.", icdCodeText,
								icdVersionText, icdPresentOnAdmissionCode));
		}

		return icdCodes;
	}

	/**
	 * Parses {@link IcdCode}s out of the specified columns of the specified
	 * {@link CSVRecord}. The columns must be arranged in order of code, version
	 * and procedure date; the first column specified must represent an ICD
	 * code, the next column must represent an ICD version (as can be parsed by
	 * {@link IcdVersion#parse(String)}), next column is procedure date and this
	 * sequence must repeat for all of the specified columns.
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
	private static List<IcdCode> parseIcdCodesProcedure(CSVRecord csvRecord, int icdColumnFirst, int icdColumnLast) {
		if ((icdColumnLast - icdColumnFirst) < 1)
			throw new BadCodeMonkeyException(
					String.format("Procedure ICD column last value ( '%s' )  is before first ICD column id ( '%s' )",
							icdColumnLast, icdColumnFirst));
		if ((icdColumnLast - icdColumnFirst + 2) % 3 != 0)
			throw new BadCodeMonkeyException(String.format(
					"Procedure ICD column last value ( '%s' ) and first ICD column id ( '%s' ) are not divisible by 3",
					icdColumnLast, icdColumnFirst));

		List<IcdCode> icdCodes = new LinkedList<>();
		for (int i = icdColumnFirst; i < icdColumnLast; i += 3) {
			String icdCodeText = csvRecord.get(i);
			String icdVersionText = csvRecord.get(i + 1);
			LocalDate icdProcedureDate = null;
			if (!csvRecord.get(i + 2).isEmpty()) {
				icdProcedureDate = LocalDate.parse(csvRecord.get(i + 2), formatter);
			}
			if (icdCodeText.isEmpty() && icdVersionText.isEmpty())
				continue;
			else if (!icdCodeText.isEmpty() && !icdVersionText.isEmpty() && !icdProcedureDate.toString().isEmpty())
				icdCodes.add(parseIcdCode(icdCodeText, icdVersionText, icdProcedureDate));
			else
				throw new InvalidRifFileFormatException(
						String.format("Unexpected Procedure ICD code/ver/date : '%s' and '%s' and '%s'.", icdCodeText,
								icdVersionText, icdProcedureDate));
		}

		return icdCodes;
	}
}
