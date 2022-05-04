package gov.cms.bfd.model.codegen.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Can be applied to a Java package to indicate that RIF processing code for a particular set of
 * layouts should be generated there.
 */
@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.CLASS)
public @interface RifLayoutsGenerator {
  /**
   * Gets the file name of the spreadsheet (in this package) that details the RIF layouts to
   * generate code for.
   *
   * @return the file name
   */
  String spreadsheetResource();

  /**
   * Gets the name of the sheet in the Excel file that contains the RIF layout for beneficiary data.
   *
   * @return the beneficiary sheet name
   */
  String beneficiarySheet();

  /**
   * Gets the name of the sheet in the Excel file that contains the RIF layout for beneficiary
   * history data.
   *
   * @return the beneficiary history sheet name
   */
  String beneficiaryHistorySheet();

  /**
   * Gets the name of the sheet in the Excel file that contains the RIF layout for medicare
   * beneficiary id data.
   *
   * @return the medicare beneficiary sheet name
   */
  String medicareBeneficiaryIdSheet();

  /**
   * Gets the name of the sheet in the Excel file that contains the RIF layout for PDE claims data.
   *
   * @return the PDE claims data sheet name
   */
  String pdeSheet();

  /**
   * Gets the name of the sheet in the Excel file that contains the RIF layout for carrier claims
   * data.
   *
   * @return the carrier claims data sheet name
   */
  String carrierSheet();

  /**
   * Gets the name of the sheet in the Excel file that contains the RIF layout for inpatient claims
   * data.
   *
   * @return the inpatient claims data sheet name
   */
  String inpatientSheet();

  /**
   * Gets the name of the sheet in the Excel file that contains the RIF layout for outpatient claims
   * data.
   *
   * @return the outpatient claims data sheet name
   */
  String outpatientSheet();

  /**
   * Gets the name of the sheet in the Excel file that contains the RIF layout for HHA claims data.
   *
   * @return the HHA claims data sheet name
   */
  String hhaSheet();

  /**
   * Gets the name of the sheet in the Excel file that contains the RIF layout for DME claims data.
   *
   * @return the DME claims data sheet name
   */
  String dmeSheet();

  /**
   * Gets the name of the sheet in the Excel file that contains the RIF layout for hospice claims
   * data.
   *
   * @return the hospice claims data sheet name
   */
  String hospiceSheet();

  /**
   * Gets the name of the sheet in the Excel file that contains the RIF layout for SNF claims data.
   *
   * @return the SNF claims data sheet name
   */
  String snfSheet();
}
