package gov.hhs.cms.bluebutton.datapipeline.rif.extract;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;
import com.justdavis.karl.misc.exceptions.unchecked.UncheckedIoException;

import gov.hhs.cms.bluebutton.datapipeline.rif.model.BeneficiaryRow;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RecordAction;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFileType;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFilesEvent;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifRecordEvent;

/**
 * Contains services responsible for handling new RIF files.
 */
public final class RifFilesProcessor {
	/**
	 * <p>
	 * Processes the specified {@link RifFilesEvent}, reading in the data found
	 * in it and converting that data into {@link RifRecordEvent}s that can be
	 * handled downstream in the ETL pipeline.
	 * </p>
	 * <h4>Design Notes</h4>
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
		 * https://rumianom.pl/rumianom/entry/apache-commons-csv-with-java
		 */

		InputStreamReader reader = new InputStreamReader(file.open(), file.getCharset());
		try (CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withDelimiter('|').withEscape('\\'));) {
			Stream<CSVRecord> csvRecordStream = StreamSupport.stream(parser.spliterator(), false);

			Stream<RifRecordEvent<?>> rifRecordStream;
			if (file.getFileType() == RifFileType.BENEFICIARY)
				rifRecordStream = buildBeneficiaryEvents(rifFilesEvent, file, parser, csvRecordStream);
			else
				throw new BadCodeMonkeyException();

			return rifRecordStream;
		} catch (IOException e) {
			// TODO first attempt retries, with exponential backoff
			// If an input file really can't be read, stop the ETL for triage.
			throw new UncheckedIoException(e);
		}
	}

	/**
	 * @param filesEvent
	 *            the {@link RifFilesEvent} that is being processed
	 * @param file
	 *            the specific {@link RifFile} that the {@link CSVRecord}s are
	 *            from
	 * @param parser
	 *            the {@link CSVParser} that was used to create the
	 *            {@link Stream} of {@link CSVRecord}s
	 * @param csvRecordStream
	 *            the {@link Stream} of beneficiary {@link CSVRecord}s to be
	 *            mapped
	 * @return a mapping of the specified {@link CSVRecord} {@link Stream} to a
	 *         {@link Stream} of {@link RifRecordEvent}s for beneficiaries
	 */
	private Stream<RifRecordEvent<?>> buildBeneficiaryEvents(RifFilesEvent filesEvent, RifFile file, CSVParser parser,
			Stream<CSVRecord> csvRecordStream) {
		return csvRecordStream.map(csvRecord -> {
			BeneficiaryRow beneficiaryRow = new BeneficiaryRow();
			beneficiaryRow.version = Integer.parseInt(csvRecord.get(BeneficiaryRow.Column.VERSION.ordinal()));
			beneficiaryRow.recordAction = RecordAction.match(csvRecord.get(BeneficiaryRow.Column.DML_IND.ordinal()));
			beneficiaryRow.beneficiaryId = csvRecord.get(BeneficiaryRow.Column.BENE_ID.ordinal());
			beneficiaryRow.stateCode = csvRecord.get(BeneficiaryRow.Column.STATE_CODE.ordinal());
			beneficiaryRow.countyCode = csvRecord.get(BeneficiaryRow.Column.BENE_COUNTY_CD.ordinal());
			beneficiaryRow.postalCode = csvRecord.get(BeneficiaryRow.Column.BENE_ZIP_CD.ordinal());
			// TODO finish mapping the rest of the columns

			// Sanity check:
			if (!"1".equals(beneficiaryRow.version))
				throw new IllegalArgumentException("Unsupported record version: " + beneficiaryRow);

			return new RifRecordEvent<BeneficiaryRow>(filesEvent, file, beneficiaryRow);
		});
	}
}
