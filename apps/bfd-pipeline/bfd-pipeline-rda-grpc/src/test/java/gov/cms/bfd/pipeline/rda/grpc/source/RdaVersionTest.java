package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests the functionality of the {@link RdaVersion} class. */
public class RdaVersionTest {

  /**
   * Supplies the arguments for the {@link #validVersionTests(String, boolean)} tests.
   *
   * @return The arguments for the associated parameterized tests.
   */
  public static Stream<Arguments> validVersionTests() {
    return Stream.of(
        Arguments.arguments("+0.0.1", false),
        Arguments.arguments("-0.0.1", false),
        Arguments.arguments("~0.0.1", true),
        Arguments.arguments(" 0.0.1", false),
        Arguments.arguments("+0.0.1", false),
        Arguments.arguments("^0.0.1", true),
        Arguments.arguments("~.0.1", false),
        Arguments.arguments("1", false),
        Arguments.arguments("1.0", false));
  }

  /**
   * Tests that version parsing works as expected, with invalid format version strings causing an
   * exception.
   *
   * @param version The version string to test.
   * @param isValid Denotes if the version string should be considered valid, thus determining if it
   *     will throw an exception.
   */
  @ParameterizedTest
  @MethodSource
  void validVersionTests(String version, boolean isValid) {
    Runnable logic = () -> RdaVersion.builder().versionString(version).build();

    if (isValid) {
      assertDoesNotThrow(logic::run);
    } else {
      assertThrows(IllegalArgumentException.class, logic::run);
    }
  }

  /**
   * Supplies the arguments for the {@link #versionCompatibilityTests(String, String, boolean)}
   * tests.
   *
   * @return The arguments for the associated parameterized tests.
   */
  public static Stream<Arguments> versionCompatibilityTests() {
    return Stream.of(
        Arguments.arguments("^0.0.1", "0.0.0", false), // Major match, patch too low
        Arguments.arguments("^0.0.1", "0.0.1", true), // Major match, patch same
        Arguments.arguments("^0.0.1", "0.0.2", true), // Major match, patch above
        Arguments.arguments("^0.0.1", "0.1.0", true), // Major match, minor above
        Arguments.arguments("^0.0.1", "1.0.0", false), // Major match, major too high
        Arguments.arguments("^0.1.0", "0.0.1", false), // Major match, minor too low
        Arguments.arguments("^0.1.0", "0.1.0", true), // Major match, minor same
        Arguments.arguments("^0.1.1", "0.1.0", false), // Major match, patch too low
        Arguments.arguments("^0.1.0", "0.1.1", true), // Major match, patch above
        Arguments.arguments("^0.1.0", "0.2.0", true), // Major match, minor above
        Arguments.arguments("^0.1.0", "1.0.0", false), // Major match, major too high
        Arguments.arguments("^1.0.0", "0.0.1", false), // Major match, major too low
        Arguments.arguments("^1.0.0", "0.1.0", false), // Major match, major too low
        Arguments.arguments("^1.0.0", "1.0.0", true), // Major match, major same
        Arguments.arguments("^1.0.0", "1.0.1", true), // Major match, patch above
        Arguments.arguments("^1.0.0", "1.1.0", true), // Major match, minor above
        Arguments.arguments("^1.0.1", "1.0.0", false), // Major match, patch too low
        Arguments.arguments("^1.1.0", "1.0.0", false), // Major match, minor too low
        Arguments.arguments("~0.0.1", "0.0.0", false), // Minor match, patch too low
        Arguments.arguments("~0.0.1", "0.0.1", true), // Minor match, patch same
        Arguments.arguments("~0.0.1", "0.0.2", true), // Minor match, patch above
        Arguments.arguments("~0.0.1", "0.1.0", false), // Minor match, patch too low
        Arguments.arguments("~0.0.1", "1.0.0", false), // Minor match, major too high
        Arguments.arguments("~0.1.0", "0.0.1", false), // Minor match, minor too low
        Arguments.arguments("~0.1.0", "0.1.0", true), // Minor match, minor same
        Arguments.arguments("~0.1.0", "0.1.1", true), // Minor match, patch above
        Arguments.arguments("~0.1.0", "0.2.0", false), // Minor match, minor too high
        Arguments.arguments("~0.1.0", "1.0.0", false), // Minor match, major too high
        Arguments.arguments("~1.0.0", "0.0.1", false), // Minor match, major too low
        Arguments.arguments("~1.0.0", "0.1.0", false), // Minor match, major too low
        Arguments.arguments("~1.0.0", "1.0.0", true), // Minor match, major same
        Arguments.arguments("~1.0.1", "0.0.0", false), // Minor match, major too low
        Arguments.arguments("~0.1.1", "0.1.0", false), // Minor match, patch too low
        Arguments.arguments("0.0.1", "0.0.1", true), // Patch match, same patch
        Arguments.arguments("0.0.1", "0.1.1", false), // Patch match, minor not same
        Arguments.arguments("0.0.1", "1.0.1", false)); // Patch match, major not same
  }

  /**
   * Tests that the version compatibility check {@link RdaVersion#allows(String)} works correctly.
   *
   * @param requiredVersion The required {@link RdaVersion}.
   * @param testedVersion The {@link RdaVersion} being checked.
   * @param expected If the checked {@link RdaVersion} is expected to match the required {@link
   *     RdaVersion}.
   */
  @ParameterizedTest
  @MethodSource
  void versionCompatibilityTests(String requiredVersion, String testedVersion, boolean expected) {
    RdaVersion rdaVersion = RdaVersion.builder().versionString(requiredVersion).build();

    assertEquals(expected, rdaVersion.allows(testedVersion));
  }
}
