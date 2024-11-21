package gov.cms.bfd.pipeline.sharedutils.s3.adapters;

import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.model.rif.entities.DMEClaimLine;
import gov.cms.bfd.model.rif.entities.InpatientClaim;
import gov.cms.bfd.model.rif.entities.InpatientClaimLine;
import gov.cms.bfd.pipeline.sharedutils.model.SamhsaFields;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class SamhsaInpatientAdapter extends SamhsaAdapterBase<InpatientClaim, InpatientClaimLine> {
    public SamhsaInpatientAdapter(InpatientClaim claim, List<InpatientClaimLine> claimLines) {
        super(claim, claimLines);
        this.claim = claim;
      this.claimLines = claimLines;
      this.table = "hospice_claims";
      this.linesTable = "hospice_claim_lines";
   }
   @Override
    public List<SamhsaFields> getFields() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
       getIcdDiagnosisCodes(25);
       getDiagnosisExternalCodes(12);
       getProcedureCodes(25);
       getClaimDRGCode();
       getPrincipalDiagnosis();
       getDiagnosisAdmittingCode();
       getHcpcsCode();
       return samhsaFields;
   }

}
