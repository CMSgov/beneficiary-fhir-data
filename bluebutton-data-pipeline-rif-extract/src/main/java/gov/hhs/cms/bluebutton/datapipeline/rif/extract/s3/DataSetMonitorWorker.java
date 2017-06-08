package gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.SSEAwsKeyManagementParams;
import com.amazonaws.services.s3.transfer.Copy;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.datapipeline.rif.extract.ExtractionOptions;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetManifest.DataSetManifestId;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFilesEvent;

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
	/**
	 * The maximum number of data sets that might be pending at any one time.
	 * (This is only used to avoid memory leaks, so it should err on the high
	 * side.)
	 */
	private static final int MAX_EXPECTED_DATA_SETS_PENDING = 10000;

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

	private static final Pattern REGEX_COMPLETED_MANIFEST = Pattern
			.compile("^" + S3_PREFIX_COMPLETED_DATA_SETS + "\\/(.*)\\/([0-9]+)_manifest\\.xml$");

	private final ExtractionOptions options;
	private final DataSetMonitorListener listener;
	private final AmazonS3 s3Client;

	/**
	 * Tracks the {@link DataSetManifest#getTimestamp()} values of the most
	 * recently processed data sets, to ensure that the same data set isn't
	 * processed more than once. It's constrained to a fixed number of items, to
	 * keep it from becoming a memory leak.
	 */
	private CircularFifoQueue<DataSetManifestId> recentlyProcessedManifests;

	private CircularFifoQueue<DataSetManifestId> knownInvalidManifests;

	private Optional<Integer> s3MaxKeys = Optional.empty();

	/**
	 * Constructs a new {@link DataSetMonitorWorker} instance.
	 * 
	 * @param options
	 *            the {@link ExtractionOptions} to use
	 * @param listener
	 *            the {@link DataSetMonitorListener} to send events to
	 */
	public DataSetMonitorWorker(ExtractionOptions options, DataSetMonitorListener listener) {
		this.options = options;
		this.listener = listener;
		this.s3Client = S3Utilities.createS3Client(options);
		this.recentlyProcessedManifests = new CircularFifoQueue<>(MAX_EXPECTED_DATA_SETS_PENDING);
		this.knownInvalidManifests = new CircularFifoQueue<>(MAX_EXPECTED_DATA_SETS_PENDING);
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		LOGGER.info("Scanning for data sets to process...");

		/*
		 * Request a list of all objects in the configured bucket and directory.
		 * (In the results, we'll be looking for the oldest manifest file, if
		 * any.)
		 */
		ListObjectsV2Request s3BucketListRequest = new ListObjectsV2Request();
		s3BucketListRequest.setBucketName(options.getS3BucketName());
		if (s3MaxKeys.isPresent())
			s3BucketListRequest.setMaxKeys(s3MaxKeys.get());

		/*
		 * S3 will return results in separate pages. Loop through all of the
		 * pages, looking for the oldest manifest file that is available.
		 */
		DataSetManifest manifestToProcess = null;
		int pendingManifests = 0;
		int completedManifests = 0;
		ListObjectsV2Result s3ObjectListing;
		do {
			s3ObjectListing = s3Client.listObjectsV2(s3BucketListRequest);

			for (S3ObjectSummary objectSummary : s3ObjectListing.getObjectSummaries()) {
				String key = objectSummary.getKey();
				if (REGEX_PENDING_MANIFEST.matcher(key).matches()) {
					pendingManifests++;

					/*
					 * We've got an object that *looks like* it might be a
					 * manifest file. But we need to parse the key to ensure
					 * that it starts with a valid timestamp.
					 */
					DataSetManifestId manifestId = DataSetManifestId.parseManifestIdFromS3Key(key);
					if (manifestId == null)
						continue;

					/*
					 * Skip things that we already know are invalid or have
					 * already been processed.
					 */
					if (knownInvalidManifests.contains(manifestId))
						continue;
					if (recentlyProcessedManifests.contains(manifestId)) {
						LOGGER.debug("Skipping data set that was already processed: {}", manifestId);
						continue;
					}

					/*
					 * Check to see if this data set should be skipped, and if
					 * not, it it is the oldest one we've encountered so far. If
					 * so, we mark it as the "current oldest" and continue
					 * looking through the other keys.
					 */
					DataSetManifest manifest = null;

					try {
						manifest = readManifest(s3Client, options, key);
					} catch (JAXBException e) {
						// Note: We intentionally don't log the full stack trace
						// here, as it would add a lot of unneeded noise.
						LOGGER.warn("Found data set with invalid manifest at '{}'. It will be skipped. Error: {}", key,
								e.toString());
						knownInvalidManifests.add(manifestId);
						continue;
					}
					if (!options.getDataSetFilter().test(manifest)) {
						LOGGER.debug("Skipping data set that doesn't pass filter: {}", manifest.toString());
						continue;
					}

					if (manifestToProcess == null)
						manifestToProcess = manifest;
					else if (manifestId.compareTo(manifestToProcess.getId()) < 0)
						manifestToProcess = manifest;
				} else if (REGEX_COMPLETED_MANIFEST.matcher(key).matches()) {
					completedManifests++;
				}
			}

			s3BucketListRequest.setContinuationToken(s3ObjectListing.getNextContinuationToken());
		} while (s3ObjectListing.isTruncated());

		// If no manifest was found, we're done (until next time).
		if (manifestToProcess == null) {
			LOGGER.info(LOG_MESSAGE_NO_DATA_SETS);
			listener.noDataAvailable();
			return;
		}

		// We've found the oldest manifest.
		LOGGER.info(
				"Found data set to process: '{}'."
						+ " There were '{}' total pending data sets and '{}' completed ones.",
				manifestToProcess.toString(), pendingManifests, completedManifests);

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
				if(!alreadyLoggedWaitingEvent){
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
		Instant dataSetManifestTimestamp = manifestToProcess.getTimestamp();
		Set<S3RifFile> rifFiles = manifestToProcess.getEntries().stream().map(e -> {
			String key = String.format("%s/%s/%s", S3_PREFIX_PENDING_DATA_SETS,
					DateTimeFormatter.ISO_INSTANT.format(dataSetManifestTimestamp), e.getName());
			return new S3RifFile(s3Client, e.getType(), new GetObjectRequest(options.getS3BucketName(), key));
		}).collect(Collectors.toSet());
		RifFilesEvent rifFilesEvent = new RifFilesEvent(manifestToProcess.getTimestamp(), new HashSet<>(rifFiles));

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
		recentlyProcessedManifests.add(manifestToProcess.getId());
			markDataSetCompleteInS3(manifestToProcess);
	}

	/**
	 * @param s3Client
	 *            the {@link AmazonS3} client to use
	 * @param options
	 *            the {@link ExtractionOptions} to use
	 * @param manifestToProcessKey
	 *            the {@link S3Object#getKey()} of the S3 object for the
	 *            manifest to be read
	 * @return the {@link DataSetManifest} that was contained in the specified
	 *         S3 object
	 * @throws JAXBException
	 *             Any {@link JAXBException}s that are encountered will be
	 *             bubbled up. These generally indicate that the
	 *             {@link DataSetManifest} could not be parsed because its
	 *             content was invalid in some way. Note: As of 2017-03-24, this
	 *             has been observed multiple times in production, and care
	 *             should be taken to account for its possibility.
	 */
	public static DataSetManifest readManifest(AmazonS3 s3Client, ExtractionOptions options,
			String manifestToProcessKey) throws JAXBException {
		try (S3Object manifestObject = s3Client.getObject(options.getS3BucketName(), manifestToProcessKey)) {
			JAXBContext jaxbContext = JAXBContext.newInstance(DataSetManifest.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

			DataSetManifest manifest = (DataSetManifest) jaxbUnmarshaller.unmarshal(manifestObject.getObjectContent());

			return manifest;
		} catch (AmazonServiceException e) {
			/*
			 * This could likely be retried, but we don't currently support
			 * that. For now, just go boom.
			 */
			throw new RuntimeException(e);
		} catch (AmazonClientException e) {
			/*
			 * This could likely be retried, but we don't currently support
			 * that. For now, just go boom.
			 */
			throw new RuntimeException(e);
		} catch (IOException e) {
			/*
			 * This could likely be retried, but we don't currently support
			 * that. For now, just go boom.
			 */
			throw new RuntimeException(e);
		}
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
		if (s3MaxKeys.isPresent())
			s3BucketListRequest.setMaxKeys(s3MaxKeys.get());

		Set<String> dataSetObjectNames = new HashSet<>();
		ListObjectsV2Result s3ObjectListing;
		do {
			s3ObjectListing = s3Client.listObjectsV2(s3BucketListRequest);

			/*
			 * Pull the object names from the keys that were returned, by
			 * stripping the timestamp prefix and slash from each of them.
			 */
			Set<String> namesForObjectsInPage = s3ObjectListing.getObjectSummaries().stream().map(s -> s.getKey())
					.peek(s -> LOGGER.debug("Found object: '{}'", s)).map(k -> k.substring(dataSetKeyPrefix.length()))
					.collect(Collectors.toSet());
			dataSetObjectNames.addAll(namesForObjectsInPage);

			// On to the next page! (If any.)
			s3BucketListRequest.setContinuationToken(s3ObjectListing.getNextContinuationToken());
		} while (s3ObjectListing.isTruncated());

		for (DataSetManifestEntry manifestEntry : manifest.getEntries()) {
			if (!dataSetObjectNames.contains(manifestEntry.getName()))
				return false;
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
		s3KeySuffixesToMove.add(
				String.format("%s/%d_manifest.xml",
						DateTimeFormatter.ISO_INSTANT.format(dataSetManifest.getTimestamp()),
						dataSetManifest.getSequenceId()));

		/*
		 * Then, loop through each of those objects and copy them (S3 has no
		 * bulk copy operation).
		 */
		TransferManager transferManager = TransferManagerBuilder.standard().withS3Client(s3Client).build();
		for (String s3KeySuffixToMove : s3KeySuffixesToMove) {
			String sourceKey = String.format("%s/%s", S3_PREFIX_PENDING_DATA_SETS, s3KeySuffixToMove);
			String targetKey = String.format("%s/%s", S3_PREFIX_COMPLETED_DATA_SETS, s3KeySuffixToMove);

			/*
			 * Before copying, grab the metadata of the source object to ensure
			 * that we maintain its encryption settings (by default, the copy
			 * will maintain all metadata EXCEPT: server-side-encryption,
			 * storage-class, and website-redirect-location).
			 */
			ObjectMetadata objectMetadata = s3Client.getObjectMetadata(options.getS3BucketName(), sourceKey);
			CopyObjectRequest copyRequest = new CopyObjectRequest(options.getS3BucketName(), sourceKey,
					options.getS3BucketName(), targetKey);
			if (objectMetadata.getSSEAwsKmsKeyId() != null)
			{
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
		s3Client.deleteObjects(deleteObjectsRequest);
		LOGGER.debug("Data set deleted in S3 (step 2 of move).");

		LOGGER.info(LOG_MESSAGE_DATA_SET_COMPLETE);
	}

	/**
	 * Note: This method is intended for test purposes: setting this value to
	 * <code>1</code> in tests can help to verify the S3 paging logic.
	 * 
	 * @param s3MaxKeys
	 *            the value to use for
	 *            {@link ListObjectsV2Request#setMaxKeys(Integer)} in all S3
	 *            list operations
	 */
	void setS3MaxKeys(int s3MaxKeys) {
		this.s3MaxKeys = Optional.of(s3MaxKeys);
	}
}