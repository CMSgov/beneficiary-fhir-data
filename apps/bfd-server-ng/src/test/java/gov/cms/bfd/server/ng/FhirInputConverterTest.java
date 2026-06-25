package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.NumberParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.bfd.server.ng.input.CoveragePart;
import gov.cms.bfd.server.ng.input.FhirInputConverter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FhirInputConverterTest extends IntegrationTestBase {

  // These scenarios may not be easy to reproduce in an integration test, but we should be sure our
  // input validation is resilient against null/empty inputs.
  @Test
  void testInputNullHandling() {
    assertEquals(Optional.empty(), FhirInputConverter.toIntOptional(null));
    assertEquals(Optional.empty(), FhirInputConverter.toIntOptional(new NumberParam()));

    assertEquals(List.of(), FhirInputConverter.parseTagParameter(null));
    assertEquals(List.of(), FhirInputConverter.parseTagParameter(new TokenAndListParam()));

    var emptyId = new IdType();
    var blankId = new IdType("");

    assertThrows(InvalidRequestException.class, () -> FhirInputConverter.toLong((IdType) null));
    assertThrows(InvalidRequestException.class, () -> FhirInputConverter.toLong((TokenParam) null));
    assertThrows(InvalidRequestException.class, () -> FhirInputConverter.toLong(emptyId));
    assertThrows(InvalidRequestException.class, () -> FhirInputConverter.toLong(blankId));

    var emptyReference = new ReferenceParam();

    assertThrows(InvalidRequestException.class, () -> FhirInputConverter.toLong(null, ""));
    assertThrows(
        InvalidRequestException.class, () -> FhirInputConverter.toLong(emptyReference, ""));

    assertThrows(
        InvalidRequestException.class, () -> FhirInputConverter.toCoverageCompositeId(null));
    assertThrows(
        InvalidRequestException.class, () -> FhirInputConverter.toCoverageCompositeId(emptyId));
    assertThrows(
        InvalidRequestException.class, () -> FhirInputConverter.toCoverageCompositeId(blankId));

    assertThrows(InvalidRequestException.class, () -> FhirInputConverter.toString(null, ""));
    var emptyToken = new TokenParam();
    assertThrows(InvalidRequestException.class, () -> FhirInputConverter.toString(emptyToken, ""));
  }

  @Test
  void testToLongList() {
    var param = new TokenAndListParam().addAnd(new TokenParam("1")).addAnd(new TokenParam("2"));
    assertEquals(List.of(1L, 2L), FhirInputConverter.toLongList(param));

    var paramOr =
        new TokenAndListParam()
            .addAnd(new TokenOrListParam().addOr(new TokenParam("3")).addOr(new TokenParam("4")));
    assertEquals(List.of(3L, 4L), FhirInputConverter.toLongList(paramOr));
  }

  @Test
  void testToLongListTooMany() {
    var param = new TokenAndListParam();
    for (int i = 0; i < 101; i++) {
      param.addAnd(new TokenParam(String.valueOf(i)));
    }
    var thrown =
        assertThrows(InvalidRequestException.class, () -> FhirInputConverter.toLongList(param));
    assertEquals("A maximum of 100 claim IDs may be requested at once.", thrown.getMessage());
  }

  static Stream<Arguments> provideBooleanHeaderScenarios() {
    return Stream.of(
        Arguments.of(List.of(), Optional.empty()),
        Arguments.of(List.of("true"), Optional.of(true)),
        Arguments.of(List.of("false"), Optional.of(false)),
        Arguments.of(List.of("TRUE"), Optional.of(true)),
        Arguments.of(List.of("FALSE"), Optional.of(false)),
        Arguments.of(List.of("yes"), Optional.of(false)));
  }

  @ParameterizedTest
  @MethodSource("provideBooleanHeaderScenarios")
  void parseBooleanHeader(List<String> headerValues, Optional<Boolean> expected) {
    var requestDetails = mock(RequestDetails.class);
    when(requestDetails.getHeaders(INCLUDE_TAX_NUMBERS)).thenReturn(headerValues);

    assertEquals(
        expected, FhirInputConverter.parseBooleanHeader(requestDetails, INCLUDE_TAX_NUMBERS));
  }

  public static Stream<Arguments> provideOutcomeParameterScenarios() {
    var completeOrPartial =
        new TokenOrListParam()
            .addOr(new TokenParam(OUTCOME_COMPLETE))
            .addOr(new TokenParam(OUTCOME_PARTIAL));

    return Stream.of(
        Arguments.of(
            "complete",
            new TokenAndListParam().addAnd(new TokenParam(OUTCOME_COMPLETE)),
            List.of(List.of(ExplanationOfBenefit.RemittanceOutcome.COMPLETE))),
        Arguments.of(
            "partial",
            new TokenAndListParam().addAnd(new TokenParam(OUTCOME_PARTIAL)),
            List.of(List.of(ExplanationOfBenefit.RemittanceOutcome.PARTIAL))),
        Arguments.of(
            "queued",
            new TokenAndListParam().addAnd(new TokenParam(OUTCOME_QUEUED)),
            List.of(List.of(ExplanationOfBenefit.RemittanceOutcome.QUEUED))),
        Arguments.of(
            "error",
            new TokenAndListParam().addAnd(new TokenParam(OUTCOME_ERROR)),
            List.of(List.of(ExplanationOfBenefit.RemittanceOutcome.ERROR))),
        Arguments.of(
            "case insensitive",
            new TokenAndListParam().addAnd(new TokenParam(OUTCOME_PARTIAL_CASE_INSENSITIVE)),
            List.of(List.of(ExplanationOfBenefit.RemittanceOutcome.PARTIAL))),
        Arguments.of(
            "complete OR partial",
            new TokenAndListParam().addAnd(completeOrPartial),
            List.of(
                List.of(
                    ExplanationOfBenefit.RemittanceOutcome.COMPLETE,
                    ExplanationOfBenefit.RemittanceOutcome.PARTIAL))),
        Arguments.of(
            "complete AND partial",
            new TokenAndListParam()
                .addAnd(new TokenParam(OUTCOME_COMPLETE))
                .addAnd(new TokenParam(OUTCOME_PARTIAL)),
            List.of(
                List.of(ExplanationOfBenefit.RemittanceOutcome.COMPLETE),
                List.of(ExplanationOfBenefit.RemittanceOutcome.PARTIAL))));
  }

  @ParameterizedTest
  @MethodSource("provideOutcomeParameterScenarios")
  void parseOutcomeParameter(
      String scenario,
      TokenAndListParam outcomeParam,
      List<List<ExplanationOfBenefit.RemittanceOutcome>> expectedOutcomes) {
    assertEquals(
        expectedOutcomes, FhirInputConverter.parseOutcomeParameter(outcomeParam), scenario);
  }

  @Test
  void parseBooleanHeaderThrowsWhenHeaderRepeated() {
    var requestDetails = mock(RequestDetails.class);
    when(requestDetails.getHeaders(INCLUDE_TAX_NUMBERS)).thenReturn(List.of("true", "false"));

    var thrown =
        assertThrows(
            InvalidRequestException.class,
            () -> FhirInputConverter.parseBooleanHeader(requestDetails, INCLUDE_TAX_NUMBERS));

    assertEquals("Multiple values supplied for header: IncludeTaxNumbers", thrown.getMessage());
  }

  @Test
  void parseFromQueryParam_validScenarios() {
    assertEquals(
        CoveragePart.PART_A, FhirInputConverter.parseCoverageClassPart("part-a").orElse(null));
    assertEquals(
        CoveragePart.PART_B, FhirInputConverter.parseCoverageClassPart("part-b").orElse(null));
    assertEquals(
        CoveragePart.PART_C, FhirInputConverter.parseCoverageClassPart("part-c").orElse(null));
    assertEquals(
        CoveragePart.PART_D, FhirInputConverter.parseCoverageClassPart("part-d").orElse(null));
    assertEquals(CoveragePart.DUAL, FhirInputConverter.parseCoverageClassPart("dual").orElse(null));

    assertEquals(
        CoveragePart.PART_A, FhirInputConverter.parseCoverageClassPart("Part A").orElse(null));
    assertEquals(
        CoveragePart.PART_B, FhirInputConverter.parseCoverageClassPart("PART_B").orElse(null));
    assertEquals(
        CoveragePart.PART_C, FhirInputConverter.parseCoverageClassPart(" part-c ").orElse(null));
    assertEquals(
        CoveragePart.PART_D, FhirInputConverter.parseCoverageClassPart("Part_D").orElse(null));
  }

  @Test
  void parseFromQueryParam_invalidScenarios() {
    assertTrue(FhirInputConverter.parseCoverageClassPart(null).isEmpty());
    assertTrue(FhirInputConverter.parseCoverageClassPart("").isEmpty());
    assertTrue(FhirInputConverter.parseCoverageClassPart("   ").isEmpty());
    assertTrue(FhirInputConverter.parseCoverageClassPart("part-e").isEmpty());
    assertTrue(FhirInputConverter.parseCoverageClassPart("medicare").isEmpty());
    assertTrue(FhirInputConverter.parseCoverageClassPart("part/a").isEmpty());
  }
}
