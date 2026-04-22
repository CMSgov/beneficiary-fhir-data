package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.rest.param.NumberParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.bfd.server.ng.input.FhirInputConverter;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.Test;

class FhirInputConverterTest {

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
}
