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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
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
   * @param startIndex start 160
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
    var eobs =
        claimRepository.findByBeneXrefSk(
            beneXrefSk.get(), serviceDate, lastUpdated, count, startIndex, sourceIds);

    return getFhirEobs(eobs, true);
  }

  /**
   * Converts a list of {@link Claim} objects into a FHIR {@link Bundle} of Explanation of Benefits
   * (EOBs). Optionally filters out claims that contain SAMHSA-coded procedures based on the
   * provided flag.
   *
   * @param claims the list of {@link Claim} objects to be converted into FHIR EOBs
   * @param filterSamhsa a boolean flag indicating whether to filter out claims containing
   *     SAMHSA-coded procedures
   * @return a FHIR {@link Bundle} containing the converted Explanation of Benefits (EOBs)
   */
  protected Bundle getFhirEobs(List<Claim> claims, boolean filterSamhsa) {

    var filteredClaims = claims;
    if (filterSamhsa) {
      filteredClaims =
          claims.stream()
              // keep claims that do NOT contain any SAMHSA-coded procedures
              .filter(claim -> !claimHasSamhsa(claim, SECURITY_LABELS_MAP))
              .toList();
    }

    return FhirUtil.bundleOrDefault(
        filteredClaims.stream().map(Claim::toFhir).map(Resource.class::cast),
        claimRepository::claimLastUpdated);
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
    var eobs = claimRepository.findById(claimUniqueId, serviceDate, lastUpdated).stream().toList();
    Bundle bundle = getFhirEobs(eobs, true);
    return bundle.getEntry().stream()
        .map(Bundle.BundleEntryComponent::getResource)
        .map(ExplanationOfBenefit.class::cast)
        .findFirst();
  }

  private String getDiagnosisCode(ClaimProcedure e) {
    return normalize(e.getDiagnosisCode());
  }

  private String getClaimProcedureCode(ClaimProcedure e) {
    return normalize(e.getProcedureCode());
  }

  private String getDictCode(Map<String, Object> entry) {
    return normalize(Optional.ofNullable(entry.get("code").toString()));
  }

  private String normalize(Optional<String> code) {
    return code.orElse("").trim().replace(".", "").toLowerCase();
  }

  /**
   * Compares two raw code strings using the SAMHSA normalization rules.
   *
   * <p>Normalization steps (applied to both inputs):
   *
   * <ul>
   *   <li>Null -> empty string
   *   <li>Trim leading/trailing whitespace
   *   <li>Remove all '.' characters
   *   <li>Lowercase the result
   * </ul>
   *
   * This ensures comparisons are insensitive to formatting differences (case, dots, or padding).
   *
   * @param source the first (potentially null) raw code value
   * @param target the second (potentially null) raw code value
   * @return true if the normalized forms are equal; false otherwise
   */
  private boolean compare(String source, String target) {
    return normalize(Optional.ofNullable(source)).equals(normalize(Optional.ofNullable(target)));
  }

  // Returns true if the given claim contains any procedure that matches a SAMHSA
  // security label code from the provided dictionary and keys.
  private boolean claimHasSamhsa(Claim claim, Map<String, List<Map<String, Object>>> dict) {

    return drgCodeMatches(claim, dict)
        || claim.getClaimItems().stream()
            .anyMatch(
                e -> {
                  var cl = e.getClaimLine();
                  var cp = e.getClaimProcedure();

                  return procedureMatchesDict(cp, dict) || hcpcsCodeMatches(cl, dict);
                });
  }

  // Checks DRG.
  private boolean drgCodeMatches(Claim claim, Map<String, List<Map<String, Object>>> dict) {
    var entries = dict.get(SystemUrls.CMS_MS_DRG);
    for (var entry : entries) {
      if (claim.getDrgCode().filter(drgCode -> compare(getDictCode(entry), drgCode)).isPresent()) {
        return true;
      }
    }

    return false;
  }

  // Checks HCPCS and cpt.
  private boolean hcpcsCodeMatches(
      ClaimLine claimLine, Map<String, List<Map<String, Object>>> dict) {
    String hcpcs = claimLine.getHcpcsCode().getHcpcsCode().map(String::toLowerCase).orElse("");
    if (hcpcs.isEmpty()) {
      return false;
    }
    for (String system : List.of(SystemUrls.AMA_CPT, SystemUrls.CMS_HCPCS)) {
      var entries = dict.getOrDefault(system, List.of());
      for (var entry : entries) {
        if (compare(getDictCode(entry), hcpcs)) {
          return true;
        }
      }
    }
    return false;
  }

  // Checks ICDs.
  private boolean procedureMatchesDict(
      ClaimProcedure proc, Map<String, List<Map<String, Object>>> dict) {
    String diagnosisCode = getDiagnosisCode(proc);
    String procedureCode = getClaimProcedureCode(proc);

    // Fallback to DEFAULT (currently ICD-9) if absent; may adjust to ICD_10 per business decision.
    var icdIndicator = proc.getIcdIndicator().orElse(IcdIndicator.DEFAULT);
    var procSystem = icdIndicator.getProcedureSystem();
    var entries = dict.getOrDefault(procSystem, List.of());

    for (var entry : entries) {
      String dictCode = getDictCode(entry);
      if (compare(dictCode, procedureCode)) {
        return true;
      }
      if (icdIndicator.getDiagnosisSytem().equals(procSystem) && compare(dictCode, diagnosisCode)) {
        return true;
      }
    }
    return false;
  }
}
