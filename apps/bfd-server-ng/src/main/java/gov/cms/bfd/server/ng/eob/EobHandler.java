package gov.cms.bfd.server.ng.eob;

import gov.cms.bfd.server.ng.FhirUtil;
import gov.cms.bfd.server.ng.beneficiary.BeneficiaryRepository;
import gov.cms.bfd.server.ng.claim.ClaimRepository;
import gov.cms.bfd.server.ng.claim.model.Claim;
import gov.cms.bfd.server.ng.claim.model.ClaimProcedure;
import gov.cms.bfd.server.ng.claim.model.ClaimSourceId;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.sharedutils.SecurityLabels;
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
    var beneXrefSk = beneficiaryRepository.getXrefBeneSk(beneSk);
    // Don't return data for historical beneSks
    if (beneXrefSk.isEmpty() || !beneXrefSk.get().equals(beneSk)) {
      return new Bundle();
    }
    var eobs =
        claimRepository.findByBeneXrefSk(
            beneXrefSk.get(), serviceDate, lastUpdated, count, startIndex, sourceIds);

    return getFhirEobs(eobs, true);
  }

  Bundle getFhirEobs(List<Claim> eobs, boolean filterSamhsa) {

    var filteredEobs = eobs;
    var dict = SecurityLabels.securityLabelsDict();

    if (filterSamhsa) {

      var sys = SecurityLabels.getSecurityLabelKeys();

      filteredEobs =
          eobs.stream()
              // keep claims that do NOT contain any SAMHSA-coded procedures
              .filter(claim -> !claimHasSamhsa(claim, dict, sys))
              .toList();
    }

    if (filteredEobs.isEmpty()) {
      Bundle emptyBundle = new Bundle();
      emptyBundle.setType(Bundle.BundleType.COLLECTION);
      emptyBundle.setTypeElement(null);

      return emptyBundle;
    }

    var fhirEobs = filteredEobs.stream().map(Claim::toFhir).toList();

    return FhirUtil.bundleOrDefault(
        fhirEobs.stream().map(eob -> (Resource) eob), claimRepository::claimLastUpdated);
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
        .filter(resource -> resource instanceof ExplanationOfBenefit)
        .map(resource -> (ExplanationOfBenefit) resource)
        .findFirst();
  }

  private String getCode(ClaimProcedure e) {
    String code = e.getDiagnosisCode().orElse("").trim().toLowerCase();
    return code;
  }

  private String getDictCode(Map<String, Object> entry) {
    String dictCode =
        entry.get("code") != null
            ? entry.get("code").toString().trim().replace(".", "").toLowerCase()
            : "";
    return dictCode;
  }

  // Returns true if the given claim contains any procedure that matches a SAMHSA
  // security label code from the provided dictionary and keys.
  private boolean claimHasSamhsa(
      Claim claim, Map<String, List<Map<String, Object>>> dict, List<String> sys) {
    return claim.getClaimProcedures().stream()
        .anyMatch(proc -> procedureMatchesDict(proc, dict, sys));
  }

  private boolean procedureMatchesDict(
      ClaimProcedure proc, Map<String, List<Map<String, Object>>> dict, List<String> sys) {
    String code = getCode(proc);
    for (String link : sys) {
      List<Map<String, Object>> entries = dict.getOrDefault(link, List.of());
      for (Map<String, Object> entry : entries) {
        if (getDictCode(entry).equals(code)) return true;
      }
    }
    return false;
  }
}
