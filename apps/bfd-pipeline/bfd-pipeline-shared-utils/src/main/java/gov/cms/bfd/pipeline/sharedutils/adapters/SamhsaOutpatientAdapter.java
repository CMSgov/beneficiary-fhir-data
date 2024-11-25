package gov.cms.bfd.pipeline.sharedutils.adapters;

import static java.util.Map.entry;

import gov.cms.bfd.model.rif.entities.OutpatientClaim;
import gov.cms.bfd.model.rif.entities.OutpatientClaimLine;
import gov.cms.bfd.pipeline.sharedutils.model.SamhsaFields;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/** Adapter to get a list of SAMHSA codes from an OutpatientClaim. */
public class SamhsaOutpatientAdapter
    extends SamhsaAdapterBase<OutpatientClaim, OutpatientClaimLine> {
  /**
   * Constructor.
   *
   * @param claim The claim to process.
   * @param claimLines The claim's claim lines.
   */
  public SamhsaOutpatientAdapter(OutpatientClaim claim, List<OutpatientClaimLine> claimLines) {
    super(claim, claimLines);
    this.claim = claim;
    this.claimLines = claimLines;
    this.table = "outpatient_claims";
    this.linesTable = "outpatient_claim_lines";
  }

  /** Retrieves a list of SAMHSA fields. */
  @Override
  public List<SamhsaFields> getFields() {
    getCodes();
    return samhsaFields;
  }

  /** {@inheritDoc} */
  @Override
  Map<Supplier<Optional<String>>, String> getClaimLineMethods(
      OutpatientClaimLine outpatientClaimLine) {
    return Map.ofEntries(
        entry(outpatientClaimLine::getHcpcsCode, "hcpcs_cd"),
        entry(outpatientClaimLine::getApcOrHippsCode, "rev_cntr_apc_hipps_cd"));
  }

  /** {@inheritDoc} */
  @Override
  Map<Supplier<Optional<String>>, String> getClaimMethods() {
    return Map.ofEntries(
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
        entry(claim::getDiagnosis12Code, "icd_dgns_cd12"),
        entry(claim::getDiagnosis13Code, "icd_dgns_cd13"),
        entry(claim::getDiagnosis14Code, "icd_dgns_cd14"),
        entry(claim::getDiagnosis15Code, "icd_dgns_cd15"),
        entry(claim::getDiagnosis16Code, "icd_dgns_cd16"),
        entry(claim::getDiagnosis17Code, "icd_dgns_cd17"),
        entry(claim::getDiagnosis18Code, "icd_dgns_cd18"),
        entry(claim::getDiagnosis19Code, "icd_dgns_cd19"),
        entry(claim::getDiagnosis20Code, "icd_dgns_cd20"),
        entry(claim::getDiagnosis21Code, "icd_dgns_cd21"),
        entry(claim::getDiagnosis22Code, "icd_dgns_cd22"),
        entry(claim::getDiagnosis23Code, "icd_dgns_cd23"),
        entry(claim::getDiagnosis24Code, "icd_dgns_cd24"),
        entry(claim::getDiagnosis25Code, "icd_dgns_cd25"),
        entry(claim::getDiagnosisExternal1Code, "icd_dgns_e_cd1"),
        entry(claim::getDiagnosisExternal2Code, "icd_dgns_e_cd2"),
        entry(claim::getDiagnosisExternal3Code, "icd_dgns_e_cd3"),
        entry(claim::getDiagnosisExternal4Code, "icd_dgns_e_cd4"),
        entry(claim::getDiagnosisExternal5Code, "icd_dgns_e_cd5"),
        entry(claim::getDiagnosisExternal6Code, "icd_dgns_e_cd6"),
        entry(claim::getDiagnosisExternal7Code, "icd_dgns_e_cd7"),
        entry(claim::getDiagnosisExternal8Code, "icd_dgns_e_cd8"),
        entry(claim::getDiagnosisExternal9Code, "icd_dgns_e_cd9"),
        entry(claim::getDiagnosisExternal10Code, "icd_dgns_e_cd10"),
        entry(claim::getDiagnosisExternal11Code, "icd_dgns_e_cd11"),
        entry(claim::getDiagnosisExternal12Code, "icd_dgns_e_cd12"),
        entry(claim::getDiagnosisPrincipalCode, "prncpal_dgns_cd"),
        entry(claim::getDiagnosisExternalFirstCode, "fst_dgns_e_cd"),
        entry(claim::getDiagnosisAdmission1Code, "rsn_visit_cd1"),
        entry(claim::getDiagnosisAdmission2Code, "rsn_visit_cd2"),
        entry(claim::getDiagnosisAdmission3Code, "rsn_visit_cd3"),
        entry(claim::getProcedure1Code, "icd_prcdr_cd1"),
        entry(claim::getProcedure2Code, "icd_prcdr_cd2"),
        entry(claim::getProcedure3Code, "icd_prcdr_cd3"),
        entry(claim::getProcedure4Code, "icd_prcdr_cd4"),
        entry(claim::getProcedure5Code, "icd_prcdr_cd5"),
        entry(claim::getProcedure6Code, "icd_prcdr_cd6"),
        entry(claim::getProcedure7Code, "icd_prcdr_cd7"),
        entry(claim::getProcedure8Code, "icd_prcdr_cd8"),
        entry(claim::getProcedure9Code, "icd_prcdr_cd9"),
        entry(claim::getProcedure10Code, "icd_prcdr_cd10"),
        entry(claim::getProcedure11Code, "icd_prcdr_cd11"),
        entry(claim::getProcedure12Code, "icd_prcdr_cd12"),
        entry(claim::getProcedure13Code, "icd_prcdr_cd13"),
        entry(claim::getProcedure14Code, "icd_prcdr_cd14"),
        entry(claim::getProcedure15Code, "icd_prcdr_cd15"),
        entry(claim::getProcedure16Code, "icd_prcdr_cd16"),
        entry(claim::getProcedure17Code, "icd_prcdr_cd17"),
        entry(claim::getProcedure18Code, "icd_prcdr_cd18"),
        entry(claim::getProcedure19Code, "icd_prcdr_cd19"),
        entry(claim::getProcedure20Code, "icd_prcdr_cd20"),
        entry(claim::getProcedure21Code, "icd_prcdr_cd21"),
        entry(claim::getProcedure22Code, "icd_prcdr_cd22"),
        entry(claim::getProcedure23Code, "icd_prcdr_cd23"),
        entry(claim::getProcedure24Code, "icd_prcdr_cd24"),
        entry(claim::getProcedure25Code, "icd_prcdr_cd25"));
  }

  /** {@inheritDoc} */
  @Override
  Short getLineNum(OutpatientClaimLine outpatientClaimLine) {
    return outpatientClaimLine.getLineNumber();
  }
}
