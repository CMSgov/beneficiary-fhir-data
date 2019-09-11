package gov.cms.bfd.pipeline.rif.extract.s3;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import gov.cms.bfd.pipeline.rif.extract.ExtractionOptions;

/** Contains utility/helper methods for AWS S3 that can be used in application and test code. */
public final class S3Utilities {
  /** The default AWS {@link Region} to interact with. */
  public static final Regions REGION_DEFAULT = Regions.US_EAST_1;

  /**
   * @param options the {@link ExtractionOptions} to use
   * @return the {@link AmazonS3} client to use
   */
  public static AmazonS3 createS3Client(ExtractionOptions options) {
    return createS3Client(options.getS3Region());
  }

  /**
   * @param awsS3Region the AWS {@link Regions} that should be used when interacting with S3
   * @return the {@link AmazonS3} client to use
   */
  public static AmazonS3 createS3Client(Regions awsS3Region) {
    AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(awsS3Region).build();
    return s3Client;
  }
}
