package gov.cms.bfd.pipeline.rda.grpc.server;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import com.amazonaws.services.s3.AmazonS3;
import org.junit.Test;

public class S3JsonMessageSourcesTest {
  @Test
  public void testPathConstructionWithNoDirectory() {
    AmazonS3 s3Client = mock(AmazonS3.class);
    S3JsonMessageSources sources = new S3JsonMessageSources(s3Client, "bucket", "");
    assertEquals("fiss.ndjson", sources.createFissObjectKey());
    assertEquals("fiss-1-100.ndjson", sources.createFissObjectKey(1, 100));
    assertEquals("mcs.ndjson", sources.createMcsObjectKey());
    assertEquals("mcs-80-471.ndjson", sources.createMcsObjectKey(80, 471));
  }

  @Test
  public void testPathConstructionWithADirectory() {
    AmazonS3 s3Client = mock(AmazonS3.class);
    S3JsonMessageSources sources = new S3JsonMessageSources(s3Client, "bucket", "a/bb/ccc");
    assertEquals("a/bb/ccc/fiss.ndjson", sources.createFissObjectKey());
    assertEquals("a/bb/ccc/fiss-1-100.ndjson", sources.createFissObjectKey(1, 100));
    assertEquals("a/bb/ccc/mcs.ndjson", sources.createMcsObjectKey());
    assertEquals("a/bb/ccc/mcs-80-471.ndjson", sources.createMcsObjectKey(80, 471));
  }

  @Test
  public void testPathConstructionWithADirectoryAndTrailingSlash() {
    AmazonS3 s3Client = mock(AmazonS3.class);
    S3JsonMessageSources sources = new S3JsonMessageSources(s3Client, "bucket", "a/bb/");
    assertEquals("a/bb/fiss.ndjson", sources.createFissObjectKey());
    assertEquals("a/bb/fiss-1-100.ndjson", sources.createFissObjectKey(1, 100));
    assertEquals("a/bb/mcs.ndjson", sources.createMcsObjectKey());
    assertEquals("a/bb/mcs-80-471.ndjson", sources.createMcsObjectKey(80, 471));
  }
}
