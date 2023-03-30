package gov.cms.bfd.server.war.r4.providers.pac;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.NumberParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import gov.cms.bfd.server.war.ServerRequiredTest;
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

/** R4ClaimResourceProviderIT. */
public class R4ClaimResourceProviderIT extends ServerRequiredTest {

  /** Test utils for the test. */
  private static final RDATestUtils testUtils = new RDATestUtils();

  /** An ignore pattern for testing. */
  private static final Set<String> IGNORE_PATTERNS =
      Set.of("\"/link/[0-9]+/url\"", "\"/created\"", "\"/meta/lastUpdated\"");

  /** Sets the test up. */
  @BeforeAll
  public static void init() {
    testUtils.init();
    testUtils.seedData(false);
  }

  /** Cleans up the tests. */
  @AfterAll
  public static void tearDown() {
    testUtils.truncateTables();
    testUtils.destroy();
  }

  /**
   * Tests to see if the correct response is given when a FISS {@link Claim} is looked up by a
   * specific ID.
   */
  @Test
  public void shouldGetCorrectFissClaimResourceById() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    Claim claimResult = fhirClient.read().resource(Claim.class).withId("f-123456").execute();

    String expected = testUtils.expectedResponseFor("claimFissRead");
    String actual = FhirContext.forR4().newJsonParser().encodeResourceToString(claimResult);

    AssertUtils.assertJsonEquals(expected, actual, IGNORE_PATTERNS);
  }

  /**
   * Tests to see if the correct response is given when a FISS {@link Claim} is looked up by a
   * specific ID with tax numbers included.
   */
  @Test
  public void shouldGetCorrectFissClaimResourceByIdWithTaxNumbers() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    Claim claimResult =
        fhirClient
            .read()
            .resource(Claim.class)
            .withId("f-123456")
            .withAdditionalHeader("IncludeTaxNumbers", "true")
            .execute();

    String expected = testUtils.expectedResponseFor("claimFissReadWithTaxNumbers");
    String actual = FhirContext.forR4().newJsonParser().encodeResourceToString(claimResult);

    AssertUtils.assertJsonEquals(expected, actual, IGNORE_PATTERNS);
  }

  /**
   * Tests to see if the correct response is given when an MCS {@link Claim} is looked up by a
   * specific ID.
   */
  @Test
  public void shouldGetCorrectMcsClaimResourceById() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    Claim claimResult = fhirClient.read().resource(Claim.class).withId("m-654321").execute();

    String expected = testUtils.expectedResponseFor("claimMcsRead");
    String actual = FhirContext.forR4().newJsonParser().encodeResourceToString(claimResult);

    AssertUtils.assertJsonEquals(expected, actual, IGNORE_PATTERNS);
  }

  /**
   * Tests to see if the correct response is given when an MCS {@link Claim} is looked up by a
   * specific ID with tax numbers included.
   */
  @Test
  public void shouldGetCorrectMcsClaimResourceByIdWithTaxNumbers() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    Claim claimResult =
        fhirClient
            .read()
            .resource(Claim.class)
            .withId("m-654321")
            .withAdditionalHeader("IncludeTaxNumbers", "true")
            .execute();

    String expected = testUtils.expectedResponseFor("claimMcsReadWithTaxNumbers");
    String actual = FhirContext.forR4().newJsonParser().encodeResourceToString(claimResult);

    AssertUtils.assertJsonEquals(expected, actual, IGNORE_PATTERNS);
  }

  /**
   * Tests to see if the correct response is given when a search is done for {@link Claim}s using
   * given mbi and service-date range. In this test case the query finds the matched claims because
   * their to dates are within the date range even though their from dates are not.
   */
  @Test
  public void shouldGetCorrectClaimResourcesByMbiHash() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    Bundle claimResult =
        fhirClient
            .search()
            .forResource(Claim.class)
            .where(
                Map.of(
                    "mbi",
                    List.of(new ReferenceParam(RDATestUtils.MBI_HASH)),
                    "service-date",
                    List.of(new DateParam("gt1970-07-18"), new DateParam("lt1970-07-25"))))
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

  /**
   * Tests to see if the correct response is given when a search is done for {@link Claim}s using
   * given mbi and service-date range with tax numbers included. In this test case the query finds
   * the matched claims because their to dates are within the date range even though their from
   * dates are not.
   */
  @Test
  public void shouldGetCorrectClaimResourcesByMbiHashWithTaxNumbers() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    Bundle claimResult =
        fhirClient
            .search()
            .forResource(Claim.class)
            .where(
                Map.of(
                    "mbi",
                    List.of(new ReferenceParam(RDATestUtils.MBI_HASH)),
                    "service-date",
                    List.of(new DateParam("gt1970-07-18"), new DateParam("lt1970-07-25"))))
            .returnBundle(Bundle.class)
            .withAdditionalHeader("IncludeTaxNumbers", "true")
            .execute();

    // Sort entries for consistent testing results
    claimResult.getEntry().sort(Comparator.comparing(a -> a.getResource().getId()));

    String expected = testUtils.expectedResponseFor("claimSearchWithTaxNumbers");
    String actual = FhirContext.forR4().newJsonParser().encodeResourceToString(claimResult);

    Set<String> ignorePatterns = new HashSet<>(IGNORE_PATTERNS);
    ignorePatterns.add("\"/id\"");
    ignorePatterns.add("\"/entry/[0-9]+/resource/created\"");

    AssertUtils.assertJsonEquals(expected, actual, ignorePatterns);
  }

  /**
   * Tests to see if the correct paginated response is given when a search is done for {@link
   * Claim}s using given mbi and service-date range. In this test case the query finds the matched
   * claims because their from dates are within the date range even though their to dates are not.
   */
  @Test
  public void shouldGetCorrectClaimResourcesByMbiHashWithPagination() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    Bundle claimResult =
        fhirClient
            .search()
            .forResource(Claim.class)
            .where(
                Map.of(
                    "mbi",
                    List.of(new ReferenceParam(RDATestUtils.MBI_HASH)),
                    "service-date",
                    List.of(new DateParam("ge1970-07-10"), new DateParam("le1970-07-18")),
                    "_count",
                    List.of(new NumberParam("5")),
                    "startIndex",
                    List.of(new NumberParam("1"))))
            .returnBundle(Bundle.class)
            .execute();

    // Sort entries for consistent testing results
    claimResult.getEntry().sort(Comparator.comparing(a -> a.getResource().getId()));

    String expected = testUtils.expectedResponseFor("claimSearchPaginated");
    String actual = FhirContext.forR4().newJsonParser().encodeResourceToString(claimResult);

    Set<String> ignorePatterns = new HashSet<>(IGNORE_PATTERNS);
    ignorePatterns.add("\"/id\"");
    ignorePatterns.add("\"/entry/[0-9]+/resource/created\"");

    AssertUtils.assertJsonEquals(expected, actual, ignorePatterns);
  }
}
