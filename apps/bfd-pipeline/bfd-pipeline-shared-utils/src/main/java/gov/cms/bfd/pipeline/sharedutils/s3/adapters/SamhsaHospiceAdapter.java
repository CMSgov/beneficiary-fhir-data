package gov.cms.bfd.pipeline.sharedutils.s3.adapters;

import gov.cms.bfd.model.rif.entities.HHAClaim;
import gov.cms.bfd.model.rif.entities.HHAClaimLine;
import gov.cms.bfd.model.rif.entities.HospiceClaim;
import gov.cms.bfd.model.rif.entities.HospiceClaimLine;
import gov.cms.bfd.pipeline.sharedutils.model.SamhsaFields;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class SamhsaHospiceAdapter extends SamhsaAdapterBase<HospiceClaim, HospiceClaimLine> {
    public SamhsaHospiceAdapter(HospiceClaim claim, List<HospiceClaimLine> claimLines) {
        super(claim, claimLines);
        this.claim = claim;
      this.claimLines = claimLines;
      this.table = "hospice_claims";
      this.linesTable = "hospice_claim_lines";
   }
   @Override
    public List<SamhsaFields> getFields() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
       getIcdDiagnosisCodes(25);
       getPrincipalDiagnosis();
       getDiagnosisExternalCodes(12);
       getDiagnosisFirstCode();
       getHcpcsCode();
       return samhsaFields;
   }
}
