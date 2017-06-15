package gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

import gov.hhs.cms.bluebutton.datapipeline.rif.extract.ExtractionOptions;

/**
 * Contains utility/helper methods for AWS S3 that can be used in application
 * and test code.
 */
public final class S3Utilities {
	/**
	 * The default AWS {@link Region} to interact with.
	 */
	public static final Region REGION_DEFAULT = Region.getRegion(Regions.US_EAST_1);

	/**
	 * @param options
	 *            the {@link ExtractionOptions} to use
	 * @return the {@link AmazonS3} client to use
	 */
	public static AmazonS3 createS3Client(ExtractionOptions options) {
		return createS3Client(options.getS3Region());
	}

	/**
	 * @param awsS3Region
	 *            the AWS {@link Region} that should be used when interacting
	 *            with S3
	 * @return the {@link AmazonS3} client to use
	 */
	public static AmazonS3 createS3Client(Region awsS3Region) {
		AmazonS3Client s3Client = new AmazonS3Client(new DefaultAWSCredentialsProviderChain());
		s3Client.setRegion(awsS3Region);
		return s3Client;
	}
}
