package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.data.fda.lookup.FdaDrugCodeDisplayLookup;
import gov.cms.bfd.data.npi.lookup.NPIOrgLookup;
import gov.cms.bfd.server.war.commons.LoadedFilterManager;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import org.hl7.fhir.dstu3.model.IdType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link
 * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider}.
 */
@ExtendWith(MockitoExtension.class)
public final class ExplanationOfBenefitResourceProviderTest {

  /** The class under test. */
  ExplanationOfBenefitResourceProvider eobProvider;

  /** The mocked request details. */
  @Mock ServletRequestDetails requestDetails;

  /** The mocked input id value. */
  @Mock IdType eobId;

  /** The mock metric registry. */
  @Mock private MetricRegistry metricRegistry;
  /** The mock samhsa matcher. */
  @Mock private Stu3EobSamhsaMatcher samhsaMatcher;
  /** The mock loaded filter manager. */
  @Mock private LoadedFilterManager loadedFilterManager;
  /** The mock drug code display lookup entity. */
  @Mock private FdaDrugCodeDisplayLookup drugCodeDisplayLookup;
  /** The mock npi org lookup entity. */
  @Mock private NPIOrgLookup npiOrgLookup;
  /** The ExecutorService entity. */
  @Mock private ExecutorService executorService;

  // Mock transformers
  /** The mock transformer for carrier claims. */
  @Mock private CarrierClaimTransformer carrierClaimTransformer;
  /** The mock transformer for dme claims. */
  @Mock private DMEClaimTransformer dmeClaimTransformer;
  /** The mock transformer for hha claims. */
  @Mock private HHAClaimTransformer hhaClaimTransformer;
  /** The mock transformer for hospice claims. */
  @Mock private HospiceClaimTransformer hospiceClaimTransformer;
  /** The mock transformer for inpatient claims. */
  @Mock private InpatientClaimTransformer inpatientClaimTransformer;
  /** The mock transformer for outpatient claims. */
  @Mock private OutpatientClaimTransformer outpatientClaimTransformer;
  /** The mock transformer for part D events claims. */
  @Mock private PartDEventTransformer partDEventTransformer;
  /** The mock transformer for snf claims. */
  @Mock private SNFClaimTransformer snfClaimTransformer;

