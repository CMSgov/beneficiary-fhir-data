package gov.cms.bfd.pipeline.sharedutils.adapters;

import gov.cms.bfd.model.rif.entities.CarrierClaim;
import gov.cms.bfd.model.rif.entities.CarrierClaimLine;
import gov.cms.bfd.pipeline.sharedutils.model.SamhsaFields;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/** Adapter to get a list of SAMHSA codes from a CarrierClaim. */
public class SamhsaCarrierAdapter extends SamhsaAdapterBase<CarrierClaim, CarrierClaimLine> {
  /**
   * Constructor.
   *
   * @param claim The claim to process.
   * @param claimLines The claim's claim lines.
   */
  public SamhsaCarrierAdapter(CarrierClaim claim, List<CarrierClaimLine> claimLines) {
    super(claim, claimLines);
    this.claim = claim;
    this.claimLines = claimLines;
    this.table = "carrier_claims";
    this.linesTable = "carrier_claim_lines";
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
