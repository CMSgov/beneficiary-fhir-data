package gov.cms.bfd.server.war.commons;

public class BBCodingSystems {

  public static final String BB_BASE_URL = "https://bluebutton.cms.gov/resources";

  public static final String CLM_FACILITY_TYPE_CODE = BB_BASE_URL + "/variables/clm_fac_type_cd";

  public static final String CLM_FREQ_CODE = BB_BASE_URL + "/variables/clm_freq_cd";

  public static final String CLM_SERVICE_CLSFCTN_TYPE_CODE =
      BB_BASE_URL + "/variables/clm_srvc_clsfctn_type_cd";

  public static final String CLM_POA_IND = BB_BASE_URL + "/variables/clm_poa_ind_sw1";

  public static final String HCPCS = BB_BASE_URL + "/codesystem/hcpcs";

  public static final String PROVIDER_NUM = BB_BASE_URL + "/variables/prvdr_num";

  private BBCodingSystems() {}

  public static class FISS {

    private static final String FISS_BASE = BB_BASE_URL + "/variables/fiss";

    public static final String CURR_STATUS = FISS_BASE + "/curr-status";

    public static final String CURR_TRAN_DT_CYMD = FISS_BASE + "/curr-tran-dt-cymd";

    public static final String PAYERS_NAME = FISS_BASE + "/payers-name";

    public static final String RECD_DT_CYMD = FISS_BASE + "/recd-dt-cymd";

    public static final String TAX_NUM = FISS_BASE + "/fed-tax-nb";

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

    private MCS() {}
  }
}
