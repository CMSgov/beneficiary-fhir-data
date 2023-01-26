package gov.cms.bfd.server.war.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests the functionality of the {@link AbstractResourceProvider} class. */
public class AbstractResourceProviderTest {

  /**
   * The parameters for the {@link #taxHeaderTests(String, List, boolean, boolean)} parameterized
   * test.
   *
   * @return The parameters for the {@link #taxHeaderTests(String, List, boolean, boolean)}
   *     parameterized test.
   */
  public static Stream<Arguments> taxHeaderTests() {
    return Stream.of(
        Arguments.arguments("Tax header missing", null, false, false),
        Arguments.arguments("Tax header empty", Collections.emptyList(), false, false),
        Arguments.arguments("Tax header false", List.of("fAlse"), false, false),
        Arguments.arguments("Tax header true", List.of("tRue"), true, false),
        Arguments.arguments("Tax header invalid", List.of("banana"), false, true),
        Arguments.arguments("Tax header too many values", List.of("true", "true"), false, true));
  }

  /**
   * Tests that the {@link AbstractResourceProvider#returnIncludeTaxNumbers(RequestDetails)} method
   * correctly returns true or false to indicate if the request asked for the tax numbers to be
   * included.
   *
   * @param testName The name for the test.
   * @param headerValue The value to return from the {@link RequestDetails} for the
   *     IncludeTaxNumbers header.
   * @param expected The expected result of the tested method.
   * @param shouldThrow Indicates if the method was expected to throw for this test iteration.
   */
  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource
  void taxHeaderTests(
      String testName, List<String> headerValue, boolean expected, boolean shouldThrow) {
    AbstractResourceProvider provider = new AbstractResourceProvider();

    RequestDetails mockDetails = mock(RequestDetails.class);

    doReturn(headerValue).when(mockDetails).getHeaders("IncludeTaxNumbers");

    boolean actual;

    try {
      actual = provider.returnIncludeTaxNumbers(mockDetails);

      if (shouldThrow) {
        fail("Did not throw expected exception");
      }

      assertEquals(expected, actual);
    } catch (Exception e) {
      if (!shouldThrow) {
        fail("Unexpected exception thrown", e);
      }
    }
  }
}
