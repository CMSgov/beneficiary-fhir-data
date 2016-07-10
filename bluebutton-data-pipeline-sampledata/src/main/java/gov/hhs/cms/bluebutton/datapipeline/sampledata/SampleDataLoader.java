package gov.hhs.cms.bluebutton.datapipeline.sampledata;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.AllClaimsProfile;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.ClaimType;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.CurrentBeneficiary;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.DiagnosisRelatedGroup;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartAClaimFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartAClaimRevLineFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartBClaimFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartBClaimLineFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartDEventFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.Procedure;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.SynpufArchive;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.SynpufSample;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.SynpufSampleLoader;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.columns.SynpufColumnForBeneficiarySummary;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.columns.SynpufColumnForCarrierClaims;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.columns.SynpufColumnForInpatientClaims;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.columns.SynpufColumnForOutpatientClaims;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.columns.SynpufColumnForPartDClaims;
import gov.hhs.cms.bluebutton.datapipeline.sampledata.addresses.SampleAddress;
import gov.hhs.cms.bluebutton.datapipeline.sampledata.addresses.SampleAddressGenerator;
import gov.hhs.cms.bluebutton.datapipeline.sampledata.npi.SampleProviderGenerator;
import gov.hhs.cms.bluebutton.datapipeline.sampledata.pharmacies.SamplePharmacyGenerator;
import gov.hhs.cms.bluebutton.datapipeline.sampledata.prescribers.SamplePrescriberGenerator;

/**
 * Loads sample data into the specified database.
 */
public final class SampleDataLoader {
	private final static Logger LOGGER = LoggerFactory.getLogger(SampleDataLoader.class);
	private final static DateTimeFormatter SYNPUF_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final MetricRegistry metrics;

	/**
	 * Constructs a new {@link SampleDataLoader} instance.
	 * 
	 * @param metrics
	 *            the (injected) {@link MetricRegistry} to use
	 */
	@Inject
	public SampleDataLoader(MetricRegistry metrics) {
		this.metrics = metrics;
	}

	/**
	 * Loads the data from the specified {@link SynpufArchive}s into the (CCW)
	 * database.
	 * 
	 * @param workDir
	 *            a directory that can be used to write any temporary files
	 *            needed
	 * @return the {@link CurrentBeneficiary}s that were generated
	 * @throws SampleDataException
	 *             A {@link SampleDataException} will be thrown if any errors
	 *             occur reading in or processing the specified
	 *             {@link SynpufArchive}s.
	 */
	public List<CurrentBeneficiary> loadSampleData(Path workDir, SynpufArchive... synpufArchives)
			throws SampleDataException {
		// Extract the DE-SynPUF CSV files.
		Path synpufDir = workDir.resolve("blue-button-de-synpuf");
		List<SynpufSample> synpufSamples = Arrays.stream(synpufArchives)
				.map(a -> SynpufSampleLoader.extractSynpufFile(synpufDir, a)).collect(Collectors.toList());

		/*
		 * FIXME This design is broken: takes about 8GB of heap to process each
		 * full DE-SynPUF sample. Perhaps the comment just below about data
		 * architecture holds the correct solution: pivot the data model and
		 * asynchronously push out beneficiaries and claims as they are
		 * produced.
		 */

		/*
		 * FIXME Need to come up with a more consistent and
		 * representative-of-the-CCW way to handle missing fields, throughout
		 * this class. For example, the parseDate method is a good thing. Also:
		 * many CCW fields use '~' and '^' to indicate missing values.
		 */

		/*
		 * TODO After a good bit of reflection, I think the data architecture
		 * here is all wrong: the CCW is a claims-focused system, but what I've
		 * built here is beneficiary-focused. Need to pivot that. Eventually.
		 */

		// Load the other sample data sets.
		SampleNameGenerator nameGenerator = new SampleNameGenerator();
		SampleAddressGenerator addressGenerator = new SampleAddressGenerator();
		SampleProviderGenerator providerGenerator = new SampleProviderGenerator();

		// Process each DE-SynPUF sample.
		List<CurrentBeneficiary> sampleBeneficiaries = new ArrayList<>();
		final Timer timerSamples = metrics.timer(MetricRegistry.name(SampleDataLoader.class, "samples"));
		for (SynpufSample synpufSample : synpufSamples) {
			Timer.Context timerSamplesContext = timerSamples.time();

			/*
			 * In the DE-SynPUF, beneficiaries' ID is arbitrary text. In the
			 * CCW, those IDs are an integer. The registry keeps track of the
			 * problem (amongst other things).
			 */
			SharedDataRegistry registry = new SharedDataRegistry();

			// Process the beneficiaries
			processBeneficiaries(synpufSample, registry, nameGenerator, addressGenerator);

			// Process the Part A inpatient claims.
			processInpatientClaims(synpufSample, registry, providerGenerator);

			// Process the Part B outpatient claims.
			processOutpatientClaims(synpufSample, registry, providerGenerator);

			// Process the Part B carrier claims.
			processCarrierClaims(synpufSample, registry, providerGenerator);

			// Process the Part D claims.
			processPartDClaims(synpufSample, registry);

			sampleBeneficiaries.addAll(registry.getBeneficiaries());
			LOGGER.info("Processed DE-SynPUF sample '{}'.", synpufSample.getArchive().name());
			timerSamplesContext.stop();
		}

		return sampleBeneficiaries;
	}

