package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.bfd.server.war.commons.PatientLinkBuilder;
import java.util.Collections;
import java.util.HashMap;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/** Unit test for the {@link PatientLinkBuilder}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PatientLinkBuilderTest {
  /** Contract url for use in pagination testing. */
  public static final String TEST_CONTRACT_URL =
      "https://localhost:443/v1/fhir/Patient?_has:Coverage.extension=https://bluebutton.cms.gov/resources/variables/ptdcntrct02|S0000";

  @Mock private RequestDetails requestDetails;

  /**
   * Validate that for the base testing contract url no paging was requested, no paging was
   * returned, we start on the 'first' page, and the page size is set to the maximum value (since
   * none was requested).
   */
  @Test
  public void noCountTest() {
    PatientLinkBuilder paging = configureRequestDetails(TEST_CONTRACT_URL);

    assertFalse(paging.isPagingRequested());
    assertTrue(paging.isFirstPage());
    assertEquals(PatientLinkBuilder.MAX_PAGE_SIZE, paging.getPageSize());

    Bundle bundle = new Bundle();
    assertTrue(bundle.getLink().isEmpty());
    paging.addLinks(bundle);
    assertTrue(bundle.getLink().isEmpty());
  }

  /**
   * Validate that for the base testing contract url with a page cursor set to 999, no paging was
   * returned, we start on the 'first' page, and the page size is set to the maximum value (since
   * none was requested). This tests that when the cursor is set but count (which is required) is
   * not requested it does not count as a paging request.
   */
  @Test
  public void missingCountTest() {
    // Missing _count
    PatientLinkBuilder paging = configureRequestDetails(TEST_CONTRACT_URL + "&cursor=999");

    assertFalse(paging.isPagingRequested());
    assertTrue(paging.isFirstPage());
    assertEquals(PatientLinkBuilder.MAX_PAGE_SIZE, paging.getPageSize());

    Bundle bundle = new Bundle();
    assertTrue(bundle.getLink().isEmpty());
    paging.addLinks(bundle);
    assertTrue(bundle.getLink().isEmpty());
  }

  /**
   * Validate that for the base testing contract url with count set, paging is returned, we start on
   * the 'first' page (since no cursor is requested), and the page size is set to the input value.
   */
  @Test
  public void emptyPageTest() {
    PatientLinkBuilder paging = configureRequestDetails(TEST_CONTRACT_URL + "&_count=10");

    assertTrue(paging.isPagingRequested());
    assertTrue(paging.isFirstPage());
    assertEquals(10, paging.getPageSize());

    Bundle bundle = new Bundle();
    assertTrue(bundle.getLink().isEmpty());
    paging.addLinks(bundle);
    assertNotNull(bundle.getLink(Constants.LINK_SELF));
    assertNotNull(bundle.getLink(Constants.LINK_FIRST));
  }

  /**
   * Validate that for the base testing contract url with count set and an empty cursor, we start on
   * the 'first' page (since no cursor is requested), and the page size is set to the input value.
   */
  @Test
  public void emptyCursorTest() {
    PatientLinkBuilder paging = configureRequestDetails(TEST_CONTRACT_URL + "&_count=10&cursor=");

    assertTrue(paging.isPagingRequested());
    assertTrue(paging.isFirstPage());
    assertEquals(10, paging.getPageSize());
  }

  /**
   * Validate that when the base testing contract url has count set, the returned paging url has the
   * self/first/next links populated as expected and the first link retains the count value from the
   * original request.
   */
  @Test
  public void onePageTest() {
    PatientLinkBuilder paging = configureRequestDetails(TEST_CONTRACT_URL + "&_count=10");

    assertTrue(paging.isPagingRequested());
    assertTrue(paging.isFirstPage());
    assertEquals(10, paging.getPageSize());

    Bundle bundle = new Bundle();
    TransformerUtils.addResourcesToBundle(bundle, Collections.singletonList(new Patient()));
    assertTrue(bundle.getLink().isEmpty());
    paging.addLinks(bundle);
    assertNotNull(bundle.getLink(Constants.LINK_SELF));
    assertNotNull(bundle.getLink(Constants.LINK_FIRST));
    UriComponents firstLink =
        UriComponentsBuilder.fromUriString(bundle.getLink(Constants.LINK_FIRST).getUrl()).build();
    assertEquals("10", firstLink.getQueryParams().getFirst(Constants.PARAM_COUNT));
    assertNull(bundle.getLink(Constants.LINK_NEXT));
  }

  /**
   * Validate that when the count is set that the paging links work as expected for a patient by ID
   * bundle, bene by patient/id bundle, and patient by patient/id bundle.
   */
  @Test
  public void testMdcLogsInAddResourcesToBundle() {
    PatientLinkBuilder paging = configureRequestDetails(TEST_CONTRACT_URL + "&_count=10");

    assertTrue(paging.isPagingRequested());
    assertTrue(paging.isFirstPage());
    assertEquals(10, paging.getPageSize());

    Bundle bundle = new Bundle();
    TransformerUtils.addResourcesToBundle(
        bundle, Collections.singletonList(new Patient().setId("Id")));
    assertTrue(bundle.getLink().isEmpty());
    paging.addLinks(bundle);
    assertNotNull(bundle.getLink(Constants.LINK_SELF));
    assertNotNull(bundle.getLink(Constants.LINK_FIRST));
    UriComponents firstLink =
        UriComponentsBuilder.fromUriString(bundle.getLink(Constants.LINK_FIRST).getUrl()).build();
    assertEquals("10", firstLink.getQueryParams().getFirst(Constants.PARAM_COUNT));
    assertNull(bundle.getLink(Constants.LINK_NEXT));

    bundle = new Bundle();
    TransformerUtils.addResourcesToBundle(
        bundle,
        Collections.singletonList(new Coverage().setBeneficiary(new Reference("Patient/Id"))));
    assertTrue(bundle.getLink().isEmpty());
    paging.addLinks(bundle);
    assertNotNull(bundle.getLink(Constants.LINK_SELF));
    assertNotNull(bundle.getLink(Constants.LINK_FIRST));
    firstLink =
        UriComponentsBuilder.fromUriString(bundle.getLink(Constants.LINK_FIRST).getUrl()).build();
    assertEquals("10", firstLink.getQueryParams().getFirst(Constants.PARAM_COUNT));
    assertNull(bundle.getLink(Constants.LINK_NEXT));

    bundle = new Bundle();
    TransformerUtils.addResourcesToBundle(
        bundle,
        Collections.singletonList(
            new ExplanationOfBenefit().setPatient(new Reference("Patient/Id"))));
    assertTrue(bundle.getLink().isEmpty());
    paging.addLinks(bundle);
    assertNotNull(bundle.getLink(Constants.LINK_SELF));
    assertNotNull(bundle.getLink(Constants.LINK_FIRST));
    firstLink =
        UriComponentsBuilder.fromUriString(bundle.getLink(Constants.LINK_FIRST).getUrl()).build();
    assertEquals("10", firstLink.getQueryParams().getFirst(Constants.PARAM_COUNT));
    assertNull(bundle.getLink(Constants.LINK_NEXT));
  }

  /**
   * Verifies the case where a count of 1 is requested in the paging and there is only 1 patient in
   * the result correctly returns the result and contains the self/first page links but no next
   * link.
   */
  @Test
  public void fullPageTest() {
    // test a page with a page size of 1 and 1 patient in the result
    PatientLinkBuilder paging = configureRequestDetails(TEST_CONTRACT_URL + "&_count=1");

    assertTrue(paging.isPagingRequested());
    assertTrue(paging.isFirstPage());
    assertEquals(1, paging.getPageSize());

    Bundle bundle = new Bundle();
    Patient patient = new Patient();
    patient.setId("1");
    TransformerUtils.addResourcesToBundle(bundle, Collections.singletonList(patient));
    assertTrue(bundle.getLink().isEmpty());
    paging.addLinks(bundle);

    assertNotNull(bundle.getLink(Constants.LINK_SELF));
    assertNotNull(bundle.getLink(Constants.LINK_FIRST));
    assertNull(bundle.getLink(Constants.LINK_NEXT));
  }

  /**
   * Verifies the case where a count of 1 is requested in the paging and there is more than 1
   * patient in the result correctly returns the result and contains the self/first/next links
   * populated with the correct url, cursor, and count.
   */
  @Test
  public void fullPageTestWithPaging() {
    // test a page with a page size of 1 and 1 patient in the result
    PatientLinkBuilder paging = configureRequestDetails(TEST_CONTRACT_URL + "&_count=1");
    paging = new PatientLinkBuilder(paging, true);

    assertTrue(paging.isPagingRequested());
    assertTrue(paging.isFirstPage());
    assertEquals(1, paging.getPageSize());

    Bundle bundle = new Bundle();
    Patient patient = new Patient();
    patient.setId("1");
    TransformerUtils.addResourcesToBundle(bundle, Collections.singletonList(patient));
    assertTrue(bundle.getLink().isEmpty());
    paging.addLinks(bundle);

    assertNotNull(bundle.getLink(Constants.LINK_SELF));
    assertNotNull(bundle.getLink(Constants.LINK_FIRST));
    assertNotNull(bundle.getLink(Constants.LINK_NEXT));
    UriComponents nextLink =
        UriComponentsBuilder.fromUriString(bundle.getLink(Constants.LINK_NEXT).getUrl()).build();
    assertEquals("1", nextLink.getQueryParams().getFirst(Constants.PARAM_COUNT));
    assertEquals("1", nextLink.getQueryParams().getFirst(PatientLinkBuilder.PARAM_CURSOR));
  }

  /**
   * Tests that when the page size is not a number, an {@link InvalidRequestException} is thrown.
   */
  @Test
  public void testNonNumberPageSizeExpectException() {
    assertThrows(
        InvalidRequestException.class,
        () -> {
          configureRequestDetails(TEST_CONTRACT_URL + "&_count=abc");
        },
        "Invalid argument in request URL: _count must be a number.");
  }

  /** Tests that when the page size is negative, an {@link InvalidRequestException} is thrown. */
  @Test
  public void testNegativePageSizeExpectException() {
    assertThrows(
        InvalidRequestException.class,
        () -> {
          configureRequestDetails(TEST_CONTRACT_URL + "&_count=-1");
        },
        "Value for pageSize cannot be zero or negative: -1");
  }

  /** Tests that when the page size is zero, an {@link InvalidRequestException} is thrown. */
  @Test
  public void testZeroPageSizeExpectException() {
    assertThrows(
        InvalidRequestException.class,
        () -> {
          configureRequestDetails(TEST_CONTRACT_URL + "&_count=0");
        },
        "Value for pageSize cannot be zero or negative: 0");
  }

  /** Tests that when the page size is too large, an {@link InvalidRequestException} is thrown. */
  @Test
  public void testOverlyLargePageSizeException() {
    assertThrows(
        InvalidRequestException.class,
        () -> {
          configureRequestDetails(
              TEST_CONTRACT_URL + "&_count=" + (PatientLinkBuilder.MAX_PAGE_SIZE + 1));
        },
        "Page size must be less than " + PatientLinkBuilder.MAX_PAGE_SIZE);
  }

  /**
   * Returns a new {@link PatientLinkBuilder} configured from the supplied URI.
   *
   * @param uri URI
   * @return link builder
   */
  private PatientLinkBuilder configureRequestDetails(String uri) {
    UriComponents components = UriComponentsBuilder.fromUriString(uri).build();
    when(requestDetails.getCompleteUrl()).thenReturn(components.toUriString());
    HashMap<String, String[]> params = new HashMap<>();
    for (var param : components.getQueryParams().keySet()) {
      params.put(param, components.getQueryParams().get(param).toArray(new String[0]));
    }
    when(requestDetails.getParameters()).thenReturn(params);
    return new PatientLinkBuilder(requestDetails);
  }
}
