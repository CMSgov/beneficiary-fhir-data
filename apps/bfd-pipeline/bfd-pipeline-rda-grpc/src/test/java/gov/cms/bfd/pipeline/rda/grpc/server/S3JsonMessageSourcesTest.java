package gov.cms.bfd.pipeline.rda.grpc.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

/** Tests the {@link S3JsonMessageSources}. */
public class S3JsonMessageSourcesTest {
  /** Verifies that if object keys are constructed correctly. */
  @Test
  public void testPathConstructionWithNoDirectory() {
    S3DirectoryDao s3Dao = mock(S3DirectoryDao.class);
    S3JsonMessageSources sources = new S3JsonMessageSources(s3Dao);
    assertEquals("fiss.ndjson", sources.createFissObjectKey());
    assertEquals("fiss-1-100.ndjson", sources.createFissObjectKey(1, 100));
    assertEquals("mcs.ndjson", sources.createMcsObjectKey());
    assertEquals("mcs-80-471.ndjson", sources.createMcsObjectKey(80, 471));
  }
}
