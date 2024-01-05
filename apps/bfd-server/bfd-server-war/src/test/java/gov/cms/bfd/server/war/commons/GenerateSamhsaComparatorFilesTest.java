package gov.cms.bfd.server.war.commons;

import gov.cms.bfd.server.war.ServerTestUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Generates files needed for SAMHSA filtering tests using the SAMHSA code files. */
public class GenerateSamhsaComparatorFilesTest {

  /**
   * Generates the SAMHSA test files used when E2E testing the samhsa filtering logic. Files are
   * generated with a claim per samhsa code to comprehensively test that we will properly filter
   * each claim that has a samhsa code that applies to that claim type. To run this test, execute
   * the following Maven Command: mvn clean install -DgenerateTestData=true.
   */
  // @EnabledIfSystemProperty(named = "generateTestData", matches = "true")
  @Test
  public void generateSamhsaSampleFiles() {

    // Gather the various samhsa code lists
    List<String> drgCodes =
        AbstractSamhsaMatcher.resourceCsvColumnToList(
                "samhsa-related-codes/codes-drg.csv", "MS-DRGs")
            .stream()
            .map(AbstractSamhsaMatcher::normalizeDrgCode)
            .toList();
    List<String> cptCodes =
        AbstractSamhsaMatcher.resourceCsvColumnToList(
                "samhsa-related-codes/codes-cpt.csv", "CPT Code")
            .stream()
            .map(AbstractSamhsaMatcher::normalizeHcpcsCode)
            .toList();
    List<String> icd9ProcedureCodes =
        AbstractSamhsaMatcher.resourceCsvColumnToList(
                "samhsa-related-codes/codes-icd-9-procedure.csv", "ICD-9-CM")
            .stream()
            .map(AbstractSamhsaMatcher::normalizeIcdCode)
            .toList();
    List<String> icd9DiagnosisCodes =
        AbstractSamhsaMatcher.resourceCsvColumnToList(
                "samhsa-related-codes/codes-icd-9-diagnosis.csv", "ICD-9-CM Diagnosis Code")
            .stream()
            .map(AbstractSamhsaMatcher::normalizeIcdCode)
            .toList();
    List<String> icd10ProcedureCodes =
        AbstractSamhsaMatcher.resourceCsvColumnToList(
                "samhsa-related-codes/codes-icd-10-procedure.csv", "ICD-10-PCS Code")
            .stream()
            .map(AbstractSamhsaMatcher::normalizeIcdCode)
            .toList();
    List<String> icd10DiagnosisCodes =
        AbstractSamhsaMatcher.resourceCsvColumnToList(
                "samhsa-related-codes/codes-icd-10-diagnosis.csv", "ICD-10-CM Diagnosis Code")
            .stream()
            .map(AbstractSamhsaMatcher::normalizeIcdCode)
            .toList();

    // keep an incrementing claim id for each claim. We end at ~5000, so increase a digit if we ever
    // need to
    int claimId = 1000;

    // Create a new file for each of the samhsa-valid claim types for the test pipeline to load for
    // samhsa tests
    claimId =
        createSamhsaSampleFile(
            ClaimType.CARRIER,
            drgCodes,
            cptCodes,
            icd9ProcedureCodes,
            icd10ProcedureCodes,
            icd9DiagnosisCodes,
            icd10DiagnosisCodes,
            claimId);
    claimId =
        createSamhsaSampleFile(
            ClaimType.DME,
            drgCodes,
            cptCodes,
            icd9ProcedureCodes,
            icd10ProcedureCodes,
            icd9DiagnosisCodes,
            icd10DiagnosisCodes,
            claimId);
    claimId =
        createSamhsaSampleFile(
            ClaimType.HHA,
            drgCodes,
            cptCodes,
            icd9ProcedureCodes,
            icd10ProcedureCodes,
            icd9DiagnosisCodes,
            icd10DiagnosisCodes,
            claimId);
    claimId =
        createSamhsaSampleFile(
            ClaimType.HOSPICE,
            drgCodes,
            cptCodes,
            icd9ProcedureCodes,
            icd10ProcedureCodes,
            icd9DiagnosisCodes,
            icd10DiagnosisCodes,
            claimId);
    claimId =
        createSamhsaSampleFile(
            ClaimType.INPATIENT,
            drgCodes,
            cptCodes,
            icd9ProcedureCodes,
            icd10ProcedureCodes,
            icd9DiagnosisCodes,
            icd10DiagnosisCodes,
            claimId);
    claimId =
        createSamhsaSampleFile(
            ClaimType.OUTPATIENT,
            drgCodes,
            cptCodes,
            icd9ProcedureCodes,
            icd10ProcedureCodes,
            icd9DiagnosisCodes,
            icd10DiagnosisCodes,
            claimId);
    claimId =
        createSamhsaSampleFile(
            ClaimType.SNF,
            drgCodes,
            cptCodes,
            icd9ProcedureCodes,
            icd10ProcedureCodes,
            icd9DiagnosisCodes,
            icd10DiagnosisCodes,
            claimId);
  }

