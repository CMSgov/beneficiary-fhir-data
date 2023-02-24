package gov.cms.bfd.pipeline.rda.grpc.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

/** Tests the {@link S3JsonMessageSources}. */
public class S3JsonMessageSourcesTest {
  /**
   * Verifies that if the directory path is empty when constructing a {@link S3JsonMessageSources}
   * then the S3 keys can still be created.
   */
  @Test
  public void testPathConstructionWithNoDirectory() {
    S3Client s3Client = mock(S3Client.class);
    S3JsonMessageSources sources = new S3JsonMessageSources(s3Client, "bucket", "");
    assertEquals("fiss.ndjson", sources.createFissObjectKey());
    assertEquals("fiss-1-100.ndjson", sources.createFissObjectKey(1, 100));
    assertEquals("mcs.ndjson", sources.createMcsObjectKey());
    assertEquals("mcs-80-471.ndjson", sources.createMcsObjectKey(80, 471));
  }

  /**
   * Verifies if a directory path (with no trailing slash) is provided when constructing a {@link
   * S3JsonMessageSources} then the directory path can be read from the object and has a trailing
   * slash added to it.
   */
  @Test
  public void testPathConstructionWithADirectory() {
    S3Client s3Client = mock(S3Client.class);
    S3JsonMessageSources sources = new S3JsonMessageSources(s3Client, "bucket", "a/bb/ccc");
    assertEquals("a/bb/ccc/", sources.getDirectoryPath());
  }

  /**
   * Verifies if a directory path (with a trailing slash) is provided when constructing a {@link
   * S3JsonMessageSources} then the directory path can be read from the object and retains the
   * trailing slash.
   */
  @Test
  public void testPathConstructionWithADirectoryAndTrailingSlash() {
    S3Client s3Client = mock(S3Client.class);
    S3JsonMessageSources sources = new S3JsonMessageSources(s3Client, "bucket", "a/bb/");
    assertEquals("a/bb/", sources.getDirectoryPath());
  }
}
