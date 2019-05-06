package gov.hhs.cms.bluebutton.data.model.codegen.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Can be applied to a Java package to indicate that RIF processing code for a
 * particular set of layouts should be generated there.
 */
@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.CLASS)
public @interface RifLayoutsGenerator {
	/**
	 * @return the file name of the spreadsheet (in this package) that details
	 *         the RIF layouts to generate code for
	 */
	String spreadsheetResource();

	/**
	 * @return the name of the sheet in the Excel file that contains the RIF
	 *         layout for beneficiary data
	 */
	String beneficiarySheet();

	/**
	 * @return the name of the sheet in the Excel file that contains the RIF
	 *         layout for beneficiary history data
	 */
	String beneficiaryHistorySheet();

	/**
	 * @return the name of the sheet in the Excel file that contains the RIF layout
	 *         for beneficiary history data
	 */
	String beneficiaryHistoryTempSheet();

	/**
	 * @return the name of the sheet in the Excel file that contains the RIF layout
	 *         for medicare beneficiary id data
	 */
	String medicareBeneficiaryIdSheet();

	/**
	 * @return the name of the sheet in the Excel file that contains the RIF
	 *         layout for PDE claims data
	 */
	String pdeSheet();

	/**
	 * @return the name of the sheet in the Excel file that contains the RIF
	 *         layout for carrier claims data
	 */
	String carrierSheet();

	/**
	 * @return the name of the sheet in the Excel file that contains the RIF
	 *         layout for inpatient claims data
	 */
	String inpatientSheet();

	/**
	 * @return the name of the sheet in the Excel file that contains the RIF
	 *         layout for outpatient claims data
	 */
	String outpatientSheet();

	/**
	 * @return the name of the sheet in the Excel file that contains the RIF
	 *         layout for HHA claims data
	 */
	String hhaSheet();

	/**
	 * @return the name of the sheet in the Excel file that contains the RIF
	 *         layout for DME claims data
	 */
	String dmeSheet();

	/**
	 * @return the name of the sheet in the Excel file that contains the RIF
	 *         layout for hospice claims data
	 */
	String hospiceSheet();

	/**
	 * @return the name of the sheet in the Excel file that contains the RIF
	 *         layout for SNF claims data
	 */
	String snfSheet();
}