  /**
   * Creates a new SAMHSA test sample file using the existing SAMHSA codes.
   *
   * @param claimType the claimtype to create the file for
   * @param drgCodes the list of DRG codes from the codes file
   * @param cptCodes the list of CPT codes from the codes file
   * @param icd9ProcedureCodes the list of ICD9 procedure codes from the codes file
   * @param icd10ProcedureCodes the list of ICD10 procedure codes from the codes file
   * @param icd9DiagnosisCodes the list of ICD9 diagnosis codes from the codes file
   * @param icd10DiagnosisCodes the list of ICD10 diagnosis codes from the codes file
   * @param claimId claimId counter (so we don't have any duplicate claim IDs)
   * @return the updated claimId counter
   */
  private int createSamhsaSampleFile(
      ClaimType claimType,
      List<String> drgCodes,
      List<String> cptCodes,
      List<String> icd9ProcedureCodes,
      List<String> icd10ProcedureCodes,
      List<String> icd9DiagnosisCodes,
      List<String> icd10DiagnosisCodes,
      int claimId) {

    String header = "";
    List<String> lines = new ArrayList<>();
    int newClaimId = claimId;

    // Each claim type looks at different SAMHSA filtering data, so add only what is filterable for
    // that claim type
    switch (claimType) {
      case CARRIER:
        header =
            "DML_IND|BENE_ID|CLM_ID|CLM_GRP_ID|FINAL_ACTION|NCH_NEAR_LINE_REC_IDENT_CD|NCH_CLM_TYPE_CD|CLM_FROM_DT|CLM_THRU_DT|NCH_WKLY_PROC_DT|CARR_CLM_ENTRY_CD|CLM_DISP_CD|CARR_NUM|CARR_CLM_PMT_DNL_CD|CLM_PMT_AMT|CARR_CLM_PRMRY_PYR_PD_AMT|RFR_PHYSN_UPIN|RFR_PHYSN_NPI|CARR_CLM_PRVDR_ASGNMT_IND_SW|NCH_CLM_PRVDR_PMT_AMT|NCH_CLM_BENE_PMT_AMT|NCH_CARR_CLM_SBMTD_CHRG_AMT|NCH_CARR_CLM_ALOWD_AMT|CARR_CLM_CASH_DDCTBL_APLD_AMT|CARR_CLM_HCPCS_YR_CD|CARR_CLM_RFRNG_PIN_NUM|PRNCPAL_DGNS_CD|PRNCPAL_DGNS_VRSN_CD|ICD_DGNS_CD1|ICD_DGNS_VRSN_CD1|ICD_DGNS_CD2|ICD_DGNS_VRSN_CD2|ICD_DGNS_CD3|ICD_DGNS_VRSN_CD3|ICD_DGNS_CD4|ICD_DGNS_VRSN_CD4|ICD_DGNS_CD5|ICD_DGNS_VRSN_CD5|ICD_DGNS_CD6|ICD_DGNS_VRSN_CD6|ICD_DGNS_CD7|ICD_DGNS_VRSN_CD7|ICD_DGNS_CD8|ICD_DGNS_VRSN_CD8|ICD_DGNS_CD9|ICD_DGNS_VRSN_CD9|ICD_DGNS_CD10|ICD_DGNS_VRSN_CD10|ICD_DGNS_CD11|ICD_DGNS_VRSN_CD11|ICD_DGNS_CD12|ICD_DGNS_VRSN_CD12|CLM_CLNCL_TRIL_NUM|CARR_CLM_CNTL_NUM|CARR_CLM_BLG_NPI_NUM|LINE_NUM|CARR_PRFRNG_PIN_NUM|PRF_PHYSN_UPIN|PRF_PHYSN_NPI|ORG_NPI_NUM|CARR_LINE_PRVDR_TYPE_CD|TAX_NUM|PRVDR_STATE_CD|PRVDR_ZIP|PRVDR_SPCLTY|PRTCPTNG_IND_CD|CARR_LINE_RDCD_PMT_PHYS_ASTN_C|LINE_SRVC_CNT|LINE_CMS_TYPE_SRVC_CD|LINE_PLACE_OF_SRVC_CD|CARR_LINE_PRCNG_LCLTY_CD|LINE_1ST_EXPNS_DT|LINE_LAST_EXPNS_DT|HCPCS_CD|HCPCS_1ST_MDFR_CD|HCPCS_2ND_MDFR_CD|BETOS_CD|LINE_NCH_PMT_AMT|LINE_BENE_PMT_AMT|LINE_PRVDR_PMT_AMT|LINE_BENE_PTB_DDCTBL_AMT|LINE_BENE_PRMRY_PYR_CD|LINE_BENE_PRMRY_PYR_PD_AMT|LINE_COINSRNC_AMT|LINE_SBMTD_CHRG_AMT|LINE_ALOWD_CHRG_AMT|LINE_PRCSG_IND_CD|LINE_PMT_80_100_CD|LINE_SERVICE_DEDUCTIBLE|CARR_LINE_MTUS_CNT|CARR_LINE_MTUS_CD|LINE_ICD_DGNS_CD|LINE_ICD_DGNS_VRSN_CD|HPSA_SCRCTY_IND_CD|CARR_LINE_RX_NUM|LINE_HCT_HGB_RSLT_NUM|LINE_HCT_HGB_TYPE_CD|LINE_NDC_CD|CARR_LINE_CLIA_LAB_NUM|CARR_LINE_ANSTHSA_UNIT_CNT";
        String sampleCarrierLine =
            "INSERT|567834|123456|900|F|O|71|27-OCT-1999|27-OCT-1999|06-NOV-1999|1|1|61026|1|199.99|0|1234534|8765676|A|123.45|888.00|245.04|166.23|777.00|5|K25852|A02|0|A02|0|A05|9|B04|0|B05|0|||||||||||||||||0|74655592568216|1234567890|6|K25555||1923124|0000000000|0|204299999|IL|555558202|41|1|0|1.0|1|11|15|27-OCT-1999|27-OCT-1999|92999|LT||T2D|37.5|0|37.5|0|E|0|9.57|75|47.84|A|0|0|1|3|A52|0|||42.0|R1|000000000|BB889999AA|0";

        // Uses ICD_DGNS_CD
        String sampleCarrierIcd9Line =
            setSubColumns(sampleCarrierLine, header, "ICD_DGNS_CD1", true, false);
        newClaimId = addLines(lines, newClaimId, icd9DiagnosisCodes, sampleCarrierIcd9Line);

        // Uses ICD_DGNS_CD
        String sampleCarrierIcd10Line =
            setSubColumns(sampleCarrierLine, header, "ICD_DGNS_CD1", false, true);
        newClaimId = addLines(lines, newClaimId, icd10DiagnosisCodes, sampleCarrierIcd10Line);

        // Uses HCPCS_CD
        String sampleCarrierCptLine = setSubColumns(sampleCarrierLine, header, "HCPCS_CD");
        newClaimId = addLines(lines, newClaimId, cptCodes, sampleCarrierCptLine);
        break;
      case DME:
        header =
            "DML_IND|BENE_ID|CLM_ID|CLM_GRP_ID|FINAL_ACTION|NCH_NEAR_LINE_REC_IDENT_CD|NCH_CLM_TYPE_CD|CLM_FROM_DT|CLM_THRU_DT|NCH_WKLY_PROC_DT|CARR_CLM_ENTRY_CD|CLM_DISP_CD|CARR_NUM|CARR_CLM_PMT_DNL_CD|CLM_PMT_AMT|CARR_CLM_PRMRY_PYR_PD_AMT|CARR_CLM_PRVDR_ASGNMT_IND_SW|NCH_CLM_PRVDR_PMT_AMT|NCH_CLM_BENE_PMT_AMT|NCH_CARR_CLM_SBMTD_CHRG_AMT|NCH_CARR_CLM_ALOWD_AMT|CARR_CLM_CASH_DDCTBL_APLD_AMT|CARR_CLM_HCPCS_YR_CD|PRNCPAL_DGNS_CD|PRNCPAL_DGNS_VRSN_CD|ICD_DGNS_CD1|ICD_DGNS_VRSN_CD1|ICD_DGNS_CD2|ICD_DGNS_VRSN_CD2|ICD_DGNS_CD3|ICD_DGNS_VRSN_CD3|ICD_DGNS_CD4|ICD_DGNS_VRSN_CD4|ICD_DGNS_CD5|ICD_DGNS_VRSN_CD5|ICD_DGNS_CD6|ICD_DGNS_VRSN_CD6|ICD_DGNS_CD7|ICD_DGNS_VRSN_CD7|ICD_DGNS_CD8|ICD_DGNS_VRSN_CD8|ICD_DGNS_CD9|ICD_DGNS_VRSN_CD9|ICD_DGNS_CD10|ICD_DGNS_VRSN_CD10|ICD_DGNS_CD11|ICD_DGNS_VRSN_CD11|ICD_DGNS_CD12|ICD_DGNS_VRSN_CD12|RFR_PHYSN_UPIN|RFR_PHYSN_NPI|CLM_CLNCL_TRIL_NUM|CARR_CLM_CNTL_NUM|LINE_NUM|TAX_NUM|PRVDR_SPCLTY|PRTCPTNG_IND_CD|LINE_SRVC_CNT|LINE_CMS_TYPE_SRVC_CD|LINE_PLACE_OF_SRVC_CD|LINE_1ST_EXPNS_DT|LINE_LAST_EXPNS_DT|HCPCS_CD|HCPCS_1ST_MDFR_CD|HCPCS_2ND_MDFR_CD|BETOS_CD|LINE_NCH_PMT_AMT|LINE_BENE_PMT_AMT|LINE_PRVDR_PMT_AMT|LINE_BENE_PTB_DDCTBL_AMT|LINE_BENE_PRMRY_PYR_CD|LINE_BENE_PRMRY_PYR_PD_AMT|LINE_COINSRNC_AMT|LINE_PRMRY_ALOWD_CHRG_AMT|LINE_SBMTD_CHRG_AMT|LINE_ALOWD_CHRG_AMT|LINE_PRCSG_IND_CD|LINE_PMT_80_100_CD|LINE_SERVICE_DEDUCTIBLE|LINE_ICD_DGNS_CD|LINE_ICD_DGNS_VRSN_CD|LINE_DME_PRCHS_PRICE_AMT|PRVDR_NUM|PRVDR_NPI|DMERC_LINE_PRCNG_STATE_CD|PRVDR_STATE_CD|DMERC_LINE_SUPPLR_TYPE_CD|HCPCS_3RD_MDFR_CD|HCPCS_4TH_MDFR_CD|DMERC_LINE_SCRN_SVGS_AMT|DMERC_LINE_MTUS_CNT|DMERC_LINE_MTUS_CD|LINE_HCT_HGB_RSLT_NUM|LINE_HCT_HGB_TYPE_CD|LINE_NDC_CD";
        String sampleDmeLine =
            "INSERT|567834|123456|900|F|M|82|03-FEB-2014|03-FEB-2014|14-FEB-2014|1|01|99999|1|777.75|0|A|666.75|666.66|1752.75|754.79|777.00|3|B04|0|A05|9|A37|0|||||||||||||||||||||I99999|1306849450|0|74655592568216|1|9994931888|A5|1|60|P|12|03-FEB-2014|03-FEB-2014|G0137|YY||D9Z|123.45|11.00|120.00|18.00|E|11.00|20.20|20.29|130.45|129.45|A|0|0|A25|0|82.29|1219966666|1244444444|AL|MO|3|||0.00|60.234|3|44.4|R2|000000000|";

        // Uses ICD_DGNS_CD
        String sampleDmeIcd9Line =
            setSubColumns(sampleDmeLine, header, "ICD_DGNS_CD1", true, false);
        newClaimId = addLines(lines, newClaimId, icd9DiagnosisCodes, sampleDmeIcd9Line);

        // Uses ICD_DGNS_CD
        // ICD Version determines if they are 9 or 10 (10 is 0, since its a 1 char field), so that
        // is adjusted for these ICD10 codes
        String sampleDmeIcd10Line =
            setSubColumns(sampleDmeLine, header, "ICD_DGNS_CD1", false, true);
        newClaimId = addLines(lines, newClaimId, icd10DiagnosisCodes, sampleDmeIcd10Line);

        // Uses HCPCS_CD
        String sampleDmeCptLine = setSubColumns(sampleDmeLine, header, "HCPCS_CD");
        newClaimId = addLines(lines, newClaimId, cptCodes, sampleDmeCptLine);
        break;
      case HHA:
        header =
            "DML_IND|BENE_ID|CLM_ID|CLM_GRP_ID|FINAL_ACTION|NCH_NEAR_LINE_REC_IDENT_CD|NCH_CLM_TYPE_CD|CLM_FROM_DT|CLM_THRU_DT|NCH_WKLY_PROC_DT|FI_CLM_PROC_DT|PRVDR_NUM|CLM_FAC_TYPE_CD|CLM_SRVC_CLSFCTN_TYPE_CD|CLM_FREQ_CD|FI_NUM|CLM_MDCR_NON_PMT_RSN_CD|CLM_PMT_AMT|NCH_PRMRY_PYR_CLM_PD_AMT|NCH_PRMRY_PYR_CD|PRVDR_STATE_CD|ORG_NPI_NUM|AT_PHYSN_UPIN|AT_PHYSN_NPI|PTNT_DSCHRG_STUS_CD|CLM_PPS_IND_CD|CLM_TOT_CHRG_AMT|PRNCPAL_DGNS_CD|PRNCPAL_DGNS_VRSN_CD|ICD_DGNS_CD1|ICD_DGNS_VRSN_CD1|ICD_DGNS_CD2|ICD_DGNS_VRSN_CD2|ICD_DGNS_CD3|ICD_DGNS_VRSN_CD3|ICD_DGNS_CD4|ICD_DGNS_VRSN_CD4|ICD_DGNS_CD5|ICD_DGNS_VRSN_CD5|ICD_DGNS_CD6|ICD_DGNS_VRSN_CD6|ICD_DGNS_CD7|ICD_DGNS_VRSN_CD7|ICD_DGNS_CD8|ICD_DGNS_VRSN_CD8|ICD_DGNS_CD9|ICD_DGNS_VRSN_CD9|ICD_DGNS_CD10|ICD_DGNS_VRSN_CD10|ICD_DGNS_CD11|ICD_DGNS_VRSN_CD11|ICD_DGNS_CD12|ICD_DGNS_VRSN_CD12|ICD_DGNS_CD13|ICD_DGNS_VRSN_CD13|ICD_DGNS_CD14|ICD_DGNS_VRSN_CD14|ICD_DGNS_CD15|ICD_DGNS_VRSN_CD15|ICD_DGNS_CD16|ICD_DGNS_VRSN_CD16|ICD_DGNS_CD17|ICD_DGNS_VRSN_CD17|ICD_DGNS_CD18|ICD_DGNS_VRSN_CD18|ICD_DGNS_CD19|ICD_DGNS_VRSN_CD19|ICD_DGNS_CD20|ICD_DGNS_VRSN_CD20|ICD_DGNS_CD21|ICD_DGNS_VRSN_CD21|ICD_DGNS_CD22|ICD_DGNS_VRSN_CD22|ICD_DGNS_CD23|ICD_DGNS_VRSN_CD23|ICD_DGNS_CD24|ICD_DGNS_VRSN_CD24|ICD_DGNS_CD25|ICD_DGNS_VRSN_CD25|FST_DGNS_E_CD|FST_DGNS_E_VRSN_CD|ICD_DGNS_E_CD1|ICD_DGNS_E_VRSN_CD1|ICD_DGNS_E_CD2|ICD_DGNS_E_VRSN_CD2|ICD_DGNS_E_CD3|ICD_DGNS_E_VRSN_CD3|ICD_DGNS_E_CD4|ICD_DGNS_E_VRSN_CD4|ICD_DGNS_E_CD5|ICD_DGNS_E_VRSN_CD5|ICD_DGNS_E_CD6|ICD_DGNS_E_VRSN_CD6|ICD_DGNS_E_CD7|ICD_DGNS_E_VRSN_CD7|ICD_DGNS_E_CD8|ICD_DGNS_E_VRSN_CD8|ICD_DGNS_E_CD9|ICD_DGNS_E_VRSN_CD9|ICD_DGNS_E_CD10|ICD_DGNS_E_VRSN_CD10|ICD_DGNS_E_CD11|ICD_DGNS_E_VRSN_CD11|ICD_DGNS_E_CD12|ICD_DGNS_E_VRSN_CD12|CLM_HHA_LUPA_IND_CD|CLM_HHA_RFRL_CD|CLM_HHA_TOT_VISIT_CNT|CLM_ADMSN_DT|FI_DOC_CLM_CNTL_NUM|FI_ORIG_CLM_CNTL_NUM|CLAIM_QUERY_CODE|CLM_LINE_NUM|REV_CNTR|REV_CNTR_DT|REV_CNTR_1ST_ANSI_CD|REV_CNTR_APC_HIPPS_CD|HCPCS_CD|HCPCS_1ST_MDFR_CD|HCPCS_2ND_MDFR_CD|REV_CNTR_PMT_MTHD_IND_CD|REV_CNTR_UNIT_CNT|REV_CNTR_RATE_AMT|REV_CNTR_PMT_AMT_AMT|REV_CNTR_TOT_CHRG_AMT|REV_CNTR_NCVRD_CHRG_AMT|REV_CNTR_DDCTBL_COINSRNC_CD|REV_CNTR_STUS_IND_CD|REV_CNTR_NDC_QTY|REV_CNTR_NDC_QTY_QLFR_CD|RNDRNG_PHYSN_UPIN|RNDRNG_PHYSN_NPI";
        String sampleHhaLine =
            "INSERT|567834|123456|900|F|W|10|23-JUN-2015|23-JUN-2015|06-NOV-2015|30-OCT-2015|45645|3|2|1|15999|P|188.00|11.00|A|UT|0000000000||2222222222|30|2|199.99|A05|0|A06|9|B01|0|||||||||||||||||||||||||||||||||||||||||||||||B05|0|A06|9|B30|0|||||||||||||||||||||L|1|3|23-JUN-2015|08683096577486|10493204767560565|3|1|0023|23-JUN-2015|CO120|0|2GGGG|KO||3|1|0|26.00|25.00|24.00|4|A|666|DD||345345345|";

        // Uses ICD_DGNS_CD
        String sampleHhaIcd9Line =
            setSubColumns(sampleHhaLine, header, "ICD_DGNS_CD1", true, false);
        newClaimId = addLines(lines, newClaimId, icd9DiagnosisCodes, sampleHhaIcd9Line);

        // Uses ICD_DGNS_CD
        // ICD Version determines if they are 9 or 10 (10 is 0, since its a 1 char field), so that
        // is adjusted for these ICD10 codes
        String sampleHhaIcd10Line =
            setSubColumns(sampleHhaLine, header, "ICD_DGNS_CD1", false, true);
        newClaimId = addLines(lines, newClaimId, icd10DiagnosisCodes, sampleHhaIcd10Line);

        // Uses HCPCS_CD
        String sampleHhaCptLine = setSubColumns(sampleHhaLine, header, "HCPCS_CD");
        newClaimId = addLines(lines, newClaimId, cptCodes, sampleHhaCptLine);
        break;
      case HOSPICE:
        header =
            "DML_IND|BENE_ID|CLM_ID|CLM_GRP_ID|FINAL_ACTION|NCH_NEAR_LINE_REC_IDENT_CD|NCH_CLM_TYPE_CD|CLM_FROM_DT|CLM_THRU_DT|NCH_WKLY_PROC_DT|FI_CLM_PROC_DT|PRVDR_NUM|CLM_FAC_TYPE_CD|CLM_SRVC_CLSFCTN_TYPE_CD|CLM_FREQ_CD|FI_NUM|CLM_MDCR_NON_PMT_RSN_CD|CLM_PMT_AMT|NCH_PRMRY_PYR_CLM_PD_AMT|NCH_PRMRY_PYR_CD|PRVDR_STATE_CD|ORG_NPI_NUM|AT_PHYSN_UPIN|AT_PHYSN_NPI|PTNT_DSCHRG_STUS_CD|CLM_TOT_CHRG_AMT|NCH_PTNT_STATUS_IND_CD|CLM_UTLZTN_DAY_CNT|NCH_BENE_DSCHRG_DT|PRNCPAL_DGNS_CD|PRNCPAL_DGNS_VRSN_CD|ICD_DGNS_CD1|ICD_DGNS_VRSN_CD1|ICD_DGNS_CD2|ICD_DGNS_VRSN_CD2|ICD_DGNS_CD3|ICD_DGNS_VRSN_CD3|ICD_DGNS_CD4|ICD_DGNS_VRSN_CD4|ICD_DGNS_CD5|ICD_DGNS_VRSN_CD5|ICD_DGNS_CD6|ICD_DGNS_VRSN_CD6|ICD_DGNS_CD7|ICD_DGNS_VRSN_CD7|ICD_DGNS_CD8|ICD_DGNS_VRSN_CD8|ICD_DGNS_CD9|ICD_DGNS_VRSN_CD9|ICD_DGNS_CD10|ICD_DGNS_VRSN_CD10|ICD_DGNS_CD11|ICD_DGNS_VRSN_CD11|ICD_DGNS_CD12|ICD_DGNS_VRSN_CD12|ICD_DGNS_CD13|ICD_DGNS_VRSN_CD13|ICD_DGNS_CD14|ICD_DGNS_VRSN_CD14|ICD_DGNS_CD15|ICD_DGNS_VRSN_CD15|ICD_DGNS_CD16|ICD_DGNS_VRSN_CD16|ICD_DGNS_CD17|ICD_DGNS_VRSN_CD17|ICD_DGNS_CD18|ICD_DGNS_VRSN_CD18|ICD_DGNS_CD19|ICD_DGNS_VRSN_CD19|ICD_DGNS_CD20|ICD_DGNS_VRSN_CD20|ICD_DGNS_CD21|ICD_DGNS_VRSN_CD21|ICD_DGNS_CD22|ICD_DGNS_VRSN_CD22|ICD_DGNS_CD23|ICD_DGNS_VRSN_CD23|ICD_DGNS_CD24|ICD_DGNS_VRSN_CD24|ICD_DGNS_CD25|ICD_DGNS_VRSN_CD25|FST_DGNS_E_CD|FST_DGNS_E_VRSN_CD|ICD_DGNS_E_CD1|ICD_DGNS_E_VRSN_CD1|ICD_DGNS_E_CD2|ICD_DGNS_E_VRSN_CD2|ICD_DGNS_E_CD3|ICD_DGNS_E_VRSN_CD3|ICD_DGNS_E_CD4|ICD_DGNS_E_VRSN_CD4|ICD_DGNS_E_CD5|ICD_DGNS_E_VRSN_CD5|ICD_DGNS_E_CD6|ICD_DGNS_E_VRSN_CD6|ICD_DGNS_E_CD7|ICD_DGNS_E_VRSN_CD7|ICD_DGNS_E_CD8|ICD_DGNS_E_VRSN_CD8|ICD_DGNS_E_CD9|ICD_DGNS_E_VRSN_CD9|ICD_DGNS_E_CD10|ICD_DGNS_E_VRSN_CD10|ICD_DGNS_E_CD11|ICD_DGNS_E_VRSN_CD11|ICD_DGNS_E_CD12|ICD_DGNS_E_VRSN_CD12|CLM_HOSPC_START_DT_ID|BENE_HOSPC_PRD_CNT|FI_DOC_CLM_CNTL_NUM|FI_ORIG_CLM_CNTL_NUM|CLAIM_QUERY_CODE|CLM_LINE_NUM|REV_CNTR|REV_CNTR_DT|HCPCS_CD|HCPCS_1ST_MDFR_CD|HCPCS_2ND_MDFR_CD|REV_CNTR_UNIT_CNT|REV_CNTR_RATE_AMT|REV_CNTR_PRVDR_PMT_AMT|REV_CNTR_BENE_PMT_AMT|REV_CNTR_PMT_AMT_AMT|REV_CNTR_TOT_CHRG_AMT|REV_CNTR_NCVRD_CHRG_AMT|REV_CNTR_DDCTBL_COINSRNC_CD|REV_CNTR_NDC_QTY|REV_CNTR_NDC_QTY_QLFR_CD|RNDRNG_PHYSN_UPIN|RNDRNG_PHYSN_NPI";
        String sampleHospiceLine =
            "INSERT|567834|%s|900|F|V|50|01-JAN-2014|30-JAN-2014|10-OCT-2014|07-OCT-2014|12345|8|1|1|6666|P|130.32|0|A|AZ|0000000000||8888888888|30|199.99|C|30|29-JUN-2015|72761|9|A05|9|B30|0|||||||||||||||||||||||||||||||||||||||||||||||A05|9|A06|0|A52|0|||||||||||||||||||||06-JUL-2014|2|2718813985998|38875439343923937|3|1|651|01-SEP-2014|A5C|Q9999||||29.00|28.00|26.00|2555.00|300.00|0|5454|B|0|345345345|";

        // Uses ICD_DGNS_CD
        String sampleHospiceIcd9Line =
            setSubColumns(sampleHospiceLine, header, "ICD_DGNS_CD1", true, false);
        newClaimId = addLines(lines, newClaimId, icd9DiagnosisCodes, sampleHospiceIcd9Line);

        // Uses ICD_DGNS_CD
        String sampleHospiceIcd10Line =
            setSubColumns(sampleHospiceLine, header, "ICD_DGNS_CD1", false, true);
        newClaimId = addLines(lines, newClaimId, icd10DiagnosisCodes, sampleHospiceIcd10Line);

        // Uses HCPCS_CD
        String sampleHospiceCptLine = setSubColumns(sampleHospiceLine, header, "HCPCS_CD");
        newClaimId = addLines(lines, newClaimId, cptCodes, sampleHospiceCptLine);
        break;
      case INPATIENT:
        header =
            "DML_IND|BENE_ID|CLM_ID|CLM_GRP_ID|FINAL_ACTION|NCH_NEAR_LINE_REC_IDENT_CD|NCH_CLM_TYPE_CD|CLM_FROM_DT|CLM_THRU_DT|NCH_WKLY_PROC_DT|FI_CLM_PROC_DT|CLAIM_QUERY_CODE|PRVDR_NUM|CLM_FAC_TYPE_CD|CLM_SRVC_CLSFCTN_TYPE_CD|CLM_FREQ_CD|FI_NUM|CLM_MDCR_NON_PMT_RSN_CD|CLM_PMT_AMT|NCH_PRMRY_PYR_CLM_PD_AMT|NCH_PRMRY_PYR_CD|FI_CLM_ACTN_CD|PRVDR_STATE_CD|ORG_NPI_NUM|AT_PHYSN_UPIN|AT_PHYSN_NPI|OP_PHYSN_UPIN|OP_PHYSN_NPI|OT_PHYSN_UPIN|OT_PHYSN_NPI|CLM_MCO_PD_SW|PTNT_DSCHRG_STUS_CD|CLM_PPS_IND_CD|CLM_TOT_CHRG_AMT|CLM_ADMSN_DT|CLM_IP_ADMSN_TYPE_CD|CLM_SRC_IP_ADMSN_CD|NCH_PTNT_STATUS_IND_CD|CLM_PASS_THRU_PER_DIEM_AMT|NCH_BENE_IP_DDCTBL_AMT|NCH_BENE_PTA_COINSRNC_LBLTY_AM|NCH_BENE_BLOOD_DDCTBL_LBLTY_AM|NCH_PROFNL_CMPNT_CHRG_AMT|NCH_IP_NCVRD_CHRG_AMT|NCH_IP_TOT_DDCTN_AMT|CLM_TOT_PPS_CPTL_AMT|CLM_PPS_CPTL_FSP_AMT|CLM_PPS_CPTL_OUTLIER_AMT|CLM_PPS_CPTL_DSPRPRTNT_SHR_AMT|CLM_PPS_CPTL_IME_AMT|CLM_PPS_CPTL_EXCPTN_AMT|CLM_PPS_OLD_CPTL_HLD_HRMLS_AMT|CLM_PPS_CPTL_DRG_WT_NUM|CLM_UTLZTN_DAY_CNT|BENE_TOT_COINSRNC_DAYS_CNT|BENE_LRD_USED_CNT|CLM_NON_UTLZTN_DAYS_CNT|NCH_BLOOD_PNTS_FRNSHD_QTY|NCH_VRFD_NCVRD_STAY_FROM_DT|NCH_VRFD_NCVRD_STAY_THRU_DT|NCH_ACTV_OR_CVRD_LVL_CARE_THRU|NCH_BENE_MDCR_BNFTS_EXHTD_DT_I|NCH_BENE_DSCHRG_DT|CLM_DRG_CD|CLM_DRG_OUTLIER_STAY_CD|NCH_DRG_OUTLIER_APRVD_PMT_AMT|ADMTG_DGNS_CD|ADMTG_DGNS_VRSN_CD|PRNCPAL_DGNS_CD|PRNCPAL_DGNS_VRSN_CD|ICD_DGNS_CD1|ICD_DGNS_VRSN_CD1|CLM_POA_IND_SW1|ICD_DGNS_CD2|ICD_DGNS_VRSN_CD2|CLM_POA_IND_SW2|ICD_DGNS_CD3|ICD_DGNS_VRSN_CD3|CLM_POA_IND_SW3|ICD_DGNS_CD4|ICD_DGNS_VRSN_CD4|CLM_POA_IND_SW4|ICD_DGNS_CD5|ICD_DGNS_VRSN_CD5|CLM_POA_IND_SW5|ICD_DGNS_CD6|ICD_DGNS_VRSN_CD6|CLM_POA_IND_SW6|ICD_DGNS_CD7|ICD_DGNS_VRSN_CD7|CLM_POA_IND_SW7|ICD_DGNS_CD8|ICD_DGNS_VRSN_CD8|CLM_POA_IND_SW8|ICD_DGNS_CD9|ICD_DGNS_VRSN_CD9|CLM_POA_IND_SW9|ICD_DGNS_CD10|ICD_DGNS_VRSN_CD10|CLM_POA_IND_SW10|ICD_DGNS_CD11|ICD_DGNS_VRSN_CD11|CLM_POA_IND_SW11|ICD_DGNS_CD12|ICD_DGNS_VRSN_CD12|CLM_POA_IND_SW12|ICD_DGNS_CD13|ICD_DGNS_VRSN_CD13|CLM_POA_IND_SW13|ICD_DGNS_CD14|ICD_DGNS_VRSN_CD14|CLM_POA_IND_SW14|ICD_DGNS_CD15|ICD_DGNS_VRSN_CD15|CLM_POA_IND_SW15|ICD_DGNS_CD16|ICD_DGNS_VRSN_CD16|CLM_POA_IND_SW16|ICD_DGNS_CD17|ICD_DGNS_VRSN_CD17|CLM_POA_IND_SW17|ICD_DGNS_CD18|ICD_DGNS_VRSN_CD18|CLM_POA_IND_SW18|ICD_DGNS_CD19|ICD_DGNS_VRSN_CD19|CLM_POA_IND_SW19|ICD_DGNS_CD20|ICD_DGNS_VRSN_CD20|CLM_POA_IND_SW20|ICD_DGNS_CD21|ICD_DGNS_VRSN_CD21|CLM_POA_IND_SW21|ICD_DGNS_CD22|ICD_DGNS_VRSN_CD22|CLM_POA_IND_SW22|ICD_DGNS_CD23|ICD_DGNS_VRSN_CD23|CLM_POA_IND_SW23|ICD_DGNS_CD24|ICD_DGNS_VRSN_CD24|CLM_POA_IND_SW24|ICD_DGNS_CD25|ICD_DGNS_VRSN_CD25|CLM_POA_IND_SW25|FST_DGNS_E_CD|FST_DGNS_E_VRSN_CD|ICD_DGNS_E_CD1|ICD_DGNS_E_VRSN_CD1|CLM_E_POA_IND_SW1|ICD_DGNS_E_CD2|ICD_DGNS_E_VRSN_CD2|CLM_E_POA_IND_SW2|ICD_DGNS_E_CD3|ICD_DGNS_E_VRSN_CD3|CLM_E_POA_IND_SW3|ICD_DGNS_E_CD4|ICD_DGNS_E_VRSN_CD4|CLM_E_POA_IND_SW4|ICD_DGNS_E_CD5|ICD_DGNS_E_VRSN_CD5|CLM_E_POA_IND_SW5|ICD_DGNS_E_CD6|ICD_DGNS_E_VRSN_CD6|CLM_E_POA_IND_SW6|ICD_DGNS_E_CD7|ICD_DGNS_E_VRSN_CD7|CLM_E_POA_IND_SW7|ICD_DGNS_E_CD8|ICD_DGNS_E_VRSN_CD8|CLM_E_POA_IND_SW8|ICD_DGNS_E_CD9|ICD_DGNS_E_VRSN_CD9|CLM_E_POA_IND_SW9|ICD_DGNS_E_CD10|ICD_DGNS_E_VRSN_CD10|CLM_E_POA_IND_SW10|ICD_DGNS_E_CD11|ICD_DGNS_E_VRSN_CD11|CLM_E_POA_IND_SW11|ICD_DGNS_E_CD12|ICD_DGNS_E_VRSN_CD12|CLM_E_POA_IND_SW12|ICD_PRCDR_CD1|ICD_PRCDR_VRSN_CD1|PRCDR_DT1|ICD_PRCDR_CD2|ICD_PRCDR_VRSN_CD2|PRCDR_DT2|ICD_PRCDR_CD3|ICD_PRCDR_VRSN_CD3|PRCDR_DT3|ICD_PRCDR_CD4|ICD_PRCDR_VRSN_CD4|PRCDR_DT4|ICD_PRCDR_CD5|ICD_PRCDR_VRSN_CD5|PRCDR_DT5|ICD_PRCDR_CD6|ICD_PRCDR_VRSN_CD6|PRCDR_DT6|ICD_PRCDR_CD7|ICD_PRCDR_VRSN_CD7|PRCDR_DT7|ICD_PRCDR_CD8|ICD_PRCDR_VRSN_CD8|PRCDR_DT8|ICD_PRCDR_CD9|ICD_PRCDR_VRSN_CD9|PRCDR_DT9|ICD_PRCDR_CD10|ICD_PRCDR_VRSN_CD10|PRCDR_DT10|ICD_PRCDR_CD11|ICD_PRCDR_VRSN_CD11|PRCDR_DT11|ICD_PRCDR_CD12|ICD_PRCDR_VRSN_CD12|PRCDR_DT12|ICD_PRCDR_CD13|ICD_PRCDR_VRSN_CD13|PRCDR_DT13|ICD_PRCDR_CD14|ICD_PRCDR_VRSN_CD14|PRCDR_DT14|ICD_PRCDR_CD15|ICD_PRCDR_VRSN_CD15|PRCDR_DT15|ICD_PRCDR_CD16|ICD_PRCDR_VRSN_CD16|PRCDR_DT16|ICD_PRCDR_CD17|ICD_PRCDR_VRSN_CD17|PRCDR_DT17|ICD_PRCDR_CD18|ICD_PRCDR_VRSN_CD18|PRCDR_DT18|ICD_PRCDR_CD19|ICD_PRCDR_VRSN_CD19|PRCDR_DT19|ICD_PRCDR_CD20|ICD_PRCDR_VRSN_CD20|PRCDR_DT20|ICD_PRCDR_CD21|ICD_PRCDR_VRSN_CD21|PRCDR_DT21|ICD_PRCDR_CD22|ICD_PRCDR_VRSN_CD22|PRCDR_DT22|ICD_PRCDR_CD23|ICD_PRCDR_VRSN_CD23|PRCDR_DT23|ICD_PRCDR_CD24|ICD_PRCDR_VRSN_CD24|PRCDR_DT24|ICD_PRCDR_CD25|ICD_PRCDR_VRSN_CD25|PRCDR_DT25|IME_OP_CLM_VAL_AMT|DSH_OP_CLM_VAL_AMT|CLM_UNCOMPD_CARE_PMT_AMT|FI_DOC_CLM_CNTL_NUM|FI_ORIG_CLM_CNTL_NUM|CLM_LINE_NUM|REV_CNTR|HCPCS_CD|REV_CNTR_UNIT_CNT|REV_CNTR_RATE_AMT|REV_CNTR_TOT_CHRG_AMT|REV_CNTR_NCVRD_CHRG_AMT|REV_CNTR_DDCTBL_COINSRNC_CD|REV_CNTR_NDC_QTY|REV_CNTR_NDC_QTY_QLFR_CD|RNDRNG_PHYSN_UPIN|RNDRNG_PHYSN_NPI";
        String sampleInpatientLine =
            "INSERT|567834|123456|900|F|V|60|15-JAN-2016|27-JAN-2016|26-FEB-2016|19-FEB-2016|3|777776|1|1|1|8299|A|7699.48|11.00|A|1|IA|0000000000||161999999||3333444555||161943433|0|51|2|84999.37|15-JAN-2016|1|4|A|10.00|112.00|5.00|6.00|4.00|33.00|14.00|646.23|552.56|0|25.09|68.58|0|0|1.2494|12|0|0|0|19|||||27-JAN-2016|695|0|23.99|A37|0|A40|0|A05|9|Y|A52|0|N|A05|9|N|A15|0|N|B01|0|N|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||A01|0|A01|0|N|A02|0|Y|||||||||||||||||||||||||||||||BQ0HZZZ|0|16-JAN-2016|CD1YYZZ|0|16-JAN-2016|2W52X6Z|0|15-JAN-2016|BP17ZZZ|0|17-JAN-2016|D9YD8ZZ|0|24-JAN-2016|F00ZCKZ|0|24-JAN-2016||||||||||||||||||||||||||||||||||||||||||||||||||||||||||66125.51|25|120.56|28486613848|261660474641024|1|6767|M55|0|0|84888.88|3699.00|A|77|GG||345345345|";

        // Uses ICD_DGNS_CD
        String sampleInpatientIcd9Line =
            setSubColumns(sampleInpatientLine, header, "ICD_DGNS_CD1", true, false);
        newClaimId = addLines(lines, newClaimId, icd9DiagnosisCodes, sampleInpatientIcd9Line);

        // Uses ICD_DGNS_CD
        String sampleInpatientIcd10Line =
            setSubColumns(sampleInpatientLine, header, "ICD_DGNS_CD1", false, true);
        newClaimId = addLines(lines, newClaimId, icd10DiagnosisCodes, sampleInpatientIcd10Line);

        // Inpatient and SNF have CLM_DRG_CD
        String sampleInpatientDrgLine = setSubColumns(sampleInpatientLine, header, "CLM_DRG_CD");
        newClaimId = addLines(lines, newClaimId, drgCodes, sampleInpatientDrgLine);

        // Uses HCPCS_CD
        String sampleInpatientCptLine = setSubColumns(sampleInpatientLine, header, "HCPCS_CD");
        newClaimId = addLines(lines, newClaimId, cptCodes, sampleInpatientCptLine);

        // Inpatient/outpatient/snf also check for ICD 9/10 procedure codes
        String sampleInpatientProc9Line =
            setSubColumns(sampleInpatientLine, header, "ICD_PRCDR_CD1", true, false);
        newClaimId = addLines(lines, newClaimId, icd9ProcedureCodes, sampleInpatientProc9Line);
        String sampleInpatientProc10Line =
            setSubColumns(sampleInpatientLine, header, "ICD_PRCDR_CD1", false, true);
        newClaimId = addLines(lines, newClaimId, icd10ProcedureCodes, sampleInpatientProc10Line);
        break;
      case OUTPATIENT:
        header =
            "DML_IND|BENE_ID|CLM_ID|CLM_GRP_ID|FINAL_ACTION|NCH_NEAR_LINE_REC_IDENT_CD|NCH_CLM_TYPE_CD|CLM_FROM_DT|CLM_THRU_DT|NCH_WKLY_PROC_DT|FI_CLM_PROC_DT|CLAIM_QUERY_CODE|PRVDR_NUM|CLM_FAC_TYPE_CD|CLM_SRVC_CLSFCTN_TYPE_CD|CLM_FREQ_CD|FI_NUM|CLM_MDCR_NON_PMT_RSN_CD|CLM_PMT_AMT|NCH_PRMRY_PYR_CLM_PD_AMT|NCH_PRMRY_PYR_CD|PRVDR_STATE_CD|ORG_NPI_NUM|AT_PHYSN_UPIN|AT_PHYSN_NPI|OP_PHYSN_UPIN|OP_PHYSN_NPI|OT_PHYSN_UPIN|OT_PHYSN_NPI|CLM_MCO_PD_SW|PTNT_DSCHRG_STUS_CD|CLM_TOT_CHRG_AMT|NCH_BENE_BLOOD_DDCTBL_LBLTY_AM|NCH_PROFNL_CMPNT_CHRG_AMT|PRNCPAL_DGNS_CD|PRNCPAL_DGNS_VRSN_CD|ICD_DGNS_CD1|ICD_DGNS_VRSN_CD1|ICD_DGNS_CD2|ICD_DGNS_VRSN_CD2|ICD_DGNS_CD3|ICD_DGNS_VRSN_CD3|ICD_DGNS_CD4|ICD_DGNS_VRSN_CD4|ICD_DGNS_CD5|ICD_DGNS_VRSN_CD5|ICD_DGNS_CD6|ICD_DGNS_VRSN_CD6|ICD_DGNS_CD7|ICD_DGNS_VRSN_CD7|ICD_DGNS_CD8|ICD_DGNS_VRSN_CD8|ICD_DGNS_CD9|ICD_DGNS_VRSN_CD9|ICD_DGNS_CD10|ICD_DGNS_VRSN_CD10|ICD_DGNS_CD11|ICD_DGNS_VRSN_CD11|ICD_DGNS_CD12|ICD_DGNS_VRSN_CD12|ICD_DGNS_CD13|ICD_DGNS_VRSN_CD13|ICD_DGNS_CD14|ICD_DGNS_VRSN_CD14|ICD_DGNS_CD15|ICD_DGNS_VRSN_CD15|ICD_DGNS_CD16|ICD_DGNS_VRSN_CD16|ICD_DGNS_CD17|ICD_DGNS_VRSN_CD17|ICD_DGNS_CD18|ICD_DGNS_VRSN_CD18|ICD_DGNS_CD19|ICD_DGNS_VRSN_CD19|ICD_DGNS_CD20|ICD_DGNS_VRSN_CD20|ICD_DGNS_CD21|ICD_DGNS_VRSN_CD21|ICD_DGNS_CD22|ICD_DGNS_VRSN_CD22|ICD_DGNS_CD23|ICD_DGNS_VRSN_CD23|ICD_DGNS_CD24|ICD_DGNS_VRSN_CD24|ICD_DGNS_CD25|ICD_DGNS_VRSN_CD25|FST_DGNS_E_CD|FST_DGNS_E_VRSN_CD|ICD_DGNS_E_CD1|ICD_DGNS_E_VRSN_CD1|ICD_DGNS_E_CD2|ICD_DGNS_E_VRSN_CD2|ICD_DGNS_E_CD3|ICD_DGNS_E_VRSN_CD3|ICD_DGNS_E_CD4|ICD_DGNS_E_VRSN_CD4|ICD_DGNS_E_CD5|ICD_DGNS_E_VRSN_CD5|ICD_DGNS_E_CD6|ICD_DGNS_E_VRSN_CD6|ICD_DGNS_E_CD7|ICD_DGNS_E_VRSN_CD7|ICD_DGNS_E_CD8|ICD_DGNS_E_VRSN_CD8|ICD_DGNS_E_CD9|ICD_DGNS_E_VRSN_CD9|ICD_DGNS_E_CD10|ICD_DGNS_E_VRSN_CD10|ICD_DGNS_E_CD11|ICD_DGNS_E_VRSN_CD11|ICD_DGNS_E_CD12|ICD_DGNS_E_VRSN_CD12|ICD_PRCDR_CD1|ICD_PRCDR_VRSN_CD1|PRCDR_DT1|ICD_PRCDR_CD2|ICD_PRCDR_VRSN_CD2|PRCDR_DT2|ICD_PRCDR_CD3|ICD_PRCDR_VRSN_CD3|PRCDR_DT3|ICD_PRCDR_CD4|ICD_PRCDR_VRSN_CD4|PRCDR_DT4|ICD_PRCDR_CD5|ICD_PRCDR_VRSN_CD5|PRCDR_DT5|ICD_PRCDR_CD6|ICD_PRCDR_VRSN_CD6|PRCDR_DT6|ICD_PRCDR_CD7|ICD_PRCDR_VRSN_CD7|PRCDR_DT7|ICD_PRCDR_CD8|ICD_PRCDR_VRSN_CD8|PRCDR_DT8|ICD_PRCDR_CD9|ICD_PRCDR_VRSN_CD9|PRCDR_DT9|ICD_PRCDR_CD10|ICD_PRCDR_VRSN_CD10|PRCDR_DT10|ICD_PRCDR_CD11|ICD_PRCDR_VRSN_CD11|PRCDR_DT11|ICD_PRCDR_CD12|ICD_PRCDR_VRSN_CD12|PRCDR_DT12|ICD_PRCDR_CD13|ICD_PRCDR_VRSN_CD13|PRCDR_DT13|ICD_PRCDR_CD14|ICD_PRCDR_VRSN_CD14|PRCDR_DT14|ICD_PRCDR_CD15|ICD_PRCDR_VRSN_CD15|PRCDR_DT15|ICD_PRCDR_CD16|ICD_PRCDR_VRSN_CD16|PRCDR_DT16|ICD_PRCDR_CD17|ICD_PRCDR_VRSN_CD17|PRCDR_DT17|ICD_PRCDR_CD18|ICD_PRCDR_VRSN_CD18|PRCDR_DT18|ICD_PRCDR_CD19|ICD_PRCDR_VRSN_CD19|PRCDR_DT19|ICD_PRCDR_CD20|ICD_PRCDR_VRSN_CD20|PRCDR_DT20|ICD_PRCDR_CD21|ICD_PRCDR_VRSN_CD21|PRCDR_DT21|ICD_PRCDR_CD22|ICD_PRCDR_VRSN_CD22|PRCDR_DT22|ICD_PRCDR_CD23|ICD_PRCDR_VRSN_CD23|PRCDR_DT23|ICD_PRCDR_CD24|ICD_PRCDR_VRSN_CD24|PRCDR_DT24|ICD_PRCDR_CD25|ICD_PRCDR_VRSN_CD25|PRCDR_DT25|RSN_VISIT_CD1|RSN_VISIT_VRSN_CD1|RSN_VISIT_CD2|RSN_VISIT_VRSN_CD2|RSN_VISIT_CD3|RSN_VISIT_VRSN_CD3|NCH_BENE_PTB_DDCTBL_AMT|NCH_BENE_PTB_COINSRNC_AMT|CLM_OP_PRVDR_PMT_AMT|CLM_OP_BENE_PMT_AMT|FI_DOC_CLM_CNTL_NUM|FI_ORIG_CLM_CNTL_NUM|CLM_LINE_NUM|REV_CNTR|REV_CNTR_DT|REV_CNTR_1ST_ANSI_CD|REV_CNTR_2ND_ANSI_CD|REV_CNTR_3RD_ANSI_CD|REV_CNTR_4TH_ANSI_CD|REV_CNTR_APC_HIPPS_CD|HCPCS_CD|HCPCS_1ST_MDFR_CD|HCPCS_2ND_MDFR_CD|REV_CNTR_PMT_MTHD_IND_CD|REV_CNTR_DSCNT_IND_CD|REV_CNTR_PACKG_IND_CD|REV_CNTR_OTAF_PMT_CD|REV_CNTR_IDE_NDC_UPC_NUM|REV_CNTR_UNIT_CNT|REV_CNTR_RATE_AMT|REV_CNTR_BLOOD_DDCTBL_AMT|REV_CNTR_CASH_DDCTBL_AMT|REV_CNTR_COINSRNC_WGE_ADJSTD_C|REV_CNTR_RDCD_COINSRNC_AMT|REV_CNTR_1ST_MSP_PD_AMT|REV_CNTR_2ND_MSP_PD_AMT|REV_CNTR_PRVDR_PMT_AMT|REV_CNTR_BENE_PMT_AMT|REV_CNTR_PTNT_RSPNSBLTY_PMT|REV_CNTR_PMT_AMT_AMT|REV_CNTR_TOT_CHRG_AMT|REV_CNTR_NCVRD_CHRG_AMT|REV_CNTR_STUS_IND_CD|REV_CNTR_NDC_QTY|REV_CNTR_NDC_QTY_QLFR_CD|RNDRNG_PHYSN_UPIN|RNDRNG_PHYSN_NPI";
        String sampleOutpatientLine =
            "INSERT|567834|123456|900|F|W|40|24-JAN-2011|24-JAN-2011|26-FEB-2011|18-FEB-2011|3|999999|1|3|1|15444|A|693.11|11.00|A|KY|0000000000||2222222222||3333333333||4444|0|1|8888.85|6.00|66.89|A40|0|29189|9|A52|0|||||||||||||||||||||||||||||||||||||||||||||||A05|9|A06|0|A15|0|||||||||||||||||||||CD1YYZZ|0|16-JAN-2016|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||A37|0|||||112.00|175.73|693.92|44.00|32490593716374487|373273882012|25|1|03-JAN-1942|CO120|CR121|||0|M99|XX||2||||000000000|111|5|10.45|12.89|15.23|11.00|0|0|200|300|500|5000.00|999.85|134.00| A|77|GG||345345345|";

        // Uses ICD_DGNS_CD
        String sampleOutpatientIcd9Line =
            setSubColumns(sampleOutpatientLine, header, "ICD_DGNS_CD1", true, false);
        newClaimId = addLines(lines, newClaimId, icd9DiagnosisCodes, sampleOutpatientIcd9Line);

        // Uses ICD_DGNS_CD
        String sampleOutpatientIcd10Line =
            setSubColumns(sampleOutpatientLine, header, "ICD_DGNS_CD1", false, true);
        newClaimId = addLines(lines, newClaimId, icd10DiagnosisCodes, sampleOutpatientIcd10Line);

        // Uses HCPCS_CD
        String sampleOutpatientCptLine = setSubColumns(sampleOutpatientLine, header, "HCPCS_CD");
        newClaimId = addLines(lines, newClaimId, cptCodes, sampleOutpatientCptLine);

        // Inpatient/outpatient/snf also check for ICD 9/10 procedure codes
        String sampleOutpatientProc9Line =
            setSubColumns(sampleOutpatientLine, header, "ICD_PRCDR_CD1", true, false);
        newClaimId = addLines(lines, newClaimId, icd9ProcedureCodes, sampleOutpatientProc9Line);
        String sampleOutpatientProc10Line =
            setSubColumns(sampleOutpatientLine, header, "ICD_PRCDR_CD1", false, true);
        newClaimId = addLines(lines, newClaimId, icd10ProcedureCodes, sampleOutpatientProc10Line);
        break;
      case SNF:
        header =
            "DML_IND|BENE_ID|CLM_ID|CLM_GRP_ID|FINAL_ACTION|NCH_NEAR_LINE_REC_IDENT_CD|NCH_CLM_TYPE_CD|CLM_FROM_DT|CLM_THRU_DT|NCH_WKLY_PROC_DT|FI_CLM_PROC_DT|CLAIM_QUERY_CODE|PRVDR_NUM|CLM_FAC_TYPE_CD|CLM_SRVC_CLSFCTN_TYPE_CD|CLM_FREQ_CD|FI_NUM|CLM_MDCR_NON_PMT_RSN_CD|CLM_PMT_AMT|NCH_PRMRY_PYR_CLM_PD_AMT|NCH_PRMRY_PYR_CD|FI_CLM_ACTN_CD|PRVDR_STATE_CD|ORG_NPI_NUM|AT_PHYSN_UPIN|AT_PHYSN_NPI|OP_PHYSN_UPIN|OP_PHYSN_NPI|OT_PHYSN_UPIN|OT_PHYSN_NPI|CLM_MCO_PD_SW|PTNT_DSCHRG_STUS_CD|CLM_PPS_IND_CD|CLM_TOT_CHRG_AMT|CLM_ADMSN_DT|CLM_IP_ADMSN_TYPE_CD|CLM_SRC_IP_ADMSN_CD|NCH_PTNT_STATUS_IND_CD|NCH_BENE_IP_DDCTBL_AMT|NCH_BENE_PTA_COINSRNC_LBLTY_AM|NCH_BENE_BLOOD_DDCTBL_LBLTY_AM|NCH_IP_NCVRD_CHRG_AMT|NCH_IP_TOT_DDCTN_AMT|CLM_PPS_CPTL_FSP_AMT|CLM_PPS_CPTL_OUTLIER_AMT|CLM_PPS_CPTL_DSPRPRTNT_SHR_AMT|CLM_PPS_CPTL_IME_AMT|CLM_PPS_CPTL_EXCPTN_AMT|CLM_PPS_OLD_CPTL_HLD_HRMLS_AMT|CLM_UTLZTN_DAY_CNT|BENE_TOT_COINSRNC_DAYS_CNT|CLM_NON_UTLZTN_DAYS_CNT|NCH_BLOOD_PNTS_FRNSHD_QTY|NCH_QLFYD_STAY_FROM_DT|NCH_QLFYD_STAY_THRU_DT|NCH_VRFD_NCVRD_STAY_FROM_DT|NCH_VRFD_NCVRD_STAY_THRU_DT|NCH_ACTV_OR_CVRD_LVL_CARE_THRU|NCH_BENE_MDCR_BNFTS_EXHTD_DT_I|NCH_BENE_DSCHRG_DT|CLM_DRG_CD|ADMTG_DGNS_CD|ADMTG_DGNS_VRSN_CD|PRNCPAL_DGNS_CD|PRNCPAL_DGNS_VRSN_CD|ICD_DGNS_CD1|ICD_DGNS_VRSN_CD1|ICD_DGNS_CD2|ICD_DGNS_VRSN_CD2|ICD_DGNS_CD3|ICD_DGNS_VRSN_CD3|ICD_DGNS_CD4|ICD_DGNS_VRSN_CD4|ICD_DGNS_CD5|ICD_DGNS_VRSN_CD5|ICD_DGNS_CD6|ICD_DGNS_VRSN_CD6|ICD_DGNS_CD7|ICD_DGNS_VRSN_CD7|ICD_DGNS_CD8|ICD_DGNS_VRSN_CD8|ICD_DGNS_CD9|ICD_DGNS_VRSN_CD9|ICD_DGNS_CD10|ICD_DGNS_VRSN_CD10|ICD_DGNS_CD11|ICD_DGNS_VRSN_CD11|ICD_DGNS_CD12|ICD_DGNS_VRSN_CD12|ICD_DGNS_CD13|ICD_DGNS_VRSN_CD13|ICD_DGNS_CD14|ICD_DGNS_VRSN_CD14|ICD_DGNS_CD15|ICD_DGNS_VRSN_CD15|ICD_DGNS_CD16|ICD_DGNS_VRSN_CD16|ICD_DGNS_CD17|ICD_DGNS_VRSN_CD17|ICD_DGNS_CD18|ICD_DGNS_VRSN_CD18|ICD_DGNS_CD19|ICD_DGNS_VRSN_CD19|ICD_DGNS_CD20|ICD_DGNS_VRSN_CD20|ICD_DGNS_CD21|ICD_DGNS_VRSN_CD21|ICD_DGNS_CD22|ICD_DGNS_VRSN_CD22|ICD_DGNS_CD23|ICD_DGNS_VRSN_CD23|ICD_DGNS_CD24|ICD_DGNS_VRSN_CD24|ICD_DGNS_CD25|ICD_DGNS_VRSN_CD25|FST_DGNS_E_CD|FST_DGNS_E_VRSN_CD|ICD_DGNS_E_CD1|ICD_DGNS_E_VRSN_CD1|ICD_DGNS_E_CD2|ICD_DGNS_E_VRSN_CD2|ICD_DGNS_E_CD3|ICD_DGNS_E_VRSN_CD3|ICD_DGNS_E_CD4|ICD_DGNS_E_VRSN_CD4|ICD_DGNS_E_CD5|ICD_DGNS_E_VRSN_CD5|ICD_DGNS_E_CD6|ICD_DGNS_E_VRSN_CD6|ICD_DGNS_E_CD7|ICD_DGNS_E_VRSN_CD7|ICD_DGNS_E_CD8|ICD_DGNS_E_VRSN_CD8|ICD_DGNS_E_CD9|ICD_DGNS_E_VRSN_CD9|ICD_DGNS_E_CD10|ICD_DGNS_E_VRSN_CD10|ICD_DGNS_E_CD11|ICD_DGNS_E_VRSN_CD11|ICD_DGNS_E_CD12|ICD_DGNS_E_VRSN_CD12|ICD_PRCDR_CD1|ICD_PRCDR_VRSN_CD1|PRCDR_DT1|ICD_PRCDR_CD2|ICD_PRCDR_VRSN_CD2|PRCDR_DT2|ICD_PRCDR_CD3|ICD_PRCDR_VRSN_CD3|PRCDR_DT3|ICD_PRCDR_CD4|ICD_PRCDR_VRSN_CD4|PRCDR_DT4|ICD_PRCDR_CD5|ICD_PRCDR_VRSN_CD5|PRCDR_DT5|ICD_PRCDR_CD6|ICD_PRCDR_VRSN_CD6|PRCDR_DT6|ICD_PRCDR_CD7|ICD_PRCDR_VRSN_CD7|PRCDR_DT7|ICD_PRCDR_CD8|ICD_PRCDR_VRSN_CD8|PRCDR_DT8|ICD_PRCDR_CD9|ICD_PRCDR_VRSN_CD9|PRCDR_DT9|ICD_PRCDR_CD10|ICD_PRCDR_VRSN_CD10|PRCDR_DT10|ICD_PRCDR_CD11|ICD_PRCDR_VRSN_CD11|PRCDR_DT11|ICD_PRCDR_CD12|ICD_PRCDR_VRSN_CD12|PRCDR_DT12|ICD_PRCDR_CD13|ICD_PRCDR_VRSN_CD13|PRCDR_DT13|ICD_PRCDR_CD14|ICD_PRCDR_VRSN_CD14|PRCDR_DT14|ICD_PRCDR_CD15|ICD_PRCDR_VRSN_CD15|PRCDR_DT15|ICD_PRCDR_CD16|ICD_PRCDR_VRSN_CD16|PRCDR_DT16|ICD_PRCDR_CD17|ICD_PRCDR_VRSN_CD17|PRCDR_DT17|ICD_PRCDR_CD18|ICD_PRCDR_VRSN_CD18|PRCDR_DT18|ICD_PRCDR_CD19|ICD_PRCDR_VRSN_CD19|PRCDR_DT19|ICD_PRCDR_CD20|ICD_PRCDR_VRSN_CD20|PRCDR_DT20|ICD_PRCDR_CD21|ICD_PRCDR_VRSN_CD21|PRCDR_DT21|ICD_PRCDR_CD22|ICD_PRCDR_VRSN_CD22|PRCDR_DT22|ICD_PRCDR_CD23|ICD_PRCDR_VRSN_CD23|PRCDR_DT23|ICD_PRCDR_CD24|ICD_PRCDR_VRSN_CD24|PRCDR_DT24|ICD_PRCDR_CD25|ICD_PRCDR_VRSN_CD25|PRCDR_DT25|FI_DOC_CLM_CNTL_NUM|FI_ORIG_CLM_CNTL_NUM|CLM_LINE_NUM|REV_CNTR|HCPCS_CD|REV_CNTR_UNIT_CNT|REV_CNTR_RATE_AMT|REV_CNTR_TOT_CHRG_AMT|REV_CNTR_NCVRD_CHRG_AMT|REV_CNTR_DDCTBL_COINSRNC_CD|REV_CNTR_NDC_QTY|REV_CNTR_NDC_QTY_QLFR_CD|RNDRNG_PHYSN_UPIN|RNDRNG_PHYSN_NPI";
        String sampleSnfLine =
            "INSERT|567834|123456|900|F|V|20|01-DEC-2013|18-DEC-2013|14-FEB-2014|07-FEB-2014|3|299999|2|1|1|11111|B|3333.33|11.00|A|1|FL|0000000000||2222222222||3333333333||4444444444|0|1|2|5555.03|05-NOV-2013|3|4|A|112.00|5.00|6.00|33.00|14.00|9.00|8.00|7.00|6.00|5.00|4.00|17|17|0|19|23-SEP-2013|05-NOV-2013|11-JAN-2002|21-JAN-2002||31-JAN-2002|18-DEC-2013|645|A05|9|A05|9|A05|9|A05|9|||||||||||||||||||||||||||||||||||A05||||||||||||E9281|9|E9281|9|3310|9|||||||||||||||||||||9214|9|16-JAN-2016|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||23443453453|34534535535|1|22|MMM|477|5.00|95.00|88.00|A|234.567|ML||345345345|";

        // Uses ICD_DGNS_CD
        String sampleSnfIcd9Line =
            setSubColumns(sampleSnfLine, header, "ICD_DGNS_CD1", true, false);
        ;
        newClaimId = addLines(lines, newClaimId, icd9DiagnosisCodes, sampleSnfIcd9Line);

        // Uses ICD_DGNS_CD
        String sampleSnfIcd10Line =
            setSubColumns(sampleSnfLine, header, "ICD_DGNS_CD1", false, true);
        newClaimId = addLines(lines, newClaimId, icd10DiagnosisCodes, sampleSnfIcd10Line);

        // Inpatient and SNF have CLM_DRG_CD
        String sampleSnfDrgLine = setSubColumns(sampleSnfLine, header, "CLM_DRG_CD");
        newClaimId = addLines(lines, newClaimId, drgCodes, sampleSnfDrgLine);

        // Uses HCPCS_CD
        String sampleSnfCptLine = setSubColumns(sampleSnfLine, header, "HCPCS_CD");
        newClaimId = addLines(lines, newClaimId, cptCodes, sampleSnfCptLine);

        // Inpatient/outpatient/snf also check for ICD 9/10 procedure codes
        String sampleSnfProc9Line =
            setSubColumns(sampleSnfLine, header, "ICD_PRCDR_CD1", true, false);
        newClaimId = addLines(lines, newClaimId, icd9ProcedureCodes, sampleSnfProc9Line);
        String sampleSnfProc10Line =
            setSubColumns(sampleSnfLine, header, "ICD_PRCDR_CD1", false, true);
        newClaimId = addLines(lines, newClaimId, icd10ProcedureCodes, sampleSnfProc10Line);
        break;
      case PDE:
        throw new IllegalArgumentException(
            "PDE does not contain SAMHSA data; use the existing sample A PDE file");
      default:
        throw new IllegalStateException("Unexpected value: " + claimType);
    }

    String finalContents = header + "\n" + String.join("\n", lines);
    String fileName = "sample-a-" + claimType.toString().toLowerCase() + ".txt";

    ServerTestUtils.writeFile(finalContents, getSamhsaSamplesFilePath(fileName));
    return newClaimId;
  }

