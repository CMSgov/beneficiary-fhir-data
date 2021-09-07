package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import com.amazonaws.services.s3.AmazonS3;
import gov.cms.bfd.pipeline.ccw.rif.extract.ExtractionOptions;
import gov.cms.bfd.pipeline.sharedutils.s3.SharedS3Utilities;

/**
 * Contains utility/helper methods for AWS S3 that are specific to the RIF load process. Implemented
 * using the shared code in S3Utilities but pulls the configuration from RIF specific
 * ExtractionOptions.
 */
public final class S3Utilities {
  /**
   * @param options the {@link ExtractionOptions} to use
   * @return the {@link AmazonS3} client to use
   */
  public static AmazonS3 createS3Client(ExtractionOptions options) {
    return SharedS3Utilities.createS3Client(options.getS3Region());
  }
}
