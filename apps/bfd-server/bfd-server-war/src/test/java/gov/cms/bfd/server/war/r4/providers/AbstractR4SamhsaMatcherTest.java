package gov.cms.bfd.server.war.r4.providers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.server.war.adapters.CodeableConcept;
import gov.cms.bfd.server.war.adapters.Coding;
import gov.cms.bfd.server.war.adapters.FhirResource;
import gov.cms.bfd.server.war.commons.CCWUtils;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class AbstractR4SamhsaMatcherTest {

  private final String name;
  private final List<String> systems;
  private final boolean expectedResult;
  private final String errorMessage;

  @Parameterized.Parameters(name = "{index}: {0}")
  public static Iterable<Object[]> parameters() {
    final String HCPCS = TransformerConstants.CODING_SYSTEM_HCPCS;
    final String OLDER_HCPCS = CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.HCPCS_CD);
    final String OTHER = "other system";
    return List.of(
        new Object[][] {
          {
            "Empty list",
            Collections.emptyList(),
            false,
            "should NOT return true (all known systems), but DID."
          },
          {
            "HCPCS only systems",
            List.of(HCPCS, HCPCS, HCPCS),
            true,
            "SHOULD return true (all known systems), but did NOT."
          },
          {
            "HCPCS and older HCPCS system",
            List.of(HCPCS, HCPCS, OLDER_HCPCS, OLDER_HCPCS),
            true,
            "SHOULD return true (all known systems), but did NOT."
          },
          {
            "Other system only",
            List.of(OTHER, OTHER),
            false,
            "should NOT return true (all known systems), but DID."
          },
          {
            "HCPCS and other system",
            List.of(HCPCS, HCPCS, OTHER),
            false,
            "should NOT return true (all known systems), but DID."
          },
          {
            "HCPCS, older HCPCS, and other system",
            List.of(HCPCS, HCPCS, OLDER_HCPCS, OLDER_HCPCS, OTHER),
            false,
            "should NOT return true (all known systems), but DID."
          },
        });
  }

  public AbstractR4SamhsaMatcherTest(
      String name, List<String> systems, boolean expectedResult, String errorMessage) {
    this.name = name;
    this.systems = systems;
    this.expectedResult = expectedResult;
    this.errorMessage = errorMessage;
  }

  @Test
  public void test() {
    // unchecked - This is ok for making a mock.
    //noinspection unchecked
    AbstractR4SamhsaMatcher<FhirResource> matcherSpy = spy(AbstractR4SamhsaMatcher.class);

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
        name + " " + errorMessage,
        expectedResult,
        matcherSpy.containsOnlyKnownSystems(mockConcept));
  }
}
