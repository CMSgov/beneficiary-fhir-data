package gov.cms.bfd.pipeline;

import static java.time.Instant.now;

import gov.cms.bfd.DatabaseTestUtils;
import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaFissDiagnosisCode;
import gov.cms.bfd.model.rda.entities.RdaFissPayer;
import gov.cms.bfd.model.rda.entities.RdaFissProcCode;
import gov.cms.bfd.model.rda.entities.RdaFissRevenueLine;
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import gov.cms.bfd.model.rda.entities.RdaMcsDetail;
import gov.cms.bfd.model.rda.entities.RdaMcsDiagnosisCode;
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import lombok.Getter;

/**
 * Utility class for supporting testing of the CleanupJob tasks. (Borrows from
 * gov.cms.bfd.server.war.utils.RDATestUtils.)
 */
@Getter
public class CleanupTestUtils {

  /** Tracking entities (tables) so they can be cleaned after. */
  private static final List<Class<?>> TABLE_ENTITIES =
      List.of(
          RdaFissRevenueLine.class,
          RdaFissPayer.class,
          RdaFissDiagnosisCode.class,
          RdaFissProcCode.class,
          RdaFissClaim.class,
          RdaMcsDetail.class,
          RdaMcsDiagnosisCode.class,
          RdaMcsClaim.class,
          Mbi.class);

  /** Path to use for persistence units. */
  public static final String PERSISTENCE_UNIT_NAME = "gov.cms.bfd.rda";

  /** Test mbi. */
  public static final String MBI = "123456MBI";

  /** Test mbi hash. */
  public static final String MBI_HASH = "a7f8e93f09";

  /** Test fiss claim (DCN). */
  public static final String FISS_CLAIM_DCN = "123456d";

  /** TransactionManager to use with tests. */
  private TransactionManager transactionManager;

  /** EntityManagerFactory to use with tests. */
  private EntityManagerFactory entityManagerFactory;

  /** Initializes the test utility. */
  public void init() {
    final DataSource dataSource = DatabaseTestUtils.get().getUnpooledDataSource();

    final Map<String, Object> hibernateProperties =
        Map.of(org.hibernate.cfg.AvailableSettings.DATASOURCE, dataSource);

    entityManagerFactory =
        Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, hibernateProperties);

