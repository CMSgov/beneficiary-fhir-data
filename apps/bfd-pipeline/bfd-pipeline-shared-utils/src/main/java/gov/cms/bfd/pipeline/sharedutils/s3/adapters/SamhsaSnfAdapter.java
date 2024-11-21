package gov.cms.bfd.pipeline.sharedutils.s3.adapters;

import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.model.rif.entities.DMEClaimLine;
import gov.cms.bfd.model.rif.entities.SNFClaim;
import gov.cms.bfd.model.rif.entities.SNFClaimLine;
import gov.cms.bfd.pipeline.sharedutils.model.SamhsaFields;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class SamhsaSnfAdapter extends SamhsaAdapterBase<SNFClaim, SNFClaimLine> {
    public SamhsaSnfAdapter(SNFClaim claim, List<SNFClaimLine> claimLines) {
        super(claim, claimLines);
        this.claim = claim;
      this.claimLines = claimLines;
      this.table = "snf_claims";
      this.linesTable = "snf_claim_lines";
   }
   @Override
    public List<SamhsaFields> getFields() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
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
