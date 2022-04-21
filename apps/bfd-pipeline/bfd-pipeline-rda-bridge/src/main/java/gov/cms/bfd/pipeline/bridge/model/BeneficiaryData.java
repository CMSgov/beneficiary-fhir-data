package gov.cms.bfd.pipeline.bridge.model;

import gov.cms.bfd.model.rif.BeneficiaryColumn;
import gov.cms.bfd.pipeline.bridge.etl.Parser;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Simple class for storing commonly used beneficiary lookup data for use with transformation logic.
 */
@Getter
@RequiredArgsConstructor
public class BeneficiaryData {

  public static final String MBI_NUM = BeneficiaryColumn.MBI_NUM.name();
  public static final String BENE_ID = BeneficiaryColumn.BENE_ID.name();
  public static final String BENE_CRNT_HIC_NUM = BeneficiaryColumn.BENE_CRNT_HIC_NUM.name();

  public static final String BENE_SRNM_NAME = BeneficiaryColumn.BENE_SRNM_NAME.name();
  public static final String BENE_GVN_NAME = BeneficiaryColumn.BENE_GVN_NAME.name();
  public static final String BENE_MDL_NAME = BeneficiaryColumn.BENE_MDL_NAME.name();
  public static final String BENE_BIRTH_DT = BeneficiaryColumn.BENE_BIRTH_DT.name();
  public static final String BENE_SEX_IDENT_CD = BeneficiaryColumn.BENE_SEX_IDENT_CD.name();

  private final String beneId;
  private final String mbi;
  private final String hicNo;

  private final String firstName;
  private final String lastName;
  private final String midName;
  private final String dob;
  private final String gender;

  /**
   * Creates a single {@link BeneficiaryData} object from the given {@link Parser.Data}.
   *
   * @param data The extracted {@link Parser.Data}.
   * @return A {@link BeneficiaryData} built from the given {@link Parser.Data}.
   */
  public static BeneficiaryData fromData(Parser.Data<String> data) {
    String beneId = data.get(BENE_ID).orElse(null);
    String mbi = data.get(MBI_NUM).orElse(null);
    String hicNo = data.get(BENE_CRNT_HIC_NUM).orElse(null);
    String firstName = data.get(BENE_GVN_NAME).orElse(null);
    String lastName = data.get(BENE_SRNM_NAME).orElse(null);
    String midName = data.get(BENE_MDL_NAME).orElse(null);
    String dob = data.getFromType(BENE_BIRTH_DT, Parser.Data.Type.DATE).orElse(null);
    String gender = data.get(BENE_SEX_IDENT_CD).orElse(null);

    return new BeneficiaryData(beneId, mbi, hicNo, firstName, lastName, midName, dob, gender);
  }
}
