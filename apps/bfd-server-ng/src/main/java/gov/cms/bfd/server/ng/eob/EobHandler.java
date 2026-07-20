package gov.cms.bfd.server.ng.eob;

import static gov.cms.bfd.server.ng.util.MetricRecorder.SAMHSA_FILTER_MODE;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import gov.cms.bfd.server.ng.ClaimFilterOptions;
import gov.cms.bfd.server.ng.ClaimSecurityStatus;
import gov.cms.bfd.server.ng.SamhsaFilterMode;
import gov.cms.bfd.server.ng.SecurityLabel;
import gov.cms.bfd.server.ng.beneficiary.BeneficiaryRepository;
import gov.cms.bfd.server.ng.claim.ClaimRepository;
import gov.cms.bfd.server.ng.claim.PriorAuthorizationRepository;
import gov.cms.bfd.server.ng.claim.model.*;
import gov.cms.bfd.server.ng.input.ClaimIdSearchCriteria;
import gov.cms.bfd.server.ng.input.ClaimSearchCriteria;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.loadprogress.LoadProgressRepository;
import gov.cms.bfd.server.ng.util.FhirUtil;
import gov.cms.bfd.server.ng.util.IdrConstants;
import gov.cms.bfd.server.ng.util.MetricRecorder;
import gov.cms.bfd.server.ng.util.SystemUrls;
import io.micrometer.core.instrument.Tags;
import java.time.LocalDate;
import java.util.*;
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
  private final PriorAuthorizationRepository priorAuthorizationRepository;
  private final MetricRecorder metricRecorder;

  // Cache the security labels map to avoid repeated I/O and parsing
  private static final Map<String, List<SecurityLabel>> SECURITY_LABELS =
      SecurityLabel.getSecurityLabels();

  /**
   * Returns an {@link ExplanationOfBenefit} by its FHIR ID.
   *
   * @param fhirId FHIR ID
   * @param options claim filter options
   * @return an Optional containing the ExplanationOfBenefit if found
   */
  public Optional<ExplanationOfBenefit> find(final Long fhirId, ClaimFilterOptions options) {
    var eobs =
        searchByIdsInner(
            new ClaimIdSearchCriteria(
                List.of(fhirId),
                new DateTimeRange(),
                new DateTimeRange(),
                Collections.emptyList(),
                Collections.emptyList()),
            options);
    return eobs.stream().findFirst();
  }

  /**
   * Search for claims data by bene.
   *
   * @param criteria filter criteria
   * @param options claim filter options
   * @param requestDetails Hapi FHIR request details
   * @return bundle
   */
  public Bundle searchByBene(
      ClaimSearchCriteria criteria,
      ClaimFilterOptions options,
      Optional<RequestDetails> requestDetails) {

    var beneSk = criteria.beneSk();
    var beneSimple = beneficiaryRepository.getXrefSkFromBeneSk(beneSk);
    // Don't return data for historical beneSks
    if (beneSimple.isEmpty() || (beneSimple.get().getXrefSk() != beneSk)) {
      return new Bundle();
    }
    var beneficiary = beneSimple.get();

    var repositoryCriteria =
        new ClaimSearchCriteria(
            beneficiary.getXrefSk(),
            beneficiary.getMbi(),
            criteria.claimThroughDate(),
            criteria.lastUpdated(),
            criteria.limit(),
            criteria.offset(),
            criteria.tagCriteria(),
            criteria.claimTypeCodes(),
            criteria.outcomes(),
            criteria.sources());

    var claimAndAuthResult = claimRepository.findByBeneXrefSk(repositoryCriteria);
    var claims = claimAndAuthResult.claims();
    var priorAuths = claimAndAuthResult.priorAuths();
    var samhsaFilterMode = options.getSamhsaFilterMode();

    var bundle =
        metricRecorder.recordMetric(
            "application.eob.handler.transform",
            () -> {
              var filteredClaims =
                  filterSamhsaClaims(claims, samhsaFilterMode)
                      .map(claim -> transformToFhir(claim, options));
              var filteredPriorAuths =
                  filterPriorAuthorizations(priorAuths, samhsaFilterMode)
                      .map(priorAuth -> transformPriorAuthorizationToFhir(priorAuth, options));
              var combinedResources =
                  Stream.concat(filteredClaims, filteredPriorAuths)
                      .skip(repositoryCriteria.resolveOffset())
                      .limit(repositoryCriteria.resolveLimitWithExtra(1));
              return FhirUtil.bundleOrDefault(
                  combinedResources,
                  loadProgressRepository::lastUpdated,
                  requestDetails,
                  // we want the raw limit
                  Optional.of(repositoryCriteria.resolveLimit()),
                  Optional.of(repositoryCriteria.resolveOffset()));
            },
            _ -> Tags.of(SAMHSA_FILTER_MODE, samhsaFilterMode.name()));
    metricRecorder.recordDistribution(
        "application.eob.handler.results.size",
        bundle.getEntry().size(),
        SAMHSA_FILTER_MODE,
        samhsaFilterMode.name());
    return bundle;
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

  private Stream<PriorAuthorization> filterPriorAuthorizations(
      List<PriorAuthorization> priorAuths, SamhsaFilterMode samhsaFilterMode) {
    var priorAuthStream = priorAuths.parallelStream();
    return switch (samhsaFilterMode) {
      case INCLUDE -> priorAuthStream;
      case ONLY_SAMHSA -> priorAuthStream.filter(this::priorAuthorizationHasSamhsa);
      case EXCLUDE -> priorAuthStream.filter(priorAuth -> !priorAuthorizationHasSamhsa(priorAuth));
    };
  }

  /**
   * Search for claims data by claim ID.
   *
   * @param criteria id search criteria
   * @param options claim filter options
   * @return bundle
   */
  public Bundle searchById(ClaimIdSearchCriteria criteria, ClaimFilterOptions options) {
    var eobs = searchByIdsInner(criteria, options);
    return FhirUtil.bundleOrDefault(eobs.stream(), loadProgressRepository::lastUpdated);
  }

  private List<ExplanationOfBenefit> searchByIdsInner(
      ClaimIdSearchCriteria criteria, ClaimFilterOptions options) {
    var claims = claimRepository.findByIds(criteria);

    return filterSamhsaClaims(claims, options.getSamhsaFilterMode())
        .map(claim -> transformToFhir(claim, options))
        .toList();
  }

  /**
   * Transforms a claim to an EOB, applying SAMHSA security labels if applicable based on the filter
   * mode.
   *
   * @param claim the claim
   * @param options claim filter options
   * @return the transformed EOB
   */
  private ExplanationOfBenefit transformToFhir(ClaimBase claim, ClaimFilterOptions options) {
    var isSamhsa =
        switch (options.getSamhsaFilterMode()) {
          case ONLY_SAMHSA -> true;
          case EXCLUDE -> false;
          case INCLUDE -> claimHasSamhsa(claim);
        };

    var securityStatus =
        isSamhsa ? ClaimSecurityStatus.SAMHSA_APPLICABLE : ClaimSecurityStatus.NONE;

    var claimState = ClaimState.builder().securityStatus(securityStatus).build();

    return claim.toFhir(options, claimState);
  }

  private ExplanationOfBenefit transformPriorAuthorizationToFhir(
      PriorAuthorization priorAuth, ClaimFilterOptions options) {
    var isSamhsa =
        switch (options.getSamhsaFilterMode()) {
          case ONLY_SAMHSA -> true;
          case EXCLUDE -> false;
          case INCLUDE -> priorAuthorizationHasSamhsa(priorAuth);
        };

    var securityStatus =
        isSamhsa ? ClaimSecurityStatus.SAMHSA_APPLICABLE : ClaimSecurityStatus.NONE;
    var claimState = ClaimState.builder().securityStatus(securityStatus).build();
    return priorAuth.toFhir(claimState);
  }

  private boolean isCodeSamhsa(
      String targetCode,
      LocalDate claimDate,
      SecurityLabel entry,
      String type,
      String id,
      String system) {
    if (targetCode.isEmpty()) {
      return false;
    }
    var isSamhsa = isClaimDateWithinBounds(claimDate, entry) && entry.matches(targetCode);
    if (isSamhsa) {
      LOGGER
          .atInfo()
          .setMessage("SAMHSA eob filtered: type=" + type)
          .addKeyValue("type", type)
          .addKeyValue("id", id)
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
    var drg = claim.getDrgCode().map(Object::toString).orElse("");
    return codeIsSamhsa(
        drg, claimDate, String.valueOf(claimUniqueId), "DRG", List.of(SystemUrls.CMS_MS_DRG));
  }

  private boolean hcpcsIsSamhsa(
      Optional<ClaimLineHcpcsCode> hcpcsCode, LocalDate claimDate, long claimUniqueId) {
    if (hcpcsCode.isEmpty()) {
      return false;
    }
    var hcpcs = hcpcsCode.get().getHcpcsCode().orElse("");
    return codeIsSamhsa(
        hcpcs,
        claimDate,
        String.valueOf(claimUniqueId),
        "HCPCS",
        List.of(SystemUrls.AMA_CPT, SystemUrls.CMS_HCPCS));
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

    var procedureHasSamhsa =
        codeIsSamhsa(
            procedureCode,
            claimDate,
            String.valueOf(claimUniqueId),
            "Procedure",
            List.of(icdIndicator.getProcedureSystem()));
    var diagnosisHasSamhsa =
        codeIsSamhsa(
            diagnosisCode,
            claimDate,
            String.valueOf(claimUniqueId),
            "Diagnosis",
            List.of(icdIndicator.getDiagnosisSystem()));

    return procedureHasSamhsa || diagnosisHasSamhsa;
  }

  private boolean isClaimDateWithinBounds(LocalDate claimDate, SecurityLabel entry) {
    var entryStart = entry.getStartDate();
    var entryEnd = entry.getEndDate();
    return !entryStart.isAfter(claimDate) && !entryEnd.isBefore(claimDate);
  }

  private boolean codeIsSamhsa(
      String code, LocalDate date, String id, String type, Collection<String> systems) {
    return systems.stream()
        .flatMap(
            system -> SECURITY_LABELS.get(system).stream().map(label -> Map.entry(system, label)))
        .anyMatch(entry -> isCodeSamhsa(code, date, entry.getValue(), type, id, entry.getKey()));
  }

  private boolean priorAuthorizationHasSamhsa(PriorAuthorization priorAuth) {
    var priorAuthDate = priorAuth.getUniqueTrackingNumberPeriod().getStartDate();
    return priorAuth.getItems().stream()
        .anyMatch(item -> priorAuthorizationItemIsSamhsa(priorAuth, item, priorAuthDate));
  }

  private boolean priorAuthorizationItemIsSamhsa(
      PriorAuthorization priorAuth, PriorAuthorizationItem item, LocalDate priorAuthDate) {
    var code = item.getHcpcsOrCptOrHipps().getHcpcsOrCptOrHipps();
    var claimType = priorAuth.getClaimType();

    return codeIsSamhsa(
        code,
        priorAuthDate,
        priorAuth.getResourceId() + ":" + item.getCurrentSegment(),
        "PriorAuth HCPCS/CPT/HIPPS",
        List.of(item.getHcpcsOrCptOrHipps().getCodingSystem(claimType)));
  }
}
