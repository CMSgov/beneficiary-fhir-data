package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import gov.cms.bfd.pipeline.ccw.rif.extract.ExtractionOptions;
import gov.cms.bfd.pipeline.sharedutils.s3.SharedS3Utilities;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Contains utility/helper methods for AWS S3 that are specific to the RIF load process. Implemented
 * using the shared code in {@link SharedS3Utilities} but pulls the configuration from RIF specific
 * {@link ExtractionOptions}.
 */
public final class S3Utilities {
  /**
   * Create an S3 client.
   *
   * @param options the {@link ExtractionOptions} to use
   * @return the {@link S3Client} client to use
   */
  public static S3Client createS3Client(ExtractionOptions options) {
    return SharedS3Utilities.createS3Client(options.getS3Region());
  }

  /**
   * Create an Async S3 client.
   *
   * @param options the {@link ExtractionOptions} to use
   * @return the {@link S3AsyncClient} client to use
   */
  public static S3AsyncClient createS3AsyncClient(ExtractionOptions options) {
    return SharedS3Utilities.createS3AsyncClient(options.getS3Region());
  }
}
