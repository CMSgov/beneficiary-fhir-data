package gov.cms.bfd.server.war.commons;

public class BBCodingSystems {

  public static final String BB_BASE_URL = "https://bluebutton.cms.gov/resources";

  public static final String CARR_CLM_CONTROL_NUM = BB_BASE_URL + "/variables/carr_clm_cntrl_num";

  public static final String CLM_FACILITY_TYPE_CODE = BB_BASE_URL + "/variables/clm_fac_type_cd";

  public static final String CLM_FREQ_CODE = BB_BASE_URL + "/variables/clm_freq_cd";

  public static final String CLM_SERVICE_CLSFCTN_TYPE_CODE =
      BB_BASE_URL + "/variables/clm_srvc_clsfctn_type_cd";

  public static final String FI_DOC_CLM_CONTROL_NUM =
      BB_BASE_URL + "/variables/fi_doc_clm_cntrl_num";

  public static final String HCPCS = BB_BASE_URL + "/codesystem/hcpcs";

  public static final String PROVIDER_NUM = BB_BASE_URL + "/variables/prvdr_num";

  private BBCodingSystems() {}

  public static class FISS {

    public static final String PAYERS_NAME = BB_BASE_URL + "/variables/fiss/payers-name";

    public static final String TAX_NUM = BB_BASE_URL + "/variables/fiss/fed-tax-nb";

    private FISS() {}
  }

  public static class MCS {

    public static final String IDR_BILL_PROV_EIN = BB_BASE_URL + "/variables/mcs/idr-bill-prov-ein";

    public static final String IDR_BILL_PROV_TYPE =
        BB_BASE_URL + "/variables/mcs/idr-bill-prov-type";

    public static final String IDR_CLM_TYPE = BB_BASE_URL + "/variables/prvdr_num";

    public static final String PROVIDER_SPECIALTY =
        BB_BASE_URL + "/variables/mcs/idr-bill-prov-spec";

    private MCS() {}
  }
}
