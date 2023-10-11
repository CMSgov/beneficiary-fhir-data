package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.server.war.commons.LoadedFilterManager;
import org.hl7.fhir.dstu3.model.IdType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Units tests for the {@link CoverageResourceProvider} that do not require a full fhir setup to
 * validate. Anything we want to validate from the fhir client level should go in {@link
 * CoverageResourceProviderIT}.
 */
@ExtendWith(MockitoExtension.class)
public class CoverageResourceProviderTest {

  /** The class under test. */
  CoverageResourceProvider coverageProvider;

  /** The mocked input id value. */
  @Mock IdType coverageId;

  /** The Metric registry. */
  @Mock private MetricRegistry metricRegistry;
  /** The Loaded filter manager. */
  @Mock private LoadedFilterManager loadedFilterManager;

  /** The mock coverage transformer. */
  @Mock private CoverageTransformer coverageTransformer;

  /** Sets up the test class. */
  @BeforeEach
  public void setup() {
    coverageProvider =
        new CoverageResourceProvider(metricRegistry, loadedFilterManager, coverageTransformer);
    lenient().when(coverageId.getVersionIdPartAsLong()).thenReturn(null);
  }

  /**
   * Verifies that {@link CoverageResourceProvider#read} throws an exception when the {@link IdType}
   * has an invalidly formatted coverageId parameter.
   */
  @Test
  public void testCoverageReadWhereInvalidIdExpectException() {
    when(coverageId.getIdPart()).thenReturn("1?234");

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> {
              coverageProvider.read(coverageId);
            });
    assertEquals(
        "Coverage ID pattern: '1?234' does not match expected pattern: {alphaNumericString}-{singleCharacter}-{idNumber}",
        exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link CoverageResourceProvider#read} throws an exception when the {@link IdType}
   * has a blank value.
   */
  @Test
  public void testCoverageReadWhereBlankIdExpectException() {
    when(coverageId.getIdPart()).thenReturn("");

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> {
              coverageProvider.read(coverageId);
            });
    assertEquals("Missing required coverage ID", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link CoverageResourceProvider#read} throws an exception when the {@link IdType}
   * has a null value.
   */
  @Test
  public void testCoverageReadWhereNullIdExpectException() {
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> {
              coverageProvider.read(coverageId);
            });
    assertEquals("Missing required coverage ID", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link CoverageResourceProvider#read} throws an exception when the {@link IdType}
   * has a version supplied with the coverageId parameter, as our read requests do not support
   * versioned requests.
   */
  @Test
  public void testCoverageReadWhereVersionedIdExpectException() {
    when(coverageId.getVersionIdPartAsLong()).thenReturn(1234L);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> {
              coverageProvider.read(coverageId);
            });
    assertEquals("Coverage ID must not define a version", exception.getLocalizedMessage());
  }
}
