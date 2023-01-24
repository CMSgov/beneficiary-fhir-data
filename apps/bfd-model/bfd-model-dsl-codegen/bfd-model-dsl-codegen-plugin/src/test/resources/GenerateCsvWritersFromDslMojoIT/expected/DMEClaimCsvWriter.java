package gov.cms.test;

import java.lang.Object;
import java.lang.String;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class DMEClaimCsvWriter {
  public static Map<String, Object[][]> toCsvRecordsByTable(DMEClaim entity) {
    // Verify the input.
    Objects.requireNonNull(entity);

    Map<String, Object[][]> csvRecordsByTable = new HashMap<>(2);

    // Convert the header fields.
    Object[][] headerRecords = new Object[2][];
    headerRecords[0] = new Object[]{ "BENE_ID", "CLM_ID", "CLM_GRP_ID", "FINAL_ACTION", "LAST_UPDATED" };
    Object[] headerRecord = new Object[]{ entity.getBeneficiaryId(), entity.getClaimId(), entity.getClaimGroupId(), entity.getFinalAction(), entity.getLastUpdated() };
    headerRecords[1] = headerRecord;
    csvRecordsByTable.put("dme_claims", headerRecords);

    // Convert the line fields.
    Object[][] lineRecords = new Object[entity.getLines().size() + 1][];
    csvRecordsByTable.put("dme_claim_lines", lineRecords);
    lineRecords[0] = new Object[]{ "LINE_NUM", "TAX_NUM", "PRVDR_SPCLTY" };
    for (int lineIndex = 0; lineIndex < entity.getLines().size();lineIndex++) {
      DMEClaimLine lineEntity = entity.getLines().get(lineIndex);
      Object[] lineRecord = new Object[]{ lineEntity.getParentClaim().getClaimId(), lineEntity.getLineNumber(), lineEntity.getProviderTaxNumber(), lineEntity.getProviderSpecialityCode().orElse(null) };
      lineRecords[lineIndex + 1] = lineRecord;
    }
    return csvRecordsByTable;
  }
}
