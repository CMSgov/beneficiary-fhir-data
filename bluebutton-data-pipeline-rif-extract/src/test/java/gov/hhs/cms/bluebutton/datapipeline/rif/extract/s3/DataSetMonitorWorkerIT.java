package gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;

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
 * Integration tests for {@link DataSetMonitorWorker}.
 */
public final class DataSetMonitorWorkerIT {
	private static final Logger LOGGER = LoggerFactory.getLogger(DataSetMonitorIT.class);

	/**
	 * Tests {@link DataSetMonitorWorker} when run against an empty bucket.
	 */
	@Test
	public void emptyBucketTest() {
		AmazonS3 s3Client = new AmazonS3Client(new DefaultAWSCredentialsProviderChain());
		Bucket bucket = null;
		try {
			// Create the (empty) bucket to run against.
			bucket = s3Client.createBucket(String.format("bb-test-%d", new Random().nextInt(1000)));
			LOGGER.info("Bucket created: '{}:{}'", s3Client.getS3AccountOwner().getDisplayName(), bucket.getName());

			// Run the worker.
			MockDataSetMonitorListener listener = new MockDataSetMonitorListener();
			DataSetMonitorWorker monitorWorker = new DataSetMonitorWorker(bucket.getName(), listener);
			monitorWorker.run();

			// Verify that no data sets were generated.
			Assert.assertEquals(1, listener.getNoDataAvailableEvents());
			Assert.assertEquals(0, listener.getDataEvents().size());
			Assert.assertEquals(0, listener.getErrorEvents().size());
		} finally {
			if (bucket != null)
				s3Client.deleteBucket(bucket.getName());
		}
	}

	/**
	 * Tests {@link DataSetMonitorWorker} when run against a bucket with a
	 * single data set.
	 */
	@Test
	public void singleDataSetTest() {
		AmazonS3 s3Client = new AmazonS3Client(new DefaultAWSCredentialsProviderChain());
		Bucket bucket = null;
		try {
			/*
			 * Create the (empty) bucket to run against, and populate it with a
			 * data set.
			 */
			bucket = s3Client.createBucket(String.format("bb-test-%d", new Random().nextInt(1000)));
			LOGGER.info("Bucket created: '{}:{}'", s3Client.getS3AccountOwner().getDisplayName(), bucket.getName());
			DataSetManifest manifest = new DataSetManifest(Instant.now(),
					new DataSetManifestEntry("beneficiaries.rif", RifFileType.BENEFICIARY),
					new DataSetManifestEntry("carrier.rif", RifFileType.CARRIER));
			s3Client.putObject(DataSetTestUtilities.createPutRequest(bucket, manifest));
			s3Client.putObject(DataSetTestUtilities.createPutRequest(bucket, manifest, manifest.getEntries().get(0),
					"rif-static-samples/sample-a-beneficiaries.txt"));
			s3Client.putObject(DataSetTestUtilities.createPutRequest(bucket, manifest, manifest.getEntries().get(1),
					"rif-static-samples/sample-a-bcarrier.txt"));

			// Run the worker.
			MockDataSetMonitorListener listener = new MockDataSetMonitorListener();
			DataSetMonitorWorker monitorWorker = new DataSetMonitorWorker(bucket.getName(), listener);
			monitorWorker.run();

			// Verify what was handed off to the DataSetMonitorListener.
			Assert.assertEquals(0, listener.getNoDataAvailableEvents());
			Assert.assertEquals(1, listener.getDataEvents().size());
			Assert.assertEquals(manifest.getTimestamp(), listener.getDataEvents().get(0).getTimestamp());
			Assert.assertEquals(manifest.getEntries().size(), listener.getDataEvents().get(0).getFiles().size());
			Assert.assertEquals(0, listener.getErrorEvents().size());

			// Verify that the bucket is now empty.
			Assert.assertEquals(0, s3Client.listObjects(bucket.getName()).getObjectSummaries().size());
		} finally {
			if (bucket != null)
				DataSetTestUtilities.deleteObjectsAndBucket(s3Client, bucket);
		}
	}

	/**
	 * Tests {@link DataSetMonitorWorker} when run against an empty bucket.
	 */
	@Test
	public void multipleDataSetsTest() {
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

			// Run the worker.
			MockDataSetMonitorListener listener = new MockDataSetMonitorListener();
			DataSetMonitorWorker monitorWorker = new DataSetMonitorWorker(bucket.getName(), listener);
			monitorWorker.run();

			// Verify what was handed off to the DataSetMonitorListener.
			Assert.assertEquals(0, listener.getNoDataAvailableEvents());
			Assert.assertEquals(1, listener.getDataEvents().size());
			Assert.assertEquals(manifestA.getTimestamp(), listener.getDataEvents().get(0).getTimestamp());
			Assert.assertEquals(manifestA.getEntries().size(), listener.getDataEvents().get(0).getFiles().size());
			Assert.assertEquals(0, listener.getErrorEvents().size());

			// Verify that the bucket now just has the second data set.
			Assert.assertEquals(1 + manifestB.getEntries().size(),
					s3Client.listObjects(bucket.getName()).getObjectSummaries().size());
		} finally {
			if (bucket != null)
				DataSetTestUtilities.deleteObjectsAndBucket(s3Client, bucket);
		}
	}
}
