package gov.cms.bfd.server.ng.eob;

import gov.cms.bfd.server.ng.FhirUtil;
import gov.cms.bfd.server.ng.SecurityLabels;
import gov.cms.bfd.server.ng.beneficiary.BeneficiaryRepository;
import gov.cms.bfd.server.ng.claim.ClaimRepository;
import gov.cms.bfd.server.ng.claim.model.Claim;
import gov.cms.bfd.server.ng.claim.model.ClaimLine;
import gov.cms.bfd.server.ng.claim.model.ClaimProcedure;
import gov.cms.bfd.server.ng.claim.model.ClaimSourceId;
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

  // Cache the security labels dictionary to avoid repeated I/O and parsing
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
    var eobs =
        claimRepository.findByBeneXrefSk(
            beneXrefSk.get(), serviceDate, lastUpdated, count, startIndex, sourceIds);

    return getFhirEobs(eobs, true);
  }

  protected Bundle getFhirEobs(List<Claim> eobs, boolean filterSamhsa) {

    var filteredEobs = eobs;
    var dict = SECURITY_LABELS_MAP;

    if (filterSamhsa) {

      var sys = SECURITY_LABELS_MAP.keySet().stream().toList();

      filteredEobs =
          eobs.stream()
              // keep claims that do NOT contain any SAMHSA-coded procedures
              .filter(claim -> !claimHasSamhsa(claim, dict, sys))
              .toList();
    }

    var fhirEobs = filteredEobs.stream().map(Claim::toFhir).toList();

    return FhirUtil.bundleOrDefault(
        fhirEobs.stream().map(Resource.class::cast), claimRepository::claimLastUpdated);
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
        .map(entry -> entry.getResource())
        .filter(ExplanationOfBenefit.class::isInstance)
        .map(ExplanationOfBenefit.class::cast)
        .findFirst();
  }

  private String getDiagnosisCode(ClaimProcedure e) {
    return e.getDiagnosisCode().orElse("").trim().toLowerCase();
  }

  private String getClaimProcedureCode(ClaimProcedure e) {
    return e.getProcedureCode().orElse("").trim().toLowerCase();
  }

  private String getDictCode(Map<String, Object> entry) {
    return entry.get("code").toString().trim().replace(".", "").toLowerCase();
  }

  // Returns true if the given claim contains any procedure that matches a SAMHSA
  // security label code from the provided dictionary and keys.
  private boolean claimHasSamhsa(
      Claim claim, Map<String, List<Map<String, Object>>> dict, List<String> sys) {

    return claim.getClaimItems().stream()
        .anyMatch(
            e -> {
              var cl = e.getClaimLine();
              var cp = e.getClaimProcedure();
              return procedureMatchesDict(cp, dict, sys) || hcpcsCodeMatches(cl, dict, sys);
            });
  }

  // cpt, Software and CodeSets.
  private boolean hcpcsCodeMatches(
      ClaimLine claimLine, Map<String, List<Map<String, Object>>> dict, List<String> sys) {
    String hcpcs = claimLine.getHcpcsCode().getHcpcsCode().orElse("");
    for (String link : sys) {
      var entries = dict.getOrDefault(link, List.of());
      for (var entry : entries) {
        if (getDictCode(entry).equals(hcpcs)) {
          return true;
        }
      }
    }
    return false;
  }

  // icds
  private boolean procedureMatchesDict(
      ClaimProcedure proc, Map<String, List<Map<String, Object>>> dict, List<String> sys) {
    String diagnosisCode = getDiagnosisCode(proc);
    String procedureCode = getClaimProcedureCode(proc);
    for (String link : sys) {
      var entries = dict.getOrDefault(link, List.of());
      for (var entry : entries) {
        if (getDictCode(entry).equals(procedureCode)) {
          return true;
        }
        if (proc.getIcdIndicator().get().getDiagnosisSytem().equals(link)
            && getDictCode(entry).equals(diagnosisCode)) {
          return true;
        }
      }
    }
    return false;
  }
}
