package gov.cms.bfd.server.war.stu3.providers;

import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.google.common.collect.ImmutableList;
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
import gov.cms.bfd.pipeline.sharedutils.PipelineTestUtils;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.CommonHeaders;
import gov.cms.bfd.server.war.commons.RequestHeaders;
import gov.cms.bfd.server.war.commons.TransformerConstants;
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
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Patient;
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
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
    CarrierClaimTransformerTest.assertMatches(claim, eob, Optional.empty());
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for a {@link CarrierClaim}-derived {@link ExplanationOfBenefit} that does not
   * exist in the DB.
   */
  @Test(expected = ResourceNotFoundException.class)
  public void readEobForMissingCarrierClaim() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
    DMEClaimTransformerTest.assertMatches(claim, eob, Optional.empty());
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for a {@link DMEClaim}-derived {@link ExplanationOfBenefit} that does not
   * exist in the DB.
   */
  @Test(expected = ResourceNotFoundException.class)
  public void readEobForMissingDMEClaim() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
   * works as expected for a {@link PartDEvent}-derived {@link ExplanationOfBenefit} that does not
   * exist in the DB using a negative ID.
   */
  @Test(expected = ResourceNotFoundException.class)
  public void readEobForMissingNegativePartDEvent() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

    // No data is loaded, so this should return nothing. Tests negative ID will pass regex pattern.
    fhirClient
        .read()
        .resource(ExplanationOfBenefit.class)
        .withId(TransformerUtils.buildEobId(ClaimType.PDE, "-1234"))
        .execute();
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for a {@link PartDEvent}-derived {@link ExplanationOfBenefit} that has an
   * invalid {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#IdParam} parameter.
   */
  @Test(expected = InvalidRequestException.class)
  public void readEobForInvalidIdParamPartDEvent() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

    // The IdParam is not valid, so this should return an exception.
    fhirClient
        .read()
        .resource(ExplanationOfBenefit.class)
        .withId(TransformerUtils.buildEobId(ClaimType.PDE, "-1?234"))
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
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
        carrierClaim, filterToClaimType(searchResults, ClaimType.CARRIER).get(0), Optional.empty());

    DMEClaim dmeClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof DMEClaim)
            .map(r -> (DMEClaim) r)
            .findFirst()
            .get();
    DMEClaimTransformerTest.assertMatches(
        dmeClaim, filterToClaimType(searchResults, ClaimType.DME).get(0), Optional.empty());

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
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
        carrierClaim,
        filterToClaimTypeFromList(combinedResults, ClaimType.CARRIER).get(0),
        Optional.empty());

    DMEClaim dmeClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof DMEClaim)
            .map(r -> (DMEClaim) r)
            .findFirst()
            .get();
    DMEClaimTransformerTest.assertMatches(
        dmeClaim,
        filterToClaimTypeFromList(combinedResults, ClaimType.DME).get(0),
        Optional.empty());

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
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
        carrierClaim, filterToClaimType(searchResults, ClaimType.CARRIER).get(0), Optional.empty());

    DMEClaim dmeClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof DMEClaim)
            .map(r -> (DMEClaim) r)
            .findFirst()
            .get();
    DMEClaimTransformerTest.assertMatches(
        dmeClaim, filterToClaimType(searchResults, ClaimType.DME).get(0), Optional.empty());

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
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
        carrierClaim, filterToClaimType(searchResults, ClaimType.CARRIER).get(0), Optional.empty());

    DMEClaim dmeClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof DMEClaim)
            .map(r -> (DMEClaim) r)
            .findFirst()
            .get();
    DMEClaimTransformerTest.assertMatches(
        dmeClaim, filterToClaimType(searchResults, ClaimType.DME).get(0), Optional.empty());

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
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_B.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
   * Load the SAMPLE_A resources and then tweak each of the claim types that support SAMHSA (all
   * except PDE) to have a SAMHSA diagnosis code.
   *
   * @return the beneficary record loaded by Sample A
   */
  private Beneficiary loadSampleAWithSamhsa() {
    // Load the SAMPLE_A resources normally.
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    EntityManager entityManager = null;

    try {
      EntityManagerFactory entityManagerFactory =
          PipelineTestUtils.get().getPipelineApplicationState().getEntityManagerFactory();
      entityManager = entityManagerFactory.createEntityManager();

      // Tweak the SAMPLE_A claims such that they are SAMHSA-related.
      adjustCarrierClaimForSamhsaDiagnosis(loadedRecords, entityManager);
      adjustInpatientRecordForSamhsaDiagnosis(loadedRecords, entityManager);
      adjustOutpatientRecordForSamhsaDiagnosis(loadedRecords, entityManager);
      adjustHhaRecordForSamhsaDiagnosis(loadedRecords, entityManager);
      adjustSnfRecordForSamhsaDiagnosis(loadedRecords, entityManager);
      adjustHospiceRecordForSamhsaDiagnosis(loadedRecords, entityManager);
      adjustDmeRecordForSamhsaDiagnosis(loadedRecords, entityManager);

    } finally {
      if (entityManager != null && entityManager.getTransaction().isActive())
        entityManager.getTransaction().rollback();
      if (entityManager != null) entityManager.close();
    }

    // Return beneficiary information
    return findFirstBeneficary(loadedRecords);
  }

  /**
   * Adjusts the carrier claim to support samhsa.
   *
   * @param loadedRecords the loaded records
   * @param entityManager the entity manager
   */
  private void adjustCarrierClaimForSamhsaDiagnosis(
      List<Object> loadedRecords, EntityManager entityManager) {

    CarrierClaim carrierRifRecord =
        loadedRecords.stream()
            .filter(r -> r instanceof CarrierClaim)
            .map(r -> (CarrierClaim) r)
            .findFirst()
            .get();

    entityManager.getTransaction().begin();
    carrierRifRecord = entityManager.find(CarrierClaim.class, carrierRifRecord.getClaimId());
    carrierRifRecord.setDiagnosis2Code(
        Optional.of(SamhsaMatcherTest.SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE));
    carrierRifRecord.setDiagnosis2CodeVersion(Optional.of('9'));
    entityManager.merge(carrierRifRecord);
    entityManager.getTransaction().commit();
  }

  /**
   * Adjusts the first inpatient record to support samhsa.
   *
   * @param loadedRecords the loaded records
   * @param entityManager the entity manager
   */
  private void adjustInpatientRecordForSamhsaDiagnosis(
      List<Object> loadedRecords, EntityManager entityManager) {

    InpatientClaim inpatientRifRecord =
        loadedRecords.stream()
            .filter(r -> r instanceof InpatientClaim)
            .map(r -> (InpatientClaim) r)
            .findFirst()
            .get();

    entityManager.getTransaction().begin();
    inpatientRifRecord = entityManager.find(InpatientClaim.class, inpatientRifRecord.getClaimId());
    inpatientRifRecord.setDiagnosis2Code(
        Optional.of(SamhsaMatcherTest.SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE));
    inpatientRifRecord.setDiagnosis2CodeVersion(Optional.of('9'));
    entityManager.merge(inpatientRifRecord);
    entityManager.getTransaction().commit();
  }

  /**
   * Adjusts the first outpatient record to support samhsa.
   *
   * @param loadedRecords the loaded records
   * @param entityManager the entity manager
   */
  private void adjustOutpatientRecordForSamhsaDiagnosis(
      List<Object> loadedRecords, EntityManager entityManager) {

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
  }

  /**
   * Adjusts the first HHA record to support samhsa.
   *
   * @param loadedRecords the loaded records
   * @param entityManager the entity manager
   */
  private void adjustHhaRecordForSamhsaDiagnosis(
      List<Object> loadedRecords, EntityManager entityManager) {

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
  }

  /**
   * Adjusts the first SNF record to support samhsa.
   *
   * @param loadedRecords the loaded records
   * @param entityManager the entity manager
   */
  private void adjustSnfRecordForSamhsaDiagnosis(
      List<Object> loadedRecords, EntityManager entityManager) {

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
  }

  /**
   * Adjusts the first Hospice record to support samhsa.
   *
   * @param loadedRecords the loaded records
   * @param entityManager the entity manager
   */
  private void adjustHospiceRecordForSamhsaDiagnosis(
      List<Object> loadedRecords, EntityManager entityManager) {

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
  }

  /**
   * Adjusts the first DME record to support samhsa.
   *
   * @param loadedRecords the loaded records
   * @param entityManager the entity manager
   */
  private void adjustDmeRecordForSamhsaDiagnosis(
      List<Object> loadedRecords, EntityManager entityManager) {

    DMEClaim dmeRifRecord =
        loadedRecords.stream()
            .filter(r -> r instanceof DMEClaim)
            .map(r -> (DMEClaim) r)
            .findFirst()
            .get();

    entityManager.getTransaction().begin();
    dmeRifRecord = entityManager.find(DMEClaim.class, dmeRifRecord.getClaimId());
    dmeRifRecord.setDiagnosis2Code(
        Optional.of(SamhsaMatcherTest.SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE));
    dmeRifRecord.setDiagnosis2CodeVersion(Optional.of('9'));
    entityManager.merge(dmeRifRecord);
    entityManager.getTransaction().commit();
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#findByPatient} with
   * <code>excludeSAMHSA=true</code> properly filters out SAMHSA-related claims.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void searchForSamhsaEobsWithExcludeSamhsaTrue() throws FHIRException {
    Beneficiary beneficiary = loadSampleAWithSamhsa();

    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

    Bundle searchResults =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(
                ExplanationOfBenefit.PATIENT.hasId(
                    TransformerUtils.buildPatientId(beneficiary.getBeneficiaryId())))
            .and(new StringClientParam("excludeSAMHSA").matches().value("true"))
            .returnBundle(Bundle.class)
            .execute();
    Assert.assertNotNull(searchResults);
    for (ClaimType claimType : ClaimType.values()) {
      /*
       * SAMHSA fields are present on all claim types except for PDE so we should not
       * get any claims back in the results except for PDE.
       */
      if (claimType == ClaimType.PDE) {
        Assert.assertEquals(1, filterToClaimType(searchResults, claimType).size());
      } else {
        Assert.assertEquals(0, filterToClaimType(searchResults, claimType).size());
      }
    }
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#findByPatient} with
   * <code>excludeSAMHSA=false</code> does not filter out SAMHSA-related claims.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void searchForSamhsaEobsWithExcludeSamhsaFalse() throws FHIRException {
    Beneficiary beneficiary = loadSampleAWithSamhsa();

    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

    Bundle searchResults =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(
                ExplanationOfBenefit.PATIENT.hasId(
                    TransformerUtils.buildPatientId(beneficiary.getBeneficiaryId())))
            .and(new StringClientParam("excludeSAMHSA").matches().value("false"))
            .returnBundle(Bundle.class)
            .execute();
    Assert.assertNotNull(searchResults);
    for (ClaimType claimType : ClaimType.values()) {
      // Without filtering we expect one claim for each claim type.
      Assert.assertEquals(1, filterToClaimType(searchResults, claimType).size());
    }
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#findByPatient} with
   * <code>excludeSAMHSA=true</code> properly returns claims that are not SAMHSA-related.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void searchForNonSamhsaEobsWithExcludeSamhsaTrue() throws FHIRException {
    // Load the SAMPLE_A resources normally.
    Beneficiary beneficiary = loadSampleA();

    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

    Bundle searchResults =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(
                ExplanationOfBenefit.PATIENT.hasId(
                    TransformerUtils.buildPatientId(beneficiary.getBeneficiaryId())))
            .and(new StringClientParam("excludeSAMHSA").matches().value("true"))
            .returnBundle(Bundle.class)
            .execute();
    Assert.assertNotNull(searchResults);
    for (ClaimType claimType : ClaimType.values()) {
      // None of the claims are SAMHSA so we expect one record per claim type in the results.
      Assert.assertEquals(
          String.format("Verify claims of type '%s' are present", claimType),
          1,
          filterToClaimType(searchResults, claimType).size());
    }
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#findByPatient} with
   * <code>excludeSAMHSA=false</code> properly returns claims that are not SAMHSA-related.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void searchForNonSamhsaEobsWithExcludeSamhsaFalse() throws FHIRException {
    // Load the SAMPLE_A resources normally.
    Beneficiary beneficiary = loadSampleA();

    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

    Bundle searchResults =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(
                ExplanationOfBenefit.PATIENT.hasId(
                    TransformerUtils.buildPatientId(beneficiary.getBeneficiaryId())))
            .and(new StringClientParam("excludeSAMHSA").matches().value("false"))
            .returnBundle(Bundle.class)
            .execute();
    Assert.assertNotNull(searchResults);
    for (ClaimType claimType : ClaimType.values()) {
      // None of the claims are SAMHSA so we expect one record per claim type in the results.
      Assert.assertEquals(
          String.format("Verify claims of type '%s' are present", claimType),
          1,
          filterToClaimType(searchResults, claimType).size());
    }
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider#findByPatient(ca.uhn.fhir.rest.param.ReferenceParam)}
   * handles the {@link ExplanationOfBenefitResourceProvider#HEADER_NAME_INCLUDE_TAX_NUMBERS} header
   * properly.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void searchForEobsIncludeTaxNumbersHandling() throws FHIRException {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();
    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    CarrierClaim carrierClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof CarrierClaim)
            .map(r -> (CarrierClaim) r)
            .findFirst()
            .get();
    DMEClaim dmeClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof DMEClaim)
            .map(r -> (DMEClaim) r)
            .findFirst()
            .get();
    Bundle searchResults;
    ExplanationOfBenefit carrierEob;
    ExplanationOfBenefit dmeEob;

    // Run the search without requesting tax numbers.
    searchResults =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(ExplanationOfBenefit.PATIENT.hasId(TransformerUtils.buildPatientId(beneficiary)))
            .returnBundle(Bundle.class)
            .execute();
    Assert.assertNotNull(searchResults);

    // Verify that tax numbers aren't present for carrier claims.
    carrierEob = filterToClaimType(searchResults, ClaimType.CARRIER).get(0);
    Assert.assertNull(
        TransformerTestUtils.findCareTeamEntryForProviderTaxNumber(
            carrierClaim.getLines().get(0).getProviderTaxNumber(), carrierEob.getCareTeam()));

    // Verify that tax numbers aren't present for DME claims.
    dmeEob = filterToClaimType(searchResults, ClaimType.DME).get(0);
    Assert.assertNull(
        TransformerTestUtils.findCareTeamEntryForProviderTaxNumber(
            dmeClaim.getLines().get(0).getProviderTaxNumber(), dmeEob.getCareTeam()));

    RequestHeaders requestHeader =
        RequestHeaders.getHeaderWrapper(CommonHeaders.HEADER_NAME_INCLUDE_TAX_NUMBERS, "true");
    fhirClient = ServerTestUtils.get().createFhirClientWithHeaders(requestHeader);

    searchResults =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(ExplanationOfBenefit.PATIENT.hasId(TransformerUtils.buildPatientId(beneficiary)))
            .returnBundle(Bundle.class)
            .execute();
    Assert.assertNotNull(searchResults);

    // Verify that tax numbers are present for carrier claims.
    carrierEob = filterToClaimType(searchResults, ClaimType.CARRIER).get(0);

    Assert.assertNotNull(
        TransformerTestUtils.findCareTeamEntryForProviderTaxNumber(
            carrierClaim.getLines().get(0).getProviderTaxNumber(), carrierEob.getCareTeam()));

    // Verify that tax numbers are present for DME claims.
    dmeEob = filterToClaimType(searchResults, ClaimType.DME).get(0);
    Assert.assertNotNull(
        TransformerTestUtils.findCareTeamEntryForProviderTaxNumber(
            dmeClaim.getLines().get(0).getProviderTaxNumber(), dmeEob.getCareTeam()));
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
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
        Arrays.asList("_lastUpdated=lt" + earlyDateTime, "_lastUpdated=le" + earlyDateTime);
    testLastUpdatedUrls(fhirClient, beneficiary.getBeneficiaryId(), emptyUrls, 0);
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
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    String claimId = findFirstCarrierClaim(loadedRecords).getClaimId();
    String beneId = findFirstBeneficary(loadedRecords).getBeneficiaryId();
    clearCarrierClaimLastUpdated(claimId);

    // Find all EOBs without lastUpdated
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();
    Bundle searchAll =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(ExplanationOfBenefit.PATIENT.hasId(TransformerUtils.buildPatientId(beneId)))
            .returnBundle(Bundle.class)
            .execute();
    Assert.assertEquals(
        "Expect null lastUpdated fields to map to the FALLBACK_LAST_UPDATED",
        Date.from(TransformerConstants.FALLBACK_LAST_UPDATED),
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
    Assert.assertEquals(
        "Expect null lastUpdated fields to map to the FALLBACK_LAST_UPDATED",
        Date.from(TransformerConstants.FALLBACK_LAST_UPDATED),
        filterToClaimType(searchWithLessThan, ClaimType.CARRIER).get(0).getMeta().getLastUpdated());
    Assert.assertEquals(
        "Expected the search for lastUpdated <= now() to include resources with fallback lastUpdated values",
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

  @Test
  public void searchEobWithServiceDate() {
    Beneficiary beneficiary = loadSampleA();
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();
    // For SampleA data, we have the following service dates
    // HHA 23-JUN-2015
    // Hospice 30-JAN-2014
    // Inpatient 27-JAN-2016
    // Outpatient 24-JAN-2011
    // SNF 18-DEC-2013
    // Carrier 27-OCT-1999
    // DME 03-FEB-2014
    // PDE 12-MAY-2015

    // TestName:serviceDate:ExpectedCount
    List<Triple<String, String, Integer>> testCases =
        ImmutableList.of(
            ImmutableTriple.of("No service date filter", "", 8),
            ImmutableTriple.of(
                "Contains all", "service-date=ge1999-10-27&service-date=le2016-01-27", 8),
            ImmutableTriple.of("Contains none - upper bound", "service-date=gt2016-01-27", 0),
            ImmutableTriple.of("Contains none - lower bound", "service-date=lt1999-10-27", 0),
            ImmutableTriple.of(
                "Exclusive check - no earliest/latest",
                "service-date=gt1999-10-27&service-date=lt2016-01-27",
                6),
            ImmutableTriple.of(
                "Year end 2015 inclusive check (using last day of 2015)",
                "service-date=le2015-12-31",
                7),
            ImmutableTriple.of(
                "Year end 2014 exclusive check (using first day of 2015)",
                "service-date=lt2015-01-01",
                5));

    testCases.forEach(
        testCase -> {
          Bundle bundle =
              fetchWithServiceDate(
                  fhirClient, beneficiary.getBeneficiaryId(), testCase.getMiddle());
          Assert.assertNotNull(bundle);
          Assert.assertEquals(
              testCase.getLeft(), testCase.getRight().intValue(), bundle.getTotal());
        });
  }

  /**
   * Ensures that {@link PipelineTestUtils#truncateTablesInDataSource()} is called after each test
   * case.
   */
  @After
  public void cleanDatabaseServerAfterEachTestCase() {
    PipelineTestUtils.get().truncateTablesInDataSource();
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
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

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
    ServerTestUtils.get()
        .doTransaction(
            (em) -> {
              em.createQuery("update CarrierClaim set lastUpdated=null where claimId=:claimId")
                  .setParameter("claimId", claimId)
                  .executeUpdate();
            });
  }

  private Bundle fetchWithServiceDate(
      IGenericClient fhirClient, String id, String serviceEndParam) {
    String url =
        "ExplanationOfBenefit?patient=Patient%2F"
            + id
            + (serviceEndParam.isEmpty() ? "" : "&" + serviceEndParam)
            + "&_format=application%2Fjson%2Bfhir";
    return fhirClient.search().byUrl(url).returnBundle(Bundle.class).execute();
  }
}
