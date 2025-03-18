package gov.cms.bfd.pipeline.bridge.model;

import gov.cms.bfd.model.rif.entities.BeneficiaryColumn;
import gov.cms.bfd.pipeline.bridge.etl.Parser;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Simple class for storing commonly used beneficiary lookup data for use with transformation logic.
 */
@Getter
@RequiredArgsConstructor
public class BeneficiaryData {

  /** MBI Number. */
  public static final String MBI_NUM = BeneficiaryColumn.MBI_NUM.name();

  /** Beneficiary ID. */
  public static final String BENE_ID = BeneficiaryColumn.BENE_ID.name();

  /** Beneficiary Hic Number. */
  public static final String BENE_CRNT_HIC_NUM = BeneficiaryColumn.BENE_CRNT_HIC_NUM.name();

  /** Beneficiary Sur Name. */
  public static final String BENE_SRNM_NAME = BeneficiaryColumn.BENE_SRNM_NAME.name();

  /** Beneficiary Given Name. */
  public static final String BENE_GVN_NAME = BeneficiaryColumn.BENE_GVN_NAME.name();

  /** Beneficiary Middle Name. */
  public static final String BENE_MDL_NAME = BeneficiaryColumn.BENE_MDL_NAME.name();

  /** Beneficiary Birth Date. */
  public static final String BENE_BIRTH_DT = BeneficiaryColumn.BENE_BIRTH_DT.name();

  /** Neneficiary Sex Identification Code. */
  public static final String BENE_SEX_IDENT_CD = BeneficiaryColumn.BENE_SEX_IDENT_CD.name();

  /** Benenficiary ID. */
  private final String beneId;

  /** MBI Number. */
  private final String mbi;

  /** HICN Number. */
  private final String hicNo;

  /** Beneficiary First Name. */
  private final String firstName;

  /** Beneficiary Last Name. */
  private final String lastName;

  /** Beneficiary Middle Name. */
  private final String midName;

  /** Beneficiary Date of Birth. */
  private final String dob;

  /** Beneficiary sex. */
  private final String sex;

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
    String sex = data.get(BENE_SEX_IDENT_CD).orElse(null);

    return new BeneficiaryData(beneId, mbi, hicNo, firstName, lastName, midName, dob, sex);
  }
}
