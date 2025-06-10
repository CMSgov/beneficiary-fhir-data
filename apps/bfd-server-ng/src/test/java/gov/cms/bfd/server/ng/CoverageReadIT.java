package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.rest.gclient.IReadTyped;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.r4.model.Coverage;
import org.junit.jupiter.api.Test;

public class CoverageReadIT extends IntegrationTestBase {

  private IReadTyped<Coverage> coverageRead() {
    return getFhirClient().read().resource(Coverage.class);
  }

  //  @Test
  //  void coverageReadValidCompositeId() {
  //    String validCoverageId = "part-a-1";
  //
  //    Coverage coverage = coverageRead().withId(validCoverageId).execute();
  //    assertNotNull(coverage, "Coverage resource should not be null for a valid ID");
  //    assertFalse(coverage.isEmpty(), "Coverage resource should not be empty for a valid ID");
  //  }

  //  @Test
  //  void coverageReadBeneExistsButPartOrVersionNotFound() {
  //    String idForNonExistentCoverage = "part-c-1";
  //
  //    assertThrows(
  //        ResourceNotFoundException.class,
  //        () -> coverageRead().withId(idForNonExistentCoverage).execute(),
  //        "Should throw ResourceNotFoundException for a validly formatted ID that doesn't map to a
  // resource.");
  //  }

  /**
   * Test reading a Coverage resource where the bene_sk part of the ID does not correspond to any
   * existing Beneficiary.
   */
  //  @Test
  //  void coverageReadBeneSkNotFound() {
  //    String nonExistentBeneSkId = "part-a-9999999"; // Assuming bene_sk 9999999 does not exist
  //
  //    assertThrows(
  //        ResourceNotFoundException.class,
  //        () -> coverageRead().withId(nonExistentBeneSkId).execute(),
  //        "Should throw ResourceNotFoundException if the beneficiary part of the ID does not
  // exist.");
  //  }

  //    /**
  //     * Test reading a Coverage resource with an ID that is syntactically invalid
  //     * according to the expected composite ID pattern (e.g., missing bene_sk, malformed part).
  //     * This should result in a 400 Bad Request.
  //     *
  //     * @param invalidId An invalid ID string.
  //     */
  //    @ParameterizedTest
  //    @ValueSource(strings = {
  //            "part-a",         // Missing bene_sk
  //            "parta",          // Missing bene_sk and hyphen (might be caught by part validation
  // if "parta" not a known prefix)
  //            "-12345",         // Missing part identifier
  //            "part-a-abc",     // Invalid bene_sk (not a number)
  //            "foo-12345",      // Invalid part prefix "foo"
  //            "part-e-12345"    // Invalid part "part-e" if not defined in CoveragePart enum
  //    })
  //    void coverageReadInvalidIdFormatBadRequest(String invalidId) {
  //        assertThrows(InvalidRequestException.class,
  //                () -> coverageRead().withId(invalidId).execute(),
  //                "Should throw InvalidRequestException for ID: " + invalidId);
  //    }

  /**
   * Test reading a Coverage resource with an ID that is syntactically valid (matches pattern) but
   * uses a CoveragePart that is not supported by the FFS endpoint (e.g., Part C or D, assuming your
   * FhirInputConverter or Handler throws InvalidRequestException for these).
   */
  @Test
  void coverageReadUnsupportedValidPartBadRequest() {
    String unsupportedPartId = "part-c-1";
    assertThrows(
        InvalidRequestException.class,
        () -> coverageRead().withId(unsupportedPartId).execute(),
        "Should throw InvalidRequestException for an unsupported part like Part C/D.");
  }

  //
  //    @ParameterizedTest
  //    @EmptySource // Provides ""
  //    // @ValueSource(strings = {" ", "  "}) // Can add if you want to test whitespace explicitly
  //    void coverageReadNoIdBadRequest(String id) {
  //        // If ID is part of the path like /Coverage/{id}
  //        // HAPI client .withId("") might send /Coverage/
  //        // If your server treats /Coverage/ as "search all" it might be 200.
  //        // If it expects an ID, it should be 400 or 404.
  //        // RestAssured helps send exactly /Coverage or /Coverage/
  //        RestAssured.get(getServerUrl() + "/Coverage/" + id) // Note the trailing slash for empty
  // ID
  //                .then()
  //                .assertThat()
  //                .statusCode(400); // Or 404, depending on server's strictness for empty ID in
  // path
  //    }

  //  @Test
  //  void coverageReadWhitespaceIdBadRequest() {
  //    String whitespaceId = "   ";
  //    assertThrows(
  //        InvalidRequestException.class,
  //        () -> coverageRead().withId(whitespaceId).execute(),
  //        "Should throw InvalidRequestException for whitespace ID.");
  //  }
}
