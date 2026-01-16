package gov.cms.bfd.server.war.r4.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.entities.BeneficiaryHistory;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.CommonHeaders;
import gov.cms.bfd.server.war.commons.LoadedFilterManager;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.SingularAttribute;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Integration tests for {@link R4PatientResourceProvider}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public final class R4PatientResourceProviderIT {

  /** The Patient resource provider. */
  private R4PatientResourceProvider patientProvider;

  /** The mocked request details. */
  @Mock private ServletRequestDetails requestDetails;

  /** The mocked input id value. */
  @Mock IdType patientId;

  /** The Metric registry. */
  @Mock private MetricRegistry metricRegistry;

  /** The Loaded filter manager. */
  @Mock private LoadedFilterManager loadedFilterManager;

  /** The mock entity manager for mocking database calls. */
  @Mock private EntityManager entityManager;

  /** The Beneficiary transformer. */
  private BeneficiaryTransformerV2 beneficiaryTransformer;

  /** The mock metric timer. */
  @Mock Timer mockTimer;

  /** The mock metric timer context (used to stop the metric). */
  @Mock Timer.Context mockTimerContext;

  /** The mock query, for mocking DB returns. */
  @Mock TypedQuery mockQuery;

  /** The mock query, for mocking native function call. */
  @Mock TypedQuery mockQueryFunction;

  /** The Test data bene. */
  private Beneficiary testBene;

  /** Test return Patient. */
  @Mock private Patient testPatient;

  /** Test made-up hash to use for the mbi hash. * */
  private static final String TEST_HASH =
      "7004708ca44c2ff45b663ef661059ac98131ccb90b4a7e53917e3af7f50c4c56";

  /** Sets the test resources up. */
  @BeforeEach
  public void setup() {
    beneficiaryTransformer = new BeneficiaryTransformerV2(metricRegistry, false);

    patientProvider =
        new R4PatientResourceProvider(metricRegistry, loadedFilterManager, beneficiaryTransformer);
    patientProvider.setEntityManager(entityManager);

    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    testBene =
        parsedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();

    when(patientId.getIdPart()).thenReturn(String.valueOf(testBene.getBeneficiaryId()));
    when(patientId.getVersionIdPartAsLong()).thenReturn(null);

    // entity manager mocking
    mockEntityManager();

    // metrics mocking
    when(metricRegistry.timer(any())).thenReturn(mockTimer);
    when(mockTimer.time()).thenReturn(mockTimerContext);

    // transformer mocking
    when(testPatient.getId()).thenReturn("123test");

    setupLastUpdatedMocks();

    mockHeaders();
  }

  /** Sets up the last updated mocks. */
  private void setupLastUpdatedMocks() {
    Meta mockMeta = mock(Meta.class);
    when(mockMeta.getLastUpdated())
        .thenReturn(Date.from(Instant.now().minus(1, ChronoUnit.SECONDS)));
    when(testPatient.getMeta()).thenReturn(mockMeta);
    when(loadedFilterManager.getTransactionTime()).thenReturn(Instant.now());
  }

  /** Mocks the default header values. */
  private void mockHeaders() {
    when(requestDetails.getHeader(CommonHeaders.HEADER_NAME_INCLUDE_ADDRESS_FIELDS))
        .thenReturn("false");
    // We dont use this anymore on v2, so set it to false since everything should work regardless of
    // its value
    when(requestDetails.getHeader(CommonHeaders.HEADER_NAME_INCLUDE_IDENTIFIERS))
        .thenReturn("false");
  }

  /** Sets up the default entity manager mocks. */
  private void mockEntityManager() {
    CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
    CriteriaQuery<Beneficiary> mockCriteria = mock(CriteriaQuery.class);
    Root<Beneficiary> root = mock(Root.class);
    Path mockPath = mock(Path.class);
    Subquery mockSubquery = mock(Subquery.class);
    when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
    doReturn(mockCriteria).when(criteriaBuilder).createQuery(any());
    when(mockCriteria.select(any())).thenReturn(mockCriteria);
    when(mockCriteria.from(any(Class.class))).thenReturn(root);
    when(root.get(isNull(SingularAttribute.class))).thenReturn(mockPath);
    when(root.get(any(SingularAttribute.class))).thenReturn(mockPath);
    when(entityManager.createQuery(mockCriteria)).thenReturn(mockQuery);
    when(mockQuery.setHint(any(), anyBoolean())).thenReturn(mockQuery);
    when(mockQuery.setMaxResults(anyInt())).thenReturn(mockQuery);
    when(mockQuery.getResultList()).thenReturn(List.of(testBene));
    when(mockQuery.getSingleResult()).thenReturn(testBene);
    when(mockCriteria.subquery(any(Class.class))).thenReturn(mockSubquery);
    when(mockCriteria.distinct(anyBoolean())).thenReturn(mockCriteria);
    when(mockSubquery.select(any())).thenReturn(mockSubquery);
    when(mockSubquery.from(any(Class.class))).thenReturn(root);

    when(entityManager.createNativeQuery(anyString())).thenReturn(mockQueryFunction);
    when(mockQueryFunction.setHint(any(), any())).thenReturn(mockQueryFunction);
    when(mockQueryFunction.setParameter(anyString(), anyString())).thenReturn(mockQueryFunction);

    List<Object> rawValues =
        new ArrayList<Object>() {
          {
            add(Long.toString(testBene.getBeneficiaryId()));
          }
        };
    when(mockQueryFunction.getResultList()).thenReturn(rawValues);
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#searchByIdentifier} returns the historical MBI
   * values in the response when searching by historic (non-current) MBI hash. The search should
   * look in beneficiaries_history for historical MBIs to include in the response.
   *
   * <p>Context: The Patient endpoint (v2) supports looking up a Patient by MBI using any MBI_NUM
   * that the patient has ever been associated with; this functionality needed for cases where the
   * patient's MBI has changed but the caller does not have the new data. The new MBI is returned in
   * the response; however BFD should also return historical MBI data to allow the caller to verify
   * the new MBI_NUM and the old MBI_NUM are associated with the same bene_id.
   */
  @Test
  public void searchForExistingPatientByMbiHashHasHistoricMbis() {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    /*
     * Historic MBI from the beneficiaries_history table (loaded from
     * sample-a-beneficiaryhistory.txt)
     */
    List<String> historicUnhashedMbis = new ArrayList<>();
    historicUnhashedMbis.add("3456689");
    // current MBI from the beneficiaries table (loaded from
    // sample-a-beneficiaries.txt)
    String currentUnhashedMbi = "3456789";

    BeneficiaryHistory beneHistoryEntry =
        parsedRecords.stream()
            .filter(r -> r instanceof BeneficiaryHistory)
            .map(r -> (BeneficiaryHistory) r)
            .findAny()
            .orElse(null);

    TokenParam identifier =
        new TokenParam(TransformerConstants.CODING_BBAPI_BENE_MBI_HASH, TEST_HASH);

    // Add history items to test bene as if they had returned from the db
    testBene.getBeneficiaryHistories().add(beneHistoryEntry);

    Bundle searchResults =
        patientProvider.searchByIdentifier(identifier, null, null, null, requestDetails);

    assertNotNull(searchResults);
    assertEquals(1, searchResults.getTotal());
    Patient patientFromSearchResult = (Patient) searchResults.getEntry().get(0).getResource();

    // Check history entries are present in identifiers.
    assertEquals(3, patientFromSearchResult.getIdentifier().size());
    List<Identifier> historicalIds =
        patientFromSearchResult.getIdentifier().stream()
            .filter(
                r ->
                    !r.getType().getCoding().get(0).getExtension().isEmpty()
                        && r.getType()
                            .getCoding()
                            .get(0)
                            .getExtension()
                            .get(0)
                            .getUrl()
                            .equals(TransformerConstants.CODING_SYSTEM_IDENTIFIER_CURRENCY)
                        && ((Coding)
                                r.getType().getCoding().get(0).getExtension().get(0).getValue())
                            .getCode()
                            .equals("historic"))
            .toList();

    for (String mbi : historicUnhashedMbis) {
      assertTrue(
          historicalIds.stream().anyMatch(h -> h.getValue().equals(mbi)),
          "Missing historical mbi: " + mbi);
    }

    Identifier currentMbiFromSearch =
        patientFromSearchResult.getIdentifier().stream()
            .filter(
                r ->
                    !r.getType().getCoding().get(0).getExtension().isEmpty()
                        && r.getType()
                            .getCoding()
                            .get(0)
                            .getExtension()
                            .get(0)
                            .getUrl()
                            .equals(TransformerConstants.CODING_SYSTEM_IDENTIFIER_CURRENCY)
                        && ((Coding)
                                r.getType().getCoding().get(0).getExtension().get(0).getValue())
                            .getCode()
                            .equals("current"))
            .findFirst()
            .get();
    assertEquals(currentUnhashedMbi, currentMbiFromSearch.getValue());
  }
}
