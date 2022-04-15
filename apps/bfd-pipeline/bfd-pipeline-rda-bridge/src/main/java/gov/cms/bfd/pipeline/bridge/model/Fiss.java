package gov.cms.bfd.pipeline.bridge.model;

import static gov.cms.bfd.model.rif.InpatientClaimColumn.CLM_POA_IND_SW1;
import static gov.cms.bfd.model.rif.InpatientClaimColumn.ICD_DGNS_CD1;
import static gov.cms.bfd.model.rif.InpatientClaimColumn.ICD_PRCDR_CD1;
import static gov.cms.bfd.model.rif.InpatientClaimColumn.PRCDR_DT1;
import static gov.cms.bfd.pipeline.bridge.model.ModelUtil.listNumberedEnumNames;

import gov.cms.bfd.model.rif.InpatientClaimColumn;
import java.util.List;

/**
 * Static references for FISS RIF file column headers. All of the names are extracted from the
 * corresponding column enum class.
 */
public class Fiss {

  public static final String ADMTG_DGNS_CD = InpatientClaimColumn.ADMTG_DGNS_CD.name();
  public static final String BENE_ID = InpatientClaimColumn.BENE_ID.name();
  public static final String CLM_FAC_TYPE_CD = InpatientClaimColumn.CLM_FAC_TYPE_CD.name();
  public static final String CLM_FREQ_CD = InpatientClaimColumn.CLM_FREQ_CD.name();
  public static final String CLM_FROM_DT = InpatientClaimColumn.CLM_FROM_DT.name();
  public static final String CLM_ID = InpatientClaimColumn.CLM_ID.name();
  public static final String CLM_LINE_NUM = InpatientClaimColumn.CLM_LINE_NUM.name();
  public static final List<String> CLM_POA_IND_SW =
      listNumberedEnumNames(InpatientClaimColumn.values(), CLM_POA_IND_SW1);
  public static final String CLM_SRVC_CLSFCTN_TYPE_CD =
      InpatientClaimColumn.CLM_SRVC_CLSFCTN_TYPE_CD.name();
  public static final String CLM_THRU_DT = InpatientClaimColumn.CLM_THRU_DT.name();
  public static final String CLM_TOT_CHRG_AMT = InpatientClaimColumn.CLM_TOT_CHRG_AMT.name();
  public static final String FI_DOC_CLM_CNTL_NUM = InpatientClaimColumn.FI_DOC_CLM_CNTL_NUM.name();
  public static final List<String> ICD_DGNS_CD =
      listNumberedEnumNames(InpatientClaimColumn.values(), ICD_DGNS_CD1);
  public static final List<String> ICD_PRCDR_CD =
      listNumberedEnumNames(InpatientClaimColumn.values(), ICD_PRCDR_CD1);
  public static final String ORG_NPI_NUM = InpatientClaimColumn.ORG_NPI_NUM.name();
  public static final List<String> PRCDR_DT =
      listNumberedEnumNames(InpatientClaimColumn.values(), PRCDR_DT1);
  public static final String PRNCPAL_DGNS_CD = InpatientClaimColumn.PRNCPAL_DGNS_CD.name();
  public static final String PRVDR_NUM = InpatientClaimColumn.PRVDR_NUM.name();

  /**
   * The maximum number of diagnosis column names available for {@link ICD_DGNS_CD} and {@link
   * CLM_POA_IND_SW}. Used when looping over both sets of fields at the same time.
   */
  public static final int MAX_DIAG_CODES = Math.max(ICD_DGNS_CD.size(), CLM_POA_IND_SW.size());

  /**
   * The maximum number of diagnosis column names available for {@link ICD_DGNS_CD} and {@link
   * PRCDR_DT}. Used when looping over both sets of fields.
   */
  public static final int MAX_PROC_CODES = Math.max(ICD_PRCDR_CD.size(), PRCDR_DT.size());

  private Fiss() {}
}
