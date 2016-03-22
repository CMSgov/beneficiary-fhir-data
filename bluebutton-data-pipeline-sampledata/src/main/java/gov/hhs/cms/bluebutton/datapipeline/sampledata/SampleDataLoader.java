package gov.hhs.cms.bluebutton.datapipeline.sampledata;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.CurrentBeneficiary;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartAClaimFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartBClaimFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartBClaimLineFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartDEventFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.Procedure;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.SynpufArchive;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.SynpufSample;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.SynpufSampleLoader;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.columns.SynpufColumnForBeneficiarySummary;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.columns.SynpufColumnForPartAOutpatient;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.columns.SynpufColumnForPartB;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.columns.SynpufColumnForPartD;
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

	private final PersistenceManager pm;

	/**
	 * Constructs a new {@link SampleDataLoader} instance.
	 * 
	 * @param pm
	 *            the (injected) {@link PersistenceManager} to use
	 */
	@Inject
	public SampleDataLoader(PersistenceManager pm) {
		this.pm = pm;
	}

	/**
	 * Loads the data from the specified {@link SynpufArchive}s into the (CCW)
	 * database.
	 * 
	 * @param workDir
	 *            a directory that can be used to write any temporary files
	 *            needed
	 * @return a {@link SampleDataSummary} instance that contains information
	 *         about the data that was loaded
	 * @throws SampleDataException
	 *             A {@link SampleDataException} will be thrown if any errors
	 *             occur reading in or processing the specified
	 *             {@link SynpufArchive}s.
	 */
	public SampleDataSummary loadSampleData(Path workDir, SynpufArchive... synpufArchives) throws SampleDataException {
		// Extract the DE-SynPUF CSV files.
		Path synpufDir = workDir.resolve("blue-button-de-synpuf");
		List<SynpufSample> synpufSamples = null;
		synpufSamples = Arrays.stream(synpufArchives).map(a -> SynpufSampleLoader.extractSynpufFile(synpufDir, a))
				.collect(Collectors.toList());

		// Load the other sample data sets.
		SampleNameGenerator nameGenerator = new SampleNameGenerator();
		SampleAddressGenerator addressGenerator = new SampleAddressGenerator();
		SampleProviderGenerator providerGenerator = new SampleProviderGenerator();

		// Process each DE-SynPUF sample.
		for (SynpufSample synpufSample : synpufSamples) {
			Transaction tx = pm.currentTransaction();
			try {
				// Start the transaction: each sample gets its own TX.
				tx.begin();

				/*
				 * In DE-SynPUF, beneficiaries' ID is arbitrary text. In the
				 * CCW, those IDs are an integer. The registry keeps track of
				 * the problem (amongst other things).
				 */
				SharedDataRegistry registry = new SharedDataRegistry();

				// Process the beneficiary summaries.
				for (Path summaryCsv : synpufSample.getBeneficiarySummaries()) {
					LOGGER.info("Processing DE-SynPUF file '{}'...", summaryCsv.getFileName());
					try (Reader in = new FileReader(summaryCsv.toFile());) {
						CSVFormat csvFormat = CSVFormat.EXCEL
								.withHeader(SynpufColumnForBeneficiarySummary.getAllColumnNames())
								.withSkipHeaderRecord();
						Iterable<CSVRecord> records = csvFormat.parse(in);
						for (CSVRecord record : records) {
							LOGGER.trace("Processing DE-SynPUF Beneficiary Summary record #{}.",
									record.getRecordNumber());

							String synpufId = record.get(SynpufColumnForBeneficiarySummary.DESYNPUF_ID);
							String birthDateText = record.get(SynpufColumnForBeneficiarySummary.BENE_BIRTH_DT);
							LocalDate birthDate = LocalDate.parse(birthDateText, SYNPUF_DATE_FORMATTER);

							/*
							 * Many beneficiaries appear in the summary file for
							 * more than one year. To keep things simple, we'll
							 * just always assume that the later years are
							 * "more correct".
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
							bene.setSurname(name.getLastName());
							SampleAddress address = addressGenerator.generateAddress();
							bene.setContactAddress(address.getAddressExceptZip());
							bene.setContactAddressZip(address.getZip());

							pm.makePersistent(bene);
							registry.register(synpufId, bene);
						}
					} catch (IOException e) {
						throw new SampleDataException(e);
					}
					LOGGER.info("Processed DE-SynPUF file '{}'.", summaryCsv.getFileName());
				}

				// Process the Part A Inpatient claims.

				/*
				 * Process the Part A Outpatient claims. Each claim will be
				 * (potentially) repeated multiple times: one entry per
				 * "claim line".
				 */
				LOGGER.info("Processing DE-SynPUF file '{}'...", synpufSample.getPartAClaimsOutpatient().getFileName());
				try (Reader in = new FileReader(synpufSample.getPartAClaimsOutpatient().toFile());) {
					Map<Long, PartAClaimFact> claimsMap = new HashMap<>();
					CSVFormat csvFormat = CSVFormat.EXCEL.withHeader(SynpufColumnForPartAOutpatient.getAllColumnNames())
							.withSkipHeaderRecord();
					Iterable<CSVRecord> records = csvFormat.parse(in);
					for (CSVRecord record : records) {
						LOGGER.trace("Processing DE-SynPUF Part A Outpatient record #{}.", record.getRecordNumber());

						String claimIdText = record.get(SynpufColumnForPartAOutpatient.CLM_ID);
						long claimId = Long.parseLong(claimIdText);
						String synpufId = record.get(SynpufColumnForPartAOutpatient.DESYNPUF_ID);
						String diagnosisCodeText = record.get(SynpufColumnForPartAOutpatient.ADMTNG_ICD9_DGNS_CD);

						if (!claimsMap.containsKey(claimId)) {
							PartAClaimFact claim = new PartAClaimFact();
							claim.setId(claimId);
							claim.setBeneficiary(registry.getBeneficiary(synpufId));
							claim.setAdmittingDiagnosisCode(diagnosisCodeText);

							pm.makePersistent(claim);
							claimsMap.put(claimId, claim);
						}
					}
				} catch (IOException e) {
					throw new SampleDataException(e);
				}
				LOGGER.info("Processed DE-SynPUF file '{}'.", synpufSample.getPartAClaimsOutpatient().getFileName());

				// Process the Part B claims.
				processPartBClaims(synpufSample, registry, providerGenerator);

				// Process the Part D claims.
				processPartDClaims(synpufSample, registry);

				// Commit the transaction.
				tx.commit();
				LOGGER.info("Committed DE-SynPUF sample '{}'.", synpufSample.getArchive().name());
			} finally {
				if (tx.isActive())
					tx.rollback();
			}
		}

		// TODO
		return null;
	}

	/**
	 * Processes the Part B claims data in the specified {@link SynpufSample}.
	 * 
	 * @param synpufSample
	 *            the {@link SynpufSample} to process
	 * @param registry
	 *            the {@link SharedDataRegistry} being used
	 * @param providerGenerator
	 *            the {@link SampleProviderGenerator} to use
	 */
	private void processPartBClaims(SynpufSample synpufSample, SharedDataRegistry registry,
			SampleProviderGenerator providerGenerator) {
		Map<Long, PartBClaimFact> claimsMap = new HashMap<>();
		for (Path claimsCsv : synpufSample.getPartBClaims()) {
			LOGGER.info("Processing DE-SynPUF file '{}'...", claimsCsv.getFileName());
			try (Reader in = new FileReader(claimsCsv.toFile());) {
				CSVFormat csvFormat = CSVFormat.EXCEL.withHeader(SynpufColumnForPartB.getAllColumnNames())
						.withSkipHeaderRecord();
				Iterable<CSVRecord> records = csvFormat.parse(in);
				for (CSVRecord record : records) {
					LOGGER.trace("Processing DE-SynPUF Part B Outpatient record #{}.", record.getRecordNumber());

					String synpufId = record.get(SynpufColumnForPartB.DESYNPUF_ID);
					String claimIdText = record.get(SynpufColumnForPartB.CLM_ID);
					long claimId = Long.parseLong(claimIdText);
					String dateFromText = record.get(SynpufColumnForPartB.CLM_FROM_DT);
					LocalDate dateFrom = LocalDate.parse(dateFromText, SYNPUF_DATE_FORMATTER);
					String dateThroughText = record.get(SynpufColumnForPartB.CLM_THRU_DT);
					LocalDate dateThrough = LocalDate.parse(dateThroughText, SYNPUF_DATE_FORMATTER);
					String diagnosisCode1 = record.get(SynpufColumnForPartB.ICD9_DGNS_CD_1);
					String diagnosisCode2 = record.get(SynpufColumnForPartB.ICD9_DGNS_CD_2);
					String diagnosisCode3 = record.get(SynpufColumnForPartB.ICD9_DGNS_CD_3);
					String diagnosisCode4 = record.get(SynpufColumnForPartB.ICD9_DGNS_CD_4);
					String diagnosisCode5 = record.get(SynpufColumnForPartB.ICD9_DGNS_CD_5);
					String diagnosisCode6 = record.get(SynpufColumnForPartB.ICD9_DGNS_CD_6);
					String diagnosisCode7 = record.get(SynpufColumnForPartB.ICD9_DGNS_CD_7);
					String diagnosisCode8 = record.get(SynpufColumnForPartB.ICD9_DGNS_CD_8);

					// Sanity check:
					if (claimsMap.containsKey(claimId)) {
						throw new IllegalStateException("Dupe claim: " + claimId);
					}

					PartBClaimFact claim = new PartBClaimFact();
					claimsMap.put(claimId, claim);
					claim.setId(claimId);
					claim.setBeneficiary(registry.getBeneficiary(synpufId));
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

						String lineDiagnosisCode = record.get(SynpufColumnForPartB.getLineIcd9DgnsCd(lineNumber));
						claimLine.setLineDiagnosisCode(lineDiagnosisCode);

						int claimLinePerformingPhysicianNpi = providerGenerator.generateProvider().getNpi();
						// TODO where to map PRF_PHYSN_NPI_#? Note: Gibberish
						// data! (Already mapped at claim level.)

						String taxNum = record.get(SynpufColumnForPartB.getTaxNum(lineNumber));
						// TODO where to map TAX_NUM_#? Note: Gibberish data!

						String hcpcsCd = record.get(SynpufColumnForPartB.getHcpcsCd(lineNumber));
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

						String nchPaymentAmountText = record.get(SynpufColumnForPartB.getLineNchPmtAmt(lineNumber));
						Double nchPaymentAmount = Double.parseDouble(nchPaymentAmountText);
						claimLine.setNchPaymentAmount(nchPaymentAmount);

						String deductibleAmountText = record
								.get(SynpufColumnForPartB.getLineBenePtbDdctblAmt(lineNumber));
						Double deductibleAmount = Double.parseDouble(deductibleAmountText);
						// TODO where to stick LINE_BENE_PTB_DDCTBL_AMT_#?

						String primaryPayerPaidAmountText = record
								.get(SynpufColumnForPartB.getLineBenePrmryPyrPdAmt(lineNumber));
						Double primaryPayerPaidAmount = Double.parseDouble(primaryPayerPaidAmountText);
						claimLine.setBeneficiaryPrimaryPayerPaidAmount(primaryPayerPaidAmount);

						String coinsuranceAmountText = record.get(SynpufColumnForPartB.getLineCoinsrncAmt(lineNumber));
						Double coinsuranceAmount = Double.parseDouble(coinsuranceAmountText);
						claimLine.setCoinsuranceAmount(coinsuranceAmount);

						String allowedAmountText = record.get(SynpufColumnForPartB.getLineAlowdChrgAmt(lineNumber));
						Double allowedAmount = Double.parseDouble(allowedAmountText);
						claimLine.setAllowedAmount(allowedAmount);

						String processingIndicationCode = record
								.get(SynpufColumnForPartB.getLinePrcsgIndCd(lineNumber));
						claimLine.setProcessingIndicationCode(processingIndicationCode);

						// TODO how to populate this?
						// claimLine.setSubmittedAmount(submittedAmount);

						// TODO how to populate this?
						// claimLine.setMiscCode(miscCode);

						if (!isMostlyBlank(claimLine))
							claim.getClaimLines().add(claimLine);
					}

					pm.makePersistent(claim);
				}
			} catch (IOException e) {
				throw new SampleDataException(e);
			}
			LOGGER.info("Processed DE-SynPUF file '{}'.", synpufSample.getPartAClaimsOutpatient().getFileName());
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
				&& isBlank(claimLine.getSubmittedAmount()) && isBlank(claimLine.getLineDiagnosisCode())
				&& claimLine.getMiscCode() == null && isBlank(claimLine.getNchPaymentAmount())
				&& isBlank(claimLine.getBeneficiaryPrimaryPayerPaidAmount())
				&& isBlank(claimLine.getCoinsuranceAmount()) && isBlank(claimLine.getProcessingIndicationCode());
	}

	/**
	 * TODO
	 * 
	 * @param synpufSample
	 * @param registry
	 */
	private void processPartDClaims(SynpufSample synpufSample, SharedDataRegistry registry) {
		SamplePrescriberGenerator prescriberGenerator = new SamplePrescriberGenerator();
		SamplePharmacyGenerator pharmacyGenerator = new SamplePharmacyGenerator();

		Path claimsCsv = synpufSample.getPartDClaims();
		LOGGER.info("Processing DE-SynPUF file '{}'...", claimsCsv.getFileName());
		try (Reader in = new FileReader(claimsCsv.toFile());) {
			CSVFormat csvFormat = CSVFormat.EXCEL.withHeader(SynpufColumnForPartD.getAllColumnNames())
					.withSkipHeaderRecord();
			Iterable<CSVRecord> records = csvFormat.parse(in);
			for (CSVRecord record : records) {
				LOGGER.trace("Processing DE-SynPUF Part D Outpatient record #{}.", record.getRecordNumber());

				String synpufId = record.get(SynpufColumnForPartD.DESYNPUF_ID);
				String eventIdText = record.get(SynpufColumnForPartD.PDE_ID);
				long eventId = Long.parseLong(eventIdText);
				String serviceDateText = record.get(SynpufColumnForPartD.SRVC_DT);
				LocalDate serviceDate = LocalDate.parse(serviceDateText, SYNPUF_DATE_FORMATTER);
				String productIdText = record.get(SynpufColumnForPartD.PROD_SRVC_ID);
				long productId = Long.parseLong(productIdText);
				String quantityText = record.get(SynpufColumnForPartD.QTY_DSPNSD_NUM);
				// FIXME some/all values have fractions, e.g. "30.000"
				long quantity = (long) Double.parseDouble(quantityText);
				String daysSupplyText = record.get(SynpufColumnForPartD.DAYS_SUPLY_NUM);
				long daysSupply = Long.parseLong(daysSupplyText);
				String patientPaymentText = record.get(SynpufColumnForPartD.PTNT_PAY_AMT);
				double patientPayment = Double.parseDouble(patientPaymentText);
				String prescriptionCostText = record.get(SynpufColumnForPartD.TOT_RX_CST_AMT);
				double prescriptionCost = Double.parseDouble(prescriptionCostText);

				PartDEventFact event = new PartDEventFact();
				event.setId(eventId);
				event.setPrescriberNpi((long) prescriberGenerator.generatePrescriber().getNpi());
				event.setServiceProviderNpi((long) pharmacyGenerator.generatePharmacy().getNpi());
				event.setProductNdc(productId);
				event.setBeneficiary(registry.getBeneficiary(synpufId));
				event.setServiceDate(serviceDate);
				event.setQuantityDispensed(quantity);
				event.setNumberDaysSupply(daysSupply);
				event.setPatientPayAmount(patientPayment);
				event.setTotalPrescriptionCost(prescriptionCost);

				pm.makePersistent(event);
			}
		} catch (IOException e) {
			throw new SampleDataException(e);
		}
		LOGGER.info("Processed DE-SynPUF file '{}'.", synpufSample.getPartAClaimsOutpatient().getFileName());
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
	 * A simple registry for shared data records that are created during a
	 * {@link SampleDataLoader#loadSampleData(Path, SynpufArchive...)}
	 * operation.
	 */
	private static final class SharedDataRegistry {
		private final Map<String, CurrentBeneficiary> beneficiariesBySynpufId = new HashMap<>();
		private final Map<String, Procedure> proceduresByCode = new HashMap<>();

		/**
		 * @return the matching {@link CurrentBeneficiary} that was passed to
		 *         {@link #register(CurrentBeneficiary)}, or <code>null</code>
		 *         if no such match is found
		 */
		public CurrentBeneficiary getBeneficiary(String synpufId) {
			return beneficiariesBySynpufId.get(synpufId);
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
	}
}