  /**
   * Sets the sample string to replace the specified column with %s for replacing later.
   *
   * @param sampleLine the sample line to replace a column of
   * @param header the header to find the column name's index
   * @param colString the column name to replace in the sample string
   * @return the adjusted line
   */
  private String setSubColumns(String sampleLine, String header, String colString) {
    return setSubColumns(sampleLine, header, colString, false, false);
  }

  /**
   * Sets the sample string to replace the specified column with %s for replacing later.
   *
   * @param sampleLine the sample line to replace a column of
   * @param header the header to find the column name's index
   * @param colString the column name to replace in the sample string
   * @param isIcd9 if the value is an ICD9 value, so we set the following column with the
   *     appropriate value
   * @param isIcd10 if the value is an ICD10 value, so we set the following column with the
   *     appropriate value
   * @return the adjusted line
   */
  private String setSubColumns(
      String sampleLine, String header, String colString, boolean isIcd9, boolean isIcd10) {

    // get the column index of the string provided
    int valueColumnIndex = getColNumberFor(header, colString);

    // Get the claim id column index
    int clmIdColumnIndex = getColNumberFor(header, "CLM_ID");

    // break the sample line into parts
    String[] sampleSplit = sampleLine.split("\\|");
    sampleSplit[valueColumnIndex] = "%s";
    // If its an icd9 or 10 value, the next column needs to be set to 9 or 0 to denote which
    if (isIcd9) {
      sampleSplit[valueColumnIndex + 1] = "9";
    } else if (isIcd10) {
      sampleSplit[valueColumnIndex + 1] = "0";
    }
    sampleSplit[clmIdColumnIndex] = "%s";

    return String.join("|", sampleSplit);
  }

