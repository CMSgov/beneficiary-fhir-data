package gov.cms.bfd.pipeline.sharedutils.s3.adapters;

import gov.cms.bfd.model.rif.entities.CarrierClaim;
import gov.cms.bfd.model.rif.entities.CarrierClaimLine;
import gov.cms.bfd.pipeline.sharedutils.model.SamhsaFields;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class SamhsaCarrierAdapter extends SamhsaAdapterBase<CarrierClaim, CarrierClaimLine> {
    public SamhsaCarrierAdapter(CarrierClaim claim, List<CarrierClaimLine> claimLines) {
        super(claim, claimLines);
      this.claim = claim;
      this.claimLines = claimLines;
      this.table = "hospice_claims";
      this.linesTable = "hospice_claim_lines";
   }
   @Override
    public List<SamhsaFields> getFields() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
       getIcdDiagnosisCodes(12);
       getPrincipalDiagnosis();
        getDiagnosisCode();
        getHcpcsCode();
       return samhsaFields;
   }
}
