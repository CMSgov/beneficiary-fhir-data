package gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import gov.hhs.cms.bluebutton.datapipeline.rif.extract.ExtractionOptions;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetManifest.DataSetManifestId;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.task.ManifestEntryDownloadTask;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.task.ManifestEntryDownloadTask.ManifestEntryDownloadResult;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.task.S3TaskManager;

/**
 * Represents and manages the queue of data sets in S3 to be processed.
 */
public final class DataSetQueue {
	private static final Logger LOGGER = LoggerFactory.getLogger(DataSetMonitorWorker.class);
	private static final int GIGA = 1000 * 1000 * 1000;

	private final MetricRegistry appMetrics;
	private final ExtractionOptions options;
	private final S3TaskManager s3TaskManager;

	/**
	 * The {@link DataSetManifest}s waiting to be processed, ordered by their
	 * {@link DataSetManifestId} in ascending order such that the first element
	 * represents the {@link DataSetManifest} that should be processed next.
	 */
	private final SortedSet<DataSetManifest> manifestsToProcess;

	/**
	 * Tracks the asynchronous downloads of {@link DataSetManifestEntry}s, which
	 * will produce {@link ManifestEntryDownloadResult}.
	 */
	private final Map<DataSetManifestEntry, Future<ManifestEntryDownloadResult>> manifestEntryDownloads;

	/**
	 * Tracks the {@link DataSetManifest#getTimestamp()} values of the most
	 * recently processed data sets, to ensure that the same data set isn't
	 * processed more than once.
	 */
	private final Set<DataSetManifestId> recentlyProcessedManifests;

	/**
	 * Tracks the {@link DataSetManifestId}s of data sets that are known to be
	 * invalid. Typically, these are data sets from a new schema version that
	 * aren't supported yet. This may seem unnecessary (i.e.
	 * "don't let admins push things until they're supported"), but it's proven
	 * to be very useful, operationally.
	 */
	private final Set<DataSetManifestId> knownInvalidManifests;

	private Integer completedManifestsCount;

	/**
	 * Constructs a new {@link DataSetQueue} instance.
	 * 
	 * @param appMetrics
	 *            the {@link MetricRegistry} for the overall application
	 * @param options
	 *            the {@link ExtractionOptions} to use
	 * @param s3TaskManager
	 *            the {@link S3TaskManager} to use
	 */
	public DataSetQueue(MetricRegistry appMetrics, ExtractionOptions options, S3TaskManager s3TaskManager) {
		this.appMetrics = appMetrics;
		this.options = options;
		this.s3TaskManager = s3TaskManager;

		this.manifestsToProcess = new TreeSet<>();
		this.manifestEntryDownloads = new HashMap<>();
		this.recentlyProcessedManifests = new HashSet<>();
		this.knownInvalidManifests = new HashSet<>();
	}

