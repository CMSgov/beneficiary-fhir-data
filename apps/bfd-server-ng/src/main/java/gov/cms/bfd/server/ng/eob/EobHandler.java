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
        .filter(claim -> !claimHasSamhsa(claim))
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
    var drgSamhsa = drgCodeIsSamhsa(claim, claimThroughDate);
    var claimItemSamhsa =
        claim.getClaimItems().stream().anyMatch(e -> claimItemIsSamhsa(e, claimThroughDate));

    return drgSamhsa || claimItemSamhsa;
  }

  private boolean claimItemIsSamhsa(ClaimItem claimItem, LocalDate claimThroughDate) {
    return procedureMatchesSamhsaCode(claimItem.getClaimProcedure(), claimThroughDate)
        || hcpcsCodeMatches(claimItem.getClaimLine(), claimThroughDate);
  }

  private boolean drgCodeIsSamhsa(Claim claim, LocalDate claimDate) {
    var entries = SECURITY_LABELS_MAP.get(SystemUrls.CMS_MS_DRG);
    var drg = claim.getDrgCode().map(Object::toString).orElse("");
    return entries.stream().anyMatch(e -> isCodeSamhsa(drg, claimDate, e));
  }

  private boolean hcpcsCodeMatches(ClaimLine claimLine, LocalDate claimDate) {
    var hcpcs = claimLine.getHcpcsCode().getHcpcsCode().orElse("");
    return Stream.of(SystemUrls.AMA_CPT, SystemUrls.CMS_HCPCS)
        .flatMap(s -> SECURITY_LABELS_MAP.get(s).stream())
        .anyMatch(c -> isCodeSamhsa(hcpcs, claimDate, c));
  }

  // Checks ICDs.
  private boolean procedureMatchesSamhsaCode(ClaimProcedure proc, LocalDate claimDate) {
    var diagnosisCode = proc.getDiagnosisCode().orElse("");
    var procedureCode = proc.getProcedureCode().orElse("");
    // If the ICD indicator isn't something valid, it's probably a PAC claim with a mistake in the
    // data entry.
    // PAC claims will almost always be using ICD 10 these days, so ICD 10 is the safer assumption
    // here.
    var icdIndicator = proc.getIcdIndicator().orElse(IcdIndicator.ICD_10);

    var procedureEntries = SECURITY_LABELS_MAP.get(icdIndicator.getProcedureSystem());
    var diagnosisEntries = SECURITY_LABELS_MAP.get(icdIndicator.getDiagnosisSystem());

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
}
