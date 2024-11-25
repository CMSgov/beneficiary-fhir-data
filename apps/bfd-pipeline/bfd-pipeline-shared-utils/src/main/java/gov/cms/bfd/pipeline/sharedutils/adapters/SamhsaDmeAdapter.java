package gov.cms.bfd.pipeline.sharedutils.adapters;

import static java.util.Map.entry;

import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.model.rif.entities.DMEClaimLine;
import gov.cms.bfd.pipeline.sharedutils.model.SamhsaFields;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/** Adapter to get a list of SAMHSA codes from a DMEClaim. */
public class SamhsaDmeAdapter extends SamhsaAdapterBase<DMEClaim, DMEClaimLine> {
  /**
   * Constructor.
   *
   * @param claim The claim to process.
   * @param claimLines The claim's claim lines.
   */
  public SamhsaDmeAdapter(DMEClaim claim, List<DMEClaimLine> claimLines) {
    super(claim, claimLines);
    this.claim = claim;
    this.claimLines = claimLines;
    this.table = "dme_claims";
    this.linesTable = "dme_claim_lines";
  }

  /**
   * Retrieves a list of SAMHSA fields.
   *
   * @return {@link SamhsaFields}
   */
  @Override
  public List<SamhsaFields> getFields() {
    getCodes();
    return samhsaFields;
  }

  /** {@inheritDoc} */
  @Override
  public Map<Supplier<Optional<String>>, String> getClaimMethods() {
    return Map.ofEntries(
        entry(claim::getDiagnosisPrincipalCode, "prncpal_dgns_cd"),
        entry(claim::getDiagnosis1Code, "icd_dgns_cd1"),
        entry(claim::getDiagnosis2Code, "icd_dgns_cd2"),
        entry(claim::getDiagnosis3Code, "icd_dgns_cd3"),
        entry(claim::getDiagnosis4Code, "icd_dgns_cd4"),
        entry(claim::getDiagnosis5Code, "icd_dgns_cd5"),
        entry(claim::getDiagnosis6Code, "icd_dgns_cd6"),
        entry(claim::getDiagnosis7Code, "icd_dgns_cd7"),
        entry(claim::getDiagnosis8Code, "icd_dgns_cd8"),
        entry(claim::getDiagnosis9Code, "icd_dgns_cd9"),
        entry(claim::getDiagnosis10Code, "icd_dgns_cd10"),
        entry(claim::getDiagnosis11Code, "icd_dgns_cd11"),
        entry(claim::getDiagnosis12Code, "icd_dgns_cd12"));
  }

  /** {@inheritDoc} */
  @Override
  Short getLineNum(DMEClaimLine dmeClaimLine) {
    return dmeClaimLine.getLineNumber();
  }

  /** {@inheritDoc} */
  public Map<Supplier<Optional<String>>, String> getClaimLineMethods(DMEClaimLine line) {
    return Map.ofEntries(
        entry(line::getDiagnosisCode, "line_icd_dgns_cd"), entry(line::getHcpcsCode, "hcpcs_cd"));
  }
}
