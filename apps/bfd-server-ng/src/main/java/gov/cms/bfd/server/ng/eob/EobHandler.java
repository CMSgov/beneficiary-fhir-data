package gov.cms.bfd.server.ng.eob;

import gov.cms.bfd.server.ng.FhirUtil;
import gov.cms.bfd.server.ng.SecurityLabels;
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
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
  private static final Map<String, List<Map<String, Object>>> SECURITY_LABELS_MAP =
      SecurityLabels.securityLabelsMap();

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

  private String getDiagnosisCode(ClaimProcedure e) {
    return normalize(e.getDiagnosisCode().orElse(""));
  }

  private String getClaimProcedureCode(ClaimProcedure e) {
    return normalize(e.getProcedureCode().orElse(""));
  }

  private String getCode(Map<String, Object> entry) {
    var codeObj = entry.get("code");
    return normalize(codeObj.toString());
  }

  /**
   * Normalizes a code string for SAMHSA comparison.
   *
   * <p>Rules:
   *
   * <ul>
   *   <li>Rejects null (throws {@link NullPointerException})
   *   <li>Trims leading/trailing whitespace
   *   <li>Removes '.' characters
   *   <li>Lowers case
   * </ul>
   *
   * @param code non-null input value
   * @return normalized code
   * @throws NullPointerException if code is null
   */
  private String normalize(String code) {
    Objects.requireNonNull(code, "code must not be null");
    return code.trim().replace(".", "").toLowerCase();
  }

  /**
   * Compares two code strings for equality after SAMHSA normalization.
   *
   * <p>Normalization steps for both inputs:
   *
   * <ul>
   *   <li>Rejects null (throws {@link NullPointerException})
   *   <li>Trims leading/trailing whitespace
   *   <li>Removes all '.' characters
   *   <li>Converts to lowercase
   * </ul>
   *
   * <p>This ensures comparisons are insensitive to case, dots, and padding.
   *
   * @param source the first code string (must not be null)
   * @param target the second code string (must not be null)
   * @return true if the normalized forms are equal; false otherwise
   * @throws NullPointerException if either input is null
   */
  private boolean compare(String source, String target) {
    return normalize(source).equals(normalize(target));
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

  // Checks DRG.
  private boolean drgCodeMatches(Claim claim, LocalDate claimDate) {
    var entries = SECURITY_LABELS_MAP.get(SystemUrls.CMS_MS_DRG);
    for (var entry : entries) {
      if (claimCodeDateInvalidInSamhsaList(claimDate, entry)) return false;
      if (claim.getDrgCode().filter(drgCode -> compare(getCode(entry), drgCode)).isPresent()) {
        return true;
      }
    }

    return false;
  }

  // Checks HCPCS and cpt.
  private boolean hcpcsCodeMatches(ClaimLine claimLine, LocalDate claimDate) {
    var hcpcs = claimLine.getHcpcsCode().getHcpcsCode().orElse("");
    if (hcpcs.isEmpty()) {
      return false;
    }
    for (String system : List.of(SystemUrls.AMA_CPT, SystemUrls.CMS_HCPCS)) {
      var entries = SECURITY_LABELS_MAP.get(system);
      for (var entry : entries) {
        if (claimCodeDateInvalidInSamhsaList(claimDate, entry)) return false;
        if (compare(getCode(entry), hcpcs)) {
          return true;
        }
      }
    }
    return false;
  }

  // Checks ICDs.
  private boolean procedureMatchesSamhsaCode(ClaimProcedure proc, LocalDate claimDate) {
    var diagnosisCode = getDiagnosisCode(proc);
    var procedureCode = getClaimProcedureCode(proc);
    var icdIndicator = proc.getIcdIndicator().orElse(IcdIndicator.ICD_10); // Fallback ICD10

    // Procedure codes live under the CMS procedure system
    // diagnosis codes under the HL7 CM system.
    var procedureEntries = SECURITY_LABELS_MAP.get(icdIndicator.getProcedureSystem());
    var diagnosisEntries = SECURITY_LABELS_MAP.get(icdIndicator.getDiagnosisSystem());

    // Check procedure code against its system entries.
    if (!procedureCode.isEmpty()) {
      boolean procMatch =
          procedureEntries.stream()
              .anyMatch(
                  pEntries -> {
                    if (claimCodeDateInvalidInSamhsaList(claimDate, pEntries)) return false;
                    return compare(getCode(pEntries), procedureCode);
                  });
      if (procMatch) {
        return true;
      }
    }

    // Only check diagnosis when indicator explicitly identifies ICD-9 or ICD-10.
    if (!diagnosisCode.isEmpty()) {
      boolean diagMatch =
          diagnosisEntries.stream()
              .anyMatch(
                  dEntry -> {
                    if (claimCodeDateInvalidInSamhsaList(claimDate, dEntry)) return false;
                    return compare(getCode(dEntry), diagnosisCode);
                  });
      if (diagMatch) {
        return true;
      }
    }
    return false;
  }

  /**
   * Converts a {@link Date} object to a {@link LocalDate} using the system's default time zone.
   *
   * @param date the {@link Date} object to be converted; must not be null
   * @return the corresponding {@link LocalDate} representation of the given {@link Date}
   * @throws NullPointerException if the provided {@link Date} is null
   */
  private LocalDate dateToLocalDate(Date date) {
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }

  /**
   * Retrieves the end date from the provided YAML entry map. If the "endDate" value in the map is
   * the string "ACTIVE" (case-insensitive), the current date is returned. Otherwise, the "endDate"
   * value is treated as a {@link Date} object and converted to a {@link LocalDate}.
   *
   * @param claimEntry a map containing the YAML entry data, where "endDate" is expected to be
   *     either the string "ACTIVE" or a {@link Date} object.
   * @return the end date as a {@link LocalDate}, or the current date if "endDate" is "ACTIVE".
   */
  private LocalDate getYmlItemEndDate(Map<String, Object> claimEntry) {
    return dateToLocalDate(
        claimEntry.get("endDate").toString().equalsIgnoreCase("ACTIVE")
            ? new Date()
            : (Date) claimEntry.get("endDate"));
  }

  private boolean claimCodeDateInvalidInSamhsaList(LocalDate claimDate, Map<String, Object> entry) {
    return !(dateToLocalDate((Date) entry.get("startDate")).isBefore(claimDate)
        && claimDate.isBefore(getYmlItemEndDate(entry)));
  }
}
