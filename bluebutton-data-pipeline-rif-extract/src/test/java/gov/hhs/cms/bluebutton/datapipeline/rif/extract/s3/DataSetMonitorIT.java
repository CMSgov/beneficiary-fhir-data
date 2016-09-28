package gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;

import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFileType;

/**
 * Integration tests for {@link DataSetMonitor}.
 */
public final class DataSetMonitorIT {
	private static final Logger LOGGER = LoggerFactory.getLogger(DataSetMonitorIT.class);

	/**
	 * Verifies that {@link DataSetMonitor} handles errors as expected when
	 * asked to run against an S3 bucket that doesn't exist. This test case
	 * isn't so much needed to test that one specific failure case, but to
	 * instead verify the overall error handling.
	 * 
	 * @throws InterruptedException
	 *             (shouldn't happen)
	 */
	@Test
	public void missingBucket() throws InterruptedException {
		// Start the monitor against a bucket that doesn't exist.
		MockDataSetMonitorListener listener = new MockDataSetMonitorListener();
		DataSetMonitor monitor = new DataSetMonitor("foo", 1, listener);
		monitor.start();

		// Wait for the monitor to error out.
		Awaitility.await().atMost(Duration.TEN_SECONDS).until(() -> !listener.errorEvents.isEmpty());
		monitor.stop();
		Assert.assertEquals(0, listener.getNoDataAvailableEvents());
		Assert.assertNotEquals(0, listener.getErrorEvents().size());
		Assert.assertEquals(0, listener.getDataEvents().size());
	}

	/**
	 * Tests {@link DataSetMonitor} when run against an empty bucket.
	 */
	@Test
	public void emptyBucketTest() {
		AmazonS3 s3Client = new AmazonS3Client(new DefaultAWSCredentialsProviderChain());
		Bucket bucket = null;
		try {
			// Create the (empty) bucket to run against.
			bucket = s3Client.createBucket(String.format("bb-test-%d", new Random().nextInt(1000)));
			LOGGER.info("Bucket created: '{}:{}'", s3Client.getS3AccountOwner().getDisplayName(), bucket.getName());

			// Start the monitor and then stop it.
			MockDataSetMonitorListener listener = new MockDataSetMonitorListener();
			DataSetMonitor monitor = new DataSetMonitor(bucket.getName(), 1, listener);
			monitor.start();
			Awaitility.await().atMost(Duration.TEN_SECONDS).until(() -> listener.getNoDataAvailableEvents() > 0);
			monitor.stop();

			// Verify that no data sets were generated.
			Assert.assertNotEquals(0, listener.getNoDataAvailableEvents());
			Assert.assertEquals(0, listener.getDataEvents().size());
			Assert.assertEquals(0, listener.errorEvents.size());
		} finally {
			if (bucket != null)
				s3Client.deleteBucket(bucket.getName());
		}
	}

	/**
	 * Tests {@link DataSetMonitor} when run against an empty bucket.
	 * 
	 * @throws InterruptedException
	 *             (shouldn't happen)
	 */
	@Test
	public void multipleDataSetsTest() throws InterruptedException {
		AmazonS3 s3Client = new AmazonS3Client(new DefaultAWSCredentialsProviderChain());
		Bucket bucket = null;
		try {
			/*
			 * Create the (empty) bucket to run against, and populate it with
			 * two data sets.
			 */
			bucket = s3Client.createBucket(String.format("bb-test-%d", new Random().nextInt(1000)));
			LOGGER.info("Bucket created: '{}:{}'", s3Client.getS3AccountOwner().getDisplayName(), bucket.getName());
			DataSetManifest manifestA = new DataSetManifest(Instant.now().minus(1L, ChronoUnit.HOURS),
					new DataSetManifestEntry("beneficiaries.rif", RifFileType.BENEFICIARY));
			s3Client.putObject(DataSetTestUtilities.createPutRequest(bucket, manifestA));
			s3Client.putObject(DataSetTestUtilities.createPutRequest(bucket, manifestA, manifestA.getEntries().get(0),
					"rif-static-samples/sample-a-beneficiaries.txt"));
			DataSetManifest manifestB = new DataSetManifest(Instant.now(),
					new DataSetManifestEntry("carrier.rif", RifFileType.CARRIER));
			s3Client.putObject(DataSetTestUtilities.createPutRequest(bucket, manifestB));
			s3Client.putObject(DataSetTestUtilities.createPutRequest(bucket, manifestB, manifestB.getEntries().get(0),
					"rif-static-samples/sample-a-bcarrier.txt"));

			// Start the monitor up.
			MockDataSetMonitorListener listener = new MockDataSetMonitorListener();
			DataSetMonitor monitor = new DataSetMonitor(bucket.getName(), 1, listener);
			monitor.start();

			// Wait for the monitor to generate events for the two data sets.
			Awaitility.await().atMost(Duration.ONE_MINUTE).until(() -> listener.getDataEvents().size() >= 2);

			// Verify what was handed off to the DataSetMonitorListener.
			Assert.assertEquals(2, listener.getDataEvents().size());
			Assert.assertEquals(0, listener.getErrorEvents().size());
			Assert.assertEquals(manifestA.getTimestamp(), listener.getDataEvents().get(0).getTimestamp());
			Assert.assertEquals(manifestB.getTimestamp(), listener.getDataEvents().get(1).getTimestamp());

			// Verify that the bucket is now empty.
			DataSetTestUtilities.waitForBucketObjectCount(s3Client, bucket, 0, java.time.Duration.ofSeconds(10));
		} finally {
			if (bucket != null)
				DataSetTestUtilities.deleteObjectsAndBucket(s3Client, bucket);
		}
	}
}
