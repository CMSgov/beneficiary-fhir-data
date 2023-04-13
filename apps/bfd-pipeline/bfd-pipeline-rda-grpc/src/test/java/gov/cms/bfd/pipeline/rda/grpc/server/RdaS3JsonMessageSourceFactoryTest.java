package gov.cms.bfd.pipeline.rda.grpc.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Tests the {@link RdaS3JsonMessageSourceFactory}. */
public class RdaS3JsonMessageSourceFactoryTest {
  /** Verifies that object keys are constructed correctly. */
  @Test
  public void testPathConstructionWithNoDirectory() {
    assertEquals("fiss.ndjson", RdaS3JsonMessageSourceFactory.createValidFissKeyForTesting());
    assertEquals("mcs.ndjson", RdaS3JsonMessageSourceFactory.createValidMcsKeyForTesting());
  }
}
