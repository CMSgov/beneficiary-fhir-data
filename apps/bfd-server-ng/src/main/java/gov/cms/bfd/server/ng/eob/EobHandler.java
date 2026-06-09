package gov.cms.bfd.server.ng.eob;

import static gov.cms.bfd.server.ng.util.MetricTimer.SAMHSA_FILTER_MODE;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import gov.cms.bfd.server.ng.ClaimSecurityStatus;
import gov.cms.bfd.server.ng.SamhsaFilterMode;
import gov.cms.bfd.server.ng.SecurityLabel;
import gov.cms.bfd.server.ng.beneficiary.BeneficiaryRepository;
import gov.cms.bfd.server.ng.claim.ClaimRepository;
import gov.cms.bfd.server.ng.claim.model.*;
import gov.cms.bfd.server.ng.input.ClaimSearchCriteria;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.loadprogress.LoadProgressRepository;
import gov.cms.bfd.server.ng.util.FhirUtil;
import gov.cms.bfd.server.ng.util.IdrConstants;
import gov.cms.bfd.server.ng.util.MetricTimer;
import gov.cms.bfd.server.ng.util.SystemUrls;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
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
  private final MeterRegistry meterRegistry;
  private final MetricTimer metricTimer;

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
    var eobs =
        searchByIdsInner(
            List.of(fhirId), new DateTimeRange(), new DateTimeRange(), samhsaFilterMode);
    return eobs.stream().findFirst();
  }

  /**
   * Search for claims data by bene.
   *
   * @param criteria filter criteria
   * @param samhsaFilterMode SAMHSA filter mode
   * @param requestDetails Hapi FHIR request details
   * @return bundle
   */
  public Bundle searchByBene(
      ClaimSearchCriteria criteria,
      SamhsaFilterMode samhsaFilterMode,
      Optional<RequestDetails> requestDetails) {

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

    var filteredClaims =
        metricTimer.recordMetric(
            "application.eob.handler.search_by_bene",
            () ->
                filterSamhsaClaims(claims, samhsaFilterMode)
                    .skip(repositoryCriteria.resolveOffset())
                    // we need to do this to know if we need include a link down stream
                    // any method of counting will consume the stream, so we do this to allow the
                    // down stream counting to see there is more than the limit without needing to
                    // materialize the entire list in memory here which could be very large and
                    // impact performance
                    .limit(repositoryCriteria.resolveLimit(true))
                    .map(claim -> transformToFhir(claim, samhsaFilterMode)),
            _ -> Tags.of(SAMHSA_FILTER_MODE, samhsaFilterMode.name()));

    var bundle =
        FhirUtil.bundleOrDefault(
            filteredClaims,
            loadProgressRepository::lastUpdated,
            requestDetails,
            // we want the raw limit
            Optional.of(repositoryCriteria.resolveLimit(false)),
            Optional.of(repositoryCriteria.resolveOffset()));
    recordResultSize(bundle, samhsaFilterMode);
    return bundle;
  }

  private void recordResultSize(Bundle bundle, SamhsaFilterMode samhsaFilterMode) {
    DistributionSummary.builder("application.eob.handler.results.size")
        .tag(SAMHSA_FILTER_MODE, samhsaFilterMode.name())
        .register(meterRegistry)
        .record(bundle.getEntry().size());
  }

  private Stream<? extends ClaimBase> filterSamhsaClaims(
      List<? extends ClaimBase> claims, SamhsaFilterMode samhsaFilterMode) {
    // Process claims in parallel
    // Note: DO NOT call toList() until the very end as materializing the list multiple times could
    // negatively impact perf.
    var claimStream = claims.parallelStream();
    return switch (samhsaFilterMode) {
      case INCLUDE -> claimStream.sorted(Comparator.comparing(ClaimBase::getClaimUniqueId));
      // it is faster to filter unordered so if we are filtering we should do it unordered first
      // before the id ordering
      case ONLY_SAMHSA ->
          claimStream
              .unordered()
              .filter(this::claimHasSamhsa)
              .sorted(Comparator.comparing(ClaimBase::getClaimUniqueId));
      case EXCLUDE ->
          claimStream
              .unordered()
              .filter(claim -> !claimHasSamhsa(claim))
              .sorted(Comparator.comparing(ClaimBase::getClaimUniqueId));
    };
  }

  /**
   * Search for claims data by claim ID.
   *
   * @param claimUniqueIds claim IDs
   * @param serviceDate service date
   * @param lastUpdated last updated
   * @param samhsaFilterMode SAMHSA filter mode
   * @return bundle
   */
  public Bundle searchById(
      List<Long> claimUniqueIds,
      DateTimeRange serviceDate,
      DateTimeRange lastUpdated,
      SamhsaFilterMode samhsaFilterMode) {
    var eobs = searchByIdsInner(claimUniqueIds, serviceDate, lastUpdated, samhsaFilterMode);
    return FhirUtil.bundleOrDefault(eobs.stream(), loadProgressRepository::lastUpdated);
  }

  private List<ExplanationOfBenefit> searchByIdsInner(
      List<Long> claimUniqueIds,
      DateTimeRange serviceDate,
      DateTimeRange lastUpdated,
      SamhsaFilterMode samhsaFilterMode) {
    var claims = claimRepository.findByIds(claimUniqueIds, serviceDate, lastUpdated);

    return filterSamhsaClaims(claims, samhsaFilterMode)
        .map(claim -> transformToFhir(claim, samhsaFilterMode))
        .toList();
  }

  /**
   * Transforms a claim to an EOB, applying SAMHSA security labels if applicable based on the filter
   * mode.
   *
   * @param claim the claim
   * @param samhsaFilterMode filter mode (only samhsa/exclude claims/include)
   * @return the transformed EOB
   */
  private ExplanationOfBenefit transformToFhir(ClaimBase claim, SamhsaFilterMode samhsaFilterMode) {
    var isSamhsa =
        switch (samhsaFilterMode) {
          case ONLY_SAMHSA -> true;
          case EXCLUDE -> false;
          case INCLUDE -> claimHasSamhsa(claim);
        };

    var securityStatus =
        isSamhsa ? ClaimSecurityStatus.SAMHSA_APPLICABLE : ClaimSecurityStatus.NONE;

    return claim.toFhir(securityStatus);
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
  private boolean claimHasSamhsa(ClaimBase claim) {
    var claimUniqueId = claim.getClaimUniqueId();
    var claimThroughDate =
        claim.getBillablePeriod().getClaimThroughDate().orElse(IdrConstants.DEFAULT_DATE);
    var drgSamhsa = drgIsSamhsa(claim, claimThroughDate, claimUniqueId);
    var claimItemSamhsa =
        claim.getItems().stream()
            .anyMatch(e -> claimItemIsSamhsa(e, claimThroughDate, claimUniqueId));

    return drgSamhsa || claimItemSamhsa;
  }

  private boolean claimItemIsSamhsa(
      ClaimItemBase claimItem, LocalDate claimThroughDate, long claimUniqueId) {
    return procedureIsSamhsa(claimItem.getProcedure(), claimThroughDate, claimUniqueId)
        || hcpcsIsSamhsa(claimItem.getClaimLineHcpcsCode(), claimThroughDate, claimUniqueId);
  }

  private boolean drgIsSamhsa(ClaimBase claim, LocalDate claimDate, long claimUniqueId) {
    var entries = SECURITY_LABELS.get(SystemUrls.CMS_MS_DRG);
    var drg = claim.getDrgCode().map(Object::toString).orElse("");
    return entries.stream()
        .anyMatch(
            e -> isCodeSamhsa(drg, claimDate, e, "DRG", claimUniqueId, SystemUrls.CMS_MS_DRG));
  }

  private boolean hcpcsIsSamhsa(
      Optional<ClaimLineHcpcsCode> hcpcsCode, LocalDate claimDate, long claimUniqueId) {
    if (hcpcsCode.isEmpty()) {
      return false;
    }
    var hcpcs = hcpcsCode.get().getHcpcsCode().orElse("");
    return Stream.of(SystemUrls.AMA_CPT, SystemUrls.CMS_HCPCS)
        .flatMap(s -> SECURITY_LABELS.get(s).stream().map(c -> Map.entry(s, c)))
        .anyMatch(
            e -> isCodeSamhsa(hcpcs, claimDate, e.getValue(), "HCPCS", claimUniqueId, e.getKey()));
  }

  // Checks ICDs.
  private boolean procedureIsSamhsa(
      Optional<? extends ClaimProcedureBase> proc, LocalDate claimDate, long claimUniqueId) {
    if (proc.isEmpty()) {
      return false;
    }
    var procedure = proc.get();
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
