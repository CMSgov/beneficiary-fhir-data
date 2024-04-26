package gov.cms.bfd.pipeline.bridge.model;

import static gov.cms.bfd.pipeline.bridge.model.ModelUtil.listNumberedEnumNames;

import gov.cms.bfd.model.rif.entities.CarrierClaimColumn;
import java.util.List;

/**
 * Static references for MCS RIF file column headers. All of the names are extracted from the
 * corresponding column enum class.
 */
public class Mcs {

  /** Beneficiary ID. */
  public static final String BENE_ID = CarrierClaimColumn.BENE_ID.name();

  /** Carrier Claim Control Number. */
  public static final String CARR_CLM_CNTL_NUM = CarrierClaimColumn.CARR_CLM_CNTL_NUM.name();

  /** Carrier From Date. */
  public static final String CLM_FROM_DT = CarrierClaimColumn.CLM_FROM_DT.name();

  /** Claim Id. */
  public static final String CLM_ID = CarrierClaimColumn.CLM_ID.name();

  /** Carrier Thru Date. */
  public static final String CLM_THRU_DT = CarrierClaimColumn.CLM_THRU_DT.name();

  /** HCPCS Code. */
  public static final String HCPCS_CD = CarrierClaimColumn.HCPCS_CD.name();

  /** HCPCS first Code. */
  public static final String HCPCS_1ST_MDFR_CD = CarrierClaimColumn.HCPCS_1ST_MDFR_CD.name();

  /** HCPCS second Code. */
  public static final String HCPCS_2ND_MDFR_CD = CarrierClaimColumn.HCPCS_2ND_MDFR_CD.name();

  /** ICD Diagnosis Code. */
  public static final List<String> ICD_DGNS_CD =
      listNumberedEnumNames(CarrierClaimColumn.values(), CarrierClaimColumn.ICD_DGNS_CD1);

  /** ICD Diagnosis Version Code. */
  public static final List<String> ICD_DGNS_VRSN_CD =
      listNumberedEnumNames(CarrierClaimColumn.values(), CarrierClaimColumn.ICD_DGNS_VRSN_CD1);

  /** Line first date. */
  public static final String LINE_1ST_EXPNS_DT = CarrierClaimColumn.LINE_1ST_EXPNS_DT.name();

  /** Line ICD Diagnosis Code. */
  public static final String LINE_ICD_DGNS_CD = CarrierClaimColumn.LINE_ICD_DGNS_CD.name();

  /** ICD Diagnosis Version Code. */
  public static final String LINE_ICD_DGNS_VRSN_CD =
      CarrierClaimColumn.LINE_ICD_DGNS_VRSN_CD.name();

  /** Line Date. */
  public static final String LINE_LAST_EXPNS_DT = CarrierClaimColumn.LINE_LAST_EXPNS_DT.name();

  /** Line Number. */
  public static final String LINE_NUM = CarrierClaimColumn.LINE_NUM.name();

  /** NCH Carrier Claim Submitted Charge Amount. */
  public static final String NCH_CARR_CLM_SBMTD_CHRG_AMT =
      CarrierClaimColumn.NCH_CARR_CLM_SBMTD_CHRG_AMT.name();

  /** Original NPI Number. */
  public static final String ORG_NPI_NUM = CarrierClaimColumn.ORG_NPI_NUM.name();

  /**
   * The maximum number of diagnosis column names available for {@link ICD_DGNS_CD} and {@link
   * ICD_DGNS_VRSN_CD}. Used when looping over both sets of fields.
   */
  public static final int MAX_DIAGNOSIS_CODES =
      Math.min(ICD_DGNS_CD.size(), ICD_DGNS_VRSN_CD.size());

  /** Private constructor method. */
  private Mcs() {}
}
