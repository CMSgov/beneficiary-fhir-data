package gov.cms.bfd.server.ng.eob;

import gov.cms.bfd.server.ng.FhirUtil;
import gov.cms.bfd.server.ng.SecurityLabel;
import gov.cms.bfd.server.ng.SystemUrls;
import gov.cms.bfd.server.ng.beneficiary.BeneficiaryRepository;
import gov.cms.bfd.server.ng.claim.ClaimRepository;
import gov.cms.bfd.server.ng.claim.model.Claim;
import gov.cms.bfd.server.ng.claim.model.ClaimLine;
import gov.cms.bfd.server.ng.claim.model.ClaimProcedure;
import gov.cms.bfd.server.ng.claim.model.ClaimSourceId;
import gov.cms.bfd.server.ng.claim.model.IcdIndicator;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

/**
 * Handler methods for the ExplanationOfBenefit resource. This is called after the FHIR inputs from
 * the resource provider are converted into input types that are easier to work with.
 */
@Component
@RequiredArgsConstructor
public class EobHandler {
  private final BeneficiaryRepository beneficiaryRepository;
  private final ClaimRepository claimRepository;

  // Cache the security labels map to avoid repeated I/O and parsing
  private static final Map<String, List<SecurityLabel>> SECURITY_LABELS_MAP =
      SecurityLabel.securityLabelsMap();

  /**
   * Returns a {@link Patient} by their {@link IdType}.
   *
   * @param fhirId FHIR ID
   * @return patient
   */
  public Optional<ExplanationOfBenefit> find(final Long fhirId) {
    return searchByIdInner(fhirId, new DateTimeRange(), new DateTimeRange());
  }

  /**
   * Search for claims data by bene.
   *
   * @param beneSk bene sk
   * @param count record count
   * @param serviceDate service date
   * @param lastUpdated last updated
   * @param startIndex start index
   * @param sourceIds sourceIds
   * @return bundle
   */
  public Bundle searchByBene(
      Long beneSk,
      Optional<Integer> count,
      DateTimeRange serviceDate,
      DateTimeRange lastUpdated,
      Optional<Integer> startIndex,
      List<ClaimSourceId> sourceIds) {
    var beneXrefSk = beneficiaryRepository.getXrefSkFromBeneSk(beneSk);
    // Don't return data for historical beneSks
    if (beneXrefSk.isEmpty() || !beneXrefSk.get().equals(beneSk)) {
      return new Bundle();
    }
    var claims =
        claimRepository.findByBeneXrefSk(
            beneXrefSk.get(), serviceDate, lastUpdated, count, startIndex, sourceIds);

    var filteredClaims = filterClaimsExcludingSamhsa(claims, true);
    return FhirUtil.bundleOrDefault(
        filteredClaims.stream().map(Claim::toFhir), claimRepository::claimLastUpdated);
  }

  /**
   * Returns a filtered list of claims with optional SAMHSA exclusion applied. This performs only
   * in-memory filtering logic and intentionally avoids any bundle creation or additional database
   * lookups so that callers can build a single bundle as the final step.
   *
   * @param claims input claims
   * @param excludeSamhsa whether to exclude SAMHSA-coded claims
   * @return filtered claims
   */
  protected List<Claim> filterClaimsExcludingSamhsa(List<Claim> claims, boolean excludeSamhsa) {
    if (!excludeSamhsa) {
      return claims; // no filtering requested
    }
    return claims.stream().filter(claim -> !claimHasSamhsa(claim)).toList();
  }

  /**
   * Search for claims data by claim ID.
   *
   * @param claimUniqueId claim ID
   * @param serviceDate service date
   * @param lastUpdated last updated
   * @return bundle
   */
  public Bundle searchById(
      Long claimUniqueId, DateTimeRange serviceDate, DateTimeRange lastUpdated) {
    var eob = searchByIdInner(claimUniqueId, serviceDate, lastUpdated);
    return FhirUtil.bundleOrDefault(eob.map(e -> e), claimRepository::claimLastUpdated);
  }