	/**
	 * Process the beneficiary data in the specified {@link SynpufSample}.
	 * 
	 * @param synpufSample
	 *            the {@link SynpufSample} to process
	 * @param registry
	 *            the {@link SharedDataRegistry} to use
	 * @param nameGenerator
	 *            the {@link SampleNameGenerator} to use
	 * @param addressGenerator
	 *            the {@link SampleAddressGenerator} to use
	 */
	private void processBeneficiaries(SynpufSample synpufSample, SharedDataRegistry registry,
			SampleNameGenerator nameGenerator, SampleAddressGenerator addressGenerator) {
		// Process the beneficiary summaries.
		for (Path summaryCsv : synpufSample.getBeneficiarySummaries()) {
			Timer.Context timerBeneficiaryFilesContext = metrics
					.timer(MetricRegistry.name(SampleDataLoader.class, "beneficiaries", "files")).time();
			Timer timerBeneficiaryRecords = metrics
					.timer(MetricRegistry.name(SampleDataLoader.class, "beneficiaries", "records"));
			LOGGER.info("Processing DE-SynPUF file '{}'...", summaryCsv.getFileName());
			try (Reader in = new FileReader(summaryCsv.toFile());) {
				CSVFormat csvFormat = CSVFormat.EXCEL.withHeader(SynpufColumnForBeneficiarySummary.getAllColumnNames())
						.withSkipHeaderRecord();
				Iterable<CSVRecord> records = csvFormat.parse(in);
				for (CSVRecord record : records) {
					Timer.Context timerBeneficiaryRecordContext = timerBeneficiaryRecords.time();
					LOGGER.trace("Processing DE-SynPUF Beneficiary Summary record #{}.", record.getRecordNumber());

					String synpufId = record.get(SynpufColumnForBeneficiarySummary.DESYNPUF_ID);
					String birthDateText = record.get(SynpufColumnForBeneficiarySummary.BENE_BIRTH_DT);
					LocalDate birthDate = LocalDate.parse(birthDateText, SYNPUF_DATE_FORMATTER);

					/*
					 * Many beneficiaries appear in the summary file for more
					 * than one year. To keep things simple, we'll just always
					 * assume that the later years are "more correct".
					 */
					CurrentBeneficiary bene;
					if (registry.getBeneficiary(synpufId) != null) {
						bene = registry.getBeneficiary(synpufId);
					} else {
						bene = new CurrentBeneficiary();
						bene.setId(registry.getBeneficiariesCount());
					}

					bene.setBirthDate(birthDate);
					SampleName name = nameGenerator.generateName();
					bene.setGivenName(name.getFirstName());
					bene.setSurname(synpufId);
					SampleAddress address = addressGenerator.generateAddress();
					bene.setContactAddress(address.getAddressExceptZip());
					bene.setContactAddressZip(address.getZip());

					registry.register(synpufId, bene);
					timerBeneficiaryRecordContext.stop();
				}
			} catch (IOException e) {
				throw new SampleDataException(e);
			}
			LOGGER.info("Processed DE-SynPUF file '{}'.", summaryCsv.getFileName());
			timerBeneficiaryFilesContext.stop();
		}
	}

