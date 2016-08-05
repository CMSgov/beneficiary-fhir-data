package gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.junit.Assert;
import org.junit.Test;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFileType;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFilesEvent;

/**
 * Integration tests for {@link DataSetMonitorWorker}.
 */
public final class DataSetMonitorWorkerIT {
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

			// Run the worker.
			MockDataSetProcessor dataSetProcessor = new MockDataSetProcessor();
			DataSetMonitorWorker monitorWorker = new DataSetMonitorWorker(bucket.getName(), dataSetProcessor);
			monitorWorker.run();

			// Verify that no data sets were generated.
			Assert.assertEquals(0, dataSetProcessor.getGeneratedDataSets().size());
		} finally {
			if (bucket != null)
				s3Client.deleteBucket(bucket.getName());
		}
	}

	/**
	 * Tests {@link DataSetMonitorWorker} when run against a bucket with a single
	 * data set.
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
			DataSetManifest manifest = new DataSetManifest(Instant.now(),
					new DataSetManifestEntry("beneficiaries.rif", RifFileType.BENEFICIARY),
					new DataSetManifestEntry("carrier.rif", RifFileType.CARRIER));
			s3Client.putObject(createPutRequest(bucket, manifest));
			s3Client.putObject(createPutRequest(bucket, manifest, manifest.getEntries().get(0),
					"rif-static-samples/sample-a-beneficiaries.txt"));
			s3Client.putObject(createPutRequest(bucket, manifest, manifest.getEntries().get(1),
					"rif-static-samples/sample-a-bcarrier.txt"));

			// Run the worker.
			MockDataSetProcessor dataSetProcessor = new MockDataSetProcessor();
			DataSetMonitorWorker monitorWorker = new DataSetMonitorWorker(bucket.getName(), dataSetProcessor);
			monitorWorker.run();

			// Verify what was handed off to the DataSetProcessor.
			Assert.assertEquals(1, dataSetProcessor.getGeneratedDataSets().size());
			Assert.assertEquals(manifest.getTimestamp(), dataSetProcessor.getGeneratedDataSets().get(0).getTimestamp());
			Assert.assertEquals(manifest.getEntries().size(),
					dataSetProcessor.getGeneratedDataSets().get(0).getFiles().size());

			// Verify that the bucket is now empty.
			Assert.assertEquals(0, s3Client.listObjects(bucket.getName()).getObjectSummaries().size());
		} finally {
			if (bucket != null)
				deleteObjectsAndBucket(s3Client, bucket);
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
			DataSetManifest manifestA = new DataSetManifest(Instant.now().minus(1L, ChronoUnit.HOURS),
					new DataSetManifestEntry("beneficiaries.rif", RifFileType.BENEFICIARY));
			s3Client.putObject(createPutRequest(bucket, manifestA));
			s3Client.putObject(createPutRequest(bucket, manifestA, manifestA.getEntries().get(0),
					"rif-static-samples/sample-a-beneficiaries.txt"));
			DataSetManifest manifestB = new DataSetManifest(Instant.now(),
					new DataSetManifestEntry("carrier.rif", RifFileType.CARRIER));
			s3Client.putObject(createPutRequest(bucket, manifestB));
			s3Client.putObject(createPutRequest(bucket, manifestB, manifestB.getEntries().get(0),
					"rif-static-samples/sample-a-bcarrier.txt"));

			// Run the worker.
			MockDataSetProcessor dataSetProcessor = new MockDataSetProcessor();
			DataSetMonitorWorker monitorWorker = new DataSetMonitorWorker(bucket.getName(), dataSetProcessor);
			monitorWorker.run();

			// Verify what was handed off to the DataSetProcessor.
			Assert.assertEquals(1, dataSetProcessor.getGeneratedDataSets().size());
			Assert.assertEquals(manifestA.getTimestamp(),
					dataSetProcessor.getGeneratedDataSets().get(0).getTimestamp());
			Assert.assertEquals(manifestA.getEntries().size(),
					dataSetProcessor.getGeneratedDataSets().get(0).getFiles().size());

			// Verify that the bucket now just has the second data set.
			Assert.assertEquals(1 + manifestB.getEntries().size(),
					s3Client.listObjects(bucket.getName()).getObjectSummaries().size());
		} finally {
			if (bucket != null)
				deleteObjectsAndBucket(s3Client, bucket);
		}
	}

	/**
	 * Deletes the specified {@link Bucket} and all objects in it.
	 * 
	 * @param s3Client
	 *            the {@link AmazonS3} client to use
	 * @param bucket
	 *            the {@link Bucket} to empty and delete
	 */
	static void deleteObjectsAndBucket(AmazonS3 s3Client, Bucket bucket) {
		ObjectListing objectListing = s3Client.listObjects(bucket.getName());
		do {
			for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
				s3Client.deleteObject(bucket.getName(), objectSummary.getKey());
			}

			objectListing = s3Client.listNextBatchOfObjects(objectListing);
		} while (objectListing.isTruncated());
		s3Client.deleteBucket(bucket.getName());
	}

	/**
	 * @param bucket
	 *            the {@link Bucket} to place the new object in
	 * @param manifest
	 *            the {@link DataSetManifest} to push as an object
	 * @return a {@link PutObjectRequest} for the specified
	 *         {@link DataSetManifest}
	 */
	static PutObjectRequest createPutRequest(Bucket bucket, DataSetManifest manifest) {
		try {
			// Serialize the manifest to a byte array.
			JAXBContext jaxbContext = JAXBContext.newInstance(DataSetManifest.class);
			Marshaller marshaller = jaxbContext.createMarshaller();
			ByteArrayOutputStream manifestOutputStream = new ByteArrayOutputStream();
			marshaller.marshal(manifest, manifestOutputStream);

			String objectKey = String.format("%s/%s", DateTimeFormatter.ISO_INSTANT.format(manifest.getTimestamp()),
					"manifest.xml");
			InputStream manifestInputStream = new ByteArrayInputStream(manifestOutputStream.toByteArray());

			PutObjectRequest request = new PutObjectRequest(bucket.getName(), objectKey, manifestInputStream,
					new ObjectMetadata());
			return request;
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param bucket
	 *            the {@link Bucket} to place the new object in
	 * @param manifest
	 *            the {@link DataSetManifest} to create an object for
	 * @param manifestEntry
	 *            the {@link DataSetManifestEntry} to create an object for
	 * @param resourceName
	 *            the name of the classpath resource to push as the new object's
	 *            content
	 * @return a {@link PutObjectRequest} for the specified content
	 */
	static PutObjectRequest createPutRequest(Bucket bucket, DataSetManifest manifest,
			DataSetManifestEntry manifestEntry, String resourceName) {
		String objectKey = String.format("%s/%s", DateTimeFormatter.ISO_INSTANT.format(manifest.getTimestamp()),
				manifestEntry.getName());
		InputStream objectContents = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);

		PutObjectRequest request = new PutObjectRequest(bucket.getName(), objectKey, objectContents,
				new ObjectMetadata());
		return request;
	}

	/**
	 * A mock {@link DataSetProcessor} that just tracks the
	 * {@link RifFilesEvent}s passed to it.
	 */
	private static final class MockDataSetProcessor implements DataSetProcessor {
		private final List<RifFilesEvent> generatedDataSets = new LinkedList<>();

		/**
		 * @see gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetProcessor#process(gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFilesEvent)
		 */
		@Override
		public void process(RifFilesEvent rifFilesEvent) {
			generatedDataSets.add(rifFilesEvent);
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
