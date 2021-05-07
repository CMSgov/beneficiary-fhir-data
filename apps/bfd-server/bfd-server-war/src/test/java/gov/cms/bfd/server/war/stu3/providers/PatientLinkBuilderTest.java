package gov.cms.bfd.server.war.stu3.providers;

import ca.uhn.fhir.rest.api.Constants;
import gov.cms.bfd.server.war.commons.PatientLinkBuilder;
import java.util.Collections;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

public class PatientLinkBuilderTest {
  public static String TEST_CONTRACT_URL =
      "https://localhost:443/v1/fhir/Patient?_has:Coverage.extension=https://bluebutton.cms.gov/resources/variables/ptdcntrct02|S0000";

  @Test
  public void noCountTest() {
    PatientLinkBuilder paging = new PatientLinkBuilder(TEST_CONTRACT_URL);

    Assert.assertFalse(paging.isPagingRequested());
    Assert.assertTrue(paging.isFirstPage());
    Assert.assertEquals(Integer.MAX_VALUE, paging.getPageSize());

    Bundle bundle = new Bundle();
    Assert.assertTrue(bundle.getLink().isEmpty());
    paging.addLinks(bundle);
    Assert.assertTrue(bundle.getLink().isEmpty());
  }

  @Test
  public void missingCountTest() {
    // Missing _count
    PatientLinkBuilder paging = new PatientLinkBuilder(TEST_CONTRACT_URL + "&cursor=999");

    Assert.assertFalse(paging.isPagingRequested());
    Assert.assertTrue(paging.isFirstPage());
    Assert.assertEquals(Integer.MAX_VALUE, paging.getPageSize());

    Bundle bundle = new Bundle();
    Assert.assertTrue(bundle.getLink().isEmpty());
    paging.addLinks(bundle);
    Assert.assertTrue(bundle.getLink().isEmpty());
  }

  @Test
  public void emptyPageTest() {
    PatientLinkBuilder paging = new PatientLinkBuilder(TEST_CONTRACT_URL + "&_count=10");

    Assert.assertTrue(paging.isPagingRequested());
    Assert.assertTrue(paging.isFirstPage());
    Assert.assertEquals(10, paging.getPageSize());

    Bundle bundle = new Bundle();
    Assert.assertTrue(bundle.getLink().isEmpty());
    paging.addLinks(bundle);
    Assert.assertNotNull(bundle.getLink(Constants.LINK_SELF));
    Assert.assertNotNull(bundle.getLink(Constants.LINK_FIRST));
  }

  @Test
  public void emptyCursorTest() {
    PatientLinkBuilder paging = new PatientLinkBuilder(TEST_CONTRACT_URL + "&_count=10&cursor=");

    Assert.assertTrue(paging.isPagingRequested());
    Assert.assertTrue(paging.isFirstPage());
    Assert.assertEquals(10, paging.getPageSize());
  }

  @Test
  public void onePageTest() {
    PatientLinkBuilder paging = new PatientLinkBuilder(TEST_CONTRACT_URL + "&_count=10");

    Assert.assertTrue(paging.isPagingRequested());
    Assert.assertTrue(paging.isFirstPage());
    Assert.assertEquals(10, paging.getPageSize());

    Bundle bundle = new Bundle();
    TransformerUtils.addResourcesToBundle(bundle, Collections.singletonList(new Patient()));
    Assert.assertTrue(bundle.getLink().isEmpty());
    paging.addLinks(bundle);
    Assert.assertNotNull(bundle.getLink(Constants.LINK_SELF));
    Assert.assertNotNull(bundle.getLink(Constants.LINK_FIRST));
    UriComponents firstLink =
        UriComponentsBuilder.fromUriString(bundle.getLink(Constants.LINK_FIRST).getUrl()).build();
    Assert.assertEquals("10", firstLink.getQueryParams().getFirst(Constants.PARAM_COUNT));
    Assert.assertNull(bundle.getLink(Constants.LINK_NEXT));
  }

  @Test
  public void fullPageTest() {
    // test a page with a page size of 1 and 1 patient in the result
    PatientLinkBuilder paging = new PatientLinkBuilder(TEST_CONTRACT_URL + "&_count=1");

    Assert.assertTrue(paging.isPagingRequested());
    Assert.assertTrue(paging.isFirstPage());
    Assert.assertEquals(1, paging.getPageSize());

    Bundle bundle = new Bundle();
    Patient patient = new Patient();
    patient.setId("1");
    TransformerUtils.addResourcesToBundle(bundle, Collections.singletonList(patient));
    Assert.assertTrue(bundle.getLink().isEmpty());
    paging.addLinks(bundle);

    Assert.assertNotNull(bundle.getLink(Constants.LINK_SELF));
    Assert.assertNotNull(bundle.getLink(Constants.LINK_FIRST));
    Assert.assertNull(bundle.getLink(Constants.LINK_NEXT));
  }

  @Test
  public void fullPageTestWithPaging() {
    // test a page with a page size of 1 and 1 patient in the result
    PatientLinkBuilder paging = new PatientLinkBuilder(TEST_CONTRACT_URL + "&_count=1");
    paging = new PatientLinkBuilder(paging, true);

    Assert.assertTrue(paging.isPagingRequested());
    Assert.assertTrue(paging.isFirstPage());
    Assert.assertEquals(1, paging.getPageSize());

    Bundle bundle = new Bundle();
    Patient patient = new Patient();
    patient.setId("1");
    TransformerUtils.addResourcesToBundle(bundle, Collections.singletonList(patient));
    Assert.assertTrue(bundle.getLink().isEmpty());
    paging.addLinks(bundle);

    Assert.assertNotNull(bundle.getLink(Constants.LINK_SELF));
    Assert.assertNotNull(bundle.getLink(Constants.LINK_FIRST));
    Assert.assertNotNull(bundle.getLink(Constants.LINK_NEXT));
    UriComponents nextLink =
        UriComponentsBuilder.fromUriString(bundle.getLink(Constants.LINK_NEXT).getUrl()).build();
    Assert.assertEquals("1", nextLink.getQueryParams().getFirst(Constants.PARAM_COUNT));
    Assert.assertEquals("1", nextLink.getQueryParams().getFirst(PatientLinkBuilder.PARAM_CURSOR));
  }
}