  private Optional<ExplanationOfBenefit> searchByIdInner(
      Long claimUniqueId, DateTimeRange serviceDate, DateTimeRange lastUpdated) {
    var claimOpt = claimRepository.findById(claimUniqueId, serviceDate, lastUpdated);
    if (claimOpt.isEmpty()) {
      return Optional.empty();
    }
    var claim = claimOpt.get();
    // Apply SAMHSA exclusion in-memory without constructing a bundle.
    if (claimHasSamhsa(claim)) {
      return Optional.empty();
    }
    return Optional.of(claim.toFhir());
  }

  private boolean compare(String target, LocalDate claimDate, SecurityLabel entry) {
    if (!isClaimDateWithinBounds(claimDate, entry)) {
      return false;
    }
    return entry.matches(target);
  }

  // Returns true if the given claim contains any procedure that matches a SAMHSA
  // security label code from the dictionary.
  private boolean claimHasSamhsa(Claim claim) {
    var claimFromDate = claim.getBillablePeriod().getClaimFromDate();
    var claimThroughDate = claim.getBillablePeriod().getClaimThroughDate();

    // clm_from_dt of the claim comes after clm_thru_dt, indicates bad data.
    var referenceDay = claimFromDate.isAfter(claimThroughDate) ? claimFromDate : claimThroughDate;

    return drgCodeMatches(claim, referenceDay)
        || claim.getClaimItems().stream()
            .anyMatch(
                e -> {
                  var cl = e.getClaimLine();
                  var cp = e.getClaimProcedure();

                  return procedureMatchesSamhsaCode(cp, referenceDay)
                      || hcpcsCodeMatches(cl, referenceDay);
                });
  }

  private boolean drgCodeMatches(Claim claim, LocalDate claimDate) {
    var entries = SECURITY_LABELS_MAP.get(SystemUrls.CMS_MS_DRG);
    for (var entry : entries) {
      if (claim
          .getDrgCode()
          .filter(drgCode -> compare(drgCode.toString(), claimDate, entry))
          .isPresent()) {
        return true;
      }
    }

    return false;
  }

  private boolean hcpcsCodeMatches(ClaimLine claimLine, LocalDate claimDate) {
    var hcpcs = claimLine.getHcpcsCode().getHcpcsCode().orElse("");

    for (var system : List.of(SystemUrls.AMA_CPT, SystemUrls.CMS_HCPCS)) {
      var entries = SECURITY_LABELS_MAP.get(system);
      for (var entry : entries) {
        if (compare(hcpcs, claimDate, entry)) {
          return true;
        }
      }
    }
    return false;
  }

  // Checks ICDs.
  private boolean procedureMatchesSamhsaCode(ClaimProcedure proc, LocalDate claimDate) {
    var diagnosisCode = proc.getDiagnosisCode().orElse("");
    var procedureCode = proc.getProcedureCode().orElse("");
    var icdIndicator = proc.getIcdIndicator().orElse(IcdIndicator.ICD_10);

    // Procedure codes live under the CMS procedure system
    // diagnosis codes under the HL7 CM system.
    var procedureEntries = SECURITY_LABELS_MAP.get(icdIndicator.getProcedureSystem());
    var diagnosisEntries = SECURITY_LABELS_MAP.get(icdIndicator.getDiagnosisSystem());

    // Check procedure code against its system entries.
    if (!procedureCode.isEmpty()) {
      var procMatch =
          procedureEntries.stream()
              .anyMatch(pEntries -> compare(procedureCode, claimDate, pEntries));
      if (procMatch) {
        return true;
      }
    }

    return diagnosisEntries.stream().anyMatch(dEntry -> compare(diagnosisCode, claimDate, dEntry));
  }

  private boolean isClaimDateWithinBounds(LocalDate claimDate, SecurityLabel entry) {
    var entryStart = entry.getStartDateAsDate();
    var entryEnd = entry.getEndDateAsDate();
    return !entryStart.isAfter(claimDate) && !entryEnd.isBefore(claimDate);
  }
}