  /** Sets up the test class. */
  @BeforeEach
  public void setup() {
    eobProvider =
        new ExplanationOfBenefitResourceProvider(
            metricRegistry,
            loadedFilterManager,
            samhsaMatcher,
            drugCodeDisplayLookup,
            npiOrgLookup,
            executorService,
            carrierClaimTransformer,
            dmeClaimTransformer,
            hhaClaimTransformer,
            hospiceClaimTransformer,
            inpatientClaimTransformer,
            outpatientClaimTransformer,
            partDEventTransformer,
            snfClaimTransformer);
    lenient().when(eobId.getVersionIdPartAsLong()).thenReturn(null);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#parseTypeParam(TokenAndListParam)}
   * works as expected.
   */
  @Test
  public void parseTypeParam() {
    TokenAndListParam typeParamNull = null;
    Set<ClaimType> typesForNull =
        ExplanationOfBenefitResourceProvider.parseTypeParam(typeParamNull);
    assertEquals(ClaimType.values().length, typesForNull.size());

    TokenAndListParam typeParamSystemWildcard =
        new TokenAndListParam()
            .addAnd(
                new TokenOrListParam()
                    .add(TransformerConstants.CODING_SYSTEM_BBAPI_EOB_TYPE, null));
    Set<ClaimType> typesForSystemWildcard =
        ExplanationOfBenefitResourceProvider.parseTypeParam(typeParamSystemWildcard);
    assertEquals(ClaimType.values().length, typesForSystemWildcard.size());

    TokenAndListParam typeParamSingle =
        new TokenAndListParam()
            .addAnd(
                new TokenOrListParam(
                    TransformerConstants.CODING_SYSTEM_BBAPI_EOB_TYPE, ClaimType.CARRIER.name()));
    Set<ClaimType> typesForSingle =
        ExplanationOfBenefitResourceProvider.parseTypeParam(typeParamSingle);
    assertEquals(1, typesForSingle.size());
    assertTrue(typesForSingle.contains(ClaimType.CARRIER));

    TokenAndListParam typeParamSingleNullSystem =
        new TokenAndListParam().addAnd(new TokenOrListParam(null, ClaimType.CARRIER.name()));
    Set<ClaimType> typesForSingleNullSystem =
        ExplanationOfBenefitResourceProvider.parseTypeParam(typeParamSingleNullSystem);
    assertEquals(1, typesForSingleNullSystem.size());
    assertTrue(typesForSingleNullSystem.contains(ClaimType.CARRIER));

    TokenAndListParam typeParamSingleInvalidSystem =
        new TokenAndListParam().addAnd(new TokenOrListParam("foo", ClaimType.CARRIER.name()));
    Set<ClaimType> typesForSingleInvalidSystem =
        ExplanationOfBenefitResourceProvider.parseTypeParam(typeParamSingleInvalidSystem);
    assertEquals(0, typesForSingleInvalidSystem.size());

    TokenAndListParam typeParamSingleInvalidCode =
        new TokenAndListParam().addAnd(new TokenOrListParam(null, "foo"));
    Set<ClaimType> typesForSingleInvalidCode =
        ExplanationOfBenefitResourceProvider.parseTypeParam(typeParamSingleInvalidCode);
    assertEquals(0, typesForSingleInvalidCode.size());

    TokenAndListParam typeParamTwoEntries =
        new TokenAndListParam()
            .addAnd(new TokenOrListParam(null, ClaimType.CARRIER.name(), ClaimType.DME.name()))
            .addAnd(new TokenOrListParam(null, ClaimType.CARRIER.name()));
    Set<ClaimType> typesForTwoEntries =
        ExplanationOfBenefitResourceProvider.parseTypeParam(typeParamTwoEntries);
    assertEquals(1, typesForTwoEntries.size());
    assertTrue(typesForTwoEntries.contains(ClaimType.CARRIER));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#parseTypeParam(TokenAndListParam)}
   * throws an exception when query param modifiers are used, which are unsupported.
   */
  @Test
  public void parseTypeParam_modifiers() {
    TokenAndListParam typeParam =
        new TokenAndListParam()
            .addAnd(
                new TokenOrListParam()
                    .add(
                        new TokenParam(
                                TransformerConstants.CODING_SYSTEM_BBAPI_EOB_TYPE,
                                ClaimType.CARRIER.name())
                            .setModifier(TokenParamModifier.ABOVE)));
    assertThrows(
        InvalidRequestException.class,
        () -> {
          ExplanationOfBenefitResourceProvider.parseTypeParam(typeParam);
        });
  }

  /**
   * Verifies that {@link ExplanationOfBenefitResourceProvider#read} throws an exception for an
   * {@link IdType} that has an invalidly formatted eobId parameter.
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
   * Verifies that {@link ExplanationOfBenefitResourceProvider#findByPatient} throws an exception
   * for an {@link IdType} that has a null eobId parameter.
   */
  @Test
  public void testEobReadWhereNullIdExpectException() {
    InvalidRequestException exception =
        assertThrows(InvalidRequestException.class, () -> eobProvider.read(eobId, requestDetails));
    assertEquals("Missing required ExplanationOfBenefit ID", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link ExplanationOfBenefitResourceProvider#findByPatient} throws an exception
   * for an {@link IdType} that has a missing eobId parameter.
   */
  @Test
  public void testEobReadWhereEmptyIdExpectException() {
    when(eobId.getIdPart()).thenReturn("");

    InvalidRequestException exception =
        assertThrows(InvalidRequestException.class, () -> eobProvider.read(eobId, requestDetails));
    assertEquals("Missing required ExplanationOfBenefit ID", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link ExplanationOfBenefitResourceProvider#findByPatient} throws an exception
   * for an {@link IdType} that has a version supplied with the eobId parameter, as our read
   * requests do not support versioned requests.
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
