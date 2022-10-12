package gov.cms.bfd.server.war.r4.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.server.war.adapters.CodeableConcept;
import gov.cms.bfd.server.war.adapters.Coding;
import gov.cms.bfd.server.war.commons.CCWUtils;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class R4EobSamhsaMatcherTest {

  /**
   * Data method for the abstractR4SamhsaMatcherTest. Used automatically via the MethodSource
   * annotation.
   *
   * @return the data for the test
   */
  public static Stream<Arguments> abstractR4SamhsaMatcherTest() {
    final String HCPCS = TransformerConstants.CODING_SYSTEM_HCPCS;
    final String OLDER_HCPCS = CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.HCPCS_CD);
    final String OTHER = "other system";

    return Stream.of(
        arguments(
            "Empty list",
            Collections.emptyList(),
            false,
            "should NOT return true (all known systems), but DID."),
        arguments(
            "HCPCS only systems",
            List.of(HCPCS, HCPCS, HCPCS),
            true,
            "SHOULD return true (all known systems), but did NOT."),
        arguments(
            "HCPCS and older HCPCS system",
            List.of(HCPCS, HCPCS, OLDER_HCPCS, OLDER_HCPCS),
            true,
            "SHOULD return true (all known systems), but did NOT."),
        arguments(
            "Other system only",
            List.of(OTHER, OTHER),
            false,
            "should NOT return true (all known systems), but DID."),
        arguments(
            "HCPCS and other system",
            List.of(HCPCS, HCPCS, OTHER),
            false,
            "should NOT return true (all known systems), but DID."),
        arguments(
            "HCPCS, older HCPCS, and other system",
            List.of(HCPCS, HCPCS, OLDER_HCPCS, OLDER_HCPCS, OTHER),
            false,
            "should NOT return true (all known systems), but DID."));
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource
  public void abstractR4SamhsaMatcherTest(
      String name, List<String> systems, boolean expectedResult, String errorMessage) {
    R4EobSamhsaMatcher matcher = new R4EobSamhsaMatcher();

    CodeableConcept mockConcept = mock(CodeableConcept.class);

    List<Coding> codings =
        systems.stream()
            .map(
                system -> {
                  Coding mockCoding = mock(Coding.class);
                  doReturn(system).when(mockCoding).getSystem();
                  return mockCoding;
                })
            .collect(Collectors.toUnmodifiableList());

    doReturn(codings).when(mockConcept).getCoding();

    assertEquals(
        expectedResult, matcher.containsOnlyKnownSystems(mockConcept), name + " " + errorMessage);
  }
}