	/**
	 * Process the Part A inpatient claims data in the specified
	 * {@link SynpufSample}.
	 * 
	 * @param synpufSample
	 *            the {@link SynpufSample} to process
	 * @param registry
	 *            the {@link SharedDataRegistry} to use
	 * @param providerGenerator
	 *            the {@link SampleProviderGenerator} to use
	 */
	private void processInpatientClaims(SynpufSample synpufSample, SharedDataRegistry registry,
			SampleProviderGenerator providerGenerator) {
		Timer.Context timerInpatientFilesContext = metrics
				.timer(MetricRegistry.name(SampleDataLoader.class, "inpatient", "files")).time();
		Timer timerInpatientRecords = metrics
				.timer(MetricRegistry.name(SampleDataLoader.class, "inpatient", "records"));
		LOGGER.info("Processing DE-SynPUF file '{}'...", synpufSample.getInpatientClaimsFile().getFileName());
		try (Reader in = new FileReader(synpufSample.getInpatientClaimsFile().toFile());) {
			Map<Long, PartAClaimFact> claimsMap = new HashMap<>();
			CSVFormat csvFormat = CSVFormat.EXCEL.withHeader(SynpufColumnForInpatientClaims.getAllColumnNames())
					.withSkipHeaderRecord();
			Iterable<CSVRecord> records = csvFormat.parse(in);
			for (CSVRecord record : records) {
				Timer.Context timerInpatientRecordContext = timerInpatientRecords.time();
				LOGGER.trace("Processing DE-SynPUF Inpatient record #{}.", record.getRecordNumber());

				/*
				 * Based on conversations with Tony Dean at CMS, it seems pretty
				 * clear that the DE-SynPUF inpatient data records are rather
				 * borked: in the CCW, there is one line/trailer per
				 * "revenue center code" associated with the claim. Each of
				 * those lines/trailers has associated payment/financial
				 * amounts. It seems that the DE-SynPUF data has arbitrarily
				 * grouped the lines/trailers into "SEGMENT"s, which do not have
				 * a revenue center code at all. More confusingly, only 0.1% of
				 * the DE-SynPUF records have more than segment, and a spot
				 * check indicates that the few second segments present contain
				 * very little information. To cope with this, we're going to
				 * ignore the HCPCSs and anything other than the first segments;
				 * we just won't have trailers for our sample inpatient claims.
				 */

				String synpufId = record.get(SynpufColumnForInpatientClaims.DESYNPUF_ID);
				long claimId = Long.parseLong(record.get(SynpufColumnForInpatientClaims.CLM_ID));
				int segment = Integer.parseInt(record.get(SynpufColumnForInpatientClaims.SEGMENT));
				LocalDate dateClaimFrom = parseDate(record, SynpufColumnForInpatientClaims.CLM_FROM_DT);
				LocalDate dateClaimThrough = parseDate(record, SynpufColumnForInpatientClaims.CLM_THRU_DT);
				// Skip PRVDR_NUM: it contains gibberish
				BigDecimal claimPayment = parseBigDecimal(record, SynpufColumnForInpatientClaims.CLM_PMT_AMT);
				BigDecimal nchPrimaryPayerClaimPaid = parseBigDecimal(record,
						SynpufColumnForInpatientClaims.NCH_PRMRY_PYR_CLM_PD_AMT);
				// Skip AT_PHYSN_NPI: it contains gibberish
				// Skip OP_PHYSN_NPI: it contains gibberish
				// Skip OT_PHYSN_NPI: it contains gibberish
				LocalDate dateClaimAdmission = parseDate(record, SynpufColumnForInpatientClaims.CLM_ADMSN_DT);
				String admittingDiagnosisCode = record.get(SynpufColumnForInpatientClaims.ADMTNG_ICD9_DGNS_CD);
				BigDecimal passThroughPerDiemAmount = parseBigDecimal(record,
						SynpufColumnForInpatientClaims.CLM_PASS_THRU_PER_DIEM_AMT);
				BigDecimal nchBeneficiaryInpatientDeductible = parseBigDecimal(record,
						SynpufColumnForInpatientClaims.NCH_BENE_IP_DDCTBL_AMT);
				BigDecimal nchBeneficiaryPartACoinsuranceLiability = parseBigDecimal(record,
						SynpufColumnForInpatientClaims.NCH_BENE_PTA_COINSRNC_LBLTY_AM);
				BigDecimal nchBeneficiaryBloodDeductible = parseBigDecimal(record,
						SynpufColumnForInpatientClaims.NCH_BENE_BLOOD_DDCTBL_LBLTY_AM);
				Long utilizationDayCount = parseLong(record, SynpufColumnForInpatientClaims.CLM_UTLZTN_DAY_CNT);
				LocalDate dateClaimDischarge = parseDate(record, SynpufColumnForInpatientClaims.NCH_BENE_DSCHRG_DT);
				String diagnosisRelatedGroupCode = record.get(SynpufColumnForInpatientClaims.CLM_DRG_CD);
				String diagnosisCode1 = record.get(SynpufColumnForInpatientClaims.ICD9_DGNS_CD_1);
				String diagnosisCode2 = record.get(SynpufColumnForInpatientClaims.ICD9_DGNS_CD_2);
				String diagnosisCode3 = record.get(SynpufColumnForInpatientClaims.ICD9_DGNS_CD_3);
				String diagnosisCode4 = record.get(SynpufColumnForInpatientClaims.ICD9_DGNS_CD_4);
				String diagnosisCode5 = record.get(SynpufColumnForInpatientClaims.ICD9_DGNS_CD_5);
				String diagnosisCode6 = record.get(SynpufColumnForInpatientClaims.ICD9_DGNS_CD_6);
				String diagnosisCode7 = record.get(SynpufColumnForInpatientClaims.ICD9_DGNS_CD_7);
				String diagnosisCode8 = record.get(SynpufColumnForInpatientClaims.ICD9_DGNS_CD_8);
				String diagnosisCode9 = record.get(SynpufColumnForInpatientClaims.ICD9_DGNS_CD_9);
				String diagnosisCode10 = record.get(SynpufColumnForInpatientClaims.ICD9_DGNS_CD_10);
				String procedureCode1 = record.get(SynpufColumnForInpatientClaims.ICD9_PRCDR_CD_1);
				String procedureCode2 = record.get(SynpufColumnForInpatientClaims.ICD9_PRCDR_CD_2);
				String procedureCode3 = record.get(SynpufColumnForInpatientClaims.ICD9_PRCDR_CD_3);
				String procedureCode4 = record.get(SynpufColumnForInpatientClaims.ICD9_PRCDR_CD_4);
				String procedureCode5 = record.get(SynpufColumnForInpatientClaims.ICD9_PRCDR_CD_5);
				String procedureCode6 = record.get(SynpufColumnForInpatientClaims.ICD9_PRCDR_CD_6);

				// As explained above, we're skipping extra segments.
				if (segment > 1)
					continue;

				PartAClaimFact claim = new PartAClaimFact();
				claim.setId(claimId);
				claim.setBeneficiary(registry.getBeneficiary(synpufId));
				claim.getBeneficiary().getPartAClaimFacts().add(claim);

				AllClaimsProfile claimProfile;
				if (registry.getClaimProfile(ClaimType.INPATIENT_CLAIM) != null) {
					claimProfile = registry.getClaimProfile(ClaimType.INPATIENT_CLAIM);
				} else {
					claimProfile = new AllClaimsProfile().setId((long) registry.getClaimProfilesCount())
							.setClaimType(ClaimType.INPATIENT_CLAIM);
					registry.register(claimProfile);
				}
				claim.setClaimProfile(claimProfile);

				claim.setDiagnosisGroup(registry.findOrCreate(diagnosisRelatedGroupCode));
				claim.setDateAdmission(dateClaimAdmission);
				claim.setDateFrom(dateClaimFrom);
				claim.setDateThrough(dateClaimThrough);
				claim.setDateDischarge(dateClaimDischarge);
				claim.setProviderAtTimeOfClaimNpi((long) providerGenerator.generateProvider().getNpi());
				claim.setUtilizationDayCount(utilizationDayCount);
				claim.setPayment(claimPayment);
				claim.setPassThroughPerDiemAmount(passThroughPerDiemAmount);
				claim.setNchBeneficiaryBloodDeductibleLiability(nchBeneficiaryBloodDeductible);
				claim.setNchBeneficiaryInpatientDeductible(nchBeneficiaryInpatientDeductible);
				claim.setNchBeneficiaryPartACoinsuranceLiability(nchBeneficiaryPartACoinsuranceLiability);
				claim.setNchPrimaryPayerPaid(nchPrimaryPayerClaimPaid);
				claim.setAttendingPhysicianNpi((long) providerGenerator.generateProvider().getNpi());
				claim.setOperatingPhysicianNpi((long) providerGenerator.generateProvider().getNpi());
				claim.setOtherPhysicianNpi((long) providerGenerator.generateProvider().getNpi());
				claim.setAdmittingDiagnosisCode(admittingDiagnosisCode);

				PartAClaimRevLineFact revLine = new PartAClaimRevLineFact();
				revLine.setClaim(claim);
				revLine.setLineNumber(1);
				claim.getClaimLines().add(revLine);
				revLine.setDiagnosisCode1(diagnosisCode1);
				revLine.setDiagnosisCode2(diagnosisCode2);
				revLine.setDiagnosisCode3(diagnosisCode3);
				revLine.setDiagnosisCode4(diagnosisCode4);
				revLine.setDiagnosisCode5(diagnosisCode5);
				revLine.setDiagnosisCode6(diagnosisCode6);
				revLine.setDiagnosisCode7(diagnosisCode7);
				revLine.setDiagnosisCode8(diagnosisCode8);
				revLine.setDiagnosisCode9(diagnosisCode9);
				revLine.setDiagnosisCode10(diagnosisCode10);
				revLine.setProcedureCode1(procedureCode1);
				revLine.setProcedureCode2(procedureCode2);
				revLine.setProcedureCode3(procedureCode3);
				revLine.setProcedureCode4(procedureCode4);
				revLine.setProcedureCode5(procedureCode5);
				revLine.setProcedureCode6(procedureCode6);

				claimsMap.put(claimId, claim);
				timerInpatientRecordContext.stop();
			}
		} catch (IOException e) {
			throw new SampleDataException(e);
		}
		LOGGER.info("Processed DE-SynPUF file '{}'.", synpufSample.getInpatientClaimsFile().getFileName());
		timerInpatientFilesContext.stop();
	}

