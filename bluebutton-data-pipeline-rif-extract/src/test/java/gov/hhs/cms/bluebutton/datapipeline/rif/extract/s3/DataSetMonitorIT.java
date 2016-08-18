package gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;

import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFileType;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFilesEvent;

/**
 * Integration tests for {@link DataSetMonitor}.
 */
public final class DataSetMonitorIT {
	/**
	 * Verifies that {@link DataSetMonitor} handles errors as expected when
	 * asked to run against an S3 bucket that doesn't exist. This test case
	 * isn't so much needed to test that one specific failure case, but to
	 * instead verify the overall error handling.
	 */
	@Test
	@Ignore
	public void missingBucket() {
		/*
		 * FIXME This test case breaks the build completely and in a novel way,
		 * because DataSetMonitor calls System.exit(...) when errors happen.
		 * That's a bad design, it turns out, but not worth fixing right now.
		 * Until then: this is @Ignore'd.
		 */

		// Start the monitor against a bucket that doesn't exist.
		MockDataSetProcessor dataSetProcessor = new MockDataSetProcessor(0);
		DataSetMonitor monitor = new DataSetMonitor("foo", 1, dataSetProcessor);
		monitor.start();

		// Wait for the monitor to error out.
		Awaitility.await().atMost(Duration.ONE_MINUTE).until(() -> monitor.isStoppedBecauseOfError());

		// If we made it here, things are working correctly. Yay!
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

			// Start the monitor and then stop it.
			MockDataSetProcessor dataSetProcessor = new MockDataSetProcessor(0);
			DataSetMonitor monitor = new DataSetMonitor(bucket.getName(), 1, dataSetProcessor);
			monitor.start();
			monitor.stop();

			// Verify that no data sets were generated.
			Assert.assertEquals(0, dataSetProcessor.getGeneratedDataSets().size());
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
			MockDataSetProcessor dataSetProcessor = new MockDataSetProcessor(2);
			DataSetMonitor monitor = new DataSetMonitor(bucket.getName(), 1, dataSetProcessor);
			monitor.start();

			// Wait for the monitor to generate events for the two data sets.
			dataSetProcessor.getCountDownLatch().await(10, TimeUnit.SECONDS);

			// Verify what was handed off to the DataSetProcessor.
			Assert.assertEquals(2, dataSetProcessor.getGeneratedDataSets().size());
			Assert.assertEquals(manifestA.getTimestamp(),
					dataSetProcessor.getGeneratedDataSets().get(0).getTimestamp());
			Assert.assertEquals(manifestB.getTimestamp(),
					dataSetProcessor.getGeneratedDataSets().get(1).getTimestamp());

			// Verify that the bucket is now empty.
			Assert.assertEquals(0, s3Client.listObjects(bucket.getName()).getObjectSummaries().size());
		} finally {
			if (bucket != null)
				DataSetTestUtilities.deleteObjectsAndBucket(s3Client, bucket);
		}
	}

	/**
	 * A mock {@link DataSetProcessor} that tracks the {@link RifFilesEvent}s
	 * passed to it and supplies a {@link CountDownLatch} that can be used to
	 * track asynchronous events.
	 */
	private static final class MockDataSetProcessor implements DataSetProcessor {
		private final CountDownLatch countDownLatch;
		private final List<RifFilesEvent> generatedDataSets = new LinkedList<>();

		/**
		 * Constructs a new {@link MockDataSetProcessor} instance.
		 * 
		 * @param latchCount
		 *            the {@link CountDownLatch#getCount()} value to start
		 *            {@link #getCountDownLatch()} at
		 */
		public MockDataSetProcessor(int latchCount) {
			this.countDownLatch = new CountDownLatch(latchCount);
		}

		/**
		 * @see gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetProcessor#process(gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFilesEvent)
		 */
		@Override
		public void process(RifFilesEvent rifFilesEvent) {
			generatedDataSets.add(rifFilesEvent);
		}

		/**
		 * @return the {@link CountDownLatch} that can be used to track
		 *         asynchronous events
		 */
		public CountDownLatch getCountDownLatch() {
			return countDownLatch;
		}

		/**
		 * @return the {@link List} of {@link RifFilesEvent}s that have been
		 *         passed to {@link #process(RifFilesEvent)}
		 */
		public List<RifFilesEvent> getGeneratedDataSets() {
			return Collections.unmodifiableList(generatedDataSets);
		}
	}
}
