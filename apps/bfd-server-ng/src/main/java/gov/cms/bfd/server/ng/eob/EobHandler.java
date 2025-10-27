package gov.cms.bfd.server.ng.eob;

import gov.cms.bfd.server.ng.SamhsaFilterMode;
import gov.cms.bfd.server.ng.SecurityLabel;
import gov.cms.bfd.server.ng.beneficiary.BeneficiaryRepository;
import gov.cms.bfd.server.ng.claim.ClaimRepository;
import gov.cms.bfd.server.ng.claim.model.Claim;
import gov.cms.bfd.server.ng.claim.model.ClaimItem;
import gov.cms.bfd.server.ng.claim.model.ClaimLine;
import gov.cms.bfd.server.ng.claim.model.ClaimProcedure;
import gov.cms.bfd.server.ng.claim.model.ClaimSourceId;
import gov.cms.bfd.server.ng.claim.model.IcdIndicator;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.util.FhirUtil;
import gov.cms.bfd.server.ng.util.SystemUrls;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Handler methods for the ExplanationOfBenefit resource. This is called after the FHIR inputs from
 * the resource provider are converted into input types that are easier to work with.
 */
@Component
@RequiredArgsConstructor
public class EobHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(EobHandler.class);

  private final BeneficiaryRepository beneficiaryRepository;
  private final ClaimRepository claimRepository;

  // Cache the security labels map to avoid repeated I/O and parsing
  private static final Map<String, List<SecurityLabel>> SECURITY_LABELS =
      SecurityLabel.getSecurityLabels();

  /**
   * Returns a {@link Patient} by their {@link IdType}.
   *
   * @param fhirId FHIR ID
   * @param samhsaFilterMode SAMHSA filter mode
   * @return patient
   */
  public Optional<ExplanationOfBenefit> find(final Long fhirId, SamhsaFilterMode samhsaFilterMode) {
    return searchByIdInner(fhirId, new DateTimeRange(), new DateTimeRange(), samhsaFilterMode);
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
   * @param samhsaFilterMode SAMHSA filter mode
   * @return bundle
   */
  public Bundle searchByBene(
      Long beneSk,
      Optional<Integer> count,
      DateTimeRange serviceDate,
      DateTimeRange lastUpdated,
      Optional<Integer> startIndex,
      List<ClaimSourceId> sourceIds,
      SamhsaFilterMode samhsaFilterMode) {
    var beneXrefSk = beneficiaryRepository.getXrefSkFromBeneSk(beneSk);
    // Don't return data for historical beneSks
    if (beneXrefSk.isEmpty() || !beneXrefSk.get().equals(beneSk)) {
      return new Bundle();
    }

    var claims =
        claimRepository.findByBeneXrefSk(
            beneXrefSk.get(), serviceDate, lastUpdated, count, startIndex, sourceIds);

    var filteredClaims = filterSamhsaClaims(claims, samhsaFilterMode);
    return FhirUtil.bundleOrDefault(
        filteredClaims.map(Claim::toFhir), claimRepository::claimLastUpdated);
  }

  private Stream<Claim> filterSamhsaClaims(List<Claim> claims, SamhsaFilterMode samhsaFilterMode) {
    if (samhsaFilterMode == SamhsaFilterMode.INCLUDE) {
      return claims.stream();
    }
    // Ordering may have changed during filtering, ensure we re-order before returning the final
    // result
    return claims.stream()
        .filter(
            claim -> {
              var samhsaCodes = findSamhsaCodesInClaim(claim);
              if (!samhsaCodes.isEmpty()) {
                logSamhsaClaimFiltered(claim.getClaimUniqueId(), samhsaCodes);
                return false; // exclude the claim
              }
              return true; // Keep the claim
            })
        .sorted(Comparator.comparing(Claim::getClaimUniqueId));
  }

  /**
   * Search for claims data by claim ID.
   *
   * @param claimUniqueId claim ID
   * @param serviceDate service date
   * @param lastUpdated last updated
   * @param samhsaFilterMode SAMHSA filter mode
   * @return bundle
   */
  public Bundle searchById(
      Long claimUniqueId,
      DateTimeRange serviceDate,
      DateTimeRange lastUpdated,
      SamhsaFilterMode samhsaFilterMode) {
    var eob = searchByIdInner(claimUniqueId, serviceDate, lastUpdated, samhsaFilterMode);
    return FhirUtil.bundleOrDefault(eob.map(e -> e), claimRepository::claimLastUpdated);
  }

  private Optional<ExplanationOfBenefit> searchByIdInner(
      Long claimUniqueId,
      DateTimeRange serviceDate,
      DateTimeRange lastUpdated,
      SamhsaFilterMode samhsaFilterMode) {
    var claimOpt = claimRepository.findById(claimUniqueId, serviceDate, lastUpdated);
    if (claimOpt.isEmpty()) {
      return Optional.empty();
    }
    var claim = claimOpt.get();

    if (samhsaFilterMode == SamhsaFilterMode.EXCLUDE && claimHasSamhsa(claim)) {
      return Optional.empty();
    }
    return Optional.of(claim.toFhir());
  }

  private boolean isCodeSamhsa(String targetCode, LocalDate claimDate, SecurityLabel entry) {
    return isClaimDateWithinBounds(claimDate, entry) && entry.matches(targetCode);
  }

  // Returns true if the given claim contains any procedure that matches a SAMHSA
  // security label code from the dictionary.
  private boolean claimHasSamhsa(Claim claim) {
    var claimThroughDate = claim.getBillablePeriod().getClaimThroughDate();
    var drgSamhsa = drgIsSamhsa(claim, claimThroughDate);
    var claimItemSamhsa =
        claim.getClaimItems().stream().anyMatch(e -> claimItemIsSamhsa(e, claimThroughDate));

    return drgSamhsa || claimItemSamhsa;
  }

  private boolean claimItemIsSamhsa(ClaimItem claimItem, LocalDate claimThroughDate) {
    return procedureIsSamhsa(claimItem.getClaimProcedure(), claimThroughDate)
        || hcpcsIsSamhsa(claimItem.getClaimLine(), claimThroughDate);
  }

  private boolean drgIsSamhsa(Claim claim, LocalDate claimDate) {
    var entries = SECURITY_LABELS.get(SystemUrls.CMS_MS_DRG);
    var drg = claim.getDrgCode().map(Object::toString).orElse("");
    return entries.stream().anyMatch(e -> isCodeSamhsa(drg, claimDate, e));
  }

  private boolean hcpcsIsSamhsa(ClaimLine claimLine, LocalDate claimDate) {
    var hcpcs = claimLine.getHcpcsCode().getHcpcsCode().orElse("");
    return Stream.of(SystemUrls.AMA_CPT, SystemUrls.CMS_HCPCS)
        .flatMap(s -> SECURITY_LABELS.get(s).stream())
        .anyMatch(c -> isCodeSamhsa(hcpcs, claimDate, c));
  }

  // Checks ICDs.
  private boolean procedureIsSamhsa(ClaimProcedure procedure, LocalDate claimDate) {
    var diagnosisCode = procedure.getDiagnosisCode().orElse("");
    var procedureCode = procedure.getProcedureCode().orElse("");
    // If the ICD indicator isn't something valid, it's probably a PAC claim with a mistake in the
    // data entry.
    // PAC claims will almost always be using ICD 10 these days, so ICD 10 is the safer assumption
    // here.
    var icdIndicator = procedure.getIcdIndicator().orElse(IcdIndicator.ICD_10);

    var procedureEntries = SECURITY_LABELS.get(icdIndicator.getProcedureSystem());
    var diagnosisEntries = SECURITY_LABELS.get(icdIndicator.getDiagnosisSystem());

    var procedureHasSamhsa =
        procedureEntries.stream()
            .anyMatch(pEntries -> isCodeSamhsa(procedureCode, claimDate, pEntries));
    var diagnosisHasSamhsa =
        diagnosisEntries.stream()
            .anyMatch(dEntry -> isCodeSamhsa(diagnosisCode, claimDate, dEntry));

    return procedureHasSamhsa || diagnosisHasSamhsa;
  }

  private boolean isClaimDateWithinBounds(LocalDate claimDate, SecurityLabel entry) {
    var entryStart = entry.getStartDateAsDate();
    var entryEnd = entry.getEndDateAsDate();
    return !entryStart.isAfter(claimDate) && !entryEnd.isBefore(claimDate);
  }

  // Finds all SAMHSA codes in the given claim.
  private List<SamhsaCodeMatch> findSamhsaCodesInClaim(Claim claim) {
    var claimThroughDate = claim.getBillablePeriod().getClaimThroughDate();
    var matches = new ArrayList<SamhsaCodeMatch>();

    // Check DRG codes
    var drgCodes = findSamhsaDrgCodes(claim, claimThroughDate);
    matches.addAll(drgCodes);

    // Check Procedures and HCPCS codes
    for (var claimItem : claim.getClaimItems()) {
      var procedureCodes =
          findSamhsaProcedureCodes(claimItem.getClaimProcedure(), claimThroughDate);
      matches.addAll(procedureCodes);

      var hcpcsCodes = findSamhsaHcpcsCodes(claimItem.getClaimLine(), claimThroughDate);
      matches.addAll(hcpcsCodes);
    }

    return matches;
  }

  // Finds SAMHSA DRG codes in the claim.
  private List<SamhsaCodeMatch> findSamhsaDrgCodes(Claim claim, LocalDate claimDate) {
    var matches = new ArrayList<SamhsaCodeMatch>();
    var entries = SECURITY_LABELS.get(SystemUrls.CMS_MS_DRG);
    var drg = claim.getDrgCode().map(Object::toString).orElse("");

    if (!drg.isEmpty()) {
      for (var entry : entries) {
        if (isCodeSamhsa(drg, claimDate, entry)) {
          matches.add(new SamhsaCodeMatch(drg, SystemUrls.CMS_MS_DRG, "DRG"));
        }
      }
    }

    return matches;
  }

  // Finds SAMHSA procedure/diagnosis codes in the claim item.
  private List<SamhsaCodeMatch> findSamhsaProcedureCodes(
      ClaimProcedure procedure, LocalDate claimDate) {
    var matches = new ArrayList<SamhsaCodeMatch>();
    var diagnosisCode = procedure.getDiagnosisCode().orElse("");
    var procedureCode = procedure.getProcedureCode().orElse("");
    var icdIndicator = procedure.getIcdIndicator().orElse(IcdIndicator.ICD_10);

    var procedureEntries = SECURITY_LABELS.get(icdIndicator.getProcedureSystem());
    var diagnosisEntries = SECURITY_LABELS.get(icdIndicator.getDiagnosisSystem());

    // Check procedure codes
    if (!procedureCode.isEmpty()) {
      for (var entry : procedureEntries) {
        if (isCodeSamhsa(procedureCode, claimDate, entry)) {
          matches.add(
              new SamhsaCodeMatch(procedureCode, icdIndicator.getProcedureSystem(), "Procedure"));
        }
      }
    }

    // Check diagnosis codes
    if (!diagnosisCode.isEmpty()) {
      for (var entry : diagnosisEntries) {
        if (isCodeSamhsa(diagnosisCode, claimDate, entry)) {
          matches.add(
              new SamhsaCodeMatch(diagnosisCode, icdIndicator.getDiagnosisSystem(), "Diagnosis"));
        }
      }
    }

    return matches;
  }

  // Finds SAMHSA HCPCS/CPT codes in the claim line.
  private List<SamhsaCodeMatch> findSamhsaHcpcsCodes(ClaimLine claimLine, LocalDate claimDate) {
    var matches = new ArrayList<SamhsaCodeMatch>();
    var hcpcs = claimLine.getHcpcsCode().getHcpcsCode().orElse("");

    if (!hcpcs.isEmpty()) {
      for (var system : List.of(SystemUrls.AMA_CPT, SystemUrls.CMS_HCPCS)) {
        var entries = SECURITY_LABELS.get(system);
        for (var entry : entries) {
          if (isCodeSamhsa(hcpcs, claimDate, entry)) {
            matches.add(new SamhsaCodeMatch(hcpcs, system, "HCPCS"));
          }
        }
      }
    }

    return matches;
  }

  // Logs when a claim is filtered due to SAMHSA codes.
  private void logSamhsaClaimFiltered(long claimId, List<SamhsaCodeMatch> samhsaCodes) {
    var codeDetails =
        samhsaCodes.stream()
            .map(
                c ->
                    String.format(
                        "%s (system: %s, type: %s)", c.getCode(), c.getSystem(), c.getCodeType()))
            .reduce((a, b) -> a + "; " + b)
            .orElse("N/A");
    LOGGER.info(
        "SAMHSA claim filtered: claimId={}, matchedCodes=[{}], count={}",
        claimId,
        codeDetails,
        samhsaCodes.size());
  }
}
