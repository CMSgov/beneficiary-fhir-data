package gov.cms.bfd.pipeline.sharedutils.s3;

import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

/** Interface for factory objects that can create S3 client objects. */
public interface S3ClientFactory {
  /**
   * Create a synchronous {@link S3Client}.
   *
   * @return the client
   */
  S3Client createS3Client();

  /**
   * Create an asynchronous {@link S3AsyncClient}.
   *
   * @return the client
   */
  S3AsyncClient createS3AsyncClient();
}
