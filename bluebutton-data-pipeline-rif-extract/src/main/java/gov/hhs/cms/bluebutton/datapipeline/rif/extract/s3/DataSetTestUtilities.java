package gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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
	public static PutObjectRequest createPutRequest(Bucket bucket, DataSetManifest manifest,
			DataSetManifestEntry manifestEntry, String resourceName) {
		String objectKey = String.format("%s/%s", DateTimeFormatter.ISO_INSTANT.format(manifest.getTimestamp()),
				manifestEntry.getName());
		InputStream objectContents = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);

		PutObjectRequest request = new PutObjectRequest(bucket.getName(), objectKey, objectContents,
				new ObjectMetadata());
		return request;
	}
}