	/**
	 * Process the Part B outpatient claims data in the specified
	 * {@link SynpufSample}.
	 * 
	 * @param synpufSample
	 *            the {@link SynpufSample} to process
	 * @param registry
	 *            the {@link SharedDataRegistry} to use
	 * @param providerGenerator
	 *            the {@link SampleProviderGenerator} to use
	 */
	private void processOutpatientClaims(SynpufSample synpufSample, SharedDataRegistry registry,
			SampleProviderGenerator providerGenerator) {
		Timer.Context timerOutpatientFilesContext = metrics
				.timer(MetricRegistry.name(SampleDataLoader.class, "outpatient", "files")).time();
		Timer timerOutpatientRecords = metrics
				.timer(MetricRegistry.name(SampleDataLoader.class, "outpatient", "records"));
		LOGGER.info("Processing DE-SynPUF file '{}'...", synpufSample.getOutpatientClaimsFile().getFileName());
		try (Reader in = new FileReader(synpufSample.getOutpatientClaimsFile().toFile());) {
			Map<Long, PartAClaimFact> claimsMap = new HashMap<>();
			CSVFormat csvFormat = CSVFormat.EXCEL.withHeader(SynpufColumnForOutpatientClaims.getAllColumnNames())
					.withSkipHeaderRecord();
			Iterable<CSVRecord> records = csvFormat.parse(in);
			for (CSVRecord record : records) {
				Timer.Context timerOutpatientRecordContext = timerOutpatientRecords.time();
				LOGGER.trace("Processing DE-SynPUF Outpatient record #{}.", record.getRecordNumber());

				/*
				 * Based on conversations with Tony Dean at CMS, it seems pretty
				 * clear that the DE-SynPUF outpatient data records are rather
				 * borked: each segment/line/trailer can have up to 45 different
				 * HCPCS in the DE-SynPUF data. This does not reflect the real
				 * world data's structure (or the CCW's), where each HCPCS is a
				 * separate trailer, with associated payment/financial amounts.
				 * It seems that the DE-SynPUF data has arbitrarily grouped
				 * HCPCS into blocks of (up to) 45. To cope with this, we'll
				 * pretend that each segment only has one HCPCS (selected
				 * arbitrarily from the possible 45).
				 */

				String synpufId = record.get(SynpufColumnForOutpatientClaims.DESYNPUF_ID);
				long claimId = Long.parseLong(record.get(SynpufColumnForOutpatientClaims.CLM_ID));
				int segment = Integer.parseInt(record.get(SynpufColumnForOutpatientClaims.SEGMENT));
				LocalDate dateClaimFrom = parseDate(record, SynpufColumnForOutpatientClaims.CLM_FROM_DT);
				LocalDate dateClaimThrough = parseDate(record, SynpufColumnForOutpatientClaims.CLM_THRU_DT);
				BigDecimal claimPayment = parseBigDecimal(record, SynpufColumnForOutpatientClaims.CLM_PMT_AMT);
				BigDecimal nchPrimaryPayerClaimPaid = parseBigDecimal(record,
						SynpufColumnForOutpatientClaims.NCH_PRMRY_PYR_CLM_PD_AMT);
				BigDecimal nchBeneficiaryBloodDeductible = parseBigDecimal(record,
						SynpufColumnForOutpatientClaims.NCH_BENE_BLOOD_DDCTBL_LBLTY_AM);
				String diagnosisCode1 = record.get(SynpufColumnForOutpatientClaims.ICD9_DGNS_CD_1);
				String diagnosisCode2 = record.get(SynpufColumnForOutpatientClaims.ICD9_DGNS_CD_2);
				String diagnosisCode3 = record.get(SynpufColumnForOutpatientClaims.ICD9_DGNS_CD_3);
				String diagnosisCode4 = record.get(SynpufColumnForOutpatientClaims.ICD9_DGNS_CD_4);
				String diagnosisCode5 = record.get(SynpufColumnForOutpatientClaims.ICD9_DGNS_CD_5);
				String diagnosisCode6 = record.get(SynpufColumnForOutpatientClaims.ICD9_DGNS_CD_6);
				String diagnosisCode7 = record.get(SynpufColumnForOutpatientClaims.ICD9_DGNS_CD_7);
				String diagnosisCode8 = record.get(SynpufColumnForOutpatientClaims.ICD9_DGNS_CD_8);
				String diagnosisCode9 = record.get(SynpufColumnForOutpatientClaims.ICD9_DGNS_CD_9);
				String diagnosisCode10 = record.get(SynpufColumnForOutpatientClaims.ICD9_DGNS_CD_10);
				String procedureCode1 = record.get(SynpufColumnForOutpatientClaims.ICD9_PRCDR_CD_1);
				String procedureCode2 = record.get(SynpufColumnForOutpatientClaims.ICD9_PRCDR_CD_2);
				String procedureCode3 = record.get(SynpufColumnForOutpatientClaims.ICD9_PRCDR_CD_3);
				String procedureCode4 = record.get(SynpufColumnForOutpatientClaims.ICD9_PRCDR_CD_4);
				String procedureCode5 = record.get(SynpufColumnForOutpatientClaims.ICD9_PRCDR_CD_5);
				String procedureCode6 = record.get(SynpufColumnForOutpatientClaims.ICD9_PRCDR_CD_6);
				BigDecimal nchBeneficiaryPartBDeductible = parseBigDecimal(record,
						SynpufColumnForOutpatientClaims.NCH_BENE_PTB_DDCTBL_AMT);
				BigDecimal nchBeneficiaryPartBCoinsurance = parseBigDecimal(record,
						SynpufColumnForOutpatientClaims.NCH_BENE_PTB_COINSRNC_AMT);
				String admittingDiagnosisCode = record.get(SynpufColumnForOutpatientClaims.ADMTNG_ICD9_DGNS_CD);
				String hcpcsCode = selectArbitraryOutpatientHcpcsCode(record);

				/*
				 * Note: The DE-SynPUF records do not present the different
				 * segments/lines for each claim in the correct order. Each CSV
				 * record represents a combined parent-claim and child-line. If,
				 * for some reason, the parent-claims differ between the
				 * child-lines (and they do, because the DE-SynPUF data is
				 * kinda' bad), whichever parent-claim is encountered last
				 * "wins".
				 */
				PartAClaimFact claim;
				if (claimsMap.containsKey(claimId)) {
					claim = claimsMap.get(claimId);
				} else {
					claim = new PartAClaimFact();
					claim.setId(claimId);

					AllClaimsProfile claimProfile;
					if (registry.getClaimProfile(ClaimType.OUTPATIENT_CLAIM) != null) {
						claimProfile = registry.getClaimProfile(ClaimType.OUTPATIENT_CLAIM);
					} else {
						claimProfile = new AllClaimsProfile().setId((long) registry.getClaimProfilesCount())
								.setClaimType(ClaimType.OUTPATIENT_CLAIM);
						registry.register(claimProfile);
					}

					claim.setClaimProfile(claimProfile);
				}

				claim.setBeneficiary(registry.getBeneficiary(synpufId));
				claim.getBeneficiary().getPartAClaimFacts().add(claim);
				claim.setDateFrom(dateClaimFrom);
				claim.setDateThrough(dateClaimThrough);
				claim.setAdmittingDiagnosisCode(admittingDiagnosisCode);
				claim.setPayment(claimPayment);
				claim.setNchBeneficiaryBloodDeductibleLiability(nchBeneficiaryBloodDeductible);
				claim.setNchBeneficiaryPartBDeductible(nchBeneficiaryPartBDeductible);
				claim.setNchBeneficiaryPartBCoinsurance(nchBeneficiaryPartBCoinsurance);
				claim.setNchPrimaryPayerPaid(nchPrimaryPayerClaimPaid);

				// Skipping SynPUF data for these fields, since it's gibberish.
				claim.setAttendingPhysicianNpi((long) providerGenerator.generateProvider().getNpi());
				claim.setOperatingPhysicianNpi((long) providerGenerator.generateProvider().getNpi());
				claim.setOtherPhysicianNpi((long) providerGenerator.generateProvider().getNpi());
				claim.setProviderAtTimeOfClaimNpi((long) providerGenerator.generateProvider().getNpi());

				PartAClaimRevLineFact revLine = new PartAClaimRevLineFact();
				revLine.setClaim(claim);
				revLine.setLineNumber(segment);
				claim.getClaimLines().add(revLine);
				if (!isBlank(hcpcsCode)) {
					Procedure procedure;
					if (registry.getProcedure(hcpcsCode) != null) {
						procedure = registry.getProcedure(hcpcsCode);
					} else {
						procedure = new Procedure().setId((long) registry.getProceduresCount()).setCode(hcpcsCode);
						registry.register(procedure);
					}
					revLine.setRevenueCenter(procedure);
				}
				revLine.setDiagnosisCode1(diagnosisCode1);
				revLine.setDiagnosisCode2(diagnosisCode2);
				revLine.setDiagnosisCode3(diagnosisCode3);
				revLine.setDiagnosisCode4(diagnosisCode4);
				revLine.setDiagnosisCode5(diagnosisCode5);
				revLine.setDiagnosisCode6(diagnosisCode6);
				revLine.setDiagnosisCode7(diagnosisCode7);
				revLine.setDiagnosisCode8(diagnosisCode8);
				revLine.setDiagnosisCode9(diagnosisCode9);
				revLine.setDiagnosisCode10(diagnosisCode10);
				revLine.setProcedureCode1(procedureCode1);
				revLine.setProcedureCode2(procedureCode2);
				revLine.setProcedureCode3(procedureCode3);
				revLine.setProcedureCode4(procedureCode4);
				revLine.setProcedureCode5(procedureCode5);
				revLine.setProcedureCode6(procedureCode6);

				claimsMap.put(claimId, claim);
				timerOutpatientRecordContext.stop();
			}
		} catch (IOException e) {
			throw new SampleDataException(e);
		}
		LOGGER.info("Processed DE-SynPUF file '{}'.", synpufSample.getOutpatientClaimsFile().getFileName());
		timerOutpatientFilesContext.stop();
	}

