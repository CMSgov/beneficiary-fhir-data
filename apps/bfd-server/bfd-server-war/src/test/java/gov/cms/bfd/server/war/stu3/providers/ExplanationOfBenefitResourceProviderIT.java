package gov.cms.bfd.server.war.stu3.providers;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.BeneficiaryHistory;
import gov.cms.bfd.model.rif.CarrierClaim;
import gov.cms.bfd.model.rif.DMEClaim;
import gov.cms.bfd.model.rif.HHAClaim;
import gov.cms.bfd.model.rif.HospiceClaim;
import gov.cms.bfd.model.rif.InpatientClaim;
import gov.cms.bfd.model.rif.MedicareBeneficiaryIdHistory;
import gov.cms.bfd.model.rif.OutpatientClaim;
import gov.cms.bfd.model.rif.PartDEvent;
import gov.cms.bfd.model.rif.SNFClaim;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.pipeline.rif.load.LoadAppOptions;
import gov.cms.bfd.pipeline.rif.load.RifLoaderTestUtils;
import gov.cms.bfd.server.war.ServerTestUtils;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration tests for {@link
 * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider}.
 */
public final class ExplanationOfBenefitResourceProviderIT {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ExplanationOfBenefitResourceProviderIT.class);

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for a {@link CarrierClaim}-derived {@link ExplanationOfBenefit} that does
   * exist in the DB.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void readEobForExistingCarrierClaim() throws FHIRException {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    CarrierClaim claim =
        loadedRecords.stream()
            .filter(r -> r instanceof CarrierClaim)
            .map(r -> (CarrierClaim) r)
            .findFirst()
            .get();
    ExplanationOfBenefit eob =
        fhirClient
            .read()
            .resource(ExplanationOfBenefit.class)
            .withId(TransformerUtils.buildEobId(ClaimType.CARRIER, claim.getClaimId()))
            .execute();

    Assert.assertNotNull(eob);
    CarrierClaimTransformerTest.assertMatches(claim, eob);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for a {@link CarrierClaim}-derived {@link ExplanationOfBenefit} that does not
   * exist in the DB.
   */
  @Test(expected = ResourceNotFoundException.class)
  public void readEobForMissingCarrierClaim() {
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    // No data is loaded, so this should return nothing.
    fhirClient
        .read()
        .resource(ExplanationOfBenefit.class)
        .withId(TransformerUtils.buildEobId(ClaimType.CARRIER, "1234"))
        .execute();
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for a {@link DMEClaim}-derived {@link ExplanationOfBenefit} that does exist
   * in the DB.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void readEobForExistingDMEClaim() throws FHIRException {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    DMEClaim claim =
        loadedRecords.stream()
            .filter(r -> r instanceof DMEClaim)
            .map(r -> (DMEClaim) r)
            .findFirst()
            .get();
    ExplanationOfBenefit eob =
        fhirClient
            .read()
            .resource(ExplanationOfBenefit.class)
            .withId(TransformerUtils.buildEobId(ClaimType.DME, claim.getClaimId()))
            .execute();

    Assert.assertNotNull(eob);
    DMEClaimTransformerTest.assertMatches(claim, eob);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for a {@link DMEClaim}-derived {@link ExplanationOfBenefit} that does not
   * exist in the DB.
   */
  @Test(expected = ResourceNotFoundException.class)
  public void readEobForMissingDMEClaim() {
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    // No data is loaded, so this should return nothing.
    fhirClient
        .read()
        .resource(ExplanationOfBenefit.class)
        .withId(TransformerUtils.buildEobId(ClaimType.DME, "1234"))
        .execute();
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for an {@link HHAClaim}-derived {@link ExplanationOfBenefit} that does exist
   * in the DB.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void readEobForExistingHHAClaim() throws FHIRException {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    HHAClaim claim =
        loadedRecords.stream()
            .filter(r -> r instanceof HHAClaim)
            .map(r -> (HHAClaim) r)
            .findFirst()
            .get();
    ExplanationOfBenefit eob =
        fhirClient
            .read()
            .resource(ExplanationOfBenefit.class)
            .withId(TransformerUtils.buildEobId(ClaimType.HHA, claim.getClaimId()))
            .execute();

    Assert.assertNotNull(eob);
    HHAClaimTransformerTest.assertMatches(claim, eob);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for an {@link HHAClaim}-derived {@link ExplanationOfBenefit} that does not
   * exist in the DB.
   */
  @Test(expected = ResourceNotFoundException.class)
  public void readEobForMissingHHAClaim() {
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    // No data is loaded, so this should return nothing.
    fhirClient
        .read()
        .resource(ExplanationOfBenefit.class)
        .withId(TransformerUtils.buildEobId(ClaimType.HHA, "1234"))
        .execute();
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for a {@link HospiceClaim}-derived {@link ExplanationOfBenefit} that does
   * exist in the DB.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void readEobForExistingHospiceClaim() throws FHIRException {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    HospiceClaim claim =
        loadedRecords.stream()
            .filter(r -> r instanceof HospiceClaim)
            .map(r -> (HospiceClaim) r)
            .findFirst()
            .get();
    ExplanationOfBenefit eob =
        fhirClient
            .read()
            .resource(ExplanationOfBenefit.class)
            .withId(TransformerUtils.buildEobId(ClaimType.HOSPICE, claim.getClaimId()))
            .execute();

    Assert.assertNotNull(eob);
    HospiceClaimTransformerTest.assertMatches(claim, eob);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for a {@link HospiceClaim}-derived {@link ExplanationOfBenefit} that does not
   * exist in the DB.
   */
  @Test(expected = ResourceNotFoundException.class)
  public void readEobForMissingHospiceClaim() {
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    // No data is loaded, so this should return nothing.
    fhirClient
        .read()
        .resource(ExplanationOfBenefit.class)
        .withId(TransformerUtils.buildEobId(ClaimType.HOSPICE, "1234"))
        .execute();
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for an {@link InpatientClaim}-derived {@link ExplanationOfBenefit} that does
   * exist in the DB.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void readEobForExistingInpatientClaim() throws FHIRException {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    InpatientClaim claim =
        loadedRecords.stream()
            .filter(r -> r instanceof InpatientClaim)
            .map(r -> (InpatientClaim) r)
            .findFirst()
            .get();
    ExplanationOfBenefit eob =
        fhirClient
            .read()
            .resource(ExplanationOfBenefit.class)
            .withId(TransformerUtils.buildEobId(ClaimType.INPATIENT, claim.getClaimId()))
            .execute();

    Assert.assertNotNull(eob);
    InpatientClaimTransformerTest.assertMatches(claim, eob);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for an {@link InpatientClaim}-derived {@link ExplanationOfBenefit} that does
   * not exist in the DB.
   */
  @Test(expected = ResourceNotFoundException.class)
  public void readEobForMissingInpatientClaim() {
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    // No data is loaded, so this should return nothing.
    fhirClient
        .read()
        .resource(ExplanationOfBenefit.class)
        .withId(TransformerUtils.buildEobId(ClaimType.INPATIENT, "1234"))
        .execute();
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for an {@link OutpatientClaim}-derived {@link ExplanationOfBenefit} that does
   * exist in the DB.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void readEobForExistingOutpatientClaim() throws FHIRException {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    OutpatientClaim claim =
        loadedRecords.stream()
            .filter(r -> r instanceof OutpatientClaim)
            .map(r -> (OutpatientClaim) r)
            .findFirst()
            .get();
    ExplanationOfBenefit eob =
        fhirClient
            .read()
            .resource(ExplanationOfBenefit.class)
            .withId(TransformerUtils.buildEobId(ClaimType.OUTPATIENT, claim.getClaimId()))
            .execute();

    Assert.assertNotNull(eob);
    OutpatientClaimTransformerTest.assertMatches(claim, eob);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for an {@link OutpatientClaim}-derived {@link ExplanationOfBenefit} that does
   * not exist in the DB.
   */
  @Test(expected = ResourceNotFoundException.class)
  public void readEobForMissingOutpatientClaim() {
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    // No data is loaded, so this should return nothing.
    fhirClient
        .read()
        .resource(ExplanationOfBenefit.class)
        .withId(TransformerUtils.buildEobId(ClaimType.OUTPATIENT, "1234"))
        .execute();
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for a {@link PartDEvent}-derived {@link ExplanationOfBenefit} that does exist
   * in the DB.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void readEobForExistingPartDEvent() throws FHIRException {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    PartDEvent claim =
        loadedRecords.stream()
            .filter(r -> r instanceof PartDEvent)
            .map(r -> (PartDEvent) r)
            .findFirst()
            .get();
    ExplanationOfBenefit eob =
        fhirClient
            .read()
            .resource(ExplanationOfBenefit.class)
            .withId(TransformerUtils.buildEobId(ClaimType.PDE, claim.getEventId()))
            .execute();

    Assert.assertNotNull(eob);
    PartDEventTransformerTest.assertMatches(claim, eob);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for a {@link PartDEvent}-derived {@link ExplanationOfBenefit} that does not
   * exist in the DB.
   */
  @Test(expected = ResourceNotFoundException.class)
  public void readEobForMissingPartDEvent() {
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    // No data is loaded, so this should return nothing.
    fhirClient
        .read()
        .resource(ExplanationOfBenefit.class)
        .withId(TransformerUtils.buildEobId(ClaimType.PDE, "1234"))
        .execute();
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for an {@link SNFClaim}-derived {@link ExplanationOfBenefit} that does exist
   * in the DB.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void readEobForExistingSNFClaim() throws FHIRException {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    SNFClaim claim =
        loadedRecords.stream()
            .filter(r -> r instanceof SNFClaim)
            .map(r -> (SNFClaim) r)
            .findFirst()
            .get();
    ExplanationOfBenefit eob =
        fhirClient
            .read()
            .resource(ExplanationOfBenefit.class)
            .withId(TransformerUtils.buildEobId(ClaimType.SNF, claim.getClaimId()))
            .execute();

    Assert.assertNotNull(eob);
    SNFClaimTransformerTest.assertMatches(claim, eob);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for an {@link SNFClaim}-derived {@link ExplanationOfBenefit} that does not
   * exist in the DB.
   */
  @Test(expected = ResourceNotFoundException.class)
  public void readEobForMissingSNFClaim() {
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    // No data is loaded, so this should return nothing.
    fhirClient
        .read()
        .resource(ExplanationOfBenefit.class)
        .withId(TransformerUtils.buildEobId(ClaimType.SNF, "1234"))
        .execute();
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#findByPatient} works
   * as expected for a {@link Patient} that does exist in the DB.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void searchForEobsByExistingPatient() throws FHIRException {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(ExplanationOfBenefit.PATIENT.hasId(TransformerUtils.buildPatientId(beneficiary)))
            .returnBundle(Bundle.class)
            .execute();

    Assert.assertNotNull(searchResults);

    /*
     * Verify the bundle contains a key for total and that the value matches the
     * number of entries in the bundle
     */
    Assert.assertEquals(
        loadedRecords.stream()
            .filter(r -> !(r instanceof Beneficiary))
            .filter(r -> !(r instanceof BeneficiaryHistory))
            .filter(r -> !(r instanceof MedicareBeneficiaryIdHistory))
            .count(),
        searchResults.getTotal());

    /*
     * Verify that no paging links exist in the bundle.
     */
    Assert.assertNull(searchResults.getLink(Constants.LINK_NEXT));
    Assert.assertNull(searchResults.getLink(Constants.LINK_PREVIOUS));
    Assert.assertNull(searchResults.getLink(Constants.LINK_FIRST));
    Assert.assertNull(searchResults.getLink(Constants.LINK_LAST));

    /*
     * Verify that each of the expected claims (one for every claim type) is present
     * and looks correct.
     */

    CarrierClaim carrierClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof CarrierClaim)
            .map(r -> (CarrierClaim) r)
            .findFirst()
            .get();
    Assert.assertEquals(1, filterToClaimType(searchResults, ClaimType.CARRIER).size());
    CarrierClaimTransformerTest.assertMatches(
        carrierClaim, filterToClaimType(searchResults, ClaimType.CARRIER).get(0));

    DMEClaim dmeClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof DMEClaim)
            .map(r -> (DMEClaim) r)
            .findFirst()
            .get();
    DMEClaimTransformerTest.assertMatches(
        dmeClaim, filterToClaimType(searchResults, ClaimType.DME).get(0));

    HHAClaim hhaClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof HHAClaim)
            .map(r -> (HHAClaim) r)
            .findFirst()
            .get();
    HHAClaimTransformerTest.assertMatches(
        hhaClaim, filterToClaimType(searchResults, ClaimType.HHA).get(0));

    HospiceClaim hospiceClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof HospiceClaim)
            .map(r -> (HospiceClaim) r)
            .findFirst()
            .get();
    HospiceClaimTransformerTest.assertMatches(
        hospiceClaim, filterToClaimType(searchResults, ClaimType.HOSPICE).get(0));

    InpatientClaim inpatientClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof InpatientClaim)
            .map(r -> (InpatientClaim) r)
            .findFirst()
            .get();
    InpatientClaimTransformerTest.assertMatches(
        inpatientClaim, filterToClaimType(searchResults, ClaimType.INPATIENT).get(0));

    OutpatientClaim outpatientClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof OutpatientClaim)
            .map(r -> (OutpatientClaim) r)
            .findFirst()
            .get();
    OutpatientClaimTransformerTest.assertMatches(
        outpatientClaim, filterToClaimType(searchResults, ClaimType.OUTPATIENT).get(0));

    PartDEvent partDEvent =
        loadedRecords.stream()
            .filter(r -> r instanceof PartDEvent)
            .map(r -> (PartDEvent) r)
            .findFirst()
            .get();
    PartDEventTransformerTest.assertMatches(
        partDEvent, filterToClaimType(searchResults, ClaimType.PDE).get(0));

    SNFClaim snfClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof SNFClaim)
            .map(r -> (SNFClaim) r)
            .findFirst()
            .get();
    SNFClaimTransformerTest.assertMatches(
        snfClaim, filterToClaimType(searchResults, ClaimType.SNF).get(0));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#findByPatient} works
   * as expected for a {@link Patient} that does exist in the DB, with paging. This test uses a
   * count of 2 to verify our code will not run into an IndexOutOfBoundsException on odd bundle
   * sizes.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void searchForEobsByExistingPatientWithEvenPaging() throws FHIRException {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();

    List<IBaseResource> combinedResults = new ArrayList<>();
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(ExplanationOfBenefit.PATIENT.hasId(TransformerUtils.buildPatientId(beneficiary)))
            .count(2)
            .returnBundle(Bundle.class)
            .execute();

    searchResults.getEntry().forEach(e -> combinedResults.add(e.getResource()));

    Assert.assertNotNull(searchResults);
    Assert.assertEquals(2, searchResults.getEntry().size());

    /*
     * Verify the bundle contains a key for total and that the value matches the
     * number of entries in the bundle.
     */
    Assert.assertEquals(
        loadedRecords.stream()
            .filter(r -> !(r instanceof Beneficiary))
            .filter(r -> !(r instanceof BeneficiaryHistory))
            .filter(r -> !(r instanceof MedicareBeneficiaryIdHistory))
            .count(),
        searchResults.getTotal());

    /*
     * Verify links to the first and last page exist.
     */
    Assert.assertNotNull(searchResults.getLink(Constants.LINK_LAST));
    Assert.assertNotNull(searchResults.getLink(Constants.LINK_FIRST));

    /*
     * Verify that accessing all next links, eventually leading to the last page,
     * will not encounter an IndexOutOfBoundsException.
     */
    while (searchResults.getLink(Constants.LINK_NEXT) != null) {
      searchResults = fhirClient.loadPage().next(searchResults).execute();
      Assert.assertNotNull(searchResults);
      Assert.assertTrue(searchResults.hasEntry());

      /*
       * Each page after the first should have a first, previous, and last links.
       */
      Assert.assertNotNull(searchResults.getLink(Constants.LINK_FIRST));
      Assert.assertNotNull(searchResults.getLink(Constants.LINK_PREVIOUS));
      Assert.assertNotNull(searchResults.getLink(Constants.LINK_LAST));

      searchResults.getEntry().forEach(e -> combinedResults.add(e.getResource()));
    }

    /*
     * Verify that the combined results are the same size as
     * "all of the claim records in the sample."
     */
    Assert.assertEquals(
        loadedRecords.stream()
            .filter(r -> !(r instanceof Beneficiary))
            .filter(r -> !(r instanceof BeneficiaryHistory))
            .filter(r -> !(r instanceof MedicareBeneficiaryIdHistory))
            .count(),
        combinedResults.size());

    /*
     * Verify that each of the expected claims (one for every claim type) is present
     * and looks correct.
     */

    CarrierClaim carrierClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof CarrierClaim)
            .map(r -> (CarrierClaim) r)
            .findFirst()
            .get();
    Assert.assertEquals(1, filterToClaimTypeFromList(combinedResults, ClaimType.CARRIER).size());
    CarrierClaimTransformerTest.assertMatches(
        carrierClaim, filterToClaimTypeFromList(combinedResults, ClaimType.CARRIER).get(0));

    DMEClaim dmeClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof DMEClaim)
            .map(r -> (DMEClaim) r)
            .findFirst()
            .get();
    DMEClaimTransformerTest.assertMatches(
        dmeClaim, filterToClaimTypeFromList(combinedResults, ClaimType.DME).get(0));

    HHAClaim hhaClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof HHAClaim)
            .map(r -> (HHAClaim) r)
            .findFirst()
            .get();
    HHAClaimTransformerTest.assertMatches(
        hhaClaim, filterToClaimTypeFromList(combinedResults, ClaimType.HHA).get(0));

    HospiceClaim hospiceClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof HospiceClaim)
            .map(r -> (HospiceClaim) r)
            .findFirst()
            .get();
    HospiceClaimTransformerTest.assertMatches(
        hospiceClaim, filterToClaimTypeFromList(combinedResults, ClaimType.HOSPICE).get(0));

    InpatientClaim inpatientClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof InpatientClaim)
            .map(r -> (InpatientClaim) r)
            .findFirst()
            .get();
    InpatientClaimTransformerTest.assertMatches(
        inpatientClaim, filterToClaimTypeFromList(combinedResults, ClaimType.INPATIENT).get(0));

    OutpatientClaim outpatientClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof OutpatientClaim)
            .map(r -> (OutpatientClaim) r)
            .findFirst()
            .get();
    OutpatientClaimTransformerTest.assertMatches(
        outpatientClaim, filterToClaimTypeFromList(combinedResults, ClaimType.OUTPATIENT).get(0));

    PartDEvent partDEvent =
        loadedRecords.stream()
            .filter(r -> r instanceof PartDEvent)
            .map(r -> (PartDEvent) r)
            .findFirst()
            .get();
    PartDEventTransformerTest.assertMatches(
        partDEvent, filterToClaimTypeFromList(combinedResults, ClaimType.PDE).get(0));

    SNFClaim snfClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof SNFClaim)
            .map(r -> (SNFClaim) r)
            .findFirst()
            .get();
    SNFClaimTransformerTest.assertMatches(
        snfClaim, filterToClaimTypeFromList(combinedResults, ClaimType.SNF).get(0));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#findByPatient} works
   * as expected for a {@link Patient} that does exist in the DB, with paging. This test uses a
   * count of 3 to verify our code will not run into an IndexOutOfBoundsException on even bundle
   * sizes.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void searchForEobsByExistingPatientWithOddPaging() throws FHIRException {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();

    Bundle searchResults =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(ExplanationOfBenefit.PATIENT.hasId(TransformerUtils.buildPatientId(beneficiary)))
            .count(3)
            .returnBundle(Bundle.class)
            .execute();

    Assert.assertNotNull(searchResults);
    Assert.assertEquals(3, searchResults.getEntry().size());

    /*
     * Verify that accessing all next links, eventually leading to the last page,
     * will not encounter an IndexOutOfBoundsException.
     */
    while (searchResults.getLink(Constants.LINK_NEXT) != null) {
      searchResults = fhirClient.loadPage().next(searchResults).execute();
      Assert.assertNotNull(searchResults);
      Assert.assertTrue(searchResults.hasEntry());
    }
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#findByPatient} works
   * as expected for a {@link Patient} that does exist in the DB, with paging, providing the
   * startIndex but not the pageSize (count).
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void searchForEobsByExistingPatientWithPageSizeNotProvided() throws FHIRException {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();

    Bundle searchResults =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(ExplanationOfBenefit.PATIENT.hasId(TransformerUtils.buildPatientId(beneficiary)))
            .returnBundle(Bundle.class)
            .execute();

    Assert.assertNotNull(searchResults);

    /*
     * Verify that no paging links exist in the bundle.
     */
    Assert.assertNull(searchResults.getLink(Constants.LINK_NEXT));
    Assert.assertNull(searchResults.getLink(Constants.LINK_PREVIOUS));
    Assert.assertNull(searchResults.getLink(Constants.LINK_FIRST));
    Assert.assertNull(searchResults.getLink(Constants.LINK_LAST));

    /*
     * Access a created link of this bundle, providing the startIndex but not the
     * pageSize (count).
     */
    Bundle pagedResults =
        fhirClient
            .loadPage()
            .byUrl(searchResults.getLink(Bundle.LINK_SELF).getUrl() + "&startIndex=4")
            .andReturnBundle(Bundle.class)
            .execute();

    Assert.assertNotNull(pagedResults);

    /*
     * Verify that paging links exist in this paged bundle.
     */
    Assert.assertNull(pagedResults.getLink(Constants.LINK_NEXT));
    Assert.assertNotNull(pagedResults.getLink(Constants.LINK_PREVIOUS));
    Assert.assertNotNull(pagedResults.getLink(Constants.LINK_FIRST));
    Assert.assertNotNull(pagedResults.getLink(Constants.LINK_LAST));

    /*
     * Add the entries in the paged results to a list and verify that only the last
     * 4 entries in the original searchResults were returned in the pagedResults.
     */
    List<IBaseResource> pagedEntries = new ArrayList<>();
    pagedResults.getEntry().forEach(e -> pagedEntries.add(e.getResource()));
    Assert.assertEquals(4, pagedEntries.size());
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#findByPatient} works
   * as expected for a {@link Patient} that does exist in the DB, with paging on a page size of 0.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void searchForEobsByExistingPatientWithPageSizeZero() throws FHIRException {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    /*
     * FIXME: According the the FHIR spec, paging for _count=0 should not return any
     * claim entries in the bundle, but instead just a total for the number of
     * entries that match the search criteria. This functionality does no work
     * currently (see https://github.com/jamesagnew/hapi-fhir/issues/1074) and so
     * for now paging with _count=0 should behave as though paging was not
     * requested.
     */
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(ExplanationOfBenefit.PATIENT.hasId(TransformerUtils.buildPatientId(beneficiary)))
            .count(0)
            .returnBundle(Bundle.class)
            .execute();

    Assert.assertNotNull(searchResults);

    /*
     * Verify the bundle contains a key for total and that the value matches the
     * number of entries in the bundle
     */
    Assert.assertEquals(
        loadedRecords.stream()
            .filter(r -> !(r instanceof Beneficiary))
            .filter(r -> !(r instanceof BeneficiaryHistory))
            .filter(r -> !(r instanceof MedicareBeneficiaryIdHistory))
            .count(),
        searchResults.getTotal());

    /*
     * Verify that no paging links exist in the bundle.
     */
    Assert.assertNull(searchResults.getLink(Constants.LINK_NEXT));
    Assert.assertNull(searchResults.getLink(Constants.LINK_PREVIOUS));
    Assert.assertNull(searchResults.getLink(Constants.LINK_FIRST));
    Assert.assertNull(searchResults.getLink(Constants.LINK_LAST));

    /*
     * Verify that each of the expected claims (one for every claim type) is present
     * and looks correct.
     */

    CarrierClaim carrierClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof CarrierClaim)
            .map(r -> (CarrierClaim) r)
            .findFirst()
            .get();
    Assert.assertEquals(1, filterToClaimType(searchResults, ClaimType.CARRIER).size());
    CarrierClaimTransformerTest.assertMatches(
        carrierClaim, filterToClaimType(searchResults, ClaimType.CARRIER).get(0));

    DMEClaim dmeClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof DMEClaim)
            .map(r -> (DMEClaim) r)
            .findFirst()
            .get();
    DMEClaimTransformerTest.assertMatches(
        dmeClaim, filterToClaimType(searchResults, ClaimType.DME).get(0));

    HHAClaim hhaClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof HHAClaim)
            .map(r -> (HHAClaim) r)
            .findFirst()
            .get();
    HHAClaimTransformerTest.assertMatches(
        hhaClaim, filterToClaimType(searchResults, ClaimType.HHA).get(0));

    HospiceClaim hospiceClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof HospiceClaim)
            .map(r -> (HospiceClaim) r)
            .findFirst()
            .get();
    HospiceClaimTransformerTest.assertMatches(
        hospiceClaim, filterToClaimType(searchResults, ClaimType.HOSPICE).get(0));

    InpatientClaim inpatientClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof InpatientClaim)
            .map(r -> (InpatientClaim) r)
            .findFirst()
            .get();
    InpatientClaimTransformerTest.assertMatches(
        inpatientClaim, filterToClaimType(searchResults, ClaimType.INPATIENT).get(0));

    OutpatientClaim outpatientClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof OutpatientClaim)
            .map(r -> (OutpatientClaim) r)
            .findFirst()
            .get();
    OutpatientClaimTransformerTest.assertMatches(
        outpatientClaim, filterToClaimType(searchResults, ClaimType.OUTPATIENT).get(0));

    PartDEvent partDEvent =
        loadedRecords.stream()
            .filter(r -> r instanceof PartDEvent)
            .map(r -> (PartDEvent) r)
            .findFirst()
            .get();
    PartDEventTransformerTest.assertMatches(
        partDEvent, filterToClaimType(searchResults, ClaimType.PDE).get(0));

    SNFClaim snfClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof SNFClaim)
            .map(r -> (SNFClaim) r)
            .findFirst()
            .get();
    SNFClaimTransformerTest.assertMatches(
        snfClaim, filterToClaimType(searchResults, ClaimType.SNF).get(0));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#findByPatient} works
   * as expected for a {@link Patient} that does exist in the DB, with a page size of 50 with fewer
   * (8) results.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void searchForEobsWithLargePageSizesOnFewerResults() throws FHIRException {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();

    Bundle searchResults =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(ExplanationOfBenefit.PATIENT.hasId(TransformerUtils.buildPatientId(beneficiary)))
            .count(50)
            .returnBundle(Bundle.class)
            .execute();

    Assert.assertNotNull(searchResults);

    /*
     * Verify the bundle contains a key for total and that the value matches the
     * number of entries in the bundle
     */
    Assert.assertEquals(
        loadedRecords.stream()
            .filter(r -> !(r instanceof Beneficiary))
            .filter(r -> !(r instanceof BeneficiaryHistory))
            .filter(r -> !(r instanceof MedicareBeneficiaryIdHistory))
            .count(),
        searchResults.getTotal());

    /*
     * Verify that only the first and last links exist as there are no previous or
     * next pages.
     */
    Assert.assertNotNull(searchResults.getLink(Constants.LINK_FIRST));
    Assert.assertNotNull(searchResults.getLink(Constants.LINK_LAST));
    Assert.assertNull(searchResults.getLink(Constants.LINK_NEXT));
    Assert.assertNull(searchResults.getLink(Constants.LINK_PREVIOUS));

    /*
     * Verify that each of the expected claims (one for every claim type) is present
     * and looks correct.
     */

    CarrierClaim carrierClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof CarrierClaim)
            .map(r -> (CarrierClaim) r)
            .findFirst()
            .get();
    Assert.assertEquals(1, filterToClaimType(searchResults, ClaimType.CARRIER).size());
    CarrierClaimTransformerTest.assertMatches(
        carrierClaim, filterToClaimType(searchResults, ClaimType.CARRIER).get(0));

    DMEClaim dmeClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof DMEClaim)
            .map(r -> (DMEClaim) r)
            .findFirst()
            .get();
    DMEClaimTransformerTest.assertMatches(
        dmeClaim, filterToClaimType(searchResults, ClaimType.DME).get(0));

    HHAClaim hhaClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof HHAClaim)
            .map(r -> (HHAClaim) r)
            .findFirst()
            .get();
    HHAClaimTransformerTest.assertMatches(
        hhaClaim, filterToClaimType(searchResults, ClaimType.HHA).get(0));

    HospiceClaim hospiceClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof HospiceClaim)
            .map(r -> (HospiceClaim) r)
            .findFirst()
            .get();
    HospiceClaimTransformerTest.assertMatches(
        hospiceClaim, filterToClaimType(searchResults, ClaimType.HOSPICE).get(0));

    InpatientClaim inpatientClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof InpatientClaim)
            .map(r -> (InpatientClaim) r)
            .findFirst()
            .get();
    InpatientClaimTransformerTest.assertMatches(
        inpatientClaim, filterToClaimType(searchResults, ClaimType.INPATIENT).get(0));

    OutpatientClaim outpatientClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof OutpatientClaim)
            .map(r -> (OutpatientClaim) r)
            .findFirst()
            .get();
    OutpatientClaimTransformerTest.assertMatches(
        outpatientClaim, filterToClaimType(searchResults, ClaimType.OUTPATIENT).get(0));

    PartDEvent partDEvent =
        loadedRecords.stream()
            .filter(r -> r instanceof PartDEvent)
            .map(r -> (PartDEvent) r)
            .findFirst()
            .get();
    PartDEventTransformerTest.assertMatches(
        partDEvent, filterToClaimType(searchResults, ClaimType.PDE).get(0));

    SNFClaim snfClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof SNFClaim)
            .map(r -> (SNFClaim) r)
            .findFirst()
            .get();
    SNFClaimTransformerTest.assertMatches(
        snfClaim, filterToClaimType(searchResults, ClaimType.SNF).get(0));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#findByPatient} works
   * as expected for a {@link Patient} that does exist in the DB, with paging, using negative values
   * for page size and start index parameters. This test expects to receive a BadRequestException,
   * as negative values should result in an HTTP 400.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test(expected = InvalidRequestException.class)
  public void searchForEobsWithPagingWithNegativePagingParameters() throws FHIRException {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();

    /*
     * FIXME: At this time we cannot check for negative page size parameters due to
     * the same bug described in
     * https://github.com/jamesagnew/hapi-fhir/issues/1074.
     */
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(ExplanationOfBenefit.PATIENT.hasId(TransformerUtils.buildPatientId(beneficiary)))
            .returnBundle(Bundle.class)
            .execute();

    Assert.assertNotNull(searchResults);

    /*
     * Access a created link of this bundle, providing the startIndex but not the
     * pageSize (count).
     */
    fhirClient
        .loadPage()
        .byUrl(searchResults.getLink(Bundle.LINK_SELF).getUrl() + "&startIndex=-1")
        .andReturnBundle(Bundle.class)
        .execute();
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#findByPatient(ca.uhn.fhir.rest.param.ReferenceParam)}
   * doesn't return duplicate results.
   *
   * <p>This is a regression test case for TODO.
   *
   * @throws FHIRException (indicates test failure)
   */
  // @Test
  public void searchForEobsHasNoDupes() throws FHIRException {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_B.getResources()));
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    loadedRecords.stream()
        .filter(r -> r instanceof Beneficiary)
        .map(r -> (Beneficiary) r)
        .forEach(
            beneficiary -> {
              Bundle searchResults =
                  fhirClient
                      .search()
                      .forResource(ExplanationOfBenefit.class)
                      .where(
                          ExplanationOfBenefit.PATIENT.hasId(
                              TransformerUtils.buildPatientId(beneficiary)))
                      .returnBundle(Bundle.class)
                      .execute();
              Assert.assertNotNull(searchResults);

              /*
               * Verify that the returned Bundle doesn't have any resources with duplicate
               * IDs.
               */
              Set<String> claimIds = new HashSet<>();
              for (BundleEntryComponent searchResultEntry : searchResults.getEntry()) {
                String resourceId = searchResultEntry.getResource().getId();
                if (claimIds.contains(resourceId))
                  Assert.assertFalse(claimIds.contains(resourceId));
                claimIds.add(resourceId);
              }
              if (searchResults.getTotal() > 0) Assert.assertFalse(claimIds.isEmpty());
            });
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#findByPatient(ca.uhn.fhir.rest.param.ReferenceParam)}
   * works as expected for a {@link Patient} that does not exist in the DB.
   */
  @Test
  public void searchForEobsByMissingPatient() {
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    // No data is loaded, so this should return 0 matches.
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(ExplanationOfBenefit.PATIENT.hasId(new IdDt("Patient", "1234")))
            .returnBundle(Bundle.class)
            .execute();

    Assert.assertNotNull(searchResults);
    Assert.assertEquals(0, searchResults.getTotal());
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#findByPatient(ca.uhn.fhir.rest.param.ReferenceParam)}
   * with <code>excludeSAMHSA=true</code> properly filters out SAMHSA-related claims.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void searchForEobsWithSamhsaFiltering() throws FHIRException {
    // Load the SAMPLE_A resources normally.
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    // Tweak the SAMPLE_A Carrier claim such that it's SAMHSA-related.
    CarrierClaim carrierRifRecord =
        loadedRecords.stream()
            .filter(r -> r instanceof CarrierClaim)
            .map(r -> (CarrierClaim) r)
            .findFirst()
            .get();

    LoadAppOptions loadAppOptions = ServerTestUtils.createRifLoaderOptions();
    EntityManagerFactory entityManagerFactory = null;
    EntityManager entityManager = null;
    try {
      entityManagerFactory = RifLoaderTestUtils.createEntityManagerFactory(loadAppOptions);
      entityManager = entityManagerFactory.createEntityManager();

      entityManager.getTransaction().begin();
      carrierRifRecord = entityManager.find(CarrierClaim.class, carrierRifRecord.getClaimId());
      carrierRifRecord.setDiagnosis2Code(
          Optional.of(SamhsaMatcherTest.SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE));
      carrierRifRecord.setDiagnosis2CodeVersion(Optional.of('9'));
      entityManager.merge(carrierRifRecord);
      entityManager.getTransaction().commit();

      // Tweak the SAMPLE_A Inpatient claim such that it's SAMHSA-related.
      InpatientClaim inpatientRifRecord =
          loadedRecords.stream()
              .filter(r -> r instanceof InpatientClaim)
              .map(r -> (InpatientClaim) r)
              .findFirst()
              .get();

      entityManager.getTransaction().begin();
      inpatientRifRecord =
          entityManager.find(InpatientClaim.class, inpatientRifRecord.getClaimId());
      inpatientRifRecord.setDiagnosis2Code(
          Optional.of(SamhsaMatcherTest.SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE));
      inpatientRifRecord.setDiagnosis2CodeVersion(Optional.of('9'));
      entityManager.merge(inpatientRifRecord);
      entityManager.getTransaction().commit();

      // Tweak the SAMPLE_A Outpatient claim such that it's SAMHSA-related.
      OutpatientClaim outpatientRifRecord =
          loadedRecords.stream()
              .filter(r -> r instanceof OutpatientClaim)
              .map(r -> (OutpatientClaim) r)
              .findFirst()
              .get();

      entityManager.getTransaction().begin();
      outpatientRifRecord =
          entityManager.find(OutpatientClaim.class, outpatientRifRecord.getClaimId());
      outpatientRifRecord.setDiagnosis2Code(
          Optional.of(SamhsaMatcherTest.SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE));
      outpatientRifRecord.setDiagnosis2CodeVersion(Optional.of('9'));
      entityManager.merge(outpatientRifRecord);
      entityManager.getTransaction().commit();

      // Tweak the SAMPLE_A HHA claim such that it's SAMHSA-related.
      HHAClaim hhaRifRecord =
          loadedRecords.stream()
              .filter(r -> r instanceof HHAClaim)
              .map(r -> (HHAClaim) r)
              .findFirst()
              .get();

      entityManager.getTransaction().begin();
      hhaRifRecord = entityManager.find(HHAClaim.class, hhaRifRecord.getClaimId());
      hhaRifRecord.setDiagnosis2Code(
          Optional.of(SamhsaMatcherTest.SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE));
      hhaRifRecord.setDiagnosis2CodeVersion(Optional.of('9'));
      entityManager.merge(hhaRifRecord);
      entityManager.getTransaction().commit();

      // Tweak the SAMPLE_A Hospice claim such that it's SAMHSA-related.
      HospiceClaim hospiceRifRecord =
          loadedRecords.stream()
              .filter(r -> r instanceof HospiceClaim)
              .map(r -> (HospiceClaim) r)
              .findFirst()
              .get();

      entityManager.getTransaction().begin();
      hospiceRifRecord = entityManager.find(HospiceClaim.class, hospiceRifRecord.getClaimId());
      hospiceRifRecord.setDiagnosis2Code(
          Optional.of(SamhsaMatcherTest.SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE));
      hospiceRifRecord.setDiagnosis2CodeVersion(Optional.of('9'));
      entityManager.merge(hospiceRifRecord);
      entityManager.getTransaction().commit();

      // Tweak the SAMPLE_A SNF claim such that it's SAMHSA-related.
      SNFClaim snfRifRecord =
          loadedRecords.stream()
              .filter(r -> r instanceof SNFClaim)
              .map(r -> (SNFClaim) r)
              .findFirst()
              .get();

      entityManager.getTransaction().begin();
      snfRifRecord = entityManager.find(SNFClaim.class, snfRifRecord.getClaimId());
      snfRifRecord.setDiagnosis2Code(
          Optional.of(SamhsaMatcherTest.SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE));
      snfRifRecord.setDiagnosis2CodeVersion(Optional.of('9'));
      entityManager.merge(snfRifRecord);
      entityManager.getTransaction().commit();
    } finally {
      if (entityManager.getTransaction().isActive()) entityManager.getTransaction().rollback();
      if (entityManager != null) entityManager.close();
      if (entityManagerFactory != null) entityManagerFactory.close();
    }

    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    Bundle searchResults =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(
                ExplanationOfBenefit.PATIENT.hasId(
                    TransformerUtils.buildPatientId(carrierRifRecord.getBeneficiaryId())))
            .and(new StringClientParam("excludeSAMHSA").matches().value("true"))
            .returnBundle(Bundle.class)
            .execute();
    Assert.assertNotNull(searchResults);
    for (ClaimType claimType : ClaimType.values()) {
      /*
       * First, verify that the claims that should have been filtered out, were. Then
       * in the `else` clause, verify that everything was **not** filtered out.
       */
      // FIXME remove the `else if`s once filtering fully supports all claim types
      if (claimType.equals(ClaimType.CARRIER))
        Assert.assertEquals(0, filterToClaimType(searchResults, claimType).size());
      else if (claimType.equals(ClaimType.HHA))
        Assert.assertEquals(0, filterToClaimType(searchResults, claimType).size());
      else if (claimType.equals(ClaimType.HOSPICE))
        Assert.assertEquals(0, filterToClaimType(searchResults, claimType).size());
      else if (claimType.equals(ClaimType.INPATIENT))
        Assert.assertEquals(0, filterToClaimType(searchResults, claimType).size());
      else if (claimType.equals(ClaimType.OUTPATIENT))
        Assert.assertEquals(0, filterToClaimType(searchResults, claimType).size());
      else if (claimType.equals(ClaimType.SNF))
        Assert.assertEquals(0, filterToClaimType(searchResults, claimType).size());
      else if (claimType.equals(ClaimType.PDE))
        // PDE Claims do not contain SAMHSA fields and thus won't be filtered.
        Assert.assertEquals(1, filterToClaimType(searchResults, claimType).size());
      else Assert.assertEquals(1, filterToClaimType(searchResults, claimType).size());
    }
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#findByPatient(ca.uhn.fhir.rest.param.ReferenceParam)}
   * works as expected for a {@link Patient} that does exist in the DB, with filtering by claim
   * type.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void searchForEobsByExistingPatientAndType() throws FHIRException {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(ExplanationOfBenefit.PATIENT.hasId(TransformerUtils.buildPatientId(beneficiary)))
            .where(new TokenClientParam("type").exactly().code(ClaimType.PDE.name()))
            .returnBundle(Bundle.class)
            .execute();

    Assert.assertNotNull(searchResults);

    /*
     * Verify the bundle contains a key for total and that the value matches the
     * number of entries in the bundle
     */
    Assert.assertEquals(
        loadedRecords.stream()
            .filter(r -> (r instanceof PartDEvent))
            .filter(r -> !(r instanceof BeneficiaryHistory))
            .count(),
        searchResults.getTotal());

    PartDEvent partDEvent =
        loadedRecords.stream()
            .filter(r -> r instanceof PartDEvent)
            .map(r -> (PartDEvent) r)
            .findFirst()
            .get();
    PartDEventTransformerTest.assertMatches(
        partDEvent, filterToClaimType(searchResults, ClaimType.PDE).get(0));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#findByPatient} works
   * as with a lastUpdated parameter after yesterday.
   *
   * <p>See https://www.hl7.org/fhir/search.html#lastUpdated for explanation of possible types
   * lastUpdatedQueries
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void searchEobWithLastUpdated() throws FHIRException {
    Beneficiary beneficiary = loadSampleA();
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    // Build up a list of lastUpdatedURLs that return > all values values
    String nowDateTime = new DateTimeDt(Date.from(Instant.now().plusSeconds(1))).getValueAsString();
    String earlyDateTime = "2019-10-01T00:00:00-04:00";
    List<String> allUrls =
        Arrays.asList(
            "_lastUpdated=gt" + earlyDateTime,
            "_lastUpdated=ge" + earlyDateTime,
            "_lastUpdated=le" + nowDateTime,
            "_lastUpdated=ge" + earlyDateTime + "&_lastUpdated=le" + nowDateTime,
            "_lastUpdated=gt" + earlyDateTime + "&_lastUpdated=lt" + nowDateTime);
    testLastUpdatedUrls(fhirClient, beneficiary.getBeneficiaryId(), allUrls, 8);

    // Empty searches
    List<String> emptyUrls =
        Arrays.asList(
            "_lastUpdated=lt" + earlyDateTime,
            "_lastUpdated=le" + earlyDateTime,
            "_lastUpdated=eq" + earlyDateTime);
    testLastUpdatedUrls(fhirClient, beneficiary.getBeneficiaryId(), emptyUrls, 0);
  }

  /** Verifies that a search with a lastUpdated with eq param works */
  @Test
  public void searchEobWithLastUpdatedEq() throws FHIRException {
    Beneficiary beneficiary = loadSampleA();
    String beneId = beneficiary.getBeneficiaryId();
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    // Find an EOB
    Bundle allEobs = fetchWithLastUpdated(fhirClient, beneId, "");
    Assert.assertTrue(allEobs.getEntry().size() > 0);
    Resource testEob = allEobs.getEntry().get(0).getResource();

    // Fetch it again with an lastupdate
    Date testDate = testEob.getMeta().getLastUpdated();
    DateTimeDt fhirTestMillis = new DateTimeDt(testDate, TemporalPrecisionEnum.MILLI);
    String lastUpdatedEq = "_lastUpdated=eq" + fhirTestMillis.getValueAsString();
    Bundle eobsWithTime = fetchWithLastUpdated(fhirClient, beneId, lastUpdatedEq);
    Assert.assertTrue("Expected to have at least one match", eobsWithTime.getEntry().size() > 0);

    // Test that each retrived match the testDate with second percision
    eobsWithTime.getEntry().stream()
        .forEach(
            eob -> {
              Date actualDate = eob.getResource().getMeta().getLastUpdated();
              Assert.assertEquals(testDate.getTime(), actualDate.getTime());
            });
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#findByPatient} works
   * as with a lastUpdated parameter after yesterday.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void searchEobWithLastUpdatedAndPagination() throws FHIRException {
    Beneficiary beneficiary = loadSampleA();
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();

    // Search with lastUpdated range between yesterday and now
    int expectedCount = 3;
    Date yesterday = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
    Date now = new Date();
    DateRangeParam afterYesterday = new DateRangeParam(yesterday, now);
    Bundle searchResultsAfter =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(ExplanationOfBenefit.PATIENT.hasId(TransformerUtils.buildPatientId(beneficiary)))
            .lastUpdated(afterYesterday)
            .count(expectedCount)
            .returnBundle(Bundle.class)
            .execute();
    Assert.assertEquals(
        "Expected number resources return to be equal to count",
        expectedCount,
        searchResultsAfter.getEntry().size());

    // Check self url
    String selfLink = searchResultsAfter.getLink(IBaseBundle.LINK_SELF).getUrl();
    Assert.assertTrue(selfLink.contains("lastUpdated"));

    // Check next bundle
    String nextLink = searchResultsAfter.getLink(IBaseBundle.LINK_NEXT).getUrl();
    Assert.assertTrue(nextLink.contains("lastUpdated"));
    Bundle nextResults = fhirClient.search().byUrl(nextLink).returnBundle(Bundle.class).execute();
    Assert.assertEquals(
        "Expected number resources return to be equal to count",
        expectedCount,
        nextResults.getEntry().size());
  }

  @Test
  public void searchEobWithNullLastUpdated() throws FHIRException {
    // Load a records and clear the lastUpdated field for one
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    String claimId = findFirstCarrierClaim(loadedRecords).getClaimId();
    String beneId = findFirstBeneficary(loadedRecords).getBeneficiaryId();
    clearCarrierClaimLastUpdated(claimId);

    // Find all EOBs without lastUpdated
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();
    Bundle searchAll =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(ExplanationOfBenefit.PATIENT.hasId(TransformerUtils.buildPatientId(beneId)))
            .returnBundle(Bundle.class)
            .execute();
    Assert.assertNull(
        filterToClaimType(searchAll, ClaimType.CARRIER).get(0).getMeta().getLastUpdated());

    // Find all EOBs with < now()
    Bundle searchWithLessThan =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(ExplanationOfBenefit.PATIENT.hasId(TransformerUtils.buildPatientId(beneId)))
            .lastUpdated(new DateRangeParam().setUpperBoundInclusive(new Date()))
            .returnBundle(Bundle.class)
            .execute();
    Assert.assertNull(
        filterToClaimType(searchWithLessThan, ClaimType.CARRIER).get(0).getMeta().getLastUpdated());
    Assert.assertEquals(
        "Expected the search for lastUpdated <= now() to include null lastUpdated resources",
        searchAll.getTotal(),
        searchWithLessThan.getTotal());

    // Find all EOBs with >= now()-100 seconds
    Bundle searchWithGreaterThan =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(ExplanationOfBenefit.PATIENT.hasId(TransformerUtils.buildPatientId(beneId)))
            .lastUpdated(
                new DateRangeParam()
                    .setLowerBoundInclusive(Date.from(Instant.now().minusSeconds(100))))
            .returnBundle(Bundle.class)
            .execute();
    Assert.assertEquals(
        "Expected the search for lastUpdated >= now()-100 to not include null lastUpdated resources",
        searchAll.getTotal() - 1,
        searchWithGreaterThan.getTotal());
  }

  /** Ensures that {@link ServerTestUtils#cleanDatabaseServer()} is called after each test case. */
  @After
  public void cleanDatabaseServerAfterEachTestCase() {
    ServerTestUtils.cleanDatabaseServer();
    // FIXME temporary workaround to free up ram
    SessionFactoryRegistry.INSTANCE.clearRegistrations();
  }

  /**
   * @param bundle the {@link Bundle} to filter
   * @param claimType the {@link gov.cms.bfd.server.war.stu3.providers.ClaimType} to use as a filter
   * @return a filtered {@link List} of the {@link ExplanationOfBenefit}s from the specified {@link
   *     Bundle} that match the specified {@link gov.cms.bfd.server.war.stu3.providers.ClaimType}
   */
  private static List<ExplanationOfBenefit> filterToClaimType(Bundle bundle, ClaimType claimType) {
    List<ExplanationOfBenefit> results =
        bundle.getEntry().stream()
            .filter(e -> e.getResource() instanceof ExplanationOfBenefit)
            .map(e -> (ExplanationOfBenefit) e.getResource())
            .filter(
                e -> {
                  return TransformerTestUtils.isCodeInConcept(
                      e.getType(),
                      TransformerConstants.CODING_SYSTEM_BBAPI_EOB_TYPE,
                      claimType.name());
                })
            .collect(Collectors.toList());
    return results;
  }

  private static List<ExplanationOfBenefit> filterToClaimTypeFromList(
      List<IBaseResource> resources, ClaimType claimType) {
    List<ExplanationOfBenefit> results =
        resources.stream()
            .filter(r -> r instanceof ExplanationOfBenefit)
            .map(e -> (ExplanationOfBenefit) e)
            .filter(
                e -> {
                  return TransformerTestUtils.isCodeInConcept(
                      e.getType(),
                      TransformerConstants.CODING_SYSTEM_BBAPI_EOB_TYPE,
                      claimType.name());
                })
            .collect(Collectors.toList());
    return results;
  }

  /**
   * Test the set of lastUpdated values
   *
   * @param fhirClient to use
   * @param id the bene id to use
   * @param urls is a list of lastUpdate values to test to find
   * @param expectedValue number of matches
   */
  private void testLastUpdatedUrls(
      IGenericClient fhirClient, String id, List<String> urls, int expectedValue) {

    // Search for each lastUpdated value
    for (String lastUpdatedValue : urls) {
      Bundle searchResults = fetchWithLastUpdated(fhirClient, id, lastUpdatedValue);
      Assert.assertEquals(
          String.format("Expected %s to filter resources correctly", lastUpdatedValue),
          expectedValue,
          searchResults.getTotal());
    }
  }

  /**
   * Load Sample A into the test database
   *
   * @return the beneficary record loaded by Sample A
   */
  private Beneficiary loadSampleA() {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    // Return beneficiary information
    return findFirstBeneficary(loadedRecords);
  }

  /**
   * Find the first Beneficiary from a record list returned by {@link ServerTestUtils#loadData(List}
   *
   * @param loadedRecords to use
   * @return the first Beneficiary
   */
  private static Beneficiary findFirstBeneficary(List<Object> loadedRecords) {
    return loadedRecords.stream()
        .filter(r -> r instanceof Beneficiary)
        .map(r -> (Beneficiary) r)
        .findFirst()
        .get();
  }

  /**
   * Find the first CarrierClaim from a record list returned by {@link
   * ServerTestUtils#loadData(List}
   *
   * @param loadedRecords to use
   * @return the first CarrierClaim in the records
   */
  private static CarrierClaim findFirstCarrierClaim(List<Object> loadedRecords) {
    return loadedRecords.stream()
        .filter(r -> r instanceof CarrierClaim)
        .map(r -> (CarrierClaim) r)
        .findFirst()
        .get();
  }

  /**
   * Fetch a bundle
   *
   * @param fhirClient to use
   * @param id the bene id to use
   * @param lastUpdatedParam to added to the fetch
   */
  private Bundle fetchWithLastUpdated(
      IGenericClient fhirClient, String id, String lastUpdatedParam) {
    String url =
        "ExplanationOfBenefit?patient=Patient%2F"
            + id
            + (lastUpdatedParam.isEmpty() ? "" : "&" + lastUpdatedParam)
            + "&_format=application%2Fjson%2Bfhir";
    return fhirClient.search().byUrl(url).returnBundle(Bundle.class).execute();
  }

  /**
   * To setup a database, clear the lastUpdated of passed in claim
   *
   * @param claimId to use
   */
  private void clearCarrierClaimLastUpdated(String claimId) {
    ServerTestUtils.doTransaction(
        (em) -> {
          em.createQuery("update CarrierClaim set lastUpdated=null where claimId=:claimId")
              .setParameter("claimId", claimId)
              .executeUpdate();
        });
  }
}
