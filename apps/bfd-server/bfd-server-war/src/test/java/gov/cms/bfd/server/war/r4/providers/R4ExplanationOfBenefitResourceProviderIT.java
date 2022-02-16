package gov.cms.bfd.server.war.r4.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider;
import gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcherTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.AdjudicationComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.BenefitBalanceComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.BenefitComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.SupportingInformationComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.TotalComponent;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Money;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** R4ExplanationOfBenefitResourceProviderIT. */
public final class R4ExplanationOfBenefitResourceProviderIT {

  public static final String EXCLUDE_SAMHSA_PARAM = "excludeSAMHSA";

  /**
   * Ensures that {@link PipelineTestUtils#truncateTablesInDataSource()} is called once to make sure
   * that any existing data is deleted from the tables before running the test suite.
   */
  @BeforeAll
  public static void cleanupDatabaseBeforeTestSuite() {
    PipelineTestUtils.get().truncateTablesInDataSource();
  }

  /**
   * Ensures that {@link PipelineTestUtils#truncateTablesInDataSource()} is called after each test
   * case.
   */
  @AfterEach
  public void cleanDatabaseServerAfterEachTestCase() {
    PipelineTestUtils.get().truncateTablesInDataSource();
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.r4.model.IdType)}
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
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

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
            .withId(TransformerUtilsV2.buildEobId(ClaimTypeV2.CARRIER, claim.getClaimId()))
            .execute();

    // Compare result to transformed EOB
    compareEob(ClaimTypeV2.CARRIER, eob, loadedRecords);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.r4.model.IdType)}
   * works as expected for a {@link CarrierClaim}-derived {@link ExplanationOfBenefit} that does not
   * exist in the DB.
   */
  @Test
  public void readEobForMissingCarrierClaim() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    assertThrows(
        ResourceNotFoundException.class,
        () -> {
          // No data is loaded, so this should return nothing.
          fhirClient
              .read()
              .resource(ExplanationOfBenefit.class)
              .withId(TransformerUtilsV2.buildEobId(ClaimTypeV2.CARRIER, "1234"))
              .execute();
        });
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.r4.model.IdType)}
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
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

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
            .withId(TransformerUtilsV2.buildEobId(ClaimTypeV2.DME, claim.getClaimId()))
            .execute();

    assertNotNull(eob);
    // Compare result to transformed EOB
    compareEob(ClaimTypeV2.DME, eob, loadedRecords);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.r4.model.IdType)}
   * works as expected for a {@link DMEClaim}-derived {@link ExplanationOfBenefit} that does not
   * exist in the DB.
   */
  @Test
  public void readEobForMissingDMEClaim() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    assertThrows(
        ResourceNotFoundException.class,
        () -> {
          // No data is loaded, so this should return nothing.
          fhirClient
              .read()
              .resource(ExplanationOfBenefit.class)
              .withId(TransformerUtilsV2.buildEobId(ClaimTypeV2.DME, "1234"))
              .execute();
        });
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.r4.model.IdType)}
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
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

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
            .withId(TransformerUtilsV2.buildEobId(ClaimTypeV2.HHA, claim.getClaimId()))
            .execute();

    assertNotNull(eob);

    // Compare result to transformed EOB
    compareEob(ClaimTypeV2.HHA, eob, loadedRecords);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.r4.model.IdType)}
   * works as expected for an {@link HHAClaim}-derived {@link ExplanationOfBenefit} that does not
   * exist in the DB.
   */
  @Test
  public void readEobForMissingHHAClaim() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    assertThrows(
        ResourceNotFoundException.class,
        () -> {
          // No data is loaded, so this should return nothing.
          fhirClient
              .read()
              .resource(ExplanationOfBenefit.class)
              .withId(TransformerUtilsV2.buildEobId(ClaimTypeV2.HHA, "1234"))
              .execute();
        });
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.r4.model.IdType)}
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
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

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
            .withId(TransformerUtilsV2.buildEobId(ClaimTypeV2.HOSPICE, claim.getClaimId()))
            .execute();

    assertNotNull(eob);
    // Compare result to transformed EOB
    compareEob(ClaimTypeV2.HOSPICE, eob, loadedRecords);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.r4.model.IdType)}
   * works as expected for a {@link HospiceClaim}-derived {@link ExplanationOfBenefit} that does not
   * exist in the DB.
   */
  @Test
  public void readEobForMissingHospiceClaim() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    assertThrows(
        ResourceNotFoundException.class,
        () -> {
          // No data is loaded, so this should return nothing.
          fhirClient
              .read()
              .resource(ExplanationOfBenefit.class)
              .withId(TransformerUtilsV2.buildEobId(ClaimTypeV2.HOSPICE, "1234"))
              .execute();
        });
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.r4.model.IdType)}
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
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

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
            .withId(TransformerUtilsV2.buildEobId(ClaimTypeV2.INPATIENT, claim.getClaimId()))
            .execute();

    assertNotNull(eob);
    // Compare result to transformed EOB
    compareEob(ClaimTypeV2.INPATIENT, eob, loadedRecords);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.r4.model.IdType)}
   * works as expected for an {@link InpatientClaim}-derived {@link ExplanationOfBenefit} that does
   * not exist in the DB.
   */
  @Test
  public void readEobForMissingInpatientClaim() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    assertThrows(
        ResourceNotFoundException.class,
        () -> {
          // No data is loaded, so this should return nothing.
          fhirClient
              .read()
              .resource(ExplanationOfBenefit.class)
              .withId(TransformerUtilsV2.buildEobId(ClaimTypeV2.INPATIENT, "1234"))
              .execute();
        });
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.r4.model.IdType)}
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
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

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
            .withId(TransformerUtilsV2.buildEobId(ClaimTypeV2.OUTPATIENT, claim.getClaimId()))
            .execute();

    assertNotNull(eob);
    // Compare result to transformed EOB
    assertEobEquals(
        OutpatientClaimTransformerV2.transform(
            PipelineTestUtils.get().getPipelineApplicationState().getMetrics(),
            claim,
            Optional.of(false)),
        eob);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.r4.model.IdType)}
   * works as expected for an {@link OutpatientClaim}-derived {@link ExplanationOfBenefit} that does
   * not exist in the DB.
   */
  @Test
  public void readEobForMissingOutpatientClaim() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    assertThrows(
        ResourceNotFoundException.class,
        () -> {
          // No data is loaded, so this should return nothing.
          fhirClient
              .read()
              .resource(ExplanationOfBenefit.class)
              .withId(TransformerUtilsV2.buildEobId(ClaimTypeV2.OUTPATIENT, "1234"))
              .execute();
        });
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.r4.model.IdType)}
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
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

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
            .withId(TransformerUtilsV2.buildEobId(ClaimTypeV2.PDE, claim.getEventId()))
            .execute();

    assertNotNull(eob);
    // Compare result to transformed EOB
    compareEob(ClaimTypeV2.PDE, eob, loadedRecords);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.r4.model.IdType)}
   * works as expected for a {@link PartDEvent}-derived {@link ExplanationOfBenefit} that does not
   * exist in the DB.
   */
  @Test
  public void readEobForMissingPartDEvent() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    assertThrows(
        ResourceNotFoundException.class,
        () -> {
          // No data is loaded, so this should return nothing.
          fhirClient
              .read()
              .resource(ExplanationOfBenefit.class)
              .withId(TransformerUtilsV2.buildEobId(ClaimTypeV2.PDE, "1234"))
              .execute();
        });
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.r4.model.IdType)}
   * works as expected for a {@link PartDEvent}-derived {@link ExplanationOfBenefit} that does not
   * exist in the DB using a negative ID.
   */
  @Test
  public void readEobForMissingNegativePartDEvent() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    assertThrows(
        ResourceNotFoundException.class,
        () -> {
          // No data is loaded, so this should return nothing. Tests negative ID will pass regex
          // pattern.
          fhirClient
              .read()
              .resource(ExplanationOfBenefit.class)
              .withId(TransformerUtilsV2.buildEobId(ClaimTypeV2.PDE, "-1234"))
              .execute();
        });
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.r4.model.IdType)}
   * works as expected for a {@link PartDEvent}-derived {@link ExplanationOfBenefit} that has an
   * invalid {@link
   * gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider#IdParam} parameter.
   */
  @Test
  public void readEobForInvalidIdParamPartDEvent() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    assertThrows(
        InvalidRequestException.class,
        () -> {
          // The IdParam is not valid, so this should return an exception.
          fhirClient
              .read()
              .resource(ExplanationOfBenefit.class)
              .withId(TransformerUtilsV2.buildEobId(ClaimTypeV2.PDE, "-1?234"))
              .execute();
        });
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.r4.model.IdType)}
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
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

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
            .withId(TransformerUtilsV2.buildEobId(ClaimTypeV2.SNF, claim.getClaimId()))
            .execute();

    assertNotNull(eob);
    // Compare result to transformed EOB
    compareEob(ClaimTypeV2.SNF, eob, loadedRecords);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.r4.model.IdType)}
   * works as expected for an {@link SNFClaim}-derived {@link ExplanationOfBenefit} that does not
   * exist in the DB.
   */
  @Test
  public void readEobForMissingSNFClaim() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    assertThrows(
        ResourceNotFoundException.class,
        () -> {
          // No data is loaded, so this should return nothing.
          fhirClient
              .read()
              .resource(ExplanationOfBenefit.class)
              .withId(TransformerUtilsV2.buildEobId(ClaimTypeV2.SNF, "1234"))
              .execute();
        });
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider#findByPatient} works
   * as expected for a {@link Patient} that does exist in the DB.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void searchForEobsByExistingPatient() throws FHIRException {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

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
            .where(
                ExplanationOfBenefit.PATIENT.hasId(TransformerUtilsV2.buildPatientId(beneficiary)))
            .returnBundle(Bundle.class)
            .execute();

    assertNotNull(searchResults);

    /*
     * Verify the bundle contains a key for total and that the value matches the
     * number of entries in the bundle
     */
    assertEquals(
        loadedRecords.stream()
            .filter(r -> !(r instanceof Beneficiary))
            .filter(r -> !(r instanceof BeneficiaryHistory))
            .filter(r -> !(r instanceof MedicareBeneficiaryIdHistory))
            .count(),
        searchResults.getTotal());

    /*
     * Verify that no paging links exist in the bundle.
     */
    assertNull(searchResults.getLink(Constants.LINK_NEXT));
    assertNull(searchResults.getLink(Constants.LINK_PREVIOUS));
    assertNull(searchResults.getLink(Constants.LINK_FIRST));
    assertNull(searchResults.getLink(Constants.LINK_LAST));

    /*
     * Verify that each of the expected claims (one for every claim type) is present
     * and looks correct.
     */

    assertEachEob(searchResults, loadedRecords);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider#findByPatient} works
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
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

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
            .where(
                ExplanationOfBenefit.PATIENT.hasId(TransformerUtilsV2.buildPatientId(beneficiary)))
            .count(3)
            .returnBundle(Bundle.class)
            .execute();

    assertNotNull(searchResults);
    assertEquals(3, searchResults.getEntry().size());

    /*
     * Verify that accessing all next links, eventually leading to the last page,
     * will not encounter an IndexOutOfBoundsException.
     */
    while (searchResults.getLink(Constants.LINK_NEXT) != null) {
      searchResults = fhirClient.loadPage().next(searchResults).execute();
      assertNotNull(searchResults);
      assertTrue(searchResults.hasEntry());
    }
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider#findByPatient} works
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
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

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
            .where(
                ExplanationOfBenefit.PATIENT.hasId(TransformerUtilsV2.buildPatientId(beneficiary)))
            .returnBundle(Bundle.class)
            .execute();

    assertNotNull(searchResults);

    /*
     * Verify that no paging links exist in the bundle.
     */
    assertNull(searchResults.getLink(Constants.LINK_NEXT));
    assertNull(searchResults.getLink(Constants.LINK_PREVIOUS));
    assertNull(searchResults.getLink(Constants.LINK_FIRST));
    assertNull(searchResults.getLink(Constants.LINK_LAST));

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

    assertNotNull(pagedResults);

    /*
     * Verify that paging links exist in this paged bundle.
     */
    assertNull(pagedResults.getLink(Constants.LINK_NEXT));
    assertNotNull(pagedResults.getLink(Constants.LINK_PREVIOUS));
    assertNotNull(pagedResults.getLink(Constants.LINK_FIRST));
    assertNotNull(pagedResults.getLink(Constants.LINK_LAST));

    /*
     * Add the entries in the paged results to a list and verify that only the last
     * 4 entries in the original searchResults were returned in the pagedResults.
     */
    List<IBaseResource> pagedEntries = new ArrayList<>();
    pagedResults.getEntry().forEach(e -> pagedEntries.add(e.getResource()));
    assertEquals(4, pagedEntries.size());
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider#findByPatient} works
   * as expected for a {@link Patient} that does exist in the DB, with paging on a page size of 0.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void searchForEobsByExistingPatientWithPageSizeZero() throws FHIRException {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

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
            .where(
                ExplanationOfBenefit.PATIENT.hasId(TransformerUtilsV2.buildPatientId(beneficiary)))
            .count(0)
            .returnBundle(Bundle.class)
            .execute();

    assertNotNull(searchResults);

    /*
     * Verify the bundle contains a key for total and that the value matches the
     * number of entries in the bundle
     */
    assertEquals(
        loadedRecords.stream()
            .filter(r -> !(r instanceof Beneficiary))
            .filter(r -> !(r instanceof BeneficiaryHistory))
            .filter(r -> !(r instanceof MedicareBeneficiaryIdHistory))
            .count(),
        searchResults.getTotal());

    /*
     * Verify that no paging links exist in the bundle.
     */
    assertNull(searchResults.getLink(Constants.LINK_NEXT));
    assertNull(searchResults.getLink(Constants.LINK_PREVIOUS));
    assertNull(searchResults.getLink(Constants.LINK_FIRST));
    assertNull(searchResults.getLink(Constants.LINK_LAST));

    /*
     * Verify that each of the expected claims (one for every claim type) is present
     * and looks correct.
     */

    assertEachEob(searchResults, loadedRecords);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider#findByPatient} works
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
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

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
            .where(
                ExplanationOfBenefit.PATIENT.hasId(TransformerUtilsV2.buildPatientId(beneficiary)))
            .count(50)
            .returnBundle(Bundle.class)
            .execute();

    assertNotNull(searchResults);

    /*
     * Verify the bundle contains a key for total and that the value matches the
     * number of entries in the bundle
     */
    assertEquals(
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
    assertNotNull(searchResults.getLink(Constants.LINK_FIRST));
    assertNotNull(searchResults.getLink(Constants.LINK_LAST));
    assertNull(searchResults.getLink(Constants.LINK_NEXT));
    assertNull(searchResults.getLink(Constants.LINK_PREVIOUS));

    /*
     * Verify that each of the expected claims (one for every claim type) is present
     * and looks correct.
     */

    assertEachEob(searchResults, loadedRecords);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider#findByPatient} works
   * as expected for a {@link Patient} that does exist in the DB, with paging, using negative values
   * for page size and start index parameters. This test expects to receive a BadRequestException,
   * as negative values should result in an HTTP 400.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void searchForEobsWithPagingWithNegativePagingParameters() throws FHIRException {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

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
            .where(
                ExplanationOfBenefit.PATIENT.hasId(TransformerUtilsV2.buildPatientId(beneficiary)))
            .returnBundle(Bundle.class)
            .execute();

    assertNotNull(searchResults);

    /*
     * Access a created link of this bundle, providing the startIndex but not the
     * pageSize (count).
     */
    assertThrows(
        InvalidRequestException.class,
        () -> {
          fhirClient
              .loadPage()
              .byUrl(searchResults.getLink(Bundle.LINK_SELF).getUrl() + "&startIndex=-1")
              .andReturnBundle(Bundle.class)
              .execute();
        });
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider#findByPatient(ca.uhn.fhir.rest.param.ReferenceParam)}
   * works as expected for a {@link Patient} that does not exist in the DB.
   */
  @Test
  public void searchForEobsByMissingPatient() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    // No data is loaded, so this should return 0 matches.
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(ExplanationOfBenefit.PATIENT.hasId(new IdDt("Patient", "1234")))
            .returnBundle(Bundle.class)
            .execute();

    assertNotNull(searchResults);
    assertEquals(0, searchResults.getTotal());
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
    carrierRifRecord =
        entityManager.find(CarrierClaim.class, String.valueOf(carrierRifRecord.getClaimId()));
    carrierRifRecord.setDiagnosis2Code(
        Optional.of(Stu3EobSamhsaMatcherTest.SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE));
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
    inpatientRifRecord =
        entityManager.find(InpatientClaim.class, String.valueOf(inpatientRifRecord.getClaimId()));
    inpatientRifRecord.setDiagnosis2Code(
        Optional.of(Stu3EobSamhsaMatcherTest.SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE));
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
        entityManager.find(OutpatientClaim.class, String.valueOf(outpatientRifRecord.getClaimId()));
    outpatientRifRecord.setDiagnosis2Code(
        Optional.of(Stu3EobSamhsaMatcherTest.SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE));
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
    hhaRifRecord = entityManager.find(HHAClaim.class, String.valueOf(hhaRifRecord.getClaimId()));
    hhaRifRecord.setDiagnosis2Code(
        Optional.of(Stu3EobSamhsaMatcherTest.SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE));
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
    snfRifRecord = entityManager.find(SNFClaim.class, String.valueOf(snfRifRecord.getClaimId()));
    snfRifRecord.setDiagnosis2Code(
        Optional.of(Stu3EobSamhsaMatcherTest.SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE));
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
    hospiceRifRecord =
        entityManager.find(HospiceClaim.class, String.valueOf(hospiceRifRecord.getClaimId()));
    hospiceRifRecord.setDiagnosis2Code(
        Optional.of(Stu3EobSamhsaMatcherTest.SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE));
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
    dmeRifRecord = entityManager.find(DMEClaim.class, String.valueOf(dmeRifRecord.getClaimId()));
    dmeRifRecord.setDiagnosis2Code(
        Optional.of(Stu3EobSamhsaMatcherTest.SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE));
    dmeRifRecord.setDiagnosis2CodeVersion(Optional.of('9'));
    entityManager.merge(dmeRifRecord);
    entityManager.getTransaction().commit();
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.R4ExplanationOfBenefitResourceProvider#findByPatient} with
   * <code>excludeSAMHSA=true</code> properly filters out SAMHSA-related claims.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void searchForSamhsaEobsWithExcludeSamhsaTrue() throws FHIRException {
    Beneficiary beneficiary = loadSampleAWithSamhsa();

    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    Bundle searchResults =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(
                ExplanationOfBenefit.PATIENT.hasId(
                    TransformerUtilsV2.buildPatientId(beneficiary.getBeneficiaryId())))
            .and(new StringClientParam(EXCLUDE_SAMHSA_PARAM).matches().value("true"))
            .returnBundle(Bundle.class)
            .execute();
    assertNotNull(searchResults);
    for (ClaimTypeV2 claimType : ClaimTypeV2.values()) {
      /*
       * SAMHSA fields are present on all claim types except for PDE so we should not
       * get any claims back in the results except for PDE.
       */
      if (claimType == ClaimTypeV2.PDE) {
        assertEquals(1, filterToClaimType(searchResults, claimType).size());
      } else {
        assertEquals(0, filterToClaimType(searchResults, claimType).size());
      }
    }
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.R4ExplanationOfBenefitResourceProvider#findByPatient} with
   * <code>excludeSAMHSA=false</code> does not filter out SAMHSA-related claims.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void searchForSamhsaEobsWithExcludeSamhsaFalse() throws FHIRException {
    Beneficiary beneficiary = loadSampleAWithSamhsa();

    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    Bundle searchResults =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(
                ExplanationOfBenefit.PATIENT.hasId(
                    TransformerUtilsV2.buildPatientId(beneficiary.getBeneficiaryId())))
            .and(new StringClientParam(EXCLUDE_SAMHSA_PARAM).matches().value("false"))
            .returnBundle(Bundle.class)
            .execute();
    assertNotNull(searchResults);
    for (ClaimTypeV2 claimType : ClaimTypeV2.values()) {
      // Without filtering we expect one claim for each claim type.
      assertEquals(1, filterToClaimType(searchResults, claimType).size());
    }
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.R4ExplanationOfBenefitResourceProvider#findByPatient} with
   * <code>excludeSAMHSA=true</code> properly returns claims that are not SAMHSA-related.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void searchForNonSamhsaEobsWithExcludeSamhsaTrue() throws FHIRException {
    // Load the SAMPLE_A resources normally.
    Beneficiary beneficiary = loadSampleA();

    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    Bundle searchResults =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(
                ExplanationOfBenefit.PATIENT.hasId(
                    TransformerUtilsV2.buildPatientId(beneficiary.getBeneficiaryId())))
            .and(new StringClientParam(EXCLUDE_SAMHSA_PARAM).matches().value("true"))
            .returnBundle(Bundle.class)
            .execute();
    assertNotNull(searchResults);
    for (ClaimTypeV2 claimType : ClaimTypeV2.values()) {
      // None of the claims are SAMHSA so we expect one record per claim type in the results.
      assertEquals(
          1,
          filterToClaimType(searchResults, claimType).size(),
          String.format("Verify claims of type '%s' are present", claimType));
    }
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.R4ExplanationOfBenefitResourceProvider#findByPatient} with
   * <code>excludeSAMHSA=false</code> properly returns claims that are not SAMHSA-related.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void searchForNonSamhsaEobsWithExcludeSamhsaFalse() throws FHIRException {
    // Load the SAMPLE_A resources normally.
    Beneficiary beneficiary = loadSampleA();

    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    Bundle searchResults =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(
                ExplanationOfBenefit.PATIENT.hasId(
                    TransformerUtilsV2.buildPatientId(beneficiary.getBeneficiaryId())))
            .and(new StringClientParam(EXCLUDE_SAMHSA_PARAM).matches().value("false"))
            .returnBundle(Bundle.class)
            .execute();
    assertNotNull(searchResults);
    for (ClaimTypeV2 claimType : ClaimTypeV2.values()) {
      // None of the claims are SAMHSA so we expect one record per claim type in the results.
      assertEquals(
          1,
          filterToClaimType(searchResults, claimType).size(),
          String.format("Verify claims of type '%s' are present", claimType));
    }
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider#findByPatient(ca.uhn.fhir.rest.param.ReferenceParam)}
   * handles the {@link ExplanationOfBenefitResourceProvider#HEADER_NAME_INCLUDE_TAX_NUMBERS} header
   * properly.
   *
   * @throws FHIRException (indicates test failure)
   */
  // TODO: Fix this test .. it isn't working. Tax number is not showing up.
  @Test
  public void searchForEobsIncludeTaxNumbersHandling() throws FHIRException {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();
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
            .where(
                ExplanationOfBenefit.PATIENT.hasId(TransformerUtilsV2.buildPatientId(beneficiary)))
            .returnBundle(Bundle.class)
            .execute();
    assertNotNull(searchResults);

    // Verify that tax numbers aren't present for carrier claims.
    carrierEob = filterToClaimType(searchResults, ClaimTypeV2.CARRIER).get(0);
    assertNull(
        TransformerTestUtilsV2.findCareTeamEntryForProviderTaxNumber(
            carrierClaim.getLines().get(0).getProviderTaxNumber(), carrierEob.getCareTeam()));

    // Verify that tax numbers aren't present for DME claims.
    dmeEob = filterToClaimType(searchResults, ClaimTypeV2.DME).get(0);
    assertNull(
        TransformerTestUtilsV2.findCareTeamEntryForProviderTaxNumber(
            dmeClaim.getLines().get(0).getProviderTaxNumber(), dmeEob.getCareTeam()));

    RequestHeaders requestHeader =
        RequestHeaders.getHeaderWrapper(CommonHeaders.HEADER_NAME_INCLUDE_TAX_NUMBERS, "true");
    fhirClient = ServerTestUtils.get().createFhirClientWithHeadersV2(requestHeader);

    searchResults =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(
                ExplanationOfBenefit.PATIENT.hasId(TransformerUtilsV2.buildPatientId(beneficiary)))
            .returnBundle(Bundle.class)
            .execute();
    assertNotNull(searchResults);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider#findByPatient(ca.uhn.fhir.rest.param.ReferenceParam)}
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
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

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
            .where(
                ExplanationOfBenefit.PATIENT.hasId(TransformerUtilsV2.buildPatientId(beneficiary)))
            .where(new TokenClientParam("type").exactly().code(ClaimTypeV2.PDE.name()))
            .returnBundle(Bundle.class)
            .execute();

    assertNotNull(searchResults);

    /*
     * Verify the bundle contains a key for total and that the value matches the
     * number of entries in the bundle
     */
    assertEquals(
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

    // Compare result to transformed EOB
    assertEobEquals(
        PartDEventTransformerV2.transform(
            PipelineTestUtils.get().getPipelineApplicationState().getMetrics(),
            partDEvent,
            Optional.of(false)),
        filterToClaimType(searchResults, ClaimTypeV2.PDE).get(0));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider#findByPatient} works
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
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

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

    testLastUpdatedUrls(fhirClient, String.valueOf(beneficiary.getBeneficiaryId()), allUrls, 8);

    // Empty searches
    List<String> emptyUrls =
        Arrays.asList("_lastUpdated=lt" + earlyDateTime, "_lastUpdated=le" + earlyDateTime);
    testLastUpdatedUrls(fhirClient, String.valueOf(beneficiary.getBeneficiaryId()), emptyUrls, 0);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider#findByPatient} works
   * as with a lastUpdated parameter after yesterday.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void searchEobWithLastUpdatedAndPagination() throws FHIRException {
    Beneficiary beneficiary = loadSampleA();
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    // Search with lastUpdated range between yesterday and now
    int expectedCount = 3;
    Date yesterday = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
    Date now = new Date();
    DateRangeParam afterYesterday = new DateRangeParam(yesterday, now);
    Bundle searchResultsAfter =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(
                ExplanationOfBenefit.PATIENT.hasId(TransformerUtilsV2.buildPatientId(beneficiary)))
            .lastUpdated(afterYesterday)
            .count(expectedCount)
            .returnBundle(Bundle.class)
            .execute();
    assertEquals(
        expectedCount,
        searchResultsAfter.getEntry().size(),
        "Expected number resources return to be equal to count");

    // Check self url
    String selfLink = searchResultsAfter.getLink(IBaseBundle.LINK_SELF).getUrl();
    assertTrue(selfLink.contains("lastUpdated"));

    // Check next bundle
    String nextLink = searchResultsAfter.getLink(IBaseBundle.LINK_NEXT).getUrl();
    assertTrue(nextLink.contains("lastUpdated"));
    Bundle nextResults = fhirClient.search().byUrl(nextLink).returnBundle(Bundle.class).execute();
    assertEquals(
        expectedCount,
        nextResults.getEntry().size(),
        "Expected number resources return to be equal to count");
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider#findByPatient} works
   * as with a lastUpdated parameter after yesterday.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void searchEobWithLastUpdatedAndPaginationAndType() throws FHIRException {
    Beneficiary beneficiary = loadSampleA();
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    // Search with lastUpdated range between yesterday and now
    int expectedCount = 3;
    Date yesterday = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
    Date now = new Date();
    DateRangeParam afterYesterday = new DateRangeParam(yesterday, now);
    Bundle searchResultsAfter =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(
                ExplanationOfBenefit.PATIENT.hasId(TransformerUtilsV2.buildPatientId(beneficiary)))
            .lastUpdated(afterYesterday)
            .count(expectedCount)
            .returnBundle(Bundle.class)
            .execute();
    assertEquals(
        expectedCount,
        searchResultsAfter.getEntry().size(),
        "Expected number resources return to be equal to count");

    // Check self url
    String selfLink = searchResultsAfter.getLink(IBaseBundle.LINK_SELF).getUrl();
    assertTrue(selfLink.contains("lastUpdated"));

    // Check next bundle
    String nextLink = searchResultsAfter.getLink(IBaseBundle.LINK_NEXT).getUrl();
    assertTrue(nextLink.contains("lastUpdated"));
    Bundle nextResults = fhirClient.search().byUrl(nextLink).returnBundle(Bundle.class).execute();
    assertEquals(
        expectedCount,
        nextResults.getEntry().size(),
        "Expected number resources return to be equal to count");
  }

  /**
   * Verifies that {@link gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider}
   * works as with a null lastUpdated parameter after yesterday.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void searchEobWithNullLastUpdated() throws FHIRException {
    // Load a records and clear the lastUpdated field for one
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    Long claimId =
        loadedRecords.stream()
            .filter(r -> r instanceof CarrierClaim)
            .map(r -> (CarrierClaim) r)
            .findFirst()
            .get()
            .getClaimId();

    Long beneId = findFirstBeneficary(loadedRecords).getBeneficiaryId();

    // Clear lastupdated in the database
    ServerTestUtils.get()
        .doTransaction(
            (em) -> {
              em.createQuery("update CarrierClaim set lastUpdated=null where claimId=:claimId")
                  .setParameter("claimId", String.valueOf(claimId))
                  .executeUpdate();
            });

    // Find all EOBs without lastUpdated
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();
    Bundle searchAll =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(ExplanationOfBenefit.PATIENT.hasId(TransformerUtilsV2.buildPatientId(beneId)))
            .returnBundle(Bundle.class)
            .execute();

    assertEquals(
        Date.from(TransformerConstants.FALLBACK_LAST_UPDATED),
        filterToClaimType(searchAll, ClaimTypeV2.CARRIER).get(0).getMeta().getLastUpdated(),
        "Expect null lastUpdated fields to map to the FALLBACK_LAST_UPDATED");

    // Find all EOBs with < now()
    Bundle searchWithLessThan =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(ExplanationOfBenefit.PATIENT.hasId(TransformerUtilsV2.buildPatientId(beneId)))
            .lastUpdated(new DateRangeParam().setUpperBoundInclusive(new Date()))
            .returnBundle(Bundle.class)
            .execute();

    assertEquals(
        Date.from(TransformerConstants.FALLBACK_LAST_UPDATED),
        filterToClaimType(searchWithLessThan, ClaimTypeV2.CARRIER)
            .get(0)
            .getMeta()
            .getLastUpdated(),
        "Expect null lastUpdated fields to map to the FALLBACK_LAST_UPDATED");

    assertEquals(
        searchAll.getTotal(),
        searchWithLessThan.getTotal(),
        "Expected the search for lastUpdated <= now() to include resources with fallback"
            + " lastUpdated values");

    // Find all EOBs with >= now()-100 seconds
    Bundle searchWithGreaterThan =
        fhirClient
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(ExplanationOfBenefit.PATIENT.hasId(TransformerUtilsV2.buildPatientId(beneId)))
            .lastUpdated(
                new DateRangeParam()
                    .setLowerBoundInclusive(Date.from(Instant.now().minusSeconds(100))))
            .returnBundle(Bundle.class)
            .execute();

    assertEquals(
        searchAll.getTotal() - 1,
        searchWithGreaterThan.getTotal(),
        "Expected the search for lastUpdated >= now()-100 to not include null lastUpdated"
            + " resources");
  }

  /**
   * Verifies that {@link gov.cms.bfd.server.war.r4.providers.ExplanationOfBenefitResourceProvider}
   * works by service date
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void searchEobWithServiceDate() {
    Beneficiary beneficiary = loadSampleA();
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();
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
          assertNotNull(bundle);
          assertEquals(testCase.getRight().intValue(), bundle.getTotal(), testCase.getLeft());
        });
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
   * @return the first Beneficiary*
   */
  private static Beneficiary findFirstBeneficary(List<Object> loadedRecords) {
    return loadedRecords.stream()
        .filter(r -> r instanceof Beneficiary)
        .map(r -> (Beneficiary) r)
        .findFirst()
        .get();
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
      assertEquals(
          expectedValue,
          searchResults.getTotal(),
          String.format("Expected %s to filter resources correctly", lastUpdatedValue));
    }
  }

  /**
   * Fetch a bundle
   *
   * @param fhirClient to use
   * @param id the bene id to use
   * @param lastUpdatedParam to added to the fetch
   * @return the bundle
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
   * Verify that each of the expected claims (one for every claim type) is present and looks
   * correct.
   *
   * @param searchResults the search results
   * @param loadedRecords the loaded records
   */
  private static void assertEachEob(Bundle searchResults, List<Object> loadedRecords) {
    compareEob(ClaimTypeV2.CARRIER, searchResults, loadedRecords);
    compareEob(ClaimTypeV2.DME, searchResults, loadedRecords);
    compareEob(ClaimTypeV2.HHA, searchResults, loadedRecords);
    compareEob(ClaimTypeV2.HOSPICE, searchResults, loadedRecords);
    compareEob(ClaimTypeV2.INPATIENT, searchResults, loadedRecords);
    compareEob(ClaimTypeV2.OUTPATIENT, searchResults, loadedRecords);
    compareEob(ClaimTypeV2.PDE, searchResults, loadedRecords);
    compareEob(ClaimTypeV2.SNF, searchResults, loadedRecords);
  }

  /**
   * Asserts that two Money values ignoring differences like "0" vs "0.0"
   *
   * @param expected the expected
   * @param actual the actual
   */
  private static void assertCurrencyEquals(Money expected, Money actual) {
    assertEquals(expected.getCurrency(), actual.getCurrency());

    // Money will return null instead of auto creating value
    if (expected.hasValue()) {
      assertTrue(actual.hasValue());
      assertEquals(expected.getValue().floatValue(), actual.getValue().floatValue(), 0.0);
    } else {
      // If expected has no value, then actual can't either
      assertFalse(actual.hasValue());
    }
  }

  /**
   * Compares two ExplanationOfBenefit objects in detail while working around serialization issues
   * like comparing "0" and "0.0" or creation differences like using "Quantity" vs "SimpleQuantity"
   *
   * @param expected the expected
   * @param actual the actual
   */
  private static void assertEobEquals(ExplanationOfBenefit expected, ExplanationOfBenefit actual) {
    // ID in the bundle will have `ExplanationOfBenefit/` in front, so make sure the last bit
    // matches up
    assertTrue(actual.getId().endsWith(expected.getId()));

    // Clean them out so we can do a deep compare later
    actual.setId("");
    expected.setId("");

    // If there are any contained resources, they might have lastupdate times in them too
    assertEquals(expected.getContained().size(), actual.getContained().size());
    for (int i = 0; i < expected.getContained().size(); i++) {
      Resource expectedContained = expected.getContained().get(i);
      Resource actualContained = actual.getContained().get(i);

      expectedContained.getMeta().setLastUpdated(null);
      actualContained.getMeta().setLastUpdated(null);

      // TODO: HAPI seems to be inserting the `#` in the ID of the contained element.
      // This is incorrect according to the FHIR spec:
      // https://build.fhir.org/references.html#contained
      // This works around that problem
      assertEquals("#" + expectedContained.getId(), actualContained.getId());

      expectedContained.setId("");
      actualContained.setId("");
    }

    // Dates are not easy to compare so just make sure they are there
    assertNotNull(actual.getMeta().getLastUpdated());

    // We compared all of meta that we care about so get it out of the way
    expected.getMeta().setLastUpdated(null);
    actual.getMeta().setLastUpdated(null);

    // Created is required, but can't be compared
    assertNotNull(actual.getCreated());
    expected.setCreated(null);
    actual.setCreated(null);

    // Extensions may have `valueMoney` elements
    assertEquals(expected.getExtension().size(), actual.getExtension().size());
    for (int i = 0; i < expected.getExtension().size(); i++) {
      Extension expectedEx = expected.getExtension().get(i);
      Extension actualEx = actual.getExtension().get(i);

      // We have to deal with Money objects separately
      if (expectedEx.hasValue() && expectedEx.getValue() instanceof Money) {
        assertTrue(actualEx.getValue() instanceof Money);
        assertCurrencyEquals((Money) expectedEx.getValue(), (Money) actualEx.getValue());

        // Now remove since we validated so we can compare the rest directly
        expectedEx.setValue(null);
        actualEx.setValue(null);
      }
    }

    // SupportingInfo can have `valueQuantity` that has the 0 vs 0.0 issue
    assertEquals(expected.getSupportingInfo().size(), actual.getSupportingInfo().size());
    for (int i = 0; i < expected.getSupportingInfo().size(); i++) {
      SupportingInformationComponent expectedSup = expected.getSupportingInfo().get(i);
      SupportingInformationComponent actualSup = actual.getSupportingInfo().get(i);

      // We have to deal with Money objects separately
      if (expectedSup.hasValueQuantity()) {
        assertTrue(actualSup.hasValueQuantity());
        assertEquals(
            expectedSup.getValueQuantity().getValue().floatValue(),
            actualSup.getValueQuantity().getValue().floatValue(),
            0.0);

        // Now remove since we validated so we can compare the rest directly
        expectedSup.setValue(null);
        actualSup.setValue(null);
      }
    }

    // line items
    assertEquals(expected.getItem().size(), actual.getItem().size());
    for (int i = 0; i < expected.getItem().size(); i++) {
      ItemComponent expectedItem = expected.getItem().get(i);
      ItemComponent actualItem = actual.getItem().get(i);

      // Compare value directly because SimpleQuantity vs Quantity can't be compared
      assertEquals(
          expectedItem.getQuantity().getValue().floatValue(),
          actualItem.getQuantity().getValue().floatValue(),
          0.0);

      expectedItem.setQuantity(null);
      actualItem.setQuantity(null);

      assertEquals(expectedItem.getAdjudication().size(), actualItem.getAdjudication().size());
      for (int j = 0; j < expectedItem.getAdjudication().size(); j++) {
        AdjudicationComponent expectedAdj = expectedItem.getAdjudication().get(j);
        AdjudicationComponent actualAdj = actualItem.getAdjudication().get(j);

        // Here is where we start getting into trouble with "0" vs "0.0", so we do this manually
        if (expectedAdj.hasAmount()) {
          assertNotNull(actualAdj.getAmount());
          assertCurrencyEquals(expectedAdj.getAmount(), actualAdj.getAmount());
        } else {
          // If expected doesn't have an amount, actual shouldn't
          assertFalse(actualAdj.hasAmount());
        }

        // We just checked manually, so null them out and check the rest of the fields
        expectedAdj.setAmount(null);
        actualAdj.setAmount(null);
      }
    }

    // Total has the same problem with values
    assertEquals(expected.getTotal().size(), actual.getTotal().size());
    for (int i = 0; i < expected.getTotal().size(); i++) {
      TotalComponent expectedTot = expected.getTotal().get(i);
      TotalComponent actualTot = actual.getTotal().get(i);

      if (expectedTot.hasAmount()) {
        assertNotNull(actualTot.getAmount());
        assertCurrencyEquals(expectedTot.getAmount(), actualTot.getAmount());
      } else {
        // If expected doesn't have an amount, actual shouldn't
        assertFalse(actualTot.hasAmount());
      }

      expectedTot.setAmount(null);
      actualTot.setAmount(null);
    }

    // Benefit Balance Financial components
    assertEquals(expected.getBenefitBalance().size(), actual.getBenefitBalance().size());
    for (int i = 0; i < expected.getBenefitBalance().size(); i++) {
      BenefitBalanceComponent expectedBen = expected.getBenefitBalance().get(i);
      BenefitBalanceComponent actualBen = actual.getBenefitBalance().get(i);

      assertEquals(expectedBen.getFinancial().size(), actualBen.getFinancial().size());
      for (int j = 0; j < expectedBen.getFinancial().size(); j++) {
        BenefitComponent expectedFinancial = expectedBen.getFinancial().get(j);
        BenefitComponent actualFinancial = actualBen.getFinancial().get(j);

        // Are we dealing with Money?
        if (expectedFinancial.hasUsedMoney()) {
          assertNotNull(actualFinancial.getUsedMoney());
          assertCurrencyEquals(expectedFinancial.getUsedMoney(), actualFinancial.getUsedMoney());

          // Clean up
          expectedFinancial.setUsed(null);
          actualFinancial.setUsed(null);
        }
      }
    }

    // As does payment
    if (expected.hasPayment()) {
      assertTrue(actual.hasPayment());
      assertCurrencyEquals(expected.getPayment().getAmount(), actual.getPayment().getAmount());
    } else {
      // If expected doesn't have an amount, actual shouldn't
      assertFalse(actual.hasPayment());
    }

    expected.getPayment().setAmount(null);
    actual.getPayment().setAmount(null);

    // Now for the grand finale, we can do an `equalsDeep` on the rest
    assertTrue(expected.equalsDeep(actual));
  }

  /**
   * Filter to claim type list.
   *
   * @param bundle the {@link Bundle} to filter
   * @param claimType the {@link gov.cms.bfd.server.war.r4.providers.ClaimType} to use as a filter
   * @return a filtered {@link List} of the {@link ExplanationOfBenefit}s from the specified {@link
   *     Bundle} that match the specified {@link gov.cms.bfd.server.war.r4.providers.ClaimType}
   */
  private static List<ExplanationOfBenefit> filterToClaimType(
      Bundle bundle, ClaimTypeV2 claimType) {
    List<ExplanationOfBenefit> results =
        bundle.getEntry().stream()
            .filter(
                e -> {
                  return e.getResource() instanceof ExplanationOfBenefit;
                })
            .map(e -> (ExplanationOfBenefit) e.getResource())
            .filter(
                e -> {
                  return TransformerTestUtilsV2.isCodeInConcept(
                      e.getType(),
                      TransformerConstants.CODING_SYSTEM_BBAPI_EOB_TYPE,
                      claimType.name());
                })
            .collect(Collectors.toList());
    return results;
  }

  /**
   * Compares two {@link ExplanationOfBenefit} objects, one from a service response and one passed
   * through the transformer
   *
   * @param claimType the claim type
   * @param searchResults the search results
   * @param loadedRecords the loaded records
   */
  public static void compareEob(
      ClaimTypeV2 claimType, ExplanationOfBenefit searchResults, List<Object> loadedRecords) {
    Object claim =
        loadedRecords.stream()
            .filter(r -> claimType.getEntityClass().isInstance(r))
            .map(r -> claimType.getEntityClass().cast(r))
            .findFirst()
            .get();

    assertEobEquals(
        claimType
            .getTransformer()
            .transform(
                PipelineTestUtils.get().getPipelineApplicationState().getMetrics(),
                claim,
                Optional.of(false)),
        searchResults);
  }

  /**
   * Compares two {@link ExplanationOfBenefit} objects where one is in a bundle
   *
   * @param claimType the claim type
   * @param searchResults the search results
   * @param loadedRecords the loaded records
   */
  public static void compareEob(
      ClaimTypeV2 claimType, Bundle searchResults, List<Object> loadedRecords) {
    // Find desired claim in the bundle
    List<ExplanationOfBenefit> eobs = filterToClaimType(searchResults, claimType);

    assertEquals(1, eobs.size());

    compareEob(claimType, eobs.get(0), loadedRecords);
  }

  /**
   * Fetch with service date bundle.
   *
   * @param fhirClient the fhir client
   * @param id the id
   * @param serviceEndParam the service end param
   * @return the bundle
   */
  private Bundle fetchWithServiceDate(IGenericClient fhirClient, Long id, String serviceEndParam) {
    String url =
        "ExplanationOfBenefit?patient=Patient%2F"
            + id
            + (serviceEndParam.isEmpty() ? "" : "&" + serviceEndParam)
            + "&_format=application%2Fjson%2Bfhir";
    return fhirClient.search().byUrl(url).returnBundle(Bundle.class).execute();
  }
}