	/**
	 * @param outpatientRecord
	 *            the outpatient claim record to select a HCPCS code from
	 * @return an arbitrary value from the
	 *         {@link SynpufColumnForOutpatientClaims#HCPCS_CD_1},
	 *         {@link SynpufColumnForOutpatientClaims#HCPCS_CD_2}, etc. columns
	 *         in the specified record, or null if none of the columns have a
	 *         value
	 */
	private static String selectArbitraryOutpatientHcpcsCode(CSVRecord outpatientRecord) {
		List<String> hcpcsColumnNames = Arrays.stream(SynpufColumnForOutpatientClaims.values())
				.filter(c -> c.name().startsWith("HCPCS_CD_")).map(c -> c.name()).collect(Collectors.toList());
		List<String> hcpcsColumnValues = hcpcsColumnNames.stream().map(c -> outpatientRecord.get(c))
				.collect(Collectors.toList());
		Optional<String> arbitraryHcpcsValue = hcpcsColumnValues.stream().filter(v -> v != null && !v.trim().isEmpty())
				.findAny();
		return arbitraryHcpcsValue.orElse(null);
	}

	/**
	 * Processes the Part B carrier claims data in the specified
	 * {@link SynpufSample}.
	 * 
	 * @param synpufSample
	 *            the {@link SynpufSample} to process
	 * @param registry
	 *            the {@link SharedDataRegistry} being used
	 * @param providerGenerator
	 *            the {@link SampleProviderGenerator} to use
	 */
	private void processCarrierClaims(SynpufSample synpufSample, SharedDataRegistry registry,
			SampleProviderGenerator providerGenerator) {
		Map<Long, PartBClaimFact> claimsMap = new HashMap<>();
		for (Path claimsCsv : synpufSample.getCarrierClaimsFiles()) {
			Timer.Context timerCarrierFilesContext = metrics
					.timer(MetricRegistry.name(SampleDataLoader.class, "carrier", "files")).time();
			Timer timerCarrierRecords = metrics
					.timer(MetricRegistry.name(SampleDataLoader.class, "carrier", "records"));
			LOGGER.info("Processing DE-SynPUF file '{}'...", claimsCsv.getFileName());
			try (Reader in = new FileReader(claimsCsv.toFile());) {
				CSVFormat csvFormat = CSVFormat.EXCEL.withHeader(SynpufColumnForCarrierClaims.getAllColumnNames())
						.withSkipHeaderRecord();
				Iterable<CSVRecord> records = csvFormat.parse(in);
				for (CSVRecord record : records) {
					Timer.Context timerCarrierRecordContext = timerCarrierRecords.time();
					LOGGER.trace("Processing DE-SynPUF Carrier record #{}.", record.getRecordNumber());

					String synpufId = record.get(SynpufColumnForCarrierClaims.DESYNPUF_ID);
					String claimIdText = record.get(SynpufColumnForCarrierClaims.CLM_ID);
					long claimId = Long.parseLong(claimIdText);
					String dateFromText = record.get(SynpufColumnForCarrierClaims.CLM_FROM_DT);
					LocalDate dateFrom = LocalDate.parse(dateFromText, SYNPUF_DATE_FORMATTER);
					String dateThroughText = record.get(SynpufColumnForCarrierClaims.CLM_THRU_DT);
					LocalDate dateThrough = LocalDate.parse(dateThroughText, SYNPUF_DATE_FORMATTER);
					String diagnosisCode1 = record.get(SynpufColumnForCarrierClaims.ICD9_DGNS_CD_1);
					String diagnosisCode2 = record.get(SynpufColumnForCarrierClaims.ICD9_DGNS_CD_2);
					String diagnosisCode3 = record.get(SynpufColumnForCarrierClaims.ICD9_DGNS_CD_3);
					String diagnosisCode4 = record.get(SynpufColumnForCarrierClaims.ICD9_DGNS_CD_4);
					String diagnosisCode5 = record.get(SynpufColumnForCarrierClaims.ICD9_DGNS_CD_5);
					String diagnosisCode6 = record.get(SynpufColumnForCarrierClaims.ICD9_DGNS_CD_6);
					String diagnosisCode7 = record.get(SynpufColumnForCarrierClaims.ICD9_DGNS_CD_7);
					String diagnosisCode8 = record.get(SynpufColumnForCarrierClaims.ICD9_DGNS_CD_8);

					// Sanity check:
					if (claimsMap.containsKey(claimId)) {
						throw new IllegalStateException("Dupe claim: " + claimId);
					}

					PartBClaimFact claim = new PartBClaimFact();
					claimsMap.put(claimId, claim);
					claim.setId(claimId);
					claim.setBeneficiary(registry.getBeneficiary(synpufId));
					claim.getBeneficiary().getPartBClaimFacts().add(claim);

					AllClaimsProfile claimProfile;
					if (registry.getClaimProfile(ClaimType.CARRIER_NON_DME_CLAIM) != null) {
						claimProfile = registry.getClaimProfile(ClaimType.CARRIER_NON_DME_CLAIM);
					} else {
						claimProfile = new AllClaimsProfile().setId((long) registry.getClaimProfilesCount())
								.setClaimType(ClaimType.CARRIER_NON_DME_CLAIM);
						registry.register(claimProfile);
					}
					claim.setClaimProfile(claimProfile);

					claim.setCarrierControlNumber(claimId);
					claim.setDiagnosisCode1(diagnosisCode1);
					claim.setDiagnosisCode2(diagnosisCode2);
					claim.setDiagnosisCode3(diagnosisCode3);
					claim.setDiagnosisCode4(diagnosisCode4);
					claim.setDiagnosisCode5(diagnosisCode5);
					claim.setDiagnosisCode6(diagnosisCode6);
					claim.setDiagnosisCode7(diagnosisCode7);
					claim.setDiagnosisCode8(diagnosisCode8);
					int claimPerformingPhysicianNpi = providerGenerator.generateProvider().getNpi();
					claim.setProviderNpi((long) claimPerformingPhysicianNpi);

					for (int lineNumber = 1; lineNumber <= 13; lineNumber++) {
						PartBClaimLineFact claimLine = new PartBClaimLineFact();
						claimLine.setClaim(claim);
						claimLine.setBeneficiary(claim.getBeneficiary());

						claimLine.setLineNumber((long) lineNumber);
						claimLine.setDateFrom(dateFrom);
						claimLine.setDateThrough(dateThrough);

						String lineDiagnosisCode = record
								.get(SynpufColumnForCarrierClaims.getLineIcd9DgnsCd(lineNumber));
						claimLine.setLineDiagnosisCode(lineDiagnosisCode);

						int claimLinePerformingPhysicianNpi = providerGenerator.generateProvider().getNpi();
						// TODO where to map PRF_PHYSN_NPI_#? Note: Gibberish
						// data! (Already mapped at claim level.)

						String taxNum = record.get(SynpufColumnForCarrierClaims.getTaxNum(lineNumber));
						// TODO where to map TAX_NUM_#? Note: Gibberish data!

						String hcpcsCd = record.get(SynpufColumnForCarrierClaims.getHcpcsCd(lineNumber));
						if (!isBlank(hcpcsCd)) {
							Procedure procedure;
							if (registry.getProcedure(hcpcsCd) != null) {
								procedure = registry.getProcedure(hcpcsCd);
							} else {
								procedure = new Procedure().setId((long) registry.getProceduresCount())
										.setCode(hcpcsCd);
								registry.register(procedure);
							}
							claimLine.setProcedure(procedure);
						}

						String nchPaymentAmountText = record
								.get(SynpufColumnForCarrierClaims.getLineNchPmtAmt(lineNumber));
						Double nchPaymentAmount = Double.parseDouble(nchPaymentAmountText);
						claimLine.setNchPaymentAmount(nchPaymentAmount);

						String deductibleAmountText = record
								.get(SynpufColumnForCarrierClaims.getLineBenePtbDdctblAmt(lineNumber));
						Double deductibleAmount = Double.parseDouble(deductibleAmountText);
						claimLine.setDeductibleAmount(deductibleAmount);

						String primaryPayerPaidAmountText = record
								.get(SynpufColumnForCarrierClaims.getLineBenePrmryPyrPdAmt(lineNumber));
						Double primaryPayerPaidAmount = Double.parseDouble(primaryPayerPaidAmountText);
						claimLine.setBeneficiaryPrimaryPayerPaidAmount(primaryPayerPaidAmount);

						String coinsuranceAmountText = record
								.get(SynpufColumnForCarrierClaims.getLineCoinsrncAmt(lineNumber));
						Double coinsuranceAmount = Double.parseDouble(coinsuranceAmountText);
						claimLine.setCoinsuranceAmount(coinsuranceAmount);

						String allowedAmountText = record
								.get(SynpufColumnForCarrierClaims.getLineAlowdChrgAmt(lineNumber));
						Double allowedAmount = Double.parseDouble(allowedAmountText);
						claimLine.setAllowedAmount(allowedAmount);

						String processingIndicationCode = record
								.get(SynpufColumnForCarrierClaims.getLinePrcsgIndCd(lineNumber));
						claimLine.setProcessingIndicationCode(processingIndicationCode);

						// TODO how to populate this?
						// claimLine.setSubmittedAmount(submittedAmount);

						// TODO how to populate this?
						// claimLine.setMiscCode(miscCode);

						if (!isMostlyBlank(claimLine))
							claim.getClaimLines().add(claimLine);
					}

					timerCarrierRecordContext.stop();
				}
			} catch (IOException e) {
				throw new SampleDataException(e);
			}
			LOGGER.info("Processed DE-SynPUF file '{}'.", claimsCsv.getFileName());
			timerCarrierFilesContext.stop();
		}
	}

