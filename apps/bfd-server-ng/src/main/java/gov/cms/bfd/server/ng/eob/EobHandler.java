package gov.cms.bfd.server.ng.eob;

import gov.cms.bfd.server.ng.ClaimSecurityStatus;
import gov.cms.bfd.server.ng.SamhsaFilterMode;
import gov.cms.bfd.server.ng.SecurityLabel;
import gov.cms.bfd.server.ng.beneficiary.BeneficiaryRepository;
import gov.cms.bfd.server.ng.claim.ClaimRepository;
import gov.cms.bfd.server.ng.claim.model.Claim;
import gov.cms.bfd.server.ng.claim.model.ClaimItem;
import gov.cms.bfd.server.ng.claim.model.ClaimLine;
import gov.cms.bfd.server.ng.claim.model.ClaimProcedure;
import gov.cms.bfd.server.ng.claim.model.IcdIndicator;
import gov.cms.bfd.server.ng.input.ClaimSearchCriteria;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.loadprogress.LoadProgressRepository;
import gov.cms.bfd.server.ng.util.FhirUtil;
import gov.cms.bfd.server.ng.util.IdrConstants;
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
  private final LoadProgressRepository loadProgressRepository;

  // Cache the security labels map to avoid repeated I/O and parsing
  private static final Map<String, List<SecurityLabel>> SECURITY_LABELS =
      SecurityLabel.getSecurityLabels();

  /**
   * Returns an {@link ExplanationOfBenefit} by its FHIR ID.
   *
   * @param fhirId FHIR ID
   * @param samhsaFilterMode SAMHSA filter mode
   * @return an Optional containing the ExplanationOfBenefit if found
   */
  public Optional<ExplanationOfBenefit> find(final Long fhirId, SamhsaFilterMode samhsaFilterMode) {
    return searchByIdInner(fhirId, new DateTimeRange(), new DateTimeRange(), samhsaFilterMode);
  }

  /**
   * Search for claims data by bene.
   *
   * @param criteria filter criteria
   * @param samhsaFilterMode SAMHSA filter mode
   * @return bundle
   */
  public Bundle searchByBene(ClaimSearchCriteria criteria, SamhsaFilterMode samhsaFilterMode) {
    var beneSk = criteria.beneSk();
    var beneXrefSk = beneficiaryRepository.getXrefSkFromBeneSk(beneSk);
    // Don't return data for historical beneSks
    if (beneXrefSk.isEmpty() || !beneXrefSk.get().equals(beneSk)) {
      return new Bundle();
    }

    var repositoryCriteria =
        new ClaimSearchCriteria(
            beneXrefSk.get(),
            criteria.claimThroughDate(),
            criteria.lastUpdated(),
            criteria.limit(),
            criteria.offset(),
            criteria.tagCriteria(),
            criteria.claimTypeCodes(),
            criteria.sources());

    var claims = claimRepository.findByBeneXrefSk(repositoryCriteria);

    var filteredClaims = filterSamhsaClaims(claims, samhsaFilterMode, repositoryCriteria);

    return FhirUtil.bundleOrDefault(
        filteredClaims.map(
            claim -> {
              var hasSamhsaClaims =
                  samhsaFilterMode == SamhsaFilterMode.INCLUDE && claimHasSamhsa(claim);

              var securityStatus =
                  hasSamhsaClaims
                      ? ClaimSecurityStatus.SAMHSA_APPLICABLE
                      : ClaimSecurityStatus.NONE;

              return claim.toFhir(securityStatus);
            }),
        loadProgressRepository::lastUpdated);
  }

  private Stream<Claim> filterSamhsaClaims(
      List<Claim> claims,
      SamhsaFilterMode samhsaFilterMode,
      ClaimSearchCriteria claimSearchCriteria) {

    var claimStream = claims.stream().sorted(Comparator.comparing(Claim::getClaimUniqueId));

    var filteredClaimStream =
        switch (samhsaFilterMode) {
          case INCLUDE -> claimStream;
          case ONLY_SAMHSA -> claimStream.filter(this::claimHasSamhsa);
          case EXCLUDE -> claimStream.filter(claim -> !claimHasSamhsa(claim));
        };

    return filteredClaimStream
        .skip(claimSearchCriteria.resolveOffset())
        .limit(claimSearchCriteria.resolveLimit());
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
    return FhirUtil.bundleOrDefault(eob.map(e -> e), loadProgressRepository::lastUpdated);
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
    var claimHasSamhsa = claimHasSamhsa(claim);

    if (samhsaFilterMode == SamhsaFilterMode.EXCLUDE && claimHasSamhsa) {
      return Optional.empty();
    }
    var securityStatus =
        claimHasSamhsa ? ClaimSecurityStatus.SAMHSA_APPLICABLE : ClaimSecurityStatus.NONE;

    return Optional.of(claim.toFhir(securityStatus));
  }

  private boolean isCodeSamhsa(
      String targetCode,
      LocalDate claimDate,
      SecurityLabel entry,
      String type,
      long claimId,
      String system) {
    var isSamhsa = isClaimDateWithinBounds(claimDate, entry) && entry.matches(targetCode);
    if (isSamhsa) {
      LOGGER
          .atInfo()
          .setMessage("SAMHSA claim filtered: type=" + type)
          .addKeyValue("type", type)
          .addKeyValue("claimId", claimId)
          .addKeyValue("matchedCode", targetCode)
          .addKeyValue("system", system)
          .log();
    }
    return isSamhsa;
  }

  // Returns true if the given claim contains any procedure that matches a SAMHSA
  // security label code from the dictionary.
  private boolean claimHasSamhsa(Claim claim) {
    var claimUniqueId = claim.getClaimUniqueId();
    var claimThroughDate =
        claim.getBillablePeriod().getClaimThroughDate().orElse(IdrConstants.DEFAULT_DATE);
    var drgSamhsa = drgIsSamhsa(claim, claimThroughDate, claimUniqueId);
    var claimItemSamhsa =
        claim.getClaimItems().stream()
            .anyMatch(e -> claimItemIsSamhsa(e, claimThroughDate, claimUniqueId));

    return drgSamhsa || claimItemSamhsa;
  }

  private boolean claimItemIsSamhsa(
      ClaimItem claimItem, LocalDate claimThroughDate, long claimUniqueId) {
    return procedureIsSamhsa(claimItem.getClaimProcedure(), claimThroughDate, claimUniqueId)
        || hcpcsIsSamhsa(claimItem.getClaimLine(), claimThroughDate, claimUniqueId);
  }

  private boolean drgIsSamhsa(Claim claim, LocalDate claimDate, long claimUniqueId) {
    var entries = SECURITY_LABELS.get(SystemUrls.CMS_MS_DRG);
    var drg = claim.getDrgCode().map(Object::toString).orElse("");
    return entries.stream()
        .anyMatch(
            e -> isCodeSamhsa(drg, claimDate, e, "DRG", claimUniqueId, SystemUrls.CMS_MS_DRG));
  }

  private boolean hcpcsIsSamhsa(ClaimLine claimLine, LocalDate claimDate, long claimUniqueId) {
    var hcpcs = claimLine.getHcpcsCode().getHcpcsCode().orElse("");
    return Stream.of(SystemUrls.AMA_CPT, SystemUrls.CMS_HCPCS)
        .flatMap(s -> SECURITY_LABELS.get(s).stream().map(c -> Map.entry(s, c)))
        .anyMatch(
            e -> isCodeSamhsa(hcpcs, claimDate, e.getValue(), "HCPCS", claimUniqueId, e.getKey()));
  }

  // Checks ICDs.
  private boolean procedureIsSamhsa(
      ClaimProcedure procedure, LocalDate claimDate, long claimUniqueId) {
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
            .anyMatch(
                pEntries ->
                    isCodeSamhsa(
                        procedureCode,
                        claimDate,
                        pEntries,
                        "Procedure",
                        claimUniqueId,
                        icdIndicator.getProcedureSystem()));

    var diagnosisHasSamhsa =
        diagnosisEntries.stream()
            .anyMatch(
                dEntry ->
                    isCodeSamhsa(
                        diagnosisCode,
                        claimDate,
                        dEntry,
                        "Diagnosis",
                        claimUniqueId,
                        icdIndicator.getDiagnosisSystem()));

    return procedureHasSamhsa || diagnosisHasSamhsa;
  }

  private boolean isClaimDateWithinBounds(LocalDate claimDate, SecurityLabel entry) {
    var entryStart = entry.getStartDateAsDate();
    var entryEnd = entry.getEndDateAsDate();
    return !entryStart.isAfter(claimDate) && !entryEnd.isBefore(claimDate);
  }
}
