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
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.SynpufArchive;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.SynpufColumnForBeneficiarySummary;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.SynpufColumnForPartAOutpatient;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.SynpufSample;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.SynpufSampleLoader;

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

		// Process each DE-SynPUF sample.
		for (SynpufSample synpufSample : synpufSamples) {
			Transaction tx = pm.currentTransaction();
			try {
				// Start the transaction: each sample gets its own TX.
				tx.begin();

				/*
				 * In DE-SynPUF, beneficiaries' ID is arbitrary text. In the
				 * CCW, those IDs are an integer. This Map keeps track of the
				 * problem.
				 */
				Map<String, CurrentBeneficiary> synpufIdsToBeneficiaries = new HashMap<>();

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
							if (synpufIdsToBeneficiaries.containsKey(synpufId)) {
								bene = synpufIdsToBeneficiaries.get(synpufId);
							} else {
								bene = new CurrentBeneficiary();
								bene.setId(synpufIdsToBeneficiaries.size());
							}

							bene.setBirthDate(birthDate);
							SampleName name = nameGenerator.generateName();
							bene.setGivenName(name.getFirstName());
							bene.setSurname(name.getLastName());

							pm.makePersistent(bene);
							synpufIdsToBeneficiaries.put(synpufId, bene);
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
							claim.setBeneficiary(synpufIdsToBeneficiaries.get(synpufId));
							claim.setAdmittingDiagnosisCode(diagnosisCodeText);

							pm.makePersistent(claim);
							claimsMap.put(claimId, claim);
						}
					}
				} catch (IOException e) {
					throw new SampleDataException(e);
				}
				LOGGER.info("Processed DE-SynPUF file '{}'...", synpufSample.getPartAClaimsOutpatient().getFileName());

				// Process the Part B claims.

				// Process the Part D claims.

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
}
