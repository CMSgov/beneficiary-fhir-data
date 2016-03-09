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

import gov.hhs.cms.bluebutton.datapipeline.desynpuf.SynpufArchive;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.SynpufSample;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.SynpufSampleLoader;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.SynpufSummaryColumn;

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

	public SampleDataSummary loadSampleData(Path workDir) throws SampleDataException {
		// Extract the DE-SynPUF CSV files.
		Path synpufDir = workDir.resolve("blue-button-de-synpuf");
		List<SynpufSample> synpufSamples = null;
		synpufSamples = Arrays.stream(SynpufArchive.values())
				.map(a -> SynpufSampleLoader.extractSynpufFile(synpufDir, a)).collect(Collectors.toList());

		// Load the other sample data sets.
		// TODO

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
				Map<String, Integer> synpufToCcwIds = new HashMap<>();

				// Process the beneficiary summaries.
				for (Path summaryCsv : synpufSample.getBeneficiarySummaries()) {
					try (Reader in = new FileReader(summaryCsv.toFile());) {
						CSVFormat csvFormat = CSVFormat.EXCEL.withHeader(SynpufSummaryColumn.getAllColumnNames())
								.withSkipHeaderRecord();
						Iterable<CSVRecord> records = csvFormat.parse(in);
						for (CSVRecord record : records) {
							LOGGER.trace("Processing DE-SynPUF record #{}.", record.getRecordNumber());

							String synpufId = record.get(SynpufSummaryColumn.DESYNPUF_ID);
							String birthDateText = record.get(SynpufSummaryColumn.BENE_BIRTH_DT);
							LocalDate birthDate = LocalDate.parse(birthDateText, SYNPUF_DATE_FORMATTER);

							CurrentBeneficiary bene = new CurrentBeneficiary();
							synpufToCcwIds.put(synpufId, synpufToCcwIds.size());
							bene.setId(synpufToCcwIds.get(synpufId));
							bene.setBirthDate(birthDate);

							pm.makePersistent(bene);
						}
					} catch (IOException e) {
						throw new SampleDataException(e);
					}
				}

				// Process the Part A claims.

				// Process the Part B claims.

				// Process the Part D claims.

				// Commit the transaction.
				tx.commit();
			} finally {
				if (tx.isActive())
					tx.rollback();
			}
		}

		// TODO
		return null;
	}
}
