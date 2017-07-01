package gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.task;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.SSEAwsKeyManagementParams;
import com.amazonaws.services.s3.transfer.Copy;
import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.datapipeline.rif.extract.ExtractionOptions;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetManifest;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetMonitorWorker;

/**
 * Represents an asynchronous operation to move/rename a data set in S3.
 */
public final class DataSetMoveTask implements Callable<Void> {
	private static final Logger LOGGER = LoggerFactory.getLogger(DataSetMoveTask.class);

	private final S3TaskManager s3TaskManager;
	private final ExtractionOptions options;
	private final DataSetManifest manifest;

	/**
	 * Constructs a new {@link DataSetMoveTask}.
	 * 
	 * @param s3TaskManager
	 *            the {@link S3TaskManager} to use
	 * @param options
	 *            the {@link ExtractionOptions} to use
	 * @param manifest
	 *            the {@link DataSetManifest} to be moved/renamed (along with
	 *            the files it references)
	 */
	public DataSetMoveTask(S3TaskManager s3TaskManager, ExtractionOptions options, DataSetManifest manifest) {
		this.s3TaskManager = s3TaskManager;
		this.options = options;
		this.manifest = manifest;
	}

	/**
	 * @see java.util.concurrent.Callable#call()
	 */
	@Override
	public Void call() throws Exception {
		LOGGER.debug("Renaming data set in S3, now that processing is complete...");

		/*
		 * S3 doesn't support batch/transactional operations, or an atomic move
		 * operation. Instead, we have to first copy all of the objects to their
		 * new location, and then remove the old objects. If something blows up
		 * in the middle of this method, orphaned S3 objects WILL be created.
		 * That's an unlikely enough occurrence, though, that we're not going to
		 * engineer around it right now.
		 */

		// First, get a list of all the object keys to work on.
		List<String> s3KeySuffixesToMove = manifest.getEntries().stream()
				.map(e -> String.format("%s/%s", manifest.getTimestamp().toString(), e.getName()))
				.collect(Collectors.toList());
		s3KeySuffixesToMove
				.add(String.format("%s/%d_manifest.xml", manifest.getTimestamp().toString(), manifest.getSequenceId()));

		/*
		 * Then, loop through each of those objects and copy them (S3 has no
		 * bulk copy operation).
		 */
		for (String s3KeySuffixToMove : s3KeySuffixesToMove) {
			String sourceKey = String.format("%s/%s", DataSetMonitorWorker.S3_PREFIX_PENDING_DATA_SETS,
					s3KeySuffixToMove);
			String targetKey = String.format("%s/%s", DataSetMonitorWorker.S3_PREFIX_COMPLETED_DATA_SETS,
					s3KeySuffixToMove);

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

			Copy copyOperation = s3TaskManager.getS3TransferManager().copy(copyRequest);
			try {
				copyOperation.waitForCopyResult();
			} catch (InterruptedException e) {
				throw new BadCodeMonkeyException(e);
			}
		}
		LOGGER.debug("Data set copied in S3 (step 1 of move).");

		DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(options.getS3BucketName());
		deleteObjectsRequest.setKeys(s3KeySuffixesToMove.stream()
				.map(k -> String.format("%s/%s", DataSetMonitorWorker.S3_PREFIX_PENDING_DATA_SETS, k))
				.map(k -> new KeyVersion(k)).collect(Collectors.toList()));
		s3TaskManager.getS3Client().deleteObjects(deleteObjectsRequest);
		LOGGER.debug("Data set deleted in S3 (step 2 of move).");

		LOGGER.debug(DataSetMonitorWorker.LOG_MESSAGE_DATA_SET_COMPLETE);
		return null;
	}
}
