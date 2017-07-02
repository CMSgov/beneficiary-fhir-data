package gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.datapipeline.rif.extract.ExtractionOptions;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.S3Utilities;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.task.ManifestEntryDownloadTask.ManifestEntryDownloadResult;

/**
 * Handles the execution and management of S3-related tasks.
 */
public final class S3TaskManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(S3TaskManager.class);

	private final AmazonS3 s3Client;
	private final TransferManager s3TransferManager;
	private final ExecutorService downloadTasksService;
	private final ExecutorService moveTasksService;

	/**
	 * Constructs a new {@link S3TaskManager}.
	 * 
	 * @param options
	 *            the {@link ExtractionOptions} to use
	 */
	public S3TaskManager(ExtractionOptions options) {
		this.s3Client = S3Utilities.createS3Client(options);
		this.s3TransferManager = TransferManagerBuilder.standard().withS3Client(s3Client).build();

		ThreadPoolExecutor cancellableTasksService = new ThreadPoolExecutor(10, 10, 100L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>());
		cancellableTasksService.allowCoreThreadTimeOut(true);
		this.downloadTasksService = cancellableTasksService;

		ThreadPoolExecutor nonCancellableTasksService = new ThreadPoolExecutor(10, 10, 100L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>());
		nonCancellableTasksService.allowCoreThreadTimeOut(true);
		this.moveTasksService = nonCancellableTasksService;
	}

	/**
	 * @return the {@link AmazonS3} client being used by this
	 *         {@link S3TaskManager}
	 */
	public AmazonS3 getS3Client() {
		return s3Client;
	}

	/**
	 * @return the Amazon S3 {@link TransferManager} being used by this
	 *         {@link S3TaskManager}
	 */
	public TransferManager getS3TransferManager() {
		return s3TransferManager;
	}

	/**
	 * Shuts down this {@link S3TaskManager} safely, which may require waiting
	 * for some already-submitted tasks to complete.
	 */
	public void shutdownSafely() {
		/*
		 * Prevent any new download tasks from being submitted and cancel those
		 * that are queued.
		 */
		this.downloadTasksService.shutdownNow();

		/*
		 * Prevent any new move tasks from being submitted, while allowing those
		 * that are queued and/or actively running to complete. This is
		 * necessary to ensure that data sets present in the database aren't
		 * left marked as pending in S3.
		 */
		this.moveTasksService.shutdown();

		try {
			if (!this.downloadTasksService.isTerminated()) {
				LOGGER.info("Waiting for in-progress downloads to complete...");
				this.downloadTasksService.awaitTermination(30, TimeUnit.MINUTES);
				LOGGER.info("All in-progress downloads are complete.");
			}

			if (!this.moveTasksService.isTerminated()) {
				LOGGER.info("Waiting for all S3 rename/move operations to complete...");
				this.moveTasksService.awaitTermination(30, TimeUnit.MINUTES);
				LOGGER.info("All S3 rename/move operations are complete.");
			}
		} catch (InterruptedException e) {
			// We're not expecting interrupts here, so go boom.
			throw new BadCodeMonkeyException(e);
		}
	}

	/**
	 * @param task
	 *            the {@link ManifestEntryDownloadTask} to be asynchronously run
	 * @return a {@link Future} for the {@link ManifestEntryDownloadResult} that
	 *         will eventually be generated from the specified
	 *         {@link ManifestEntryDownloadTask}
	 */
	public Future<ManifestEntryDownloadResult> submit(ManifestEntryDownloadTask task) {
		return downloadTasksService.submit(task);
	}

	/**
	 * @param task
	 *            the {@link DataSetMoveTask} to be asynchronously run
	 */
	public void submit(DataSetMoveTask task) {
		moveTasksService.submit(task);
	}
}
