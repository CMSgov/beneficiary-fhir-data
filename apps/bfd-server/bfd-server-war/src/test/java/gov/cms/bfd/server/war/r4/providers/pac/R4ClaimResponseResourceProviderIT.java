package gov.cms.bfd.server.war.r4.providers.pac;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.NumberParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.utils.AssertUtils;
import gov.cms.bfd.server.war.utils.RDATestUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ClaimResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class R4ClaimResponseResourceProviderIT {

  private static final RDATestUtils testUtils = new RDATestUtils();

  private static final Set<String> IGNORE_PATTERNS =
      Set.of("\"/link/[0-9]+/url\"", "\"/created\"", "\"/meta/lastUpdated\"");

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

  /**
   * Tests to see if the correct response is given when a FISS {@link ClaimResponse} is looked up by
   * a specific ID
   */
  @Test
  void shouldGetCorrectFissClaimResponseResourceById() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    ClaimResponse claimResult =
        fhirClient.read().resource(ClaimResponse.class).withId("f-123456").execute();

    String expected = testUtils.expectedResponseFor("claimResponseFissRead");
    String actual = FhirContext.forR4().newJsonParser().encodeResourceToString(claimResult);

    AssertUtils.assertJsonEquals(expected, actual, IGNORE_PATTERNS);
  }

  /**
   * Tests to see if the correct response is given when an MCS {@link ClaimResponse} is looked up by
   * a specific ID
   */
  @Test
  void shouldGetCorrectMcsClaimResponseResourceById() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    ClaimResponse claimResult =
        fhirClient.read().resource(ClaimResponse.class).withId("m-654321").execute();

    String expected = testUtils.expectedResponseFor("claimResponseMcsRead");
    String actual = FhirContext.forR4().newJsonParser().encodeResourceToString(claimResult);

    AssertUtils.assertJsonEquals(expected, actual, IGNORE_PATTERNS);
  }

  /**
   * Tests to see if the correct response is given when a search is done for {@link ClaimResponse}s
   * using given mbi and service-date range
   */
  @Test
  void shouldGetCorrectClaimResponseResourcesByMbiHash() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    Bundle claimResult =
        fhirClient
            .search()
            .forResource(ClaimResponse.class)
            .where(
                Map.of(
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

  /**
   * Tests to see if the correct paginated response is given when a search is done for {@link
   * ClaimResponse}s using given mbi and service-date range
   */
  @Test
  void shouldGetCorrectClaimResponseResourcesByMbiHashWithPagination() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    Bundle claimResult =
        fhirClient
            .search()
            .forResource(ClaimResponse.class)
            .where(
                Map.of(
                    "mbi",
                    Collections.singletonList(new ReferenceParam(RDATestUtils.MBI_OLD_HASH)),
                    "service-date",
                    Arrays.asList(new DateParam("gt1970-07-18"), new DateParam("lt1970-07-30")),
                    "_count",
                    List.of(new NumberParam("5")),
                    "startIndex",
                    List.of(new NumberParam("1"))))
            .returnBundle(Bundle.class)
            .execute();

    // Sort entries for consistent testing results
    claimResult.getEntry().sort(Comparator.comparing(a -> a.getResource().getId()));

    String expected = testUtils.expectedResponseFor("claimResponseSearchPaginated");
    String actual = FhirContext.forR4().newJsonParser().encodeResourceToString(claimResult);

    Set<String> ignorePatterns = new HashSet<>(IGNORE_PATTERNS);
    ignorePatterns.add("\"/id\"");
    ignorePatterns.add("\"/entry/[0-9]+/resource/created\"");

    AssertUtils.assertJsonEquals(expected, actual, ignorePatterns);
  }
}
