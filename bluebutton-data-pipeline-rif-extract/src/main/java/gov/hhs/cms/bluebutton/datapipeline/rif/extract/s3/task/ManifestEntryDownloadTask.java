package gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.task;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.transfer.Download;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.datapipeline.rif.extract.ExtractionOptions;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.exceptions.AwsFailureException;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetMonitorWorker;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.task.ManifestEntryDownloadTask.ManifestEntryDownloadResult;

/**
 * Represents an asynchronous operation to download the contents of a specific
 * {@link DataSetManifestEntry} from S3.
 */
public final class ManifestEntryDownloadTask implements Callable<ManifestEntryDownloadResult> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ManifestEntryDownloadTask.class);

	private final S3TaskManager s3TaskManager;
	private final MetricRegistry appMetrics;
	private final ExtractionOptions options;
	private final DataSetManifestEntry manifestEntry;

	/**
	 * Constructs a new {@link ManifestEntryDownloadTask}.
	 * 
	 * @param s3TaskManager
	 *            the {@link S3TaskManager} to use
	 * @param appMetrics
	 *            the {@link MetricRegistry} for the overall application
	 * @param options
	 *            the {@link ExtractionOptions} to use
	 * @param manifestEntry
	 *            the {@link DataSetManifestEntry} to download the file for
	 */
	public ManifestEntryDownloadTask(S3TaskManager s3TaskManager, MetricRegistry appMetrics, ExtractionOptions options,
			DataSetManifestEntry manifestEntry) {
		this.s3TaskManager = s3TaskManager;
		this.appMetrics = appMetrics;
		this.options = options;
		this.manifestEntry = manifestEntry;
	}

	/**
	 * @see java.util.concurrent.Callable#call()
	 */
	@Override
	public ManifestEntryDownloadResult call() throws Exception {
		try {
			GetObjectRequest objectRequest = new GetObjectRequest(options.getS3BucketName(),
					String.format("%s/%s/%s", DataSetMonitorWorker.S3_PREFIX_PENDING_DATA_SETS,
							manifestEntry.getParentManifest().getTimestampText(), manifestEntry.getName()));
			Path localTempFile = Files.createTempFile("data-pipeline-s3-temp", ".rif");

			Timer.Context downloadTimer = appMetrics
					.timer(MetricRegistry.name(getClass().getSimpleName(), "downloadSystemTime")).time();
			LOGGER.debug("Downloading '{}' to '{}'...", manifestEntry, localTempFile.toAbsolutePath().toString());
			Download downloadHandle = s3TaskManager.getS3TransferManager().download(objectRequest,
					localTempFile.toFile());
			downloadHandle.waitForCompletion();
			LOGGER.debug("Downloaded '{}' to '{}'.", manifestEntry, localTempFile.toAbsolutePath().toString());
			downloadTimer.close();

			return new ManifestEntryDownloadResult(manifestEntry, localTempFile);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (AmazonClientException e) {
			throw new AwsFailureException(e);
		} catch (InterruptedException e) {
			// Shouldn't happen, as our apps don't use thread interrupts.
			throw new BadCodeMonkeyException(e);
		}
	}

	/**
	 * Represents the results of a {@link ManifestEntryDownloadTask}.
	 */
	public static final class ManifestEntryDownloadResult {
		private final DataSetManifestEntry manifestEntry;
		private final Path localDownload;

		/**
		 * Constructs a new {@link ManifestEntryDownloadResult} instance.
		 * 
		 * @param manifestEntry
		 *            the value to use for {@link #getManifestEntry()}
		 * @param localDownload
		 *            the value to use for {@link #getLocalDownload()}
		 */
		public ManifestEntryDownloadResult(DataSetManifestEntry manifestEntry, Path localDownload) {
			this.manifestEntry = manifestEntry;
			this.localDownload = localDownload;
		}

		/**
		 * @return the {@link DataSetManifestEntry} whose file was downloaded
		 */
		public DataSetManifestEntry getManifestEntry() {
			return manifestEntry;
		}

		/**
		 * @return the {@link Path} to the local copy of the
		 *         {@link DataSetManifestEntry}'s contents, which should be
		 *         deleted once it is no longer needed, to prevent disk space
		 *         usage leaks
		 */
		public Path getLocalDownload() {
			return localDownload;
		}
	}
}
