package gov.cms.test;

import java.lang.Object;
import java.lang.String;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class BeneficiaryCsvWriter {
  public static Map<String, Object[][]> toCsvRecordsByTable(Beneficiary entity) {
    // Verify the input.
    Objects.requireNonNull(entity);

    Map<String, Object[][]> csvRecordsByTable = new HashMap<>(2);

    // Convert the header fields.
    Object[][] headerRecords = new Object[2][];
    headerRecords[0] = new Object[]{ "BENE_ID", "STATE_CODE", "BENE_SEX_IDENT_CD", "BENE_ENTLMT_RSN_ORIG", "BENE_MDCR_STATUS_CD", "EFCTV_END_DT", "BENE_LINK_KEY", "HICN_UNHASHED", "MBI_HASH", "LAST_UPDATED" };
    Object[] headerRecord = new Object[]{ entity.getBeneficiaryId(), entity.getStateCode(), entity.getSex(), entity.getEntitlementCodeOriginal().orElse(null), entity.getMedicareEnrollmentStatusCode().orElse(null), entity.getMbiObsoleteDate().orElse(null), entity.getBeneLinkKey(), entity.getHicnUnhashed(), entity.getMbiHash(), entity.getLastUpdated() };
    headerRecords[1] = headerRecord;
    csvRecordsByTable.put("beneficiaries", headerRecords);
    return csvRecordsByTable;
  }
}
