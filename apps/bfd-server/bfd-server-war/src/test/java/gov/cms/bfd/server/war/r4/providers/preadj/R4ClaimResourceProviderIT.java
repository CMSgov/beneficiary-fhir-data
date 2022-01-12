package gov.cms.bfd.server.war.r4.providers.preadj;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.utils.AssertUtils;
import gov.cms.bfd.server.war.utils.RDATestUtils;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Claim;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class R4ClaimResourceProviderIT {

  private static final RDATestUtils testUtils = new RDATestUtils();

  private static final Set<String> IGNORE_PATTERNS =
      Set.of("\"/link/[0-9]+/url\"", "\"/created\"", "\"/meta/lastUpdated\"");

  @BeforeAll
  public static void init() {
    testUtils.init();
    testUtils.seedData();
  }

  @AfterAll
  public static void tearDown() {
    testUtils.truncateTables();
    testUtils.destroy();
  }

  @Test
  public void shouldGetCorrectFissClaimResourceById() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    Claim claimResult = fhirClient.read().resource(Claim.class).withId("f-123456").execute();

    String expected = testUtils.expectedResponseFor("claimFissRead");
    String actual = FhirContext.forR4().newJsonParser().encodeResourceToString(claimResult);

    AssertUtils.assertJsonEquals(expected, actual, IGNORE_PATTERNS);
  }

  @Test
  public void shouldGetCorrectMcsClaimResourceById() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    Claim claimResult = fhirClient.read().resource(Claim.class).withId("m-654321").execute();

    String expected = testUtils.expectedResponseFor("claimMcsRead");
    String actual = FhirContext.forR4().newJsonParser().encodeResourceToString(claimResult);

    AssertUtils.assertJsonEquals(expected, actual, IGNORE_PATTERNS);
  }

  @Test
  public void shouldGetCorrectClaimResourcesByMbiHash() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    Bundle claimResult =
        fhirClient
            .search()
            .forResource(Claim.class)
            .where(
                Map.of(
                    "mbi", List.of(new ReferenceParam(RDATestUtils.MBI_HASH)),
                    "service-date",
                        List.of(new DateParam("gt1970-07-18"), new DateParam("lt1970-07-30"))))
            .returnBundle(Bundle.class)
            .execute();

    // Sort entries for consistent testing results
    claimResult.getEntry().sort(Comparator.comparing(a -> a.getResource().getId()));

    String expected = testUtils.expectedResponseFor("claimSearch");
    String actual = FhirContext.forR4().newJsonParser().encodeResourceToString(claimResult);

    Set<String> ignorePatterns = new HashSet<>(IGNORE_PATTERNS);
    ignorePatterns.add("\"/id\"");
    ignorePatterns.add("\"/entry/[0-9]+/resource/created\"");

    AssertUtils.assertJsonEquals(expected, actual, ignorePatterns);
  }
}
