package gov.cms.bfd.server.war.r4.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Units tests for the {@link R4ExplanationOfBenefitResourceProvider} that do not require a full
 * fhir setup to validate. Anything we want to validate from the fhir client level should go in
 * {@link R4ExplanationOfBenefitResourceProviderIT}.
 */
@ExtendWith(MockitoExtension.class)
public class R4ExplanationOfBenefitResourceProviderTest {

  /** The class under test. */
  R4ExplanationOfBenefitResourceProvider eobProvider;

  /** The mocked request details. */
  @Mock ServletRequestDetails requestDetails;

  /** The mocked input id value. */
  @Mock IdType eobId;

  /** Sets up the test class. */
  @BeforeEach
  public void setup() {
    eobProvider = new R4ExplanationOfBenefitResourceProvider();
    lenient().when(eobId.getVersionIdPartAsLong()).thenReturn(null);
  }

  /**
   * Verifies that {@link R4ExplanationOfBenefitResourceProvider#read} throws an exception for an
   * {@link org.hl7.fhir.r4.model.IdType} that has an invalidly formatted eobId parameter.
   */
  @Test
  public void testEobReadWhereInvalidIdExpectException() {
    when(eobId.getIdPart()).thenReturn("1234");

    // Parameter is invalid, should throw exception
    InvalidRequestException exception =
        assertThrows(InvalidRequestException.class, () -> eobProvider.read(eobId, requestDetails));
    assertEquals(
        "ExplanationOfBenefit ID pattern: '1234' does not match expected pattern: {alphaString}-{idNumber}",
        exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link R4ExplanationOfBenefitResourceProvider#findByPatient} throws an exception
   * for an {@link org.hl7.fhir.r4.model.IdType} that has a null eobId parameter.
   */
  @Test
  public void testEobReadWhereNullIdExpectException() {
    InvalidRequestException exception =
        assertThrows(InvalidRequestException.class, () -> eobProvider.read(eobId, requestDetails));
    assertEquals("Missing required ExplanationOfBenefit ID", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link R4ExplanationOfBenefitResourceProvider#findByPatient} throws an exception
   * for an {@link org.hl7.fhir.r4.model.IdType} that has a missing eobId parameter.
   */
  @Test
  public void testEobReadWhereEmptyIdExpectException() {
    when(eobId.getIdPart()).thenReturn("");

    InvalidRequestException exception =
        assertThrows(InvalidRequestException.class, () -> eobProvider.read(eobId, requestDetails));
    assertEquals("Missing required ExplanationOfBenefit ID", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link R4ExplanationOfBenefitResourceProvider#findByPatient} throws an exception
   * for an {@link org.hl7.fhir.r4.model.IdType} that has a version supplied with the eobId
   * parameter, as our read requests do not support versioned requests.
   */
  @Test
  public void testEobReadWhereVersionedIdExpectException() {
    when(eobId.getVersionIdPartAsLong()).thenReturn(1234L);

    InvalidRequestException exception =
        assertThrows(InvalidRequestException.class, () -> eobProvider.read(eobId, requestDetails));
    assertEquals(
        "ExplanationOfBenefit ID must not define a version", exception.getLocalizedMessage());
  }
}
