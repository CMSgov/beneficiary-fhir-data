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
import rx.Observable;

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
	public Observable<RifRecordEvent<?>> process(RifFilesEvent event) {
		/*
		 * Here Be Dragons. YMMV, but I'm new to reactive streams so this took
		 * me a long while to wrap my head around and write. If this were being
		 * done imperatively, we'd basically want something like 1) sort files
		 * correctly (to obey rules described in JavaDoc above), 2) process the
		 * files one-by-one (single threaded), chopping them up into the groups
		 * of rows that represent benes/claims/etc., 3) hand those rows off to a
		 * thread pool to be Transformed and Loaded. The key thing I was missing
		 * here for a while is that the files HAVE to be processed one-by-one;
		 * you can only run one file at a time. Otherwise, you're going to end
		 * up violating the order rules. I mean, sure, you could process all of
		 * the non-bene files from the same update-set in parallel, but why? It
		 * would make the logic much more complex, and besides, the files are
		 * each more than large enough to keep the Transform and Load threads
		 * saturated. Thinking about it like this makes the Reactive version
		 * quite a bit simpler, I think.
		 */

		List<RifFile> filesOrderedSafely = new ArrayList<>(event.getFiles());
		Comparator<RifFile> someComparator = null;
		Collections.sort(filesOrderedSafely, someComparator);

		Observable<RifRecordEvent<?>> recordProducer = Observable.from(filesOrderedSafely)
				.flatMap(file -> produceRecords(event, file));
		return recordProducer;
	}

	/**
	 * @param rifFilesEvent
	 *            the {@link RifFilesEvent} that is being processed
	 * @param file
	 *            the {@link RifFile} to produce {@link RifRecordEvent}s from
	 * @return an {@link Observable} that produces the {@link RifRecordEvent}s
	 *         represented in the specified {@link RifFile}
	 */
	private Observable<RifRecordEvent<?>> produceRecords(RifFilesEvent rifFilesEvent, RifFile file) {
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

			return Observable.from(rifRecordStream::iterator);
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
