package gov.cms.bfd.pipeline.sharedutils.s3.adapters;

import gov.cms.bfd.model.rif.entities.InpatientClaim;
import gov.cms.bfd.model.rif.entities.InpatientClaimLine;
import gov.cms.bfd.model.rif.entities.OutpatientClaim;
import gov.cms.bfd.model.rif.entities.OutpatientClaimLine;
import gov.cms.bfd.pipeline.sharedutils.model.SamhsaFields;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class SamhsaOutpatientAdapter extends SamhsaAdapterBase<OutpatientClaim, OutpatientClaimLine> {
    public SamhsaOutpatientAdapter(OutpatientClaim claim, List<OutpatientClaimLine> claimLines) {
      super(claim, claimLines);
      this.claim = claim;
      this.claimLines = claimLines;
      this.table = "outpatient_claims";
      this.linesTable = "outpatient_claim_lines";
   }
   @Override
    public List<SamhsaFields> getFields() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
       getIcdDiagnosisCodes(25);
       getDiagnosisExternalCodes(12);
       getProcedureCodes(25);
       getPrincipalDiagnosis();
       getDiagnosisFirstCode();
       getDiagnosisAdmittingCodes(3);
       getHcpcsCode();
       return samhsaFields;
   }

}
