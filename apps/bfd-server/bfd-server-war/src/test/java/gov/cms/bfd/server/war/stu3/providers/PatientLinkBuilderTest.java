package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.api.Constants;
import gov.cms.bfd.server.war.commons.PatientLinkBuilder;
import java.util.Collections;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

public class PatientLinkBuilderTest {
  public static String TEST_CONTRACT_URL =
      "https://localhost:443/v1/fhir/Patient?_has:Coverage.extension=https://bluebutton.cms.gov/resources/variables/ptdcntrct02|S0000";

  @Test
  public void noCountTest() {
    PatientLinkBuilder paging = new PatientLinkBuilder(TEST_CONTRACT_URL);

    assertFalse(paging.isPagingRequested());
    assertTrue(paging.isFirstPage());
    assertEquals(PatientLinkBuilder.MAX_PAGE_SIZE, paging.getPageSize());

    Bundle bundle = new Bundle();
    assertTrue(bundle.getLink().isEmpty());
    paging.addLinks(bundle);
    assertTrue(bundle.getLink().isEmpty());
  }

  @Test
  public void missingCountTest() {
    // Missing _count
    PatientLinkBuilder paging = new PatientLinkBuilder(TEST_CONTRACT_URL + "&cursor=999");

    assertFalse(paging.isPagingRequested());
    assertTrue(paging.isFirstPage());
    assertEquals(PatientLinkBuilder.MAX_PAGE_SIZE, paging.getPageSize());

    Bundle bundle = new Bundle();
    assertTrue(bundle.getLink().isEmpty());
    paging.addLinks(bundle);
    assertTrue(bundle.getLink().isEmpty());
  }

  @Test
  public void emptyPageTest() {
    PatientLinkBuilder paging = new PatientLinkBuilder(TEST_CONTRACT_URL + "&_count=10");

    assertTrue(paging.isPagingRequested());
    assertTrue(paging.isFirstPage());
    assertEquals(10, paging.getPageSize());

    Bundle bundle = new Bundle();
    assertTrue(bundle.getLink().isEmpty());
    paging.addLinks(bundle);
    assertNotNull(bundle.getLink(Constants.LINK_SELF));
    assertNotNull(bundle.getLink(Constants.LINK_FIRST));
  }

  @Test
  public void emptyCursorTest() {
    PatientLinkBuilder paging = new PatientLinkBuilder(TEST_CONTRACT_URL + "&_count=10&cursor=");

    assertTrue(paging.isPagingRequested());
    assertTrue(paging.isFirstPage());
    assertEquals(10, paging.getPageSize());
  }

  @Test
  public void onePageTest() {
    PatientLinkBuilder paging = new PatientLinkBuilder(TEST_CONTRACT_URL + "&_count=10");

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

  @Test
  public void testMdcLogsInAddResourcesToBundle() {
    PatientLinkBuilder paging = new PatientLinkBuilder(TEST_CONTRACT_URL + "&_count=10");

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

  @Test
  public void fullPageTest() {
    // test a page with a page size of 1 and 1 patient in the result
    PatientLinkBuilder paging = new PatientLinkBuilder(TEST_CONTRACT_URL + "&_count=1");

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

  @Test
  public void fullPageTestWithPaging() {
    // test a page with a page size of 1 and 1 patient in the result
    PatientLinkBuilder paging = new PatientLinkBuilder(TEST_CONTRACT_URL + "&_count=1");
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
}