	/**
	 * @param claimLine
	 *            the {@link PartBClaimLineFact} to check
	 * @return <code>true</code> if all of the following fields are blank,
	 *         <code>false</code> if not:
	 *         <ul>
	 *         <li>{@link PartBClaimLineFact#getProcedure()}</li>
	 *         <li>{@link PartBClaimLineFact#getAllowedAmount()}</li>
	 *         <li>{@link PartBClaimLineFact#getSubmittedAmount()}</li>
	 *         <li>{@link PartBClaimLineFact#getLineDiagnosisCode()}</li>
	 *         <li>{@link PartBClaimLineFact#getMiscCode()}</li>
	 *         <li>{@link PartBClaimLineFact#getNchPaymentAmount()}</li>
	 *         <li>
	 *         {@link PartBClaimLineFact#getBeneficiaryPrimaryPayerPaidAmount()}
	 *         </li>
	 *         <li>{@link PartBClaimLineFact#getCoinsuranceAmount()}</li>
	 *         <li>{@link PartBClaimLineFact#getProcessingIndicationCode()}</li>
	 *         </ul>
	 */
	private static boolean isMostlyBlank(PartBClaimLineFact claimLine) {
		return claimLine.getProcedure() == null && isBlank(claimLine.getAllowedAmount())
				&& isBlank(claimLine.getLineDiagnosisCode()) && claimLine.getMiscCode() == null
				&& isBlank(claimLine.getNchPaymentAmount()) && isBlank(claimLine.getBeneficiaryPrimaryPayerPaidAmount())
				&& isBlank(claimLine.getCoinsuranceAmount()) && isBlank(claimLine.getProcessingIndicationCode());
	}

