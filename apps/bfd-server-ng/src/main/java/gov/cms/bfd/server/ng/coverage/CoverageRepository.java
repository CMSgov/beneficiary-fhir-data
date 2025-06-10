package gov.cms.bfd.server.ng.coverage;

import gov.cms.bfd.server.ng.beneficiary.BeneficiaryRepository;
import gov.cms.bfd.server.ng.beneficiary.model.Beneficiary;
import gov.cms.bfd.server.ng.beneficiary.model.BeneficiaryEntitlement;
import gov.cms.bfd.server.ng.beneficiary.model.BeneficiaryEntitlementReason;
import gov.cms.bfd.server.ng.beneficiary.model.BeneficiaryStatus;
import gov.cms.bfd.server.ng.beneficiary.model.BeneficiaryThirdParty;
import gov.cms.bfd.server.ng.input.CoverageCompositeId;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

    long beneSk = coverageCompositeId.beneSk();
    String partTypeCode = coverageCompositeId.coveragePart().getStandardCode();

    Optional<Beneficiary> beneficiaryOpt =
        beneficiaryRepository
            .findById(beneSk, lastUpdatedRange)
            .filter(b -> b.getBeneSk() == b.getXrefSk());

    if (beneficiaryOpt.isEmpty()) {
      return Optional.empty();
    }
    Beneficiary beneficiary = beneficiaryOpt.get();

    ZoneId processingZone = ZoneId.of("America/New_York");
    LocalDate currentDateForPeriods = ZonedDateTime.now(processingZone).toLocalDate();
    ZonedDateTime currentTransactionalBoundary = ZonedDateTime.now(processingZone);

    String jpqlRelatedDetails =
        """
            SELECT tp, bs, be, ber
            FROM BeneficiaryThirdParty tp
            LEFT JOIN BeneficiaryStatus bs ON bs.beneSk = tp.beneSk
                AND bs.idrTransEffectiveTimestamp <= :transactionalBoundary
                AND bs.idrTransObsoleteTimestamp > :transactionalBoundary
                AND bs.medicareStatusBeginDate <= :referenceDate
                AND bs.medicareStatusEndDate >= :referenceDate
            LEFT JOIN BeneficiaryEntitlement be ON be.beneSk = tp.beneSk
                AND be.medicareEntitlementTypeCode = :partTypeCode
                AND be.idrTransEffectiveTimestamp <= :transactionalBoundary
                AND be.idrTransObsoleteTimestamp > :transactionalBoundary
                AND be.benefitRangeBeginDate <= :referenceDate
                AND be.benefitRangeEndDate >= :referenceDate
            LEFT JOIN BeneficiaryEntitlementReason ber ON ber.beneSk = tp.beneSk
                AND ber.idrTransEffectiveTimestamp <= :transactionalBoundary
                AND ber.idrTransObsoleteTimestamp > :transactionalBoundary
                AND ber.benefitRangeBeginDate <= :referenceDate
                AND ber.benefitRangeEndDate >= :referenceDate
            WHERE tp.beneSk = :beneSkParam
                AND tp.thirdPartyTypeCode = :partTypeCode
                AND tp.idrTransEffectiveTimestamp <= :transactionalBoundary
                AND tp.idrTransObsoleteTimestamp > :transactionalBoundary
                AND tp.benefitRangeBeginDate <= :referenceDate
                AND tp.benefitRangeEndDate >= :referenceDate
            """;

    TypedQuery<Object[]> query =
        entityManager
            .createQuery(jpqlRelatedDetails, Object[].class)
            .setParameter("beneSkParam", beneSk)
            .setParameter("partTypeCode", partTypeCode)
            .setParameter("referenceDate", currentDateForPeriods)
            .setParameter("transactionalBoundary", currentTransactionalBoundary)
            .setMaxResults(1);

    List<Object[]> relatedResults = query.getResultList();

    Optional<BeneficiaryThirdParty> finalThirdPartyOpt = Optional.empty();
    Optional<BeneficiaryStatus> finalStatusOpt = Optional.empty();
    Optional<BeneficiaryEntitlement> finalEntitlementOpt = Optional.empty();
    Optional<BeneficiaryEntitlementReason> finalReasonOpt = Optional.empty();

    if (!relatedResults.isEmpty()) {
      Object[] firstRelatedRow = relatedResults.get(0);
      finalThirdPartyOpt = Optional.ofNullable((BeneficiaryThirdParty) firstRelatedRow[0]);
      finalStatusOpt = Optional.ofNullable((BeneficiaryStatus) firstRelatedRow[1]);
      finalEntitlementOpt = Optional.ofNullable((BeneficiaryEntitlement) firstRelatedRow[2]);
      finalReasonOpt = Optional.ofNullable((BeneficiaryEntitlementReason) firstRelatedRow[3]);
    }

    CoverageDetails details =
        CoverageDetails.builder()
            .beneficiary(beneficiary)
            .thirdPartyDetails(finalThirdPartyOpt)
            .currentStatus(finalStatusOpt)
            .partEntitlement(finalEntitlementOpt)
            .currentEntitlementReason(finalReasonOpt)
            .build();

    return Optional.of(details);
  }
}
