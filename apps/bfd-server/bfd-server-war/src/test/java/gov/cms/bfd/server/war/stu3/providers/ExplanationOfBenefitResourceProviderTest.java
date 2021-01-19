package gov.cms.bfd.server.war.stu3.providers;

import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link
 * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider}.
 */
public final class ExplanationOfBenefitResourceProviderTest {
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
    Assert.assertEquals(ClaimType.values().length, typesForNull.size());

    TokenAndListParam typeParamSystemWildcard =
        new TokenAndListParam()
            .addAnd(
                new TokenOrListParam()
                    .add(TransformerConstants.CODING_SYSTEM_BBAPI_EOB_TYPE, null));
    Set<ClaimType> typesForSystemWildcard =
        ExplanationOfBenefitResourceProvider.parseTypeParam(typeParamSystemWildcard);
    Assert.assertEquals(ClaimType.values().length, typesForSystemWildcard.size());

    TokenAndListParam typeParamSingle =
        new TokenAndListParam()
            .addAnd(
                new TokenOrListParam(
                    TransformerConstants.CODING_SYSTEM_BBAPI_EOB_TYPE, ClaimType.CARRIER.name()));
    Set<ClaimType> typesForSingle =
        ExplanationOfBenefitResourceProvider.parseTypeParam(typeParamSingle);
    Assert.assertEquals(1, typesForSingle.size());
    Assert.assertTrue(typesForSingle.contains(ClaimType.CARRIER));

    TokenAndListParam typeParamSingleNullSystem =
        new TokenAndListParam().addAnd(new TokenOrListParam(null, ClaimType.CARRIER.name()));
    Set<ClaimType> typesForSingleNullSystem =
        ExplanationOfBenefitResourceProvider.parseTypeParam(typeParamSingleNullSystem);
    Assert.assertEquals(1, typesForSingleNullSystem.size());
    Assert.assertTrue(typesForSingleNullSystem.contains(ClaimType.CARRIER));

    TokenAndListParam typeParamSingleInvalidSystem =
        new TokenAndListParam().addAnd(new TokenOrListParam("foo", ClaimType.CARRIER.name()));
    Set<ClaimType> typesForSingleInvalidSystem =
        ExplanationOfBenefitResourceProvider.parseTypeParam(typeParamSingleInvalidSystem);
    Assert.assertEquals(0, typesForSingleInvalidSystem.size());

    TokenAndListParam typeParamSingleInvalidCode =
        new TokenAndListParam().addAnd(new TokenOrListParam(null, "foo"));
    Set<ClaimType> typesForSingleInvalidCode =
        ExplanationOfBenefitResourceProvider.parseTypeParam(typeParamSingleInvalidCode);
    Assert.assertEquals(0, typesForSingleInvalidCode.size());

    TokenAndListParam typeParamTwoEntries =
        new TokenAndListParam()
            .addAnd(new TokenOrListParam(null, ClaimType.CARRIER.name(), ClaimType.DME.name()))
            .addAnd(new TokenOrListParam(null, ClaimType.CARRIER.name()));
    Set<ClaimType> typesForTwoEntries =
        ExplanationOfBenefitResourceProvider.parseTypeParam(typeParamTwoEntries);
    Assert.assertEquals(1, typesForTwoEntries.size());
    Assert.assertTrue(typesForTwoEntries.contains(ClaimType.CARRIER));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#parseTypeParam(TokenAndListParam)}
   * works as expected when query param modifiers are used, which are unsupported.
   */
  @Test(expected = IllegalArgumentException.class)
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
    ExplanationOfBenefitResourceProvider.parseTypeParam(typeParam);
  }
}
