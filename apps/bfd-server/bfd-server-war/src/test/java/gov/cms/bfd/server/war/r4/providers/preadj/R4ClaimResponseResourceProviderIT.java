package gov.cms.bfd.server.war.r4.providers.preadj;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.utils.AssertUtils;
import gov.cms.bfd.server.war.utils.RDATestUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ClaimResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class R4ClaimResponseResourceProviderIT {

  private static final RDATestUtils testUtils = new RDATestUtils();

  private static final Set<String> IGNORE_PATTERNS =
      ImmutableSet.of("\"/link/[0-9]+/url\"", "\"/created\"", "\"/meta/lastUpdated\"");

  @BeforeAll
  public static void init() {
    testUtils.init();
    testUtils.seedData(true);
  }

  @AfterAll
  public static void tearDown() {
    testUtils.truncateTables();
    testUtils.destroy();
  }

  @Test
  void shouldGetCorrectFissClaimResponseResourceById() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    ClaimResponse claimResult =
        fhirClient.read().resource(ClaimResponse.class).withId("f-123456").execute();

    String expected = testUtils.expectedResponseFor("claimResponseFissRead");
    String actual = FhirContext.forR4().newJsonParser().encodeResourceToString(claimResult);

    AssertUtils.assertJsonEquals(expected, actual, IGNORE_PATTERNS);
  }

  @Test
  void shouldGetCorrectMcsClaimResponseResourceById() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    ClaimResponse claimResult =
        fhirClient.read().resource(ClaimResponse.class).withId("m-654321").execute();

    String expected = testUtils.expectedResponseFor("claimResponseMcsRead");
    String actual = FhirContext.forR4().newJsonParser().encodeResourceToString(claimResult);

    AssertUtils.assertJsonEquals(expected, actual, IGNORE_PATTERNS);
  }

  @Test
  void shouldGetCorrectClaimResponseResourcesByMbiHash() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    Bundle claimResult =
        fhirClient
            .search()
            .forResource(ClaimResponse.class)
            .where(
                ImmutableMap.of(
                    "mbi", Collections.singletonList(new ReferenceParam(RDATestUtils.MBI_OLD_HASH)),
                    "service-date",
                        Arrays.asList(
                            new DateParam("gt1970-07-18"), new DateParam("lt1970-07-30"))))
            .returnBundle(Bundle.class)
            .execute();

    // Sort entries for consistent testing results
    claimResult.getEntry().sort(Comparator.comparing(a -> a.getResource().getId()));

    String expected = testUtils.expectedResponseFor("claimResponseSearch");
    String actual = FhirContext.forR4().newJsonParser().encodeResourceToString(claimResult);

    Set<String> ignorePatterns = new HashSet<>(IGNORE_PATTERNS);
    ignorePatterns.add("\"/id\"");
    ignorePatterns.add("\"/entry/[0-9]+/resource/created\"");

    AssertUtils.assertJsonEquals(expected, actual, ignorePatterns);
  }
}