	/**
	 * TODO
	 * 
	 * @param synpufSample
	 * @param registry
	 */
	private void processPartDClaims(SynpufSample synpufSample, SharedDataRegistry registry) {
		Timer.Context timerDrugFilesContext = metrics
				.timer(MetricRegistry.name(SampleDataLoader.class, "drug", "files")).time();
		Timer timerDrugRecords = metrics.timer(MetricRegistry.name(SampleDataLoader.class, "drug", "records"));

		SamplePrescriberGenerator prescriberGenerator = new SamplePrescriberGenerator();
		SamplePharmacyGenerator pharmacyGenerator = new SamplePharmacyGenerator();

		Path claimsCsv = synpufSample.getPartDClaimsFile();
		LOGGER.info("Processing DE-SynPUF file '{}'...", claimsCsv.getFileName());
		try (Reader in = new FileReader(claimsCsv.toFile());) {
			CSVFormat csvFormat = CSVFormat.EXCEL.withHeader(SynpufColumnForPartDClaims.getAllColumnNames())
					.withSkipHeaderRecord();
			Iterable<CSVRecord> records = csvFormat.parse(in);
			for (CSVRecord record : records) {
				Timer.Context timerDrugRecordContext = timerDrugRecords.time();
				LOGGER.trace("Processing DE-SynPUF Part D Outpatient record #{}.", record.getRecordNumber());

				String synpufId = record.get(SynpufColumnForPartDClaims.DESYNPUF_ID);
				long eventId = Long.parseLong(record.get(SynpufColumnForPartDClaims.PDE_ID));
				String serviceDateText = record.get(SynpufColumnForPartDClaims.SRVC_DT);
				LocalDate serviceDate = LocalDate.parse(serviceDateText, SYNPUF_DATE_FORMATTER);
				Long productId = parseLong(record, SynpufColumnForPartDClaims.PROD_SRVC_ID);
				String quantityText = record.get(SynpufColumnForPartDClaims.QTY_DSPNSD_NUM);
				// FIXME some/all values have fractions, e.g. "30.000"
				long quantity = (long) Double.parseDouble(quantityText);
				Long daysSupply = parseLong(record, SynpufColumnForPartDClaims.DAYS_SUPLY_NUM);
				String patientPaymentText = record.get(SynpufColumnForPartDClaims.PTNT_PAY_AMT);
				double patientPayment = Double.parseDouble(patientPaymentText);
				String prescriptionCostText = record.get(SynpufColumnForPartDClaims.TOT_RX_CST_AMT);
				double prescriptionCost = Double.parseDouble(prescriptionCostText);

				/*
				 * Workarounds for part of CBBD-41: cope with some cases of bad
				 * data. These occur in about 5-7% of the records, each.
				 */
				if (patientPayment > prescriptionCost)
					prescriptionCost = patientPayment;
				if (patientPayment <= 0 && prescriptionCost <= 0)
					continue;

				PartDEventFact event = new PartDEventFact();
				event.setId(eventId);
				event.setPrescriberNpi((long) prescriberGenerator.generatePrescriber().getNpi());
				event.setServiceProviderNpi((long) pharmacyGenerator.generatePharmacy().getNpi());
				event.setProductNdc(productId);
				event.setBeneficiary(registry.getBeneficiary(synpufId));
				event.getBeneficiary().getPartDEventFacts().add(event);
				event.setServiceDate(serviceDate);
				event.setQuantityDispensed(quantity);
				event.setNumberDaysSupply(daysSupply);
				event.setPatientPayAmount(patientPayment);
				event.setTotalPrescriptionCost(prescriptionCost);

				timerDrugRecordContext.stop();
			}
		} catch (IOException e) {
			throw new SampleDataException(e);
		}
		LOGGER.info("Processed DE-SynPUF file '{}'.", synpufSample.getPartDClaimsFile().getFileName());
		timerDrugFilesContext.stop();
	}

