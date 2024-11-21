package gov.cms.bfd.pipeline.sharedutils.s3.adapters;

import gov.cms.bfd.model.rif.entities.HHAClaim;
import gov.cms.bfd.model.rif.entities.HHAClaimLine;
import gov.cms.bfd.pipeline.sharedutils.model.SamhsaFields;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class SamhsaHHAAdapter extends SamhsaAdapterBase<HHAClaim, HHAClaimLine> {
    public SamhsaHHAAdapter(HHAClaim claim, List<HHAClaimLine> claimLines) {
        super(claim, claimLines);
        this.claim = claim;
      this.claimLines = claimLines;
      this.table = "hha_claims";
      this.linesTable = "hha_claim_lines";
   }
   @Override
    public List<SamhsaFields> getFields() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
       getIcdDiagnosisCodes(25);
       getPrincipalDiagnosis();
       getDiagnosisExternalCodes(12);
       getDiagnosisFirstCode();
       getApcOrHippsCode();
       getHcpcsCode();

       return samhsaFields;
   }

}
