package gov.cms.bfd.server.war.commons;

public class BBCodingSystems {

  public static final String BB_BASE_URL = "https://bluebutton.cms.gov/resources";

  public static final String CARR_CLM_CONTROL_NUM = BB_BASE_URL + "/variables/carr_clm_cntrl_num";

  public static final String CLM_FACILITY_TYPE_CODE = BB_BASE_URL + "/variables/clm_fac_type_cd";

  public static final String CLM_FREQ_CODE = BB_BASE_URL + "/variables/clm_freq_cd";

  public static final String CLM_SERVICE_CLSFCTN_TYPE_CODE =
      BB_BASE_URL + "/variables/clm_srvc_clsfctn_type_cd";

  public static final String CLM_POA_IND = BB_BASE_URL + "/variables/clm_poa_ind_sw1";

  public static final String FI_DOC_CLM_CONTROL_NUM =
      BB_BASE_URL + "/variables/fi_doc_clm_cntrl_num";

  public static final String HCPCS = BB_BASE_URL + "/codesystem/hcpcs";

  public static final String PROVIDER_NUM = BB_BASE_URL + "/variables/prvdr_num";

  private BBCodingSystems() {}

  public static class FISS {

    public static final String CURR_STATUS = BB_BASE_URL + "/variables/fiss/curr-status";

    public static final String CURR_TRAN_DT_CYMD = "/variables/fiss/curr-tran-dt-cymd";

    public static final String PAYERS_NAME = BB_BASE_URL + "/variables/fiss/payers-name";

    public static final String RECD_DT_CYMD = BB_BASE_URL + "/variables/fiss/recd-dt-cymd";

    public static final String TAX_NUM = BB_BASE_URL + "/variables/fiss/fed-tax-nb";

    private FISS() {}
  }

  public static class MCS {

    public static final String BILL_PROV_EIN = BB_BASE_URL + "/variables/mcs/bill-prov-ein";

    public static final String BILL_PROV_SPEC = BB_BASE_URL + "/variables/mcs/bill-prov-spec";

    public static final String BILL_PROV_TYPE = BB_BASE_URL + "/variables/mcs/bill-prov-type";

    public static final String CLM_TYPE = BB_BASE_URL + "/variables/mcs/clm-type";

    public static final String STATUS_CODE = BB_BASE_URL + "/variables/mcs/status-code";

    private MCS() {}
  }
}
