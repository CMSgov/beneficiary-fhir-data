package gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetManifest.DataSetManifestEntry;

/**
 * <p>
 * Contains utilities that are useful when running tests that involve working
 * with data sets in S3.
 * </p>
 * <p>
 * This is being left in <code>src/main</code> so that it can be used from other
 * modules' tests, without having to delve into classpath dark arts.
 * </p>
 */
public class DataSetTestUtilities {
	/**
	 * Deletes the specified {@link Bucket} and all objects in it.
	 * 
	 * @param s3Client
	 *            the {@link AmazonS3} client to use
	 * @param bucket
	 *            the {@link Bucket} to empty and delete
	 */
	public static void deleteObjectsAndBucket(AmazonS3 s3Client, Bucket bucket) {
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
	public static PutObjectRequest createPutRequest(Bucket bucket, DataSetManifest manifest) {
		try {
			// Serialize the manifest to a byte array.
			JAXBContext jaxbContext = JAXBContext.newInstance(DataSetManifest.class);
			Marshaller marshaller = jaxbContext.createMarshaller();
			ByteArrayOutputStream manifestOutputStream = new ByteArrayOutputStream();
			marshaller.marshal(manifest, manifestOutputStream);

			String objectKey = String.format("%s/%s", DateTimeFormatter.ISO_INSTANT.format(manifest.getTimestamp()),
					"manifest.xml");
			byte[] manifestByteArray = manifestOutputStream.toByteArray();
			InputStream manifestInputStream = new ByteArrayInputStream(manifestByteArray);

			// If this isn't specified, the AWS API logs annoying warnings.
			ObjectMetadata manifestMetadata = new ObjectMetadata();
			manifestMetadata.setContentLength(manifestByteArray.length);

			PutObjectRequest request = new PutObjectRequest(bucket.getName(), objectKey, manifestInputStream,
					manifestMetadata);
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
	 * @param objectContentsUrl
	 *            a {@link URL} to the data to push as the new object's content
	 * @return a {@link PutObjectRequest} for the specified content
	 */
	public static PutObjectRequest createPutRequest(Bucket bucket, DataSetManifest manifest,
			DataSetManifestEntry manifestEntry, URL objectContentsUrl) {
		String objectKey = String.format("%s/%s", DateTimeFormatter.ISO_INSTANT.format(manifest.getTimestamp()),
				manifestEntry.getName());

		try {
			// If this isn't specified, the AWS API logs annoying warnings.
			long objectContentLength = objectContentsUrl.openConnection().getContentLength();
			ObjectMetadata objectMetadata = new ObjectMetadata();
			objectMetadata.setContentLength(objectContentLength);

			PutObjectRequest request = new PutObjectRequest(bucket.getName(), objectKey, objectContentsUrl.openStream(),
					objectMetadata);
			return request;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * <p>
	 * Waits for the number of objects in the specified {@link Bucket} to equal
	 * the specified count.
	 * </p>
	 * <p>
	 * This is needed because Amazon's S3 API is only <em>eventually</em>
	 * consistent for deletes, per
	 * <a href="https://aws.amazon.com/s3/faqs/">Amazon S3 FAQs</a>.
	 * </p>
	 * 
	 * @param s3Client
	 *            the {@link AmazonS3} client to use
	 * @param bucket
	 *            the {@link Bucket} to check
	 * @param expectedObjectCount
	 *            the number of objects that should be in the specified
	 *            {@link Bucket}
	 * @param waitDuration
	 *            the length of time to wait for the condition to be met before
	 *            throwing an error
	 */
	public static void waitForBucketObjectCount(AmazonS3 s3Client, Bucket bucket, int expectedObjectCount,
			Duration waitDuration) {
		Instant endTime = Instant.now().plus(waitDuration);

		int actualObjectCount = -1;
		while (Instant.now().isBefore(endTime)) {
			actualObjectCount = s3Client.listObjects(bucket.getName()).getObjectSummaries().size();
			if (expectedObjectCount == actualObjectCount)
				return;

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// Shouldn't happen, as we're not using interrupts for anything.
				throw new IllegalStateException(e);
			}
		}

		throw new IllegalStateException(
				String.format("S3 object count count incorrect. Expected '%d', but actual is '%d'.",
						expectedObjectCount, actualObjectCount));
	}
}