    transactionManager = new TransactionManager(entityManagerFactory);
  }

  /**
   * Generate sample claims data to fit a given test scenario.
   *
   * @param cutoff the 60-day cutoff Instant used for the test.
   * @param oldClaims the number of Fiss claims to generate that are greater than 60 days since
   *     lastUpdated.
   * @param newClaims the number of Fiss claims to generate that are less than 60 days since
   *     lastUpdated.
   */
  public void seedData(Instant cutoff, int oldClaims, int newClaims) {
    List<Instant> dateSeq = new ArrayList<>();

    // add 60-day dates
    dateSeq.addAll(Collections.nCopies(oldClaims, cutoff));
    // add 59-day dates (after cutoff)
    dateSeq.addAll(Collections.nCopies(newClaims, cutoff.plus(1, ChronoUnit.DAYS)));
    Mbi mbi =
        transactionManager.executeFunction(
            entityManager ->
                entityManager.merge(
                    Mbi.builder().mbi(MBI).hash(MBI_HASH).lastUpdated(now()).build()));
    transactionManager.executeProcedure(
        entityManager -> {
          int baseClaimId = 1;
          for (Instant i : dateSeq) {
            String claimId = "" + (baseClaimId++);
            entityManager.merge(createFissClaimForDate(claimId, mbi, i));
          }
        });
  }

  /**
   * Generate and execute a query that returns the count of RdaFissClaim entities.
   *
   * @return the count of RdaFissClaim entities.
   */
  public long count() {
    return transactionManager.executeFunction(
        entityManager -> {
          List<Long> result =
              entityManager.createQuery("select count(*) from RdaFissClaim").getResultList();
          if (result != null && !result.isEmpty()) {
            return result.getFirst();
          } else {
            return 0L;
          }
        });
  }

  /**
   * Generate and execute a query that returns the oldest lastUpdated property for RdaFissClaim
   * entities.
   *
   * @return an Instant representing the oldest lastUpdated property of RdaFissClaim.
   */
  public Instant oldestLastUpdatedDate() {
    return transactionManager.executeFunction(
        entityManager -> {
          List<Instant> result =
              entityManager
                  .createQuery("select max(lastUpdated) from RdaFissClaim")
                  .getResultList();
          if (result != null && !result.isEmpty()) {
            return result.getFirst();
          } else {
            return Instant.EPOCH;
          }
        });
  }

  /**
   * Create a new RdaFissClaim object for a given claimId, mbi and lastUpdated value.
   *
   * @param claimId the claimId to set.
   * @param mbi the mbi to set.
   * @param lastUpdated the lastUpdated to set.
   * @return a new RdaFissClaim object.
   */
  public RdaFissClaim createFissClaimForDate(String claimId, Mbi mbi, Instant lastUpdated) {
    RdaFissClaim claim =
        RdaFissClaim.builder()
            .sequenceNumber(1L)
            .claimId(claimId)
            .dcn(FISS_CLAIM_DCN)
            .intermediaryNb("99999")
            .hicNo("hicnumber")
            .currStatus('a')
            .currLoc1('z')
            .currLoc2("Somda")
            .medaProvId("meda12345")
            .medaProv_6("meda12")
            .fedTaxNumber("tax12345")
            .totalChargeAmount(new BigDecimal("1234.32"))
            .receivedDate(LocalDate.of(1970, 1, 1))
            .currTranDate(LocalDate.of(1970, 1, 2))
            .admitDiagCode("admitcd")
            .principleDiag("princcd")
            .npiNumber("8876543211")
            .mbiRecord(mbi)
            .fedTaxNumber("abc123")
            .lobCd("r")
            .lastUpdated(lastUpdated)
            .stmtCovFromDate(LocalDate.of(1970, 7, 10))
            .stmtCovToDate(LocalDate.of(1970, 7, 20))
            .servTypeCd("A")
            .freqCd("C")
            .clmTypInd("4")
            .drgCd("drgc")
            .groupCode("gr")
            .build();

    Set<RdaFissProcCode> procCodes =
        Set.of(
            RdaFissProcCode.builder()
                .claimId(claim.getClaimId())
                .rdaPosition((short) 1)
                .procCode("CODEABC")
                .procFlag("FLAG")
                .procDate(LocalDate.of(1970, 7, 20))
                .build(),
            RdaFissProcCode.builder()
                .claimId(claim.getClaimId())
                .rdaPosition((short) 2)
                .procCode("CODECBA")
                .procFlag("FLA2")
                .build());

    Set<RdaFissDiagnosisCode> diagnosisCodes =
        Set.of(
            RdaFissDiagnosisCode.builder()
                .claimId(claim.getClaimId())
                .rdaPosition((short) 1)
                .diagCd2("admitcd")
                .diagPoaInd("Z")
                .build(),
            RdaFissDiagnosisCode.builder()
                .claimId(claim.getClaimId())
                .rdaPosition((short) 2)
                .diagCd2("other")
                .diagPoaInd("U")
                .build(),
            RdaFissDiagnosisCode.builder()
                .claimId(claim.getClaimId())
                .rdaPosition((short) 3)
                .diagCd2("princcd")
                .diagPoaInd("n")
                .build());

    Set<RdaFissPayer> payers =
        Set.of(
            RdaFissPayer.builder()
                .claimId(claim.getClaimId())
                .rdaPosition((short) 1)
                .beneFirstName("jim")
                .beneMidInit("k")
                .beneLastName("baker")
                .beneSex("m")
                .beneDob(LocalDate.of(1975, 3, 1))
                .payerType(RdaFissPayer.PayerType.BeneZ)
                .payersName("MEDICARE")
                .build(),
            RdaFissPayer.builder()
                .claimId(claim.getClaimId())
                .rdaPosition((short) 2)
                .insuredName("BAKER  JIM  K")
                .payerType(RdaFissPayer.PayerType.Insured)
                .payersName("BCBS KC")
                .build());

    Set<RdaFissRevenueLine> revenueLines =
        Set.of(
            RdaFissRevenueLine.builder()
                .claimId(claim.getClaimId())
                .rdaPosition((short) 1)
                .serviceDate(LocalDate.of(1980, 12, 5))
                .serviceDateText("1980-12-05")
                .revUnitsBilled(5)
                .revServUnitCnt(6)
                .revCd("abcd")
                .nonBillRevCode("B")
                .hcpcCd("12345")
                .hcpcInd("A")
                .acoRedRarc("rarc")
                .acoRedCarc("car")
                .acoRedCagc("ca")
                .hcpcModifier("m1")
                .hcpcModifier2("m2")
                .hcpcModifier3("m3")
                .hcpcModifier4("m4")
                .hcpcModifier5("m5")
                .apcHcpcsApc("00001")
                .build());

    claim.setPayers(payers);
    claim.setProcCodes(procCodes);
    claim.setDiagCodes(diagnosisCodes);
    claim.setRevenueLines(revenueLines);

    return claim;
  }

  /** Delete all the test data from the db. */
  public void truncateTables() {
    transactionManager.executeProcedure(
        entityManager -> {
          TABLE_ENTITIES.forEach(
              e ->
                  entityManager
                      .createQuery("delete from " + e.getSimpleName() + " f")
                      .executeUpdate());
        });
  }
}