  /**
   * Gets the first column number from a pipe-delimited string that matches the entry name.
   *
   * @param fullString the full pipe-delimited string to find a column from
   * @param entryName the column value to find
   * @return the index of the entryName
   */
  private int getColNumberFor(String fullString, String entryName) {
    String[] fullStringSplit = fullString.split("\\|");
    for (int i = 0; i < fullStringSplit.length; i++) {
      if (fullStringSplit[i].equals(entryName)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Adds lines to the existing line array (representing the lines in the claim file).
   *
   * @param linesToAddTo the existing line array to add to
   * @param claimId the claimId start point
   * @param codes the list of codes to add to the list
   * @param sampleLine the sample line to copy from for each line (will replace the claimId and code
   *     location)
   * @return the updated claim id after adding all lines
   */
  private int addLines(
      List<String> linesToAddTo, int claimId, List<String> codes, String sampleLine) {

    int newClaimId = claimId;

    for (String code : codes) {
      // Replace the claim id and the samhsa code
      String newLine = String.format(sampleLine, "999183" + newClaimId, code);
      linesToAddTo.add(newLine);
      newClaimId++;
    }

    return newClaimId;
  }

  /**
   * Gets the SAMHSA samples file path.
   *
   * @param filename the filename to get a path for
   * @return the Path to the sample SAMHSA file
   */
  private Path getSamhsaSamplesFilePath(String filename) {
    Path samhsaFilesDir =
        Paths.get(
            "../../bfd-model/bfd-model-rif-samples",
            "src",
            "main",
            "resources",
            "rif-static-samples",
            "samhsa");
    if (!Files.isDirectory(samhsaFilesDir)) {
      throw new IllegalStateException(
          "Could not find samhsa files directory to recreate samhsa sample files");
    }

    return Paths.get(samhsaFilesDir.toString(), filename);
  }
}
