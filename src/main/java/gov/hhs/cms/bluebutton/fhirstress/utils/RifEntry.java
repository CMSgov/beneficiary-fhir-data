package gov.hhs.cms.bluebutton.fhirstress.utils;

import com.opencsv.bean.CsvBindByName;

public class RifEntry 
{
  @CsvBindByName
  public String DML_IND;
  @CsvBindByName
  public String BENE_ID;
  @CsvBindByName
  public String STATE_CODE;
  @CsvBindByName
  public String BENE_COUNTY_CD;
  @CsvBindByName
  public String BENE_ZIP_CD;
  @CsvBindByName
  public String BENE_BIRTH_DT;
  @CsvBindByName
  public String BENE_SEX_IDENT_CD;
  @CsvBindByName
  public String BENE_RACE_CD;
  @CsvBindByName
  public int BENE_ENTLMT_RSN_ORIG;
  @CsvBindByName
  public int BENE_ENTLMT_RSN_CURR;
  @CsvBindByName
  public String BENE_ESRD_IND;
  @CsvBindByName
  public int BENE_MDCR_STATUS_CD;
  @CsvBindByName
  public int BENE_PTA_TRMNTN_CD;
  @CsvBindByName
  public int BENE_PTB_TRMNTN_CD;
  @CsvBindByName
  public String BENE_CRNT_HIC_NUM;
  @CsvBindByName
  public String BENE_SRNM_NAME;
  @CsvBindByName
  public String BENE_GVN_NAME;
  @CsvBindByName
  public String BENE_MDL_NAME;

  public String toString() {
    String result = "DML_IND = " + DML_IND + ", ";
    result += "BENE_ID = " + BENE_ID + ", ";
    result += "STATE_CODE = " + STATE_CODE + ", ";
    result += "BENE_COUNTY_CD = " + BENE_COUNTY_CD + ", "; 
    result += "BENE_ZIP_CD = " + BENE_ZIP_CD + ", ";
    result += "BENE_BIRTH_DT = " + BENE_BIRTH_DT + ", ";
    result += "BENE_SEX_IDENT_CD = " + BENE_SEX_IDENT_CD + ", ";
    result += "BENE_RACE_CD = " + BENE_RACE_CD + ", ";
    result += "BENE_ENTLMT_RSN_ORIG = " + BENE_ENTLMT_RSN_ORIG + ", ";
    result += "BENE_ENTLMT_RSN_CURR = " + BENE_ENTLMT_RSN_CURR + ", ";
    result += "BENE_ESRD_IND = " + BENE_ESRD_IND + ", ";
    result += "BENE_MDCR_STATUS_CD = " + BENE_MDCR_STATUS_CD + ", ";
    result += "BENE_PTA_TRMNTN_CD = " + BENE_PTA_TRMNTN_CD + ", ";
    result += "BENE_PTB_TRMNTN_CD = " + BENE_PTB_TRMNTN_CD + ", ";
    result += "BENE_CRNT_HIC_NUM = " + BENE_CRNT_HIC_NUM + ", ";
    result += "BENE_SRNM_NAME = " + BENE_SRNM_NAME + ", ";
    result += "BENE_GVN_NAME = " + BENE_GVN_NAME + ", ";
    result += "BENE_MDL_NAME = " + BENE_MDL_NAME;
    return result;
  }
}

