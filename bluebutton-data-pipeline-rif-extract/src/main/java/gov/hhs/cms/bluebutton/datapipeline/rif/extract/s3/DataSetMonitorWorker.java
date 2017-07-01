package gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.SSEAwsKeyManagementParams;
import com.amazonaws.services.s3.transfer.Copy;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.data.model.rif.RifFilesEvent;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.ExtractionOptions;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetManifest.DataSetManifestId;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetQueue.QueuedDataSet;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.task.S3TaskManager;

/**
 * <p>
 * Acts as a worker {@link Runnable} for {@link DataSetMonitor}. It is expected
 * that this will be run on a repeating basis, via a
 * {@link ScheduledExecutorService} .
 * </p>
 * <p>
 * When executed via {@link #run()}, the {@link DataSetMonitorWorker} will scan
 * the specified Amazon S3 bucket. It will look for <code>manifest.xml</code>
 * objects/files and select the oldest one available. If such a manifest is
 * found, it will then wait for all of the objects in the data set represented
 * by it to become available. Once they're all available, it will kick off the
 * processing of the data set, and block until that processing has completed.
 * </p>
 */
public final class DataSetMonitorWorker implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(DataSetMonitorWorker.class);

	/**
	 * The directory name that pending/incoming RIF data sets will be pulled
	 * from in S3.
	 */
	public static final String S3_PREFIX_PENDING_DATA_SETS = "Incoming";

	/**
	 * The directory name that completed/done RIF data sets will be moved to in
	 * S3.
	 */
	public static final String S3_PREFIX_COMPLETED_DATA_SETS = "Done";

	/**
	 * The {@link Logger} message that will be recorded if/when the
	 * {@link DataSetMonitorWorker} goes and looks, but doesn't find any data
	 * sets waiting to be processed.
	 */
	public static final String LOG_MESSAGE_NO_DATA_SETS = "No data sets to process found.";

	/**
	 * The {@link Logger} message that will be recorded if/when the
	 * {@link DataSetMonitorWorker} completes the processing of a data set.
	 */
	public static final String LOG_MESSAGE_DATA_SET_COMPLETE = "Data set renamed in S3, now that processing is complete.";

	/**
	 * A regex for {@link DataSetManifest} keys in S3. Provides capturing groups
	 * for the {@link DataSetManifestId} fields.
	 */
	public static final Pattern REGEX_PENDING_MANIFEST = Pattern
			.compile("^" + S3_PREFIX_PENDING_DATA_SETS + "\\/(.*)\\/([0-9]+)_manifest\\.xml$");

	static final Pattern REGEX_COMPLETED_MANIFEST = Pattern
			.compile("^" + S3_PREFIX_COMPLETED_DATA_SETS + "\\/(.*)\\/([0-9]+)_manifest\\.xml$");

	private final MetricRegistry appMetrics;
	private final ExtractionOptions options;
	private final S3TaskManager s3TaskManager;
	private final DataSetMonitorListener listener;

	private final DataSetQueue dataSetQueue;

	/**
	 * Constructs a new {@link DataSetMonitorWorker} instance.
	 * 
	 * @param appMetrics
	 *            the {@link MetricRegistry} for the overall application
	 * @param options
	 *            the {@link ExtractionOptions} to use
	 * @param s3TaskManager
	 *            the {@link S3TaskManager} to use
	 * @param listener
	 *            the {@link DataSetMonitorListener} to send events to
	 */
	public DataSetMonitorWorker(MetricRegistry appMetrics, ExtractionOptions options, S3TaskManager s3TaskManager,
			DataSetMonitorListener listener) {
		this.appMetrics = appMetrics;
		this.options = options;
		this.s3TaskManager = s3TaskManager;
		this.listener = listener;

		this.dataSetQueue = new DataSetQueue(appMetrics, options, s3TaskManager);
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		LOGGER.info("Scanning for data sets to process...");

		// Update the queue from S3.
		dataSetQueue.updatePendingDataSets();

		// If no manifest was found, we're done (until next time).
		if (dataSetQueue.isEmpty()) {
			LOGGER.info(LOG_MESSAGE_NO_DATA_SETS);
			listener.noDataAvailable();
			return;
		}

		// We've found the oldest manifest.
		QueuedDataSet dataSetToProcess = dataSetQueue.getNextDataSetToProcess().get();
		DataSetManifest manifestToProcess = dataSetToProcess.getManifest();
		LOGGER.info(
				"Found data set to process: '{}'."
						+ " There were '{}' total pending data sets and '{}' completed ones.",
				manifestToProcess.toString(), dataSetQueue.getPendingManifestsCount(),
				dataSetQueue.getCompletedManifestsCount().get());

		/*
		 * We've got a data set to process. However, it might still be uploading
		 * to S3, so we need to wait for that to complete before we start
		 * processing it.
		 */
		boolean alreadyLoggedWaitingEvent = false;
		while (!dataSetIsAvailable(manifestToProcess)) {
			/*
			 * We're very patient here, so we keep looping, but it's prudent to
			 * pause between each iteration. TODO should eventually time out,
			 * once we know how long transfers might take
			 */
			try {
				if (!alreadyLoggedWaitingEvent) {
					LOGGER.info("Data set not ready. Waiting for it to finish uploading...");
					alreadyLoggedWaitingEvent = true;
				}
				Thread.sleep(1000 * 1);
			} catch (InterruptedException e) {
				/*
				 * Many Java applications use InterruptedExceptions to signal
				 * that a thread should stop what it's doing ASAP. This app
				 * doesn't, so this is unexpected, and accordingly, we don't
				 * know what to do. Safest bet is to blow up.
				 */
				throw new RuntimeException(e);
			}
		}

		/*
		 * Huzzah! We've got a data set to process and we've verified it's all
		 * there and ready to go.
		 */
		LOGGER.info("Data set ready. Processing it...");
		List<S3RifFile> rifFiles = manifestToProcess.getEntries().stream()
				.map(manifestEntry -> new S3RifFile(appMetrics, manifestEntry,
						dataSetToProcess.getManifestEntryDownloads().get(manifestEntry)))
				.collect(Collectors.toList());
		RifFilesEvent rifFilesEvent = new RifFilesEvent(manifestToProcess.getTimestamp(), new ArrayList<>(rifFiles));

		/*
		 * Now we hand that off to the DataSetMonitorListener, to do the *real*
		 * work of actually processing that data set. It's important that we
		 * block until it's completed, in order to ensure that we don't end up
		 * processing multiple data sets in parallel (which would lead to data
		 * consistency problems).
		 */
		listener.dataAvailable(rifFilesEvent);

		/*
		 * Now that the data set has been processed, we need to ensure that we
		 * don't end up processing it again. We ensure this two ways: 1) we keep
		 * a list of the data sets most recently processed, and 2) we rename the
		 * S3 objects that comprise that data set. (#1 is required as S3
		 * deletes/moves are only *eventually* consistent, so #2 may not take
		 * effect right away.)
		 */
		rifFiles.stream().forEach(f -> f.cleanupTempFile());
		dataSetQueue.markProcessed(manifestToProcess);
		markDataSetCompleteInS3(manifestToProcess);
	}

	/**
	 * @param manifest
	 *            the {@link DataSetManifest} that lists the objects to verify
	 *            the presence of
	 * @return <code>true</code> if all of the objects listed in the specified
	 *         manifest can be found in S3, <code>false</code> if not
	 */
	private boolean dataSetIsAvailable(DataSetManifest manifest) {
		/*
		 * There are two ways to do this: 1) list all the objects in the data
		 * set and verify the ones we're looking for are there after, or 2) try
		 * to grab the metadata for each object. Option #2 *should* be simpler,
		 * but isn't, because each missing object will result in an exception.
		 * Exceptions-as-control-flow is a poor design choice, so we'll go with
		 * option #1.
		 */

		String dataSetKeyPrefix = String.format("%s/%s/", S3_PREFIX_PENDING_DATA_SETS,
				DateTimeFormatter.ISO_INSTANT.format(manifest.getTimestamp()));

		ListObjectsV2Request s3BucketListRequest = new ListObjectsV2Request();
		s3BucketListRequest.setBucketName(options.getS3BucketName());
		s3BucketListRequest.setPrefix(dataSetKeyPrefix);
		if (options.getS3ListMaxKeys().isPresent())
			s3BucketListRequest.setMaxKeys(options.getS3ListMaxKeys().get());

		Set<String> dataSetObjectNames = new HashSet<>();
		ListObjectsV2Result s3ObjectListing;
		do {
			s3ObjectListing = s3TaskManager.getS3Client().listObjectsV2(s3BucketListRequest);

			/*
			 * Pull the object names from the keys that were returned, by
			 * stripping the timestamp prefix and slash from each of them.
			 */
			Set<String> namesForObjectsInPage = s3ObjectListing.getObjectSummaries().stream().map(s -> s.getKey())
					.peek(s -> LOGGER.debug("Found file: '{}', part of data set: '{}'.", s, manifest))
					.map(k -> k.substring(dataSetKeyPrefix.length())).collect(Collectors.toSet());
			dataSetObjectNames.addAll(namesForObjectsInPage);

			// On to the next page! (If any.)
			s3BucketListRequest.setContinuationToken(s3ObjectListing.getNextContinuationToken());
		} while (s3ObjectListing.isTruncated());

		for (DataSetManifestEntry manifestEntry : manifest.getEntries()) {
			if (!dataSetObjectNames.contains(manifestEntry.getName())) {
				LOGGER.debug("Waiting for file '{}', part of data set: '{}'.", manifestEntry.getName(), manifest);
				return false;
			}
		}

		return true;
	}

	/**
	 * Deletes the (now complete) data set in S3, after copying it to a
	 * "<code>Done</code>" folder in the same bucket.
	 * 
	 * @param dataSetManifest
	 *            the {@link DataSetManifest} of the data set to mark as
	 *            complete
	 */
	private void markDataSetCompleteInS3(DataSetManifest dataSetManifest) {
		Timer.Context timerS3Cleanup = appMetrics.timer(MetricRegistry.name(getClass().getSimpleName(), "s3Cleanup"))
				.time();
		LOGGER.info("Renaming data set in S3, now that processing is complete...");

		/*
		 * S3 doesn't support batch/transactional operations, or an atomic move
		 * operation. Instead, we have to first copy all of the objects to their
		 * new location, and then remove the old objects. If something blows up
		 * in the middle of this method, orphaned S3 object WILL be created.
		 * That's an unlikely enough occurrence, though, that we're not going to
		 * engineer around it right now.
		 */

		// First, get a list of all the object keys to work on.
		List<String> s3KeySuffixesToMove = dataSetManifest.getEntries()
				.stream().map(e -> String.format("%s/%s",
						DateTimeFormatter.ISO_INSTANT.format(dataSetManifest.getTimestamp()), e.getName()))
				.collect(Collectors.toList());
		s3KeySuffixesToMove.add(String.format("%s/%d_manifest.xml",
				DateTimeFormatter.ISO_INSTANT.format(dataSetManifest.getTimestamp()), dataSetManifest.getSequenceId()));

		/*
		 * Then, loop through each of those objects and copy them (S3 has no
		 * bulk copy operation).
		 */
		TransferManager transferManager = TransferManagerBuilder.standard().withS3Client(s3TaskManager.getS3Client())
				.build();
		for (String s3KeySuffixToMove : s3KeySuffixesToMove) {
			String sourceKey = String.format("%s/%s", S3_PREFIX_PENDING_DATA_SETS, s3KeySuffixToMove);
			String targetKey = String.format("%s/%s", S3_PREFIX_COMPLETED_DATA_SETS, s3KeySuffixToMove);

			/*
			 * Before copying, grab the metadata of the source object to ensure
			 * that we maintain its encryption settings (by default, the copy
			 * will maintain all metadata EXCEPT: server-side-encryption,
			 * storage-class, and website-redirect-location).
			 */
			ObjectMetadata objectMetadata = s3TaskManager.getS3Client().getObjectMetadata(options.getS3BucketName(),
					sourceKey);
			CopyObjectRequest copyRequest = new CopyObjectRequest(options.getS3BucketName(), sourceKey,
					options.getS3BucketName(), targetKey);
			if (objectMetadata.getSSEAwsKmsKeyId() != null) {
				copyRequest.setSSEAwsKeyManagementParams(
						new SSEAwsKeyManagementParams(objectMetadata.getSSEAwsKmsKeyId()));
			}

			Copy copyOperation = transferManager.copy(copyRequest);
			try {
				copyOperation.waitForCopyResult();
			} catch (InterruptedException e) {
				throw new BadCodeMonkeyException(e);
			}
		}
		transferManager.shutdownNow(false);
		LOGGER.debug("Data set copied in S3 (step 1 of move).");

		DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(options.getS3BucketName());
		deleteObjectsRequest
				.setKeys(s3KeySuffixesToMove.stream().map(k -> String.format("%s/%s", S3_PREFIX_PENDING_DATA_SETS, k))
						.map(k -> new KeyVersion(k)).collect(Collectors.toList()));
		s3TaskManager.getS3Client().deleteObjects(deleteObjectsRequest);
		LOGGER.debug("Data set deleted in S3 (step 2 of move).");

		LOGGER.info(LOG_MESSAGE_DATA_SET_COMPLETE);
		timerS3Cleanup.close();
	}
}