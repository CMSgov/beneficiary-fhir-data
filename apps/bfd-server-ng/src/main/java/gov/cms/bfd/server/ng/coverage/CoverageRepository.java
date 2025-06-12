package gov.cms.bfd.server.ng.coverage;

import gov.cms.bfd.server.ng.beneficiary.BeneficiaryRepository;
import gov.cms.bfd.server.ng.beneficiary.model.Beneficiary;
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
  private final BeneficiaryRepository beneficiaryRepository;

  /**
   * Finds all necessary details to construct a FHIR Coverage resource.
   *
   * @param coverageCompositeId Parsed ID containing beneSk and CoveragePart.
   * @param lastUpdatedRange Optional date range to filter records by their last update.
   * @return An Optional containing {@link CoverageDetails} if the beneficiary is found and is
   *     current-effective, otherwise empty.
   */
  public Optional<CoverageDetails> findCoverageDetailsSingleQueryAttempt(
      CoverageCompositeId coverageCompositeId, DateTimeRange lastUpdatedRange) {

    var beneSk = coverageCompositeId.beneSk();
    String partTypeCode = coverageCompositeId.coveragePart().getStandardCode();

    Optional<Beneficiary> beneficiaryOpt =
        beneficiaryRepository
            .findById(beneSk, lastUpdatedRange)
            .filter(b -> b.getBeneSk() == b.getXrefSk());

    if (beneficiaryOpt.isEmpty()) {
      return Optional.empty();
    }
    Beneficiary beneficiary = beneficiaryOpt.get();
    LocalDate currentDateForPeriods = OffsetDateTime.now(ZoneOffset.ofHours(-12)).toLocalDate();

    String jpqlRelatedDetails =
        """
            SELECT NEW gov.cms.bfd.server.ng.coverage.CoverageDetails( tp, bs, be, ber
            )
            FROM BeneficiaryEntitlement be
            LEFT JOIN BeneficiaryThirdParty tp ON be.beneSk = tp.beneSk
                AND tp.benefitRangeBeginDate = be.benefitRangeBeginDate
                ANd tp.benefitRangeEndDate = be.benefitRangeEndDate
                AND tp.thirdPartyTypeCode = be.medicareEntitlementTypeCode
                AND tp.idrTransObsoleteTimestamp >= gov.cms.bfd.server.ng.IdrConstants.DEFAULT_ZONED_DATE
            LEFT JOIN BeneficiaryStatus bs ON bs.beneSk = tp.beneSk
                AND bs.medicareStatusBeginDate = tp.benefitRangeBeginDate
                AND bs.medicareStatusEndDate= tp.benefitRangeEndDate
                AND bs.idrTransObsoleteTimestamp >= gov.cms.bfd.server.ng.IdrConstants.DEFAULT_ZONED_DATE
            LEFT JOIN BeneficiaryEntitlementReason ber ON ber.beneSk = tp.beneSk

                AND ber.benefitRangeBeginDate = be.benefitRangeBeginDate
                AND ber.benefitRangeEndDate = be.benefitRangeEndDate
                AND ber.idrTransObsoleteTimestamp >= gov.cms.bfd.server.ng.IdrConstants.DEFAULT_ZONED_DATE
            WHERE bs.beneSk = :beneSkParam
                AND be.idrTransObsoleteTimestamp >= gov.cms.bfd.server.ng.IdrConstants.DEFAULT_ZONED_DATE
                AND be.benefitRangeBeginDate <= :referenceDate
                AND be.benefitRangeEndDate >= :referenceDate
                AND be.medicareEntitlementTypeCode = :partTypeCode
            """;

    TypedQuery<CoverageDetails> query =
        entityManager
            .createQuery(jpqlRelatedDetails, CoverageDetails.class)
            .setParameter("beneSkParam", beneSk)
            .setParameter("partTypeCode", partTypeCode)
            .setParameter("referenceDate", currentDateForPeriods)
            .setMaxResults(1);

    List<CoverageDetails> relatedResults = query.getResultList();

    if (relatedResults.isEmpty()) {
      return Optional.of(
          CoverageDetails.builder()
              .beneficiary(beneficiary)
              .thirdPartyDetails(Optional.empty())
              .currentStatus(Optional.empty())
              .partEntitlement(Optional.empty())
              .currentEntitlementReason(Optional.empty())
              .build());
    }

    CoverageDetails details = relatedResults.get(0);
    //    details.setBeneficiary(beneficiary);

    return Optional.of(details);
  }

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
            SELECT NEW gov.cms.bfd.server.ng.coverage.CoverageDetails(
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
            WHERE b.beneSk = :beneSkParam
              AND b.beneSk = b.xrefSk
              AND ((cast(:lowerBound AS ZonedDateTime)) IS NULL OR b.meta.updatedTimestamp %s :lowerBound)
              AND ((cast(:upperBound AS ZonedDateTime)) IS NULL OR b.meta.updatedTimestamp %s :upperBound)
              AND NOT EXISTS (SELECT 1 FROM OvershareMbi om WHERE om.mbi = b.identity.mbi)
            ORDER BY
              b.beneSk,
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
            .setParameter("beneSkParam", beneSk)
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