	/**
	 * @param value
	 *            the value to check
	 * @return <code>true</code> if the specified value is <code>null</code> or
	 *         only contains whitespace
	 */
	private static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	/**
	 * @param value
	 *            the value to check
	 * @return <code>true</code> if the specified value is <code>null</code> or
	 *         <code>0</code>
	 */
	private static boolean isBlank(Number value) {
		return value == null || value.equals(Double.valueOf(0.0));
	}

	/**
	 * @param record
	 *            the {@link CSVRecord} to parse a value from
	 * @param column
	 *            the column to parse a value from
	 * @return the {@link LocalDate} that was parsed from the specified record
	 *         and column, or <code>null</code> if the column was empty
	 */
	private static LocalDate parseDate(CSVRecord record, Enum<?> column) {
		String columnValue = record.get(column);
		if (isBlank(columnValue))
			return null;
		else
			return LocalDate.parse(columnValue, SYNPUF_DATE_FORMATTER);
	}

	/**
	 * @param record
	 *            the {@link CSVRecord} to parse a value from
	 * @param column
	 *            the column to parse a value from
	 * @return the {@link BigDecimal} that was parsed from the specified record
	 *         and column, or <code>null</code> if the column was empty
	 */
	private static BigDecimal parseBigDecimal(CSVRecord record, Enum<?> column) {
		String columnValue = record.get(column);
		if (isBlank(columnValue))
			return null;
		else
			return new BigDecimal(columnValue);
	}

	/**
	 * @param record
	 *            the {@link CSVRecord} to parse a value from
	 * @param column
	 *            the column to parse a value from
	 * @return the {@link Long} that was parsed from the specified record and
	 *         column, or <code>null</code> if the column was empty
	 */
	private static Long parseLong(CSVRecord record, Enum<?> column) {
		String columnValue = record.get(column);
		if (isBlank(columnValue))
			return null;
		else if ("OTHER".equals(columnValue))
			return null;
		else
			return Long.parseLong(columnValue);
	}

	/**
	 * A simple registry for shared data records that are created during a
	 * {@link SampleDataLoader#loadSampleData(Path, SynpufArchive...)}
	 * operation.
	 */
	private static final class SharedDataRegistry {
		private final Map<String, CurrentBeneficiary> beneficiariesBySynpufId = new HashMap<>();
		private final Map<String, Procedure> proceduresByCode = new HashMap<>();
		private final Map<ClaimType, AllClaimsProfile> claimProfilesByType = new HashMap<>();
		private final Map<String, DiagnosisRelatedGroup> diagnosisGroupsByCode = new HashMap<>();

		/**
		 * @return the matching {@link CurrentBeneficiary} that was passed to
		 *         {@link #register(CurrentBeneficiary)}, or <code>null</code>
		 *         if no such match is found
		 */
		public CurrentBeneficiary getBeneficiary(String synpufId) {
			return beneficiariesBySynpufId.get(synpufId);
		}

		/**
		 * @return all of the {@link CurrentBeneficiary}s (with their associated
		 *         data and claims) that have been passed to
		 *         {@link #register(String, CurrentBeneficiary)}
		 */
		public Collection<CurrentBeneficiary> getBeneficiaries() {
			return beneficiariesBySynpufId.values();
		}

		/**
		 * @return the number of {@link CurrentBeneficiary}s that have been
		 *         passed to {@link #register(String, CurrentBeneficiary)}
		 */
		public int getBeneficiariesCount() {
			return beneficiariesBySynpufId.size();
		}

		/**
		 * @param beneficiary
		 *            the {@link CurrentBeneficiary} to register
		 */
		public void register(String synpufId, CurrentBeneficiary beneficiary) {
			beneficiariesBySynpufId.put(synpufId, beneficiary);
		}

		/**
		 * @return the matching {@link Procedure} that was passed to
		 *         {@link #register(Procedure)}, or <code>null</code> if no such
		 *         match is found
		 */
		public Procedure getProcedure(String code) {
			return proceduresByCode.get(code);
		}

		/**
		 * @return the number of {@link Procedure}s that have been passed to
		 *         {@link #register(Procedure)}
		 */
		public int getProceduresCount() {
			return proceduresByCode.size();
		}

		/**
		 * @param procedure
		 *            the {@link Procedure} to register
		 */
		public void register(Procedure procedure) {
			proceduresByCode.put(procedure.getCode(), procedure);
		}

		/**
		 * @return the matching {@link AllClaimsProfile} that was passed to
		 *         {@link #register(AllClaimsProfile)}, or <code>null</code> if
		 *         no such match is found
		 */
		public AllClaimsProfile getClaimProfile(ClaimType claimType) {
			return claimProfilesByType.get(claimType);
		}

		/**
		 * @return the number of {@link AllClaimsProfile}s that have been passed
		 *         to {@link #register(AllClaimsProfile)}
		 */
		public int getClaimProfilesCount() {
			return claimProfilesByType.size();
		}

		/**
		 * @param claimProfile
		 *            the {@link AllClaimsProfile} to register
		 */
		public void register(AllClaimsProfile claimProfile) {
			claimProfilesByType.put(claimProfile.getClaimType(), claimProfile);
		}

		/**
		 * @param diagnosisRelatedGroupCode
		 *            the {@link DiagnosisRelatedGroup#getCode()} value to
		 *            search for
		 * @return either an already-existing {@link DiagnosisRelatedGroup} that
		 *         matches the specified {@link DiagnosisRelatedGroup#getCode()}
		 *         , or a new {@link DiagnosisRelatedGroup} with the specified
		 *         value
		 */
		public DiagnosisRelatedGroup findOrCreate(String diagnosisRelatedGroupCode) {
			DiagnosisRelatedGroup diagnosisGroup = diagnosisGroupsByCode.containsKey(diagnosisRelatedGroupCode)
					? diagnosisGroupsByCode.get(diagnosisRelatedGroupCode)
					: new DiagnosisRelatedGroup().setId((long) diagnosisGroupsByCode.size())
							.setCode(diagnosisRelatedGroupCode);
			if (!diagnosisGroupsByCode.containsKey(diagnosisRelatedGroupCode))
				diagnosisGroupsByCode.put(diagnosisRelatedGroupCode, diagnosisGroup);
			return diagnosisGroup;
		}
	}
}
