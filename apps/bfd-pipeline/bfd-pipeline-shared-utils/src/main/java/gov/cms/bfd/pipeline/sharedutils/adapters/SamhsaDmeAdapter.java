package gov.cms.bfd.pipeline.sharedutils.adapters;

import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.model.rif.entities.DMEClaimLine;
import gov.cms.bfd.pipeline.sharedutils.model.SamhsaFields;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

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
   * @throws InvocationTargetException Thrown when the one of the claim's methods cannot be invoked.
   * @throws NoSuchMethodException Thrown when one of the claim's method does not exist.
   * @throws IllegalAccessException Thrown on illegal access invoking the claim's method.
   */
  @Override
  public List<SamhsaFields> getFields()
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    getIcdDiagnosisCodes(12);
    getPrincipalDiagnosis();
    getDiagnosisCode();
    getHcpcsCode();
    return samhsaFields;
  }
}
