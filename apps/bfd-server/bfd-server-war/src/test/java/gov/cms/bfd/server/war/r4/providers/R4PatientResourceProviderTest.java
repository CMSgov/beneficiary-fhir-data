package gov.cms.bfd.server.war.r4.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.server.war.commons.LoadedFilterManager;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Units tests for the {@link R4PatientResourceProvider} that do not require a full fhir setup to
 * validate. Anything we want to validate from the fhir client level should go in {@link
 * R4PatientResourceProviderIT}.
 */
@ExtendWith(MockitoExtension.class)
public class R4PatientResourceProviderTest {

  /** The class under test. */
  R4PatientResourceProvider patientProvider;

  /** The mocked request details. */
  @Mock ServletRequestDetails requestDetails;

  /** The mocked input id value. */
  @Mock IdType patientId;

  /** The Metric registry. */
  @Mock private MetricRegistry metricRegistry;
  /** The Loaded filter manager. */
  @Mock private LoadedFilterManager loadedFilterManager;
  /** The Beneficiary transformer. */
  @Mock private BeneficiaryTransformerV2 beneficiaryTransformerV2;

  /** Sets up the test class. */
  @BeforeEach
  public void setup() {
    patientProvider =
        new R4PatientResourceProvider(
            metricRegistry, loadedFilterManager, beneficiaryTransformerV2);
    lenient().when(patientId.getVersionIdPartAsLong()).thenReturn(null);
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#read} throws an exception for an {@link
   * org.hl7.fhir.r4.model.IdType} that has a null patientId parameter.
   */
  @Test
  public void testPatientReadWhereNullIdExpectException() {
    // pass null as id so the entire object is null
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class, () -> patientProvider.read(null, requestDetails));
    assertEquals("Missing required patient ID", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#read} throws an exception for an {@link
   * org.hl7.fhir.r4.model.IdType} that has a missing patientId parameter.
   */
  @Test
  public void testPatientReadWhereEmptyIdExpectException() {
    // Dont set up patient id to return anything, so our id value is null
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class, () -> patientProvider.read(patientId, requestDetails));
    assertEquals("Missing required patient ID", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#read} throws an exception for an {@link
   * org.hl7.fhir.r4.model.IdType} that has a non-numeric value.
   */
  @Test
  public void testPatientReadWhereNonNumericIdExpectException() {
    when(patientId.getIdPart()).thenReturn("abc");
    when(patientId.getIdPartAsLong()).thenThrow(NumberFormatException.class);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class, () -> patientProvider.read(patientId, requestDetails));
    assertEquals("Patient ID must be a number", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#read} throws an exception for an {@link
   * org.hl7.fhir.r4.model.IdType} that has a version supplied with the patientId parameter, as our
   * read requests do not support versioned requests.
   */
  @Test
  public void testPatientReadWhereVersionedIdExpectException() {
    when(patientId.getIdPart()).thenReturn("1234");
    when(patientId.getVersionIdPartAsLong()).thenReturn(1234L);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class, () -> patientProvider.read(patientId, requestDetails));
    assertEquals("Patient ID must not define a version", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#searchByCoverageContract} throws an exception
   * when searching by contract id where the contract id is not a number.
   */
  @Test
  public void testSearchByCoverageContractWhereNonNumericContractIdExpectException() {

    TokenParam contractId = new TokenParam("2001/PTDCNTRCT10", "abcde");
    TokenParam refYear = new TokenParam("", "abc");

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                patientProvider.searchByCoverageContract(
                    contractId, refYear, null, requestDetails));
    assertEquals("Contract year must be a number.", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#searchByCoverageContract} throws an exception
   * when searching by contract id where the contract id is not a valid length.
   */
  @Test
  public void testSearchByCoverageContractWhereWrongLengthContractIdExpectException() {

    TokenParam contractId = new TokenParam("2001/PTDCNTRCT10", "123");
    TokenParam refYear = new TokenParam("", "2001");

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                patientProvider.searchByCoverageContract(
                    contractId, refYear, null, requestDetails));
    assertEquals(
        "Coverage id is not expected length; value 123 is not expected length 5",
        exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#searchByLogicalId} throws an exception when
   * searching by logical id where the id is blank.
   */
  @Test
  public void testSearchByLogicalIdWhereBlankIdExpectException() {

    TokenParam logicalId = new TokenParam("", "");

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> patientProvider.searchByLogicalId(logicalId, null, null, requestDetails));
    assertEquals("Missing required id value", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#searchByLogicalId} throws an exception when
   * searching by logical id where the id.system is blank.
   */
  @Test
  public void testSearchByLogicalIdWhereNonEmptySystemExpectException() {

    TokenParam logicalId = new TokenParam("system", "1234");

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> patientProvider.searchByLogicalId(logicalId, null, null, requestDetails));
    assertEquals(
        "System is unsupported here and should not be set (system)",
        exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#searchByIdentifier} throws an exception when
   * searching by identifier where the search hash is empty.
   */
  @Test
  public void testSearchByIdentifierIdWhereEmptyHashExpectException() {

    when(requestDetails.getHeader(any())).thenReturn("");
    TokenParam identifier = new TokenParam(TransformerConstants.CODING_BBAPI_BENE_MBI_HASH, "");

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> patientProvider.searchByIdentifier(identifier, null, null, requestDetails));
    assertEquals("Hash value cannot be null/empty", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#searchByIdentifier} throws an exception when
   * searching by identifier where the search system is not supported.
   */
  @Test
  public void testSearchByIdentifierIdWhereBadSystemExpectException() {

    TokenParam identifier = new TokenParam("bad-system", "4ffa4tfgd4ggddgh4h");

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> patientProvider.searchByIdentifier(identifier, null, null, requestDetails));
    assertEquals("Unsupported identifier system: bad-system", exception.getLocalizedMessage());
  }
}
