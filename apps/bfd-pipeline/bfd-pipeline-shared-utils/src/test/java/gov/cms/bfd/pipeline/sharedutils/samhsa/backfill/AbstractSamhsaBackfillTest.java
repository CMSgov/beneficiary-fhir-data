package gov.cms.bfd.pipeline.sharedutils.samhsa.backfill;

import static gov.cms.bfd.pipeline.sharedutils.samhsa.backfill.QueryConstants.UPSERT_PROGRESS_QUERY;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import gov.cms.bfd.pipeline.sharedutils.SamhsaUtil;
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class AbstractSamhsaBackfillTest {
  EntityManager manager;
  TransactionManager transactionManagerMock;
  Query mockQuery;
  SamhsaUtil mockSamhsaUtil;
  private static final String CARRIER_TEST_QUERY =
      "SELECT clm_id, clm_from_dt, clm_thru_dt, prncpal_dgns_cd, icd_dgns_cd1, icd_dgns_cd2, icd_dgns_cd3, icd_dgns_cd4, icd_dgns_cd5, icd_dgns_cd6, icd_dgns_cd7, icd_dgns_cd8, icd_dgns_cd9, icd_dgns_cd10, icd_dgns_cd11, icd_dgns_cd12 FROM ccw.carrier_claims WHERE clm_id >= :startingClaim ORDER BY clm_id limit :limit";
  private static final String FISS_TEST_QUERY =
      "SELECT claim_id, stmt_cov_from_date, stmt_cov_to_date, admit_diag_code, drg_cd, principle_diag FROM rda.fiss_claims ${gtClaimLine} ORDER BY claim_id limit :limit";
  private static final String WRITE_ENTRY_QUERY =
      """
          INSERT INTO test_table (code, clm_id)
          VALUES (:code, :claimId)
          ON CONFLICT (code, clm_id) DO NOTHING;
          """;

  @BeforeEach
  public void setup() {
    manager = Mockito.mock(EntityManager.class);
    transactionManagerMock = Mockito.mock(TransactionManager.class);
    mockQuery = Mockito.mock(Query.class);
    mockSamhsaUtil = Mockito.mock(SamhsaUtil.class);
    Mockito.when(manager.createNativeQuery(anyString())).thenReturn(mockQuery);
    Mockito.when(mockQuery.setParameter(anyString(), anyInt())).thenReturn(mockQuery);
    Object[] objects = new Object[] {"12345", LocalDate.of(1970, 1, 1), LocalDate.of(1970, 1, 1)};
    List<Object[]> objectList = new ArrayList<>();
    objectList.add(objects);
    Mockito.when(mockQuery.getResultList()).thenReturn(objectList);
  }

  @Test
  public void testBuildQuery() {
    Optional<String> startingClaim = Optional.of("12345");
    ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);

    AbstractSamhsaBackfill backfill =
        new CCWSamhsaBackfill(
            transactionManagerMock, 100000, 900l, CCWSamhsaBackfill.CCW_TABLES.CARRIER_CLAIMS);
    backfill.buildQuery(
        startingClaim, CCWSamhsaBackfill.CCW_TABLES.CARRIER_CLAIMS.getEntry(), 100000, manager);
    verify(manager).createNativeQuery(argumentCaptor.capture());
    Assertions.assertEquals(CARRIER_TEST_QUERY, argumentCaptor.getValue());
  }

  @Test
  public void testSaveProgress() {
    AbstractSamhsaBackfill backfill =
        new CCWSamhsaBackfill(
            transactionManagerMock, 100000, 900l, CCWSamhsaBackfill.CCW_TABLES.CARRIER_CLAIMS);
    backfill.saveProgress("test_table", Optional.of("12345"), 100000l, 5000l, manager);
    verify(manager).createNativeQuery(eq(UPSERT_PROGRESS_QUERY));
    verify(mockQuery, times(2)).setParameter(anyString(), anyString());
    verify(mockQuery, times(2)).setParameter(anyString(), anyLong());
  }

  @Test
  public void testWriteEntry() {
    AbstractSamhsaBackfill backfill =
        new CCWSamhsaBackfill(
            transactionManagerMock, 100000, 900l, CCWSamhsaBackfill.CCW_TABLES.CARRIER_CLAIMS);
    backfill.writeEntry("12345", "test_table", manager);
    verify(manager).createNativeQuery(eq(WRITE_ENTRY_QUERY));
    verify(mockQuery, times(3)).setParameter(anyString(), anyString());
  }

  @Test
  public void TestRdaBackfillConstruction() {
    AbstractSamhsaBackfill backfill =
        new RDASamhsaBackfill(
            transactionManagerMock, 100000, 900l, RDASamhsaBackfill.RDA_TABLES.FISS_CLAIMS);

    Assertions.assertEquals(FISS_TEST_QUERY, backfill.getQuery());
  }

  @Test
  public void testQueryLoop() throws Exception {
    AbstractSamhsaBackfill backfill =
        new RDASamhsaBackfill(
            transactionManagerMock, 100000, 900l, RDASamhsaBackfill.RDA_TABLES.FISS_CLAIMS);
    backfill.setLastClaimId(Optional.empty());
    backfill.setTotalProcessedInInterval(0L);
    backfill.setStartTime(Instant.now().minus(1000, ChronoUnit.SECONDS));
    backfill.setSamhsaUtil(mockSamhsaUtil);
    backfill.executeQueryLoop(manager);
    verify(mockQuery, times(1)).getResultList();
    verify(mockSamhsaUtil, times(1))
        .processCodeList(any(), any(), any(), any(), any(), any(), any(), any());
  }
}
