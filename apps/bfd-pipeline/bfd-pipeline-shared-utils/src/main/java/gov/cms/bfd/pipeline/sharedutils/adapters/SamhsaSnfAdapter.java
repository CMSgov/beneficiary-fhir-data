package gov.cms.bfd.pipeline.sharedutils.adapters;

import gov.cms.bfd.model.rif.entities.SNFClaim;
import gov.cms.bfd.model.rif.entities.SNFClaimLine;
import gov.cms.bfd.pipeline.sharedutils.model.SamhsaFields;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/** Adapter to get a list of SAMHSA codes from a SNFClaim. */
public class SamhsaSnfAdapter extends SamhsaAdapterBase<SNFClaim, SNFClaimLine> {
  /**
   * Constructor.
   *
   * @param claim The claim to process.
   * @param claimLines The claim's claim lines.
   */
  public SamhsaSnfAdapter(SNFClaim claim, List<SNFClaimLine> claimLines) {
    super(claim, claimLines);
    this.claim = claim;
    this.claimLines = claimLines;
    this.table = "snf_claims";
    this.linesTable = "snf_claim_lines";
  }

  /**
   * Retrieves a list of SAMHSA fields.
   *
   * @return {@link SamhsaFields}
   * @throws InvocationTargetException Thrown when the one of the claim's methods cannot be invoked.
   * @throws NoSuchMethodException Thrown when one of the claim's method does not exist.
   * @throws IllegalAccessException Thrown on illegal access invoking the claim's method.
   */
  @Override
  public List<SamhsaFields> getFields()
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    getClaimDRGCode();
    getPrincipalDiagnosis();
    getDiagnosisFirstCode();
    getDiagnosisAdmittingCode();
    getIcdDiagnosisCodes(25);
    getDiagnosisExternalCodes(12);
    getProcedureCodes(25);
    getHcpcsCode();
    return samhsaFields;
  }
}
