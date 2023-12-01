package gov.cms.bfd.server.war.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Tests the functionality in {@link CommonTransformerUtils}. */
public class CommonTransformerUtilsTest {

  /**
   * Verifies that {@link CommonTransformerUtils#parseTypeParam} returns all claim types when the
   * input is {@code null}.
   */
  @Test
  void testParseTypeParamWhenInputNullExpectAllTypes() {
    Set<ClaimType> typesForNull = CommonTransformerUtils.parseTypeParam(null);
    assertEquals(ClaimType.values().length, typesForNull.size());
    assertTrue(typesForNull.contains(ClaimType.CARRIER));
    assertTrue(typesForNull.contains(ClaimType.DME));
    assertTrue(typesForNull.contains(ClaimType.SNF));
    assertTrue(typesForNull.contains(ClaimType.PDE));
    assertTrue(typesForNull.contains(ClaimType.HHA));
    assertTrue(typesForNull.contains(ClaimType.HOSPICE));
    assertTrue(typesForNull.contains(ClaimType.INPATIENT));
    assertTrue(typesForNull.contains(ClaimType.OUTPATIENT));
  }

  /**
   * Verifies that {@link CommonTransformerUtils#parseTypeParam} returns all claim types when the
   * input is the code system with no type specified.
   */
  @Test
  void testParseTypeParamWhenInputCodeSystemOnlyExpectAllTypes() {
    TokenAndListParam typeParamSystemWildcard =
        new TokenAndListParam()
            .addAnd(
                new TokenOrListParam()
                    .add(TransformerConstants.CODING_SYSTEM_BBAPI_EOB_TYPE, null));
    Set<ClaimType> typesForSystemWildcard =
        CommonTransformerUtils.parseTypeParam(typeParamSystemWildcard);
    assertEquals(ClaimType.values().length, typesForSystemWildcard.size());
  }

  /**
   * Verifies that {@link CommonTransformerUtils#parseTypeParam} returns a single claim type when
   * the input is a single claim type.
   */
  @Test
  void testParseTypeParamWhenInputSingleTypeExpectSingleType() {
    TokenAndListParam typeParamSingle =
        new TokenAndListParam()
            .addAnd(
                new TokenOrListParam(
                    TransformerConstants.CODING_SYSTEM_BBAPI_EOB_TYPE, ClaimType.CARRIER.name()));
    Set<ClaimType> typesForSingle = CommonTransformerUtils.parseTypeParam(typeParamSingle);
    assertEquals(1, typesForSingle.size());
    assertTrue(typesForSingle.contains(ClaimType.CARRIER));
  }

  /**
   * Verifies that {@link CommonTransformerUtils#parseTypeParam} returns a single claim type when
   * the input is a single claim type with no code system.
   */
  @Test
  void testParseTypeParamWhenInputSingleTypeNoCodeSystemExpectSingleType() {
    TokenAndListParam typeParamSingleNullSystem =
        new TokenAndListParam().addAnd(new TokenOrListParam(null, ClaimType.CARRIER.name()));
    Set<ClaimType> typesForSingleNullSystem =
        CommonTransformerUtils.parseTypeParam(typeParamSingleNullSystem);
    assertEquals(1, typesForSingleNullSystem.size());
    assertTrue(typesForSingleNullSystem.contains(ClaimType.CARRIER));
  }

  /**
   * Verifies that {@link CommonTransformerUtils#parseTypeParam} returns an empty list when the
   * input code system is present but not recognized.
   */
  @Test
  void testParseTypeParamWhenBadCodeSystemExpectEmptyList() {
    TokenAndListParam typeParamSingleInvalidSystem =
        new TokenAndListParam().addAnd(new TokenOrListParam("foo", ClaimType.CARRIER.name()));
    Set<ClaimType> typesForSingleInvalidSystem =
        CommonTransformerUtils.parseTypeParam(typeParamSingleInvalidSystem);
    assertEquals(0, typesForSingleInvalidSystem.size());
  }

  /**
   * Verifies that {@link CommonTransformerUtils#parseTypeParam} returns an empty list when the
   * claim type is present but not recognized.
   */
  @Test
  void testParseTypeParamWhenBadTypeExpectEmptyList() {
    TokenAndListParam typeParamSingleInvalidCode =
        new TokenAndListParam().addAnd(new TokenOrListParam(null, "foo"));
    Set<ClaimType> typesForSingleInvalidCode =
        CommonTransformerUtils.parseTypeParam(typeParamSingleInvalidCode);
    assertEquals(0, typesForSingleInvalidCode.size());
  }

  /**
   * Verifies that {@link CommonTransformerUtils#parseTypeParam} ignores an invalid or unspecified
   * claim type.
   */
  @Test
  void testParseTypeParamWhenInvalidClaimTypeExpectBadTypeIgnored() {
    TokenAndListParam typeParamSingleInvalidCode =
        new TokenAndListParam().addAnd(new TokenOrListParam(null, "carriers", "dme"));
    Set<ClaimType> result = CommonTransformerUtils.parseTypeParam(typeParamSingleInvalidCode);
    assertEquals(1, result.size());
    assertFalse(result.contains(ClaimType.CARRIER)); // ignored carriers, should be carrier
    assertTrue(result.contains(ClaimType.DME));
    assertFalse(result.contains(ClaimType.SNF));
  }

  /**
   * Verifies that {@link CommonTransformerUtils#parseTypeParam} uses only the types common to all
   * entries when the input has multiple tokenOrList params specified.
   */
  @Test
  void testParseTypeParamWhenMultipleListParamsExpectCommonTypesUsed() {
    TokenAndListParam typeParamTwoEntries =
        new TokenAndListParam()
            .addAnd(new TokenOrListParam(null, ClaimType.CARRIER.name(), ClaimType.DME.name()))
            .addAnd(new TokenOrListParam(null, ClaimType.DME.name()));
    Set<ClaimType> typesForTwoEntries = CommonTransformerUtils.parseTypeParam(typeParamTwoEntries);
    assertEquals(1, typesForTwoEntries.size());
    assertTrue(typesForTwoEntries.contains(ClaimType.DME));
  }

  /**
   * Verifies that {@link CommonTransformerUtils#parseTypeParam(TokenAndListParam)} throws an
   * exception when query param modifiers are used, which are unsupported.
   */
  @Test
  void testParseTypeParamWhenUsingModifiersExpectException() {
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
        IllegalArgumentException.class,
        () -> {
          CommonTransformerUtils.parseTypeParam(typeParam);
        });
  }
}
