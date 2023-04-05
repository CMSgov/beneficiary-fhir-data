package gov.cms.bfd.pipeline.rda.grpc.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Tests the {@link RdaS3JsonMessageSourceFactory}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RdaS3JsonMessageSourceFactoryTest {
  /** Verifies that object keys are constructed correctly. */
  @Test
  public void testPathConstructionWithNoDirectory() {
    assertEquals("fiss.ndjson", RdaS3JsonMessageSourceFactory.createValidFissKeyForTesting());
    assertEquals("mcs.ndjson", RdaS3JsonMessageSourceFactory.createValidMcsKeyForTesting());
  }
}
