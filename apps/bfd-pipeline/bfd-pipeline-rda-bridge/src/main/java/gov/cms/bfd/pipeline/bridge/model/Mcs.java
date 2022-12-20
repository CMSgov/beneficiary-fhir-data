package gov.cms.bfd.pipeline.bridge.model;

import static gov.cms.bfd.pipeline.bridge.model.ModelUtil.listNumberedEnumNames;

import gov.cms.bfd.model.rif.CarrierClaimColumn;
import java.util.List;

/**
 * Static references for MCS RIF file column headers. All of the names are extracted from the
 * corresponding column enum class.
 */
public class Mcs {

  public static final String BENE_ID = CarrierClaimColumn.BENE_ID.name();
  public static final String CARR_CLM_CNTL_NUM = CarrierClaimColumn.CARR_CLM_CNTL_NUM.name();
  public static final String CLM_FROM_DT = CarrierClaimColumn.CLM_FROM_DT.name();
  public static final String CLM_ID = CarrierClaimColumn.CLM_ID.name();
  public static final String CLM_THRU_DT = CarrierClaimColumn.CLM_THRU_DT.name();
  public static final String HCPCS_CD = CarrierClaimColumn.HCPCS_CD.name();
  public static final String HCPCS_1ST_MDFR_CD = CarrierClaimColumn.HCPCS_1ST_MDFR_CD.name();
  public static final String HCPCS_2ND_MDFR_CD = CarrierClaimColumn.HCPCS_2ND_MDFR_CD.name();
  public static final List<String> ICD_DGNS_CD =
      listNumberedEnumNames(CarrierClaimColumn.values(), CarrierClaimColumn.ICD_DGNS_CD1);
  public static final List<String> ICD_DGNS_VRSN_CD =
      listNumberedEnumNames(CarrierClaimColumn.values(), CarrierClaimColumn.ICD_DGNS_VRSN_CD1);
  public static final String LINE_1ST_EXPNS_DT = CarrierClaimColumn.LINE_1ST_EXPNS_DT.name();
  public static final String LINE_ICD_DGNS_CD = CarrierClaimColumn.LINE_ICD_DGNS_CD.name();
  public static final String LINE_ICD_DGNS_VRSN_CD =
      CarrierClaimColumn.LINE_ICD_DGNS_VRSN_CD.name();
  public static final String LINE_LAST_EXPNS_DT = CarrierClaimColumn.LINE_LAST_EXPNS_DT.name();
  public static final String LINE_NUM = CarrierClaimColumn.LINE_NUM.name();
  public static final String NCH_CARR_CLM_SBMTD_CHRG_AMT =
      CarrierClaimColumn.NCH_CARR_CLM_SBMTD_CHRG_AMT.name();
  public static final String ORG_NPI_NUM = CarrierClaimColumn.ORG_NPI_NUM.name();

  /**
   * The maximum number of diagnosis column names available for {@link ICD_DGNS_CD} and {@link
   * ICD_DGNS_VRSN_CD}. Used when looping over both sets of fields.
   */
  public static final int MAX_DIAGNOSIS_CODES =
      Math.min(ICD_DGNS_CD.size(), ICD_DGNS_VRSN_CD.size());

  private Mcs() {}
}
