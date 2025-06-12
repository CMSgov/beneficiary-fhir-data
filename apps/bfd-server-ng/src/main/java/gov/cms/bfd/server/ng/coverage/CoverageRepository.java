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
        // maybe start with BeneficiaryStatus or BenEntitlment
        // join on bgndt and ednDt
        """
            SELECT NEW gov.cms.bfd.server.ng.coverage.CoverageDetails( tp, bs, be, ber
            )
            FROM BeneficiaryEntitlement be
            LEFT JOIN BeneficiaryThirdParty tp ON be.beneSk = tp.beneSk
                AND tp.benefitRangeBeginDate = be.benefitRangeBeginDate
                ANd tp.benefitRangeEndDate = be.benefitRangeEndDate
                AND tp.idrTransObsoleteTimestamp = be.idrTransObsoleteTimestamp
                AND tp.thirdPartyTypeCode = be.medicareEntitlementTypeCode
            LEFT JOIN BeneficiaryStatus bs ON bs.beneSk = tp.beneSk
                AND bs.medicareStatusBeginDate = tp.benefitRangeBeginDate
                AND bs.medicareStatusEndDate= tp.benefitRangeEndDate
                AND bs.idrTransObsoleteTimestamp = tp.idrTransObsoleteTimestamp
            LEFT JOIN BeneficiaryEntitlementReason ber ON ber.beneSk = tp.beneSk
                AND ber.idrTransObsoleteTimestamp = be.idrTransObsoleteTimestamp
                AND ber.benefitRangeBeginDate = be.benefitRangeBeginDate
                AND ber.benefitRangeEndDate = be.benefitRangeEndDate
            WHERE bs.beneSk = :beneSkParam
                AND be.idrTransObsoleteTimestamp < gov.cms.bfd.server.ng.IdrConstants.DEFAULT_ZONED_DATE
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
    details.setBeneficiary(beneficiary);

    return Optional.of(details);
  }
}
