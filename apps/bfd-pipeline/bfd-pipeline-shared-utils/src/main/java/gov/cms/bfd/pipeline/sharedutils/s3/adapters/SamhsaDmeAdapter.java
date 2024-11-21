package gov.cms.bfd.pipeline.sharedutils.s3.adapters;

import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.model.rif.entities.DMEClaimLine;
import gov.cms.bfd.pipeline.sharedutils.model.SamhsaFields;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class SamhsaDmeAdapter extends SamhsaAdapterBase<DMEClaim, DMEClaimLine> {
    public SamhsaDmeAdapter(DMEClaim claim, List<DMEClaimLine> claimLines) {
        super(claim, claimLines);
        this.claim = claim;
      this.claimLines = claimLines;
      this.table = "dme_claims";
      this.linesTable = "dme_claim_lines";
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
