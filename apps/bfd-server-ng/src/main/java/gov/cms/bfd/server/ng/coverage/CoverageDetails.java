package gov.cms.bfd.server.ng.coverage;

import gov.cms.bfd.server.ng.DateUtil;
import gov.cms.bfd.server.ng.IdrConstants;
import gov.cms.bfd.server.ng.beneficiary.model.Beneficiary;
import gov.cms.bfd.server.ng.beneficiary.model.BeneficiaryEntitlement;
import gov.cms.bfd.server.ng.beneficiary.model.BeneficiaryEntitlementReason;
import gov.cms.bfd.server.ng.beneficiary.model.BeneficiaryStatus;
import gov.cms.bfd.server.ng.beneficiary.model.BeneficiaryThirdParty;
import gov.cms.bfd.server.ng.input.CoveragePart;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Period;

/**
 * A Service Object (SO) or Data Transfer Object (DTO) that aggregates all necessary data pieces
 * from various beneficiary-related tables required to construct a comprehensive FHIR Coverage
 * resource.
 */
@Getter
@Builder
@AllArgsConstructor
public class CoverageDetails {

  private final Beneficiary beneficiary;
  private final Optional<BeneficiaryThirdParty> thirdPartyDetails;
  private final Optional<BeneficiaryStatus> currentStatus;
  private final Optional<BeneficiaryEntitlement> partEntitlement;
  private final Optional<BeneficiaryEntitlementReason> currentEntitlementReason;

  /**
   * Constructor specifically for use with JPQL SELECT NEW expressions. It accepts direct entity
   * types (which can be null from LEFT JOINs) and wraps them in Optional internally.
   *
   * @param beneficiary The main Beneficiary entity.
   * @param thirdParty The joined BeneficiaryThirdParty entity (can be null).
   * @param status The joined BeneficiaryStatus entity (can be null).
   * @param entitlement The joined BeneficiaryEntitlement entity (can be null).
   * @param reason The joined BeneficiaryEntitlementReason entity (can be null).
   */
  public CoverageDetails(
      Beneficiary beneficiary,
      BeneficiaryThirdParty thirdParty,
      BeneficiaryStatus status,
      BeneficiaryEntitlement entitlement,
      BeneficiaryEntitlementReason reason) {
    this.beneficiary = beneficiary;
    this.thirdPartyDetails = Optional.ofNullable(thirdParty);
    this.currentStatus = Optional.ofNullable(status);
    this.partEntitlement = Optional.ofNullable(entitlement);
    this.currentEntitlementReason = Optional.ofNullable(reason);
  }

  /**
   * Transforms the data held in this SO into a fully populated FHIR Coverage resource.
   *
   * @param fullFhirId The ID to be assigned to the Coverage resource.
   * @param partEnumInstance The specific {@link CoveragePart} (e.g., PART_A, PART_B) this Coverage
   *     resource represents.
   * @return A FHIR {@link Coverage} object.
   */
  public Coverage toFhir(String fullFhirId, CoveragePart partEnumInstance) {

    Coverage coverage = beneficiary.toFhirCoverage(fullFhirId, partEnumInstance);

    // Populate Period, Status (derived from period), and Buy-in Extension
    this.getThirdPartyDetails()
        .ifPresent(
            currentThirdParty -> {
              Optional<LocalDate> benefitPeriodStartDate =
                  Optional.ofNullable(currentThirdParty.getBenefitRangeBeginDate());
              Optional<LocalDate> benefitPeriodEndDate =
                  Optional.ofNullable(currentThirdParty.getBenefitRangeEndDate());

              benefitPeriodStartDate.ifPresent(
                  startDate -> {
                    Period period = new Period().setStart(DateUtil.toDate(startDate));
                    benefitPeriodEndDate
                        .filter(endDate -> endDate.isBefore(IdrConstants.DEFAULT_DATE))
                        .ifPresent(validEndDate -> period.setEnd(DateUtil.toDate(validEndDate)));

                    coverage.setPeriod(period);
                    coverage.setStatus(Coverage.CoverageStatus.ACTIVE);
                  });
            });

    List<Extension> allExtensions = CoverageExtensionFactory.createAllExtensions(this);
    if (!allExtensions.isEmpty()) {
      coverage.setExtension(allExtensions);
    }

    return coverage;
  }
}
