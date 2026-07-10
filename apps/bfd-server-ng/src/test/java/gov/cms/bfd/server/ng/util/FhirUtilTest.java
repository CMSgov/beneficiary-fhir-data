package gov.cms.bfd.server.ng.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import gov.cms.bfd.server.ng.input.PageCursor;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.junit.jupiter.api.Test;

class FhirUtilTest {

  @Test
  void bundleOrDefaultWithOffset() {
    var requestDetails = mock(RequestDetails.class);
    when(requestDetails.getCompleteUrl())
        .thenReturn(
            "http://localhost:8080/fhir/ExplanationOfBenefit?patient=1&_count=10&_offset=20");

    var eob1 = new ExplanationOfBenefit();
    eob1.setId("1");
    var eob2 = new ExplanationOfBenefit();
    eob2.setId("2");

    var bundle =
        FhirUtil.bundleOrDefault(
            Stream.of(eob1, eob2),
            ZonedDateTime::now,
            Optional.of(requestDetails),
            Optional.of(1), // limit 1 to force hasMore = true since we have 2 resources
            Optional.of(20),
            false); // useCursor = false

    // We trimmed entries to limit 1
    assertEquals(1, bundle.getEntry().size());
    assertEquals("1", bundle.getEntry().get(0).getResource().getIdElement().getIdPart());

    var nextLink = bundle.getLink(Bundle.LINK_NEXT);
    assertNotNull(nextLink);
    assertTrue(nextLink.getUrl().contains("_offset=21")); // nextOffset = offset (20) + limit (1)
    assertTrue(nextLink.getUrl().contains("_count=1"));
  }

  @Test
  void bundleOrDefaultWithCursor() {
    var requestDetails = mock(RequestDetails.class);
    when(requestDetails.getCompleteUrl())
        .thenReturn("http://localhost:8080/fhir/ExplanationOfBenefit?patient=1&_count=1");

    var eob1 = new ExplanationOfBenefit();
    eob1.setId("998877");
    var eob2 = new ExplanationOfBenefit();
    eob2.setId("998878");

    var bundle =
        FhirUtil.bundleOrDefault(
            Stream.of(eob1, eob2),
            ZonedDateTime::now,
            Optional.of(requestDetails),
            Optional.of(1), // limit 1 to force hasMore = true since we have 2 resources
            Optional.of(0),
            true); // useCursor = true

    assertEquals(1, bundle.getEntry().size());
    assertEquals("998877", bundle.getEntry().get(0).getResource().getIdElement().getIdPart());

    var nextLink = bundle.getLink(Bundle.LINK_NEXT);
    assertNotNull(nextLink);
    assertTrue(nextLink.getUrl().contains("_cursor="));

    // Extract cursor value and decode it to verify it corresponds to eob-998877
    var url = nextLink.getUrl();
    var cursorVal = url.substring(url.indexOf("_cursor=") + 8);
    if (cursorVal.contains("&")) {
      cursorVal = cursorVal.substring(0, cursorVal.indexOf("&"));
    }

    var decodedCursor = PageCursor.parse(cursorVal);
    assertEquals(998877L, decodedCursor.lastClaimUniqueId());
  }
}
