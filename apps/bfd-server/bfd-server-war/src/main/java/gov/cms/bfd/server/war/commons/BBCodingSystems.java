package gov.cms.bfd.server.war.commons;

public class BBCodingSystems {

  public static final String BB_BASE_URL = "https://bluebutton.cms.gov/resources";

  public static final String HCPCS_RELEASE =
      "https://www.cms.gov/Medicare/Coding/HCPCSReleaseCodeSets";

  private BBCodingSystems() {}

  public static class FISS {

    private static final String FISS_BASE = BB_BASE_URL + "/variables/fiss";

    public static final String CURR_STATUS = FISS_BASE + "/curr-status";

    public static final String CURR_TRAN_DT_CYMD = FISS_BASE + "/curr-tran-dt-cymd";

    public static final String PAYERS_NAME = FISS_BASE + "/payers-name";

    public static final String RECD_DT_CYMD = FISS_BASE + "/recd-dt-cymd";

    public static final String TAX_NUM = FISS_BASE + "/fed-tax-nb";

    public static final String DCN = FISS_BASE + "/dcn";

    public static final String SERV_TYP_CD = FISS_BASE + "/serv-typ-cd";

    public static final String FREQ_CD = FISS_BASE + "/freq-cd";

    public static final String MEDA_PROV_6 = FISS_BASE + "/meda-prov-6";

    public static final String LOB_CD = FISS_BASE + "/lob-cd";

    public static final String DIAG_POA_IND = FISS_BASE + "/diag-poa-ind";

    private FISS() {}
  }

  public static class MCS {

    private static final String MCS_BASE = BB_BASE_URL + "/variables/mcs";

    public static final String BILL_PROV_EIN = MCS_BASE + "/bill-prov-ein";

    public static final String BILL_PROV_SPEC = MCS_BASE + "/bill-prov-spec";

    public static final String BILL_PROV_TYPE = MCS_BASE + "/bill-prov-type";

    public static final String CLAIM_RECEIPT_DATE = MCS_BASE + "/claim-receipt-date";

    public static final String CLM_TYPE = MCS_BASE + "/clm-type";

    public static final String STATUS_CODE = MCS_BASE + "/status-code";

    public static final String STATUS_DATE = MCS_BASE + "/status-date";

    public static final String ICN = MCS_BASE + "/icn";

    public static final String PROC_CODE = MCS_BASE + "/proc-code";

    public static final String MOD_PREFIX = MCS_BASE + "/mod-";

    private MCS() {}
  }
}