	/**
	 * Updates {@link #manifestsToProcess}, listing the manifests available in
	 * S3 right now, then adding those that weren't found before and removing
	 * those that are no longer pending.
	 */
	public void updatePendingDataSets() {
		// Find the pending manifests.
		Set<DataSetManifestId> manifestIdsPendingNow = listPendingManifests();

		/*
		 * Add any newly discovered manifests to the list of those to be
		 * processed. Ignore those that are already known to be invalid or
		 * complete, and watch out for newly-discovered-to-be-invalid ones.
		 */
		Set<DataSetManifestId> newManifests = new HashSet<>(manifestIdsPendingNow);
		newManifests.removeAll(knownInvalidManifests);
		newManifests.removeAll(recentlyProcessedManifests);
		newManifests.removeAll(manifestsToProcess.stream().map(m -> m.getId()).collect(Collectors.toSet()));
		newManifests.stream().forEach(manifestId -> {
			String manifestS3Key = manifestId.computeS3Key(DataSetMonitorWorker.S3_PREFIX_PENDING_DATA_SETS);
			DataSetManifest manifest = null;
			try {
				manifest = readManifest(s3TaskManager.getS3Client(), options, manifestS3Key);
			} catch (JAXBException e) {
				// Note: We intentionally don't log the full stack trace
				// here, as it would add a lot of unneeded noise.
				LOGGER.warn("Found data set with invalid manifest at '{}'. It will be skipped. Error: {}",
						manifestS3Key, e.toString());
				knownInvalidManifests.add(manifestId);
				return;
			}

			// Finally, ensure that the manifest passes the options filter.
			if (!options.getDataSetFilter().test(manifest)) {
				LOGGER.debug("Skipping data set that doesn't pass filter: {}", manifest.toString());
				return;
			}

			// Everything checks out. Add it to the list!
			manifestsToProcess.add(manifest);
		});

		/*
		 * Any manifests that weren't found have presumably been processed and
		 * we should clean up the state that relates to them, to prevent memory
		 * leaks.
		 */
		for (Iterator<DataSetManifest> manifestsToProcessIterator = manifestsToProcess
				.iterator(); manifestsToProcessIterator.hasNext();) {
			DataSetManifestId manifestId = manifestsToProcessIterator.next().getId();
			if (!manifestIdsPendingNow.contains(manifestId)) {
				manifestsToProcessIterator.remove();
				knownInvalidManifests.remove(manifestId);
				recentlyProcessedManifests.remove(manifestId);
				manifestEntryDownloads.entrySet()
						.removeIf(e -> e.getKey().getParentManifest().getId().equals(manifestId));
			}
		}

		/*
		 * Ensure that the upcoming data sets are being downloaded
		 * asynchronously.
		 */
		AtomicInteger manifestToProcessIndex = new AtomicInteger(0);
		for (Iterator<DataSetManifest> manifestsToProcessIterator = manifestsToProcess
				.iterator(); manifestsToProcessIterator.hasNext(); manifestToProcessIndex.incrementAndGet()) {
			DataSetManifest manifestToProcess = manifestsToProcessIterator.next();
			manifestToProcess.getEntries().forEach(manifestEntry -> {
				// Only create one download task per manifest entry.
				if (manifestEntryDownloads.containsKey(manifestEntry))
					return;

				/*
				 * Don't start more downloads if we're low on disk space, unless
				 * this is the very next data set to process.
				 */
				Path tmpdir = Paths.get(System.getProperty("java.io.tmpdir"));
				long usableFreeTempSpace;
				try {
					usableFreeTempSpace = Files.getFileStore(tmpdir).getUsableSpace();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
				if (manifestToProcessIndex.get() > 0 && usableFreeTempSpace < 50 * GIGA)
					return;

				Future<ManifestEntryDownloadResult> manifestEntryDownload = s3TaskManager
						.submit(new ManifestEntryDownloadTask(s3TaskManager, options, manifestEntry));
				manifestEntryDownloads.put(manifestEntry, manifestEntryDownload);
			});
		}
	}

	/**
	 * @return the {@link DataSetManifestId}s for the manifests that are found
	 *         in S3 under the {@value #S3_PREFIX_PENDING_DATA_SETS} key prefix,
	 *         sorted in expected processing order.
	 */
	private Set<DataSetManifestId> listPendingManifests() {
		Timer.Context timerS3Scanning = appMetrics.timer(MetricRegistry.name(getClass().getSimpleName(), "s3Scanning"))
				.time();
		LOGGER.debug("Scanning for data sets in S3...");
		Set<DataSetManifestId> manifestIds = new HashSet<>();

		/*
		 * Request a list of all objects in the configured bucket and directory.
		 * (In the results, we'll be looking for the oldest manifest file, if
		 * any.)
		 */
		ListObjectsV2Request s3BucketListRequest = new ListObjectsV2Request();
		s3BucketListRequest.setBucketName(options.getS3BucketName());
		if (options.getS3ListMaxKeys().isPresent())
			s3BucketListRequest.setMaxKeys(options.getS3ListMaxKeys().get());

		/*
		 * S3 will return results in separate pages. Loop through all of the
		 * pages, looking for manifests.
		 */
		int completedManifestsCount = 0;
		ListObjectsV2Result s3ObjectListing;
		do {
			s3ObjectListing = s3TaskManager.getS3Client().listObjectsV2(s3BucketListRequest);

			for (S3ObjectSummary objectSummary : s3ObjectListing.getObjectSummaries()) {
				String key = objectSummary.getKey();
				if (DataSetMonitorWorker.REGEX_PENDING_MANIFEST.matcher(key).matches()) {
					/*
					 * We've got an object that *looks like* it might be a
					 * manifest file. But we need to parse the key to ensure
					 * that it starts with a valid timestamp.
					 */
					DataSetManifestId manifestId = DataSetManifestId.parseManifestIdFromS3Key(key);
					if (manifestId != null)
						manifestIds.add(manifestId);
				} else if (DataSetMonitorWorker.REGEX_COMPLETED_MANIFEST.matcher(key).matches()) {
					completedManifestsCount++;
				}
			}

			s3BucketListRequest.setContinuationToken(s3ObjectListing.getNextContinuationToken());
		} while (s3ObjectListing.isTruncated());

		this.completedManifestsCount = completedManifestsCount;

		LOGGER.debug("Scanned for data sets in S3. Found '{}'.", manifestsToProcess.size());
		timerS3Scanning.close();

		return manifestIds;
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
	 * @return the {@link Stream} that {@link QueuedDataSet}s should be pulled
	 *         from, when requested
	 */
	private Stream<DataSetManifest> getManifestsToProcess() {
		return manifestsToProcess.stream().filter(manifest -> !recentlyProcessedManifests.contains(manifest.getId()));
	}

	/**
	 * @return <code>false</code> if there is at least one pending
	 *         {@link DataSetManifest} to process, <code>true</code> if not
	 */
	public boolean isEmpty() {
		return getManifestsToProcess().count() == 0;
	}

	/**
	 * @return the {@link DataSetManifest} for the next data set that should be
	 *         processed, if any
	 */
	public Optional<QueuedDataSet> getNextDataSetToProcess() {
		if (isEmpty()) {
			return Optional.empty();
		}

		DataSetManifest nextManifestToProcess = getManifestsToProcess().findFirst().get();
		Map<DataSetManifestEntry, Future<ManifestEntryDownloadResult>> manifestEntryDownloads = nextManifestToProcess
				.getEntries().stream().collect(Collectors.toMap(Function.identity(), this.manifestEntryDownloads::get));
		return Optional.of(new QueuedDataSet(nextManifestToProcess, manifestEntryDownloads));
	}

	/**
	 * @return the count of {@link DataSetManifest}s found for data sets that
	 *         need to be processed (including those known to be invalid)
	 */
	public int getPendingManifestsCount() {
		return manifestsToProcess.size() + knownInvalidManifests.size() - recentlyProcessedManifests.size();
	}

	/**
	 * @return the count of {@link DataSetManifest}s found for data sets that
	 *         have already been processed
	 */
	public Optional<Integer> getCompletedManifestsCount() {
		return completedManifestsCount == null ? Optional.empty()
				: Optional.of(recentlyProcessedManifests.size() + completedManifestsCount);
	}

	/**
	 * Marks the specified {@link DataSetManifest} as processed, removing it
	 * from the list of pending data sets in this {@link DataSetQueue}.
	 * 
	 * @param manifest
	 *            the {@link DataSetManifest} for the data set that has been
	 *            successfully processed
	 */
	public void markProcessed(DataSetManifest manifest) {
		this.recentlyProcessedManifests.add(manifest.getId());
	}

	/**
	 * Cleans up all of the local copies of S3 files that this
	 * {@link DataSetQueue} has downloaded, in preparation for application
	 * shutdown.
	 */
	public void cleanup() {
		this.manifestEntryDownloads.entrySet().forEach(e -> {
			// Hacky, but takes advantage of code re-use.
			new S3RifFile(appMetrics, e.getKey(), e.getValue()).cleanupTempFile();
		});
	}

	/**
	 * Represents a specific data set queued in a {@link DataSetQueue}, waiting
	 * to be processed.
	 */
	public static final class QueuedDataSet {
		private final DataSetManifest manifest;
		private final Map<DataSetManifestEntry, Future<ManifestEntryDownloadResult>> manifestEntryDownloads;

		/**
		 * Constructs a new {@link QueuedDataSet} instance.
		 * 
		 * @param manifest
		 *            the value to use for {@link #getManifest()}
		 * @param manifestEntryDownloads
		 *            the value to use for {@link #getManifestEntryDownloads()}
		 */
		public QueuedDataSet(DataSetManifest manifest,
				Map<DataSetManifestEntry, Future<ManifestEntryDownloadResult>> manifestEntryDownloads) {
			this.manifest = manifest;
			this.manifestEntryDownloads = manifestEntryDownloads;
		}

		/**
		 * @return the {@link DataSetManifest} that is queued for processing
		 */
		public DataSetManifest getManifest() {
			return manifest;
		}

		/**
		 * @return a {@link Map} of the {@link DataSetManifestEntry}s in
		 *         {@link #getManifest()} and the asynchronous {@link Future}
		 *         tasks to download their file contents locally
		 */
		public Map<DataSetManifestEntry, Future<ManifestEntryDownloadResult>> getManifestEntryDownloads() {
			return manifestEntryDownloads;
		}
	}
}
