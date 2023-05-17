package gov.cms.bfd.server.war.r4.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.data.fda.lookup.FdaDrugCodeDisplayLookup;
import gov.cms.bfd.data.npi.lookup.NPIOrgLookup;
import gov.cms.bfd.server.war.commons.LoadedFilterManager;
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

  /** The mock metric registry. */
  @Mock private MetricRegistry metricRegistry;
  /** The mock samhsa matcher. */
  @Mock private R4EobSamhsaMatcher samhsaMatcher;
  /** The mock loaded filter manager. */
  @Mock private LoadedFilterManager loadedFilterManager;
  /** The mock drug code display lookup entity. */
  @Mock private FdaDrugCodeDisplayLookup drugCodeDisplayLookup;
  /** The mock npi org lookup entity. */
  @Mock private NPIOrgLookup npiOrgLookup;

  // Mock transformers
  /** The mock transformer for carrier claims. */
  @Mock private CarrierClaimTransformerV2 carrierClaimTransformerV2;
  /** The mock transformer for dme claims. */
  @Mock private DMEClaimTransformerV2 dmeClaimTransformer;
  /** The mock transformer for hha claims. */
  @Mock private HHAClaimTransformerV2 hhaClaimTransformer;
  /** The mock transformer for hospice claims. */
  @Mock private HospiceClaimTransformerV2 hospiceClaimTransformer;
  /** The mock transformer for inpatient claims. */
  @Mock private InpatientClaimTransformerV2 inpatientClaimTransformer;
  /** The mock transformer for outpatient claims. */
  @Mock private OutpatientClaimTransformerV2 outpatientClaimTransformer;
  /** The mock transformer for part D events claims. */
  @Mock private PartDEventTransformerV2 partDEventTransformer;
  /** The mock transformer for snf claims. */
  @Mock private SNFClaimTransformerV2 snfClaimTransformerV2;

  /** Sets up the test class. */
  @BeforeEach
  public void setup() {
    eobProvider =
        new R4ExplanationOfBenefitResourceProvider(
            metricRegistry,
            loadedFilterManager,
            samhsaMatcher,
            drugCodeDisplayLookup,
            npiOrgLookup,
            carrierClaimTransformerV2,
            dmeClaimTransformer,
            hhaClaimTransformer,
            hospiceClaimTransformer,
            inpatientClaimTransformer,
            outpatientClaimTransformer,
            partDEventTransformer,
            snfClaimTransformerV2);
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
