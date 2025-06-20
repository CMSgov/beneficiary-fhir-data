package gov.cms.bfd.server.ng.coverage;

import gov.cms.bfd.server.ng.coverage.model.CoverageDetails;
import gov.cms.bfd.server.ng.input.CoverageCompositeId;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Repository for querying Coverage information. */
@Repository
@RequiredArgsConstructor
public class CoverageRepository {

  private final EntityManager entityManager;

  /**
   * Finds all necessary details to construct a FHIR Coverage resource in a single query. This
   * combines fetching the Beneficiary and its related "current active" details.
   *
   * @param coverageCompositeId Parsed ID containing beneSk and CoveragePart.
   * @param lastUpdatedRange Optional date range to filter the main Beneficiary record.
   * @return An Optional containing {@link CoverageDetails} if the beneficiary is found and meets
   *     criteria, otherwise empty.
   */
  public Optional<CoverageDetails> findCoverageDetails(
      CoverageCompositeId coverageCompositeId, DateTimeRange lastUpdatedRange) {

    long beneSk = coverageCompositeId.beneSk();
    String partTypeCode = coverageCompositeId.coveragePart().getStandardCode();

    LocalDate currentDateForPeriods = OffsetDateTime.now(ZoneOffset.ofHours(-12)).toLocalDate();

    String jpqlFormat =
        """
            SELECT NEW gov.cms.bfd.server.ng.coverage.model.CoverageDetails(
                b, tp, bs, be, ber
            )
            FROM Beneficiary b
            LEFT JOIN BeneficiaryEntitlement be ON be.beneSk = b.beneSk
                AND be.medicareEntitlementTypeCode = :partTypeCode
                AND be.idrTransObsoleteTimestamp >= gov.cms.bfd.server.ng.IdrConstants.DEFAULT_ZONED_DATE
                AND be.benefitRangeBeginDate <= :referenceDate
                AND be.benefitRangeEndDate >= :referenceDate
            LEFT JOIN BeneficiaryThirdParty tp ON tp.beneSk = b.beneSk
                AND tp.thirdPartyTypeCode = :partTypeCode
                AND tp.idrTransObsoleteTimestamp >= gov.cms.bfd.server.ng.IdrConstants.DEFAULT_ZONED_DATE
                AND tp.benefitRangeBeginDate = be.benefitRangeBeginDate
                AND tp.benefitRangeEndDate = be.benefitRangeEndDate
            LEFT JOIN BeneficiaryStatus bs ON bs.beneSk = b.beneSk
                AND bs.idrTransObsoleteTimestamp >= gov.cms.bfd.server.ng.IdrConstants.DEFAULT_ZONED_DATE
                AND bs.medicareStatusBeginDate = tp.benefitRangeBeginDate
                AND bs.medicareStatusEndDate = tp.benefitRangeEndDate
            LEFT JOIN BeneficiaryEntitlementReason ber ON ber.beneSk = b.beneSk
                AND ber.idrTransObsoleteTimestamp >= gov.cms.bfd.server.ng.IdrConstants.DEFAULT_ZONED_DATE
                AND ber.benefitRangeBeginDate = bs.medicareStatusBeginDate
                AND ber.benefitRangeEndDate = bs.medicareStatusEndDate
            WHERE b.beneSk = :beneSk
              AND b.beneSk = b.xrefSk
              AND ((cast(:lowerBound AS ZonedDateTime)) IS NULL OR b.meta.updatedTimestamp %s :lowerBound)
              AND ((cast(:upperBound AS ZonedDateTime)) IS NULL OR b.meta.updatedTimestamp %s :upperBound)
              AND NOT EXISTS (SELECT 1 FROM OvershareMbi om WHERE om.mbi = b.identity.mbi)
            ORDER BY
              be.benefitRangeBeginDate DESC
            """;

    String jpql =
        String.format(
            jpqlFormat,
            lastUpdatedRange.getLowerBoundSqlOperator(),
            lastUpdatedRange.getUpperBoundSqlOperator());

    TypedQuery<CoverageDetails> query =
        entityManager
            .createQuery(jpql, CoverageDetails.class)
            .setParameter("beneSk", beneSk)
            .setParameter("partTypeCode", partTypeCode)
            .setParameter("referenceDate", currentDateForPeriods)
            .setParameter("lowerBound", lastUpdatedRange.getLowerBoundDateTime().orElse(null))
            .setParameter("upperBound", lastUpdatedRange.getUpperBoundDateTime().orElse(null));

    query.setMaxResults(1);

    List<CoverageDetails> results = query.getResultList();

    if (results.isEmpty()) {
      return Optional.empty();
    }

    CoverageDetails details = results.get(0);

    return Optional.of(details);
  }
}
