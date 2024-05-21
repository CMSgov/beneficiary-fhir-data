package gov.cms.bfd.pipeline.bridge.model;

import static gov.cms.bfd.model.rif.entities.InpatientClaimColumn.CLM_POA_IND_SW1;
import static gov.cms.bfd.model.rif.entities.InpatientClaimColumn.ICD_DGNS_CD1;
import static gov.cms.bfd.model.rif.entities.InpatientClaimColumn.ICD_PRCDR_CD1;
import static gov.cms.bfd.model.rif.entities.InpatientClaimColumn.PRCDR_DT1;
import static gov.cms.bfd.pipeline.bridge.model.ModelUtil.listNumberedEnumNames;

import gov.cms.bfd.model.rif.entities.DMEClaimColumn;
import gov.cms.bfd.model.rif.entities.InpatientClaimColumn;
import java.util.List;

/**
 * Static references for FISS RIF file column headers. All of the names are extracted from the
 * corresponding column enum class.
 */
public class Fiss {
  /** Admitting Dianosis Code. */
  public static final String ADMTG_DGNS_CD = InpatientClaimColumn.ADMTG_DGNS_CD.name();

  /** Beneficiary ID. */
  public static final String BENE_ID = InpatientClaimColumn.BENE_ID.name();

  /** Claim Facility Type Code. */
  public static final String CLM_FAC_TYPE_CD = InpatientClaimColumn.CLM_FAC_TYPE_CD.name();

  /** Claim Frequency Code. */
  public static final String CLM_FREQ_CD = InpatientClaimColumn.CLM_FREQ_CD.name();

  /** Claim From Date. */
  public static final String CLM_FROM_DT = InpatientClaimColumn.CLM_FROM_DT.name();

  /** Claim Id. */
  public static final String CLM_ID = InpatientClaimColumn.CLM_ID.name();

  /** Claim Line Number. */
  public static final String CLM_LINE_NUM = InpatientClaimColumn.CLM_LINE_NUM.name();

  /** Claim POA Indicator. */
  public static final List<String> CLM_POA_IND_SW =
      listNumberedEnumNames(InpatientClaimColumn.values(), CLM_POA_IND_SW1);

  /**
   * Claim Service Classifacation Type Code is set to Inpatient Claim Claim Service Classifacation
   * Type Code name.
   */
  public static final String CLM_SRVC_CLSFCTN_TYPE_CD =
      InpatientClaimColumn.CLM_SRVC_CLSFCTN_TYPE_CD.name();

  /** Claim Thru Date is set to Inpatient Claim Thru Date name. */
  public static final String CLM_THRU_DT = InpatientClaimColumn.CLM_THRU_DT.name();

  /** Claim Total Charge Amount is set to Inpatient Claim Claim Total Charge Amount name. */
  public static final String CLM_TOT_CHRG_AMT = InpatientClaimColumn.CLM_TOT_CHRG_AMT.name();

  /** Document Claim Control Number is set to Inpatient Claim Document Claim Control Number name. */
  public static final String FI_DOC_CLM_CNTL_NUM = InpatientClaimColumn.FI_DOC_CLM_CNTL_NUM.name();

  /** ICD Diagnosis Code is set to Inpatient Claim ICD Diagnosis Code name. */
  public static final List<String> ICD_DGNS_CD =
      listNumberedEnumNames(InpatientClaimColumn.values(), ICD_DGNS_CD1);

  /** ICD Procedure Code is set to Inpatient Claim ICD Procedure Code name. */
  public static final List<String> ICD_PRCDR_CD =
      listNumberedEnumNames(InpatientClaimColumn.values(), ICD_PRCDR_CD1);

  /** Original NPI Number is set to Inpatient Claim Original NPI Number name. */
  public static final String ORG_NPI_NUM = InpatientClaimColumn.ORG_NPI_NUM.name();

  /** Procedure Date is set to Inpatient Claim Procedure Date name. */
  public static final List<String> PRCDR_DT =
      listNumberedEnumNames(InpatientClaimColumn.values(), PRCDR_DT1);

  /** Principal Diagnosis Code is set to Inpatient Claim Principal Diagnosis Code name. */
  public static final String PRNCPAL_DGNS_CD = InpatientClaimColumn.PRNCPAL_DGNS_CD.name();

  /** Provider Number is set to Inpatient Claim Provider Number name. */
  public static final String PRVDR_NUM = InpatientClaimColumn.PRVDR_NUM.name();

  /** Claim DRG code is set to Inpatient claim DRG code name. */
  public static final String CLM_DRG_CD = InpatientClaimColumn.CLM_DRG_CD.name();

  /** Rev Center code is set to Inpatient Rev Center name. */
  public static final String REV_CNTR = InpatientClaimColumn.REV_CNTR.name();

  /** Claim Line Service Count is set to DME claim Line Service Count name. */
  public static final String LINE_SRVC_CNT = DMEClaimColumn.LINE_SRVC_CNT.name();

  /** Rev center unit count is set to Inpatient Rev center unit count name. */
  public static final String REV_CNTR_UNIT_CNT = InpatientClaimColumn.REV_CNTR_UNIT_CNT.name();

  /** HCPCS code is set to Inpatient HCPCS code name. */
  public static final String HCPCS_CD = InpatientClaimColumn.HCPCS_CD.name();

  /** HCPCS first modifier code is set to Inpatient HCPCS first modifier code name. */
  public static final String HCPCS_1_MDFR_CD = DMEClaimColumn.HCPCS_1ST_MDFR_CD.name();

  /** HCPCS second modifier code is set to Inpatient HCPCS second modifier code name. */
  public static final String HCPCS_2_MDFR_CD = DMEClaimColumn.HCPCS_2ND_MDFR_CD.name();

  /** HCPCS third modifier code is set to Inpatient HCPCS third modifier code name. */
  public static final String HCPCS_3_MDFR_CD = DMEClaimColumn.HCPCS_3RD_MDFR_CD.name();

  /** HCPCS fourth modifier code is set to Inpatient HCPCS fourth modifier code name. */
  public static final String HCPCS_4_MDFR_CD = DMEClaimColumn.HCPCS_4TH_MDFR_CD.name();

  /**
   * The maximum number of diagnosis column names available for {@link ICD_DGNS_CD} and {@link
   * CLM_POA_IND_SW}. Used when looping over both sets of fields at the same time.
   */
  public static final int MAX_DIAG_CODES = Math.min(ICD_DGNS_CD.size(), CLM_POA_IND_SW.size());

  /**
   * The maximum number of procedure column names available for {@link ICD_DGNS_CD} and {@link
   * PRCDR_DT}. Used when looping over both sets of fields.
   */
  public static final int MAX_PROC_CODES = Math.min(ICD_PRCDR_CD.size(), PRCDR_DT.size());

  /** ADM-TYP-CD code is set to Inpatient CLM_IP_ADMSN_TYPE_CD code name. */
  public static final String ADM_TYP_CD = InpatientClaimColumn.CLM_IP_ADMSN_TYPE_CD.name();

  /** Private constructor method. */
  private Fiss() {}
}
