package gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.task;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
	private final AmazonS3 s3Client;
	private final TransferManager s3TransferManager;
	private final ThreadPoolExecutor backgroundTaskService;

	/**
	 * Constructs a new {@link S3TaskManager}.
	 * 
	 * @param options
	 *            the {@link ExtractionOptions} to use
	 */
	public S3TaskManager(ExtractionOptions options) {
		this.s3Client = S3Utilities.createS3Client(options);
		this.s3TransferManager = TransferManagerBuilder.standard().withS3Client(s3Client).build();

		this.backgroundTaskService = new ThreadPoolExecutor(10, 10, 100L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>());
		this.backgroundTaskService.allowCoreThreadTimeOut(true);
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
		 * This will prevent any tasks that have not started from starting, and
		 * will attempt to interrupt all threads currently running active tasks.
		 * Some of our tasks are interruptible (e.g. TODO), some aren't (e.g. S3
		 * renames), so we will have to wait for the service to finish shutting
		 * down, which will include waiting for the non-interruptible tasks to
		 * complete.
		 */
		List<Runnable> tasksThatNeverStarted = this.backgroundTaskService.shutdownNow();
		try {
			this.backgroundTaskService.awaitTermination(30, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			// We're not expecting interrupts here, so go boom.
			throw new BadCodeMonkeyException(e);
		}

		/*
		 * Some of the operations that never got to start **must** be run, or
		 * the application's data will be left in an inconsistent state (e.g.
		 * the data is in the database, but still marked as pending in S3). So,
		 * we run those tasks synchronously here.
		 */
		// TODO don't have any of those tasks yet
	}

	/**
	 * @param task
	 *            the {@link ManifestEntryDownloadTask} to be asynchronously run
	 * @return a {@link Future} for the {@link ManifestEntryDownloadResult} that
	 *         will eventually be generated from the specified
	 *         {@link ManifestEntryDownloadTask}
	 */
	public Future<ManifestEntryDownloadResult> submit(ManifestEntryDownloadTask task) {
		return backgroundTaskService.submit(task);
	}
}
