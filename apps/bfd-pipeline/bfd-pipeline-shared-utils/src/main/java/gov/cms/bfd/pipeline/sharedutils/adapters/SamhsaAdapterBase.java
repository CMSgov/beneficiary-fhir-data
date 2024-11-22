package gov.cms.bfd.pipeline.sharedutils.adapters;

import gov.cms.bfd.pipeline.sharedutils.model.SamhsaFields;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Base class for the SAMHSA Adapters.
 *
 * @param <TClaim> Claim type
 * @param <TClaimLine> ClaimLine type.
 */
public abstract class SamhsaAdapterBase<TClaim, TClaimLine> {
  /** The Claim to process. */
  TClaim claim;

  /** The claimLines to process. */
  List<TClaimLine> claimLines;

  /** The claim's table. */
  String table;

  /** The claimLines' table. */
  String linesTable;

  /**
   * Retrieves a list of SAMHSA fields.
   *
   * @return {@link SamhsaFields}
   * @throws InvocationTargetException Thrown when the one of the claim's methods cannot be invoked.
   * @throws NoSuchMethodException Thrown when one of the claim's method does not exist.
   * @throws IllegalAccessException Thrown on illegal access invoking the claim's method.
   */
  public abstract List<SamhsaFields> getFields()
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException;

  /** The list of samhsa fields for this claims. */
  List<SamhsaFields> samhsaFields = new ArrayList<>();

  /** The class for the claim. */
  Class claimClass;

  /** The class for the claimLines. */
  Class claimLineClass;

  /**
   * Constructor.
   *
   * @param claim The claim.
   * @param claimLines The claimLines.
   */
  public SamhsaAdapterBase(TClaim claim, List<TClaimLine> claimLines) {
    this.claimLines = claimLines;
    this.claim = claim;
    this.claimClass = claim.getClass();
    if (claimLines != null && !claimLines.isEmpty()) {
      claimLineClass = claimLines.getFirst().getClass();
    }
  }

  /**
   * Checks for a SAMHSA code on a given field.
   *
   * @param method The method of the field to process.
   * @param column the column for the field.
   * @throws InvocationTargetException Thrown when the one of the claim's methods cannot be invoked.
   * @throws NoSuchMethodException Thrown when one of the claim's method does not exist.
   * @throws IllegalAccessException Thrown on illegal access invoking the claim's method.
   */
  void getCode(String method, String column)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Optional<String> code = (Optional<String>) claimClass.getMethod(method).invoke(claim);
    if (code.isPresent()) {
      SamhsaFields field =
          SamhsaFields.builder().column(column).code(code.get()).table(table).build();
      samhsaFields.add(field);
    }
  }

  /**
   * Checks for SAMHSA data in ICD Diagnosis Codes. Since all of these fields will be similarly
   * named, we can provide a pattern.
   *
   * @param count The number of methods.
   * @throws InvocationTargetException Thrown when the one of the claim's methods cannot be invoked.
   * @throws NoSuchMethodException Thrown when one of the claim's method does not exist.
   * @throws IllegalAccessException Thrown on illegal access invoking the claim's method.
   */
  void getIcdDiagnosisCodes(int count)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    String method = "getDiagnosis%dCode";
    getClaimLoopedNumberedCode(method, "icd_dgns_cd%d", count);
  }

  /**
   * Loops through a number of similarly named methods, distinguised by number.
   *
   * @param method The method pattern.
   * @param column The column pattern.
   * @param count The number of methods.
   * @throws InvocationTargetException Thrown when the one of the claim's methods cannot be invoked.
   * @throws NoSuchMethodException Thrown when one of the claim's method does not exist.
   * @throws IllegalAccessException Thrown on illegal access invoking the claim's method.
   */
  void getClaimLoopedNumberedCode(String method, String column, int count)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    for (int i = 1; i <= count; i++) {
      Optional<String> code =
          (Optional<String>) claimClass.getMethod(String.format(method, i)).invoke(claim);
      if (code.isPresent()) {
        SamhsaFields field =
            SamhsaFields.builder()
                .code(code.get())
                .column(String.format(column, i))
                .table(table)
                .build();
        samhsaFields.add(field);
      }
    }
  }

  /**
   * Loops through the claim lines for a given method.
   *
   * @param method The method for the field to process.
   * @param column the column for the field.
   * @throws InvocationTargetException Thrown when the one of the claim's methods cannot be invoked.
   * @throws NoSuchMethodException Thrown when one of the claim's method does not exist.
   * @throws IllegalAccessException Thrown on illegal access invoking the claim's method.
   */
  void claimLinesCode(String method, String column)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    if (claimLineClass == null) {
      return;
    }
    for (TClaimLine claimLine : claimLines) {
      Optional<String> code = (Optional<String>) claimLineClass.getMethod(method).invoke(claimLine);
      short lineNum = (short) claimLineClass.getMethod("getLineNumber").invoke(claimLine);
      if (code.isPresent()) {
        SamhsaFields field =
            SamhsaFields.builder()
                .column(column)
                .code(code.get())
                .lineNum(lineNum)
                .table(linesTable)
                .build();
        samhsaFields.add(field);
      }
    }
  }

  /**
   * Checks for SAMHSA data in Diagnosis Admitting Codes. Since all of these fields will be
   * similarly named, we can provide a pattern.
   *
   * @param count The number of methods.
   * @throws InvocationTargetException Thrown when the one of the claim's methods cannot be invoked.
   * @throws NoSuchMethodException Thrown when one of the claim's method does not exist.
   * @throws IllegalAccessException Thrown on illegal access invoking the claim's method.
   */
  void getDiagnosisAdmittingCodes(int count)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    getClaimLoopedNumberedCode("getDiagnosisAdmission%dCode", "rsn_visit_cd%d", count);
  }

  /**
   * Checks for SAMHSA data in Procedure Codes. Since all of these fields will be similarly named,
   * we can provide a pattern.
   *
   * @param count The number of methods.
   * @throws InvocationTargetException Thrown when the one of the claim's methods cannot be invoked.
   * @throws NoSuchMethodException Thrown when one of the claim's method does not exist.
   * @throws IllegalAccessException Thrown on illegal access invoking the claim's method.
   */
  void getProcedureCodes(int count)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    getClaimLoopedNumberedCode("getProcedure%dCode", "icd_prcdr_cd%d", count);
  }

  /**
   * Checks for SAMHSA data in the Principal Diagnosis.
   *
   * @throws InvocationTargetException Thrown when the one of the claim's methods cannot be invoked.
   * @throws NoSuchMethodException Thrown when one of the claim's method does not exist.
   * @throws IllegalAccessException Thrown on illegal access invoking the claim's method.
   */
  void getPrincipalDiagnosis()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    getCode("getDiagnosisPrincipalCode", "prncpal_dgns_cd");
  }

  /**
   * Checks for SAMHSA data in the external diagnosis first code.
   *
   * @throws InvocationTargetException Thrown when the one of the claim's methods cannot be invoked.
   * @throws NoSuchMethodException Thrown when one of the claim's method does not exist.
   * @throws IllegalAccessException Thrown on illegal access invoking the claim's method.
   */
  void getDiagnosisFirstCode()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    getCode("getDiagnosisExternalFirstCode", "fst_dgns_e_cd");
  }

  /**
   * Checks for SAMHSA data in diagnosis external codes. Since all of these fields will be similarly
   * named, we can provide a pattern.
   *
   * @param count The number of methods.
   * @throws InvocationTargetException Thrown when the one of the claim's methods cannot be invoked.
   * @throws NoSuchMethodException Thrown when one of the claim's method does not exist.
   * @throws IllegalAccessException Thrown on illegal access invoking the claim's method.
   */
  void getDiagnosisExternalCodes(int count)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    getClaimLoopedNumberedCode("getDiagnosisExternal%dCode", "icd_dgns_e_cd%d", count);
  }

  /**
   * Checks for SAMHSA data in the HCPCS code.
   *
   * @throws InvocationTargetException Thrown when the one of the claim's methods cannot be invoked.
   * @throws NoSuchMethodException Thrown when one of the claim's method does not exist.
   * @throws IllegalAccessException Thrown on illegal access invoking the claim's method.
   */
  void getHcpcsCode()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    claimLinesCode("getHcpcsCode", "rev_cntr_apc_hipps_cd");
  }

  /**
   * Checks for SAMHSA data in the apc hipps code. This would normally not hold SAMHSA data, but it
   * is checked for accidental entries.
   *
   * @throws InvocationTargetException Thrown when the one of the claim's methods cannot be invoked.
   * @throws NoSuchMethodException Thrown when one of the claim's method does not exist.
   * @throws IllegalAccessException Thrown on illegal access invoking the claim's method.
   */
  void getApcOrHippsCode()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    claimLinesCode("getApcOrHippsCode", "rev_cntr_apc_hipps_cd");
  }

  /**
   * Checks for SAMHSA data in the line's diagnosis code.
   *
   * @throws InvocationTargetException Thrown when the one of the claim's methods cannot be invoked.
   * @throws NoSuchMethodException Thrown when one of the claim's method does not exist.
   * @throws IllegalAccessException Thrown on illegal access invoking the claim's method.
   */
  void getDiagnosisCode()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    claimLinesCode("getDiagnosisCode", "line_icd_dgns_cd");
  }

  /**
   * Checks for SAMHSA data in the claim's DRG code.
   *
   * @throws InvocationTargetException Thrown when the one of the claim's methods cannot be invoked.
   * @throws NoSuchMethodException Thrown when one of the claim's method does not exist.
   * @throws IllegalAccessException Thrown on illegal access invoking the claim's method.
   */
  void getClaimDRGCode()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    getCode("getDiagnosisRelatedGroupCd", "clm_drg_cd");
  }

  /**
   * Checks for SAMHSA data in the line's diagnosis admitting code.
   *
   * @throws InvocationTargetException Thrown when the one of the claim's methods cannot be invoked.
   * @throws NoSuchMethodException Thrown when one of the claim's method does not exist.
   * @throws IllegalAccessException Thrown on illegal access invoking the claim's method.
   */
  void getDiagnosisAdmittingCode()
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    getCode("getDiagnosisAdmittingCode", "prncpal_dgns_cd");
  }

  /**
   * Gets the from date of a claim.
   *
   * @throws InvocationTargetException Thrown when the one of the claim's methods cannot be invoked.
   * @throws NoSuchMethodException Thrown when one of the claim's method does not exist.
   * @throws IllegalAccessException Thrown on illegal access invoking the claim's method.
   * @return a LocalDate object of the claim's from date.
   */
  public LocalDate getFromDate()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    LocalDate date = (LocalDate) claimClass.getMethod("getDateFrom").invoke(claim);
    return date == null ? LocalDate.MAX : date;
  }

  /**
   * Gets the to date of a claim.
   *
   * @throws InvocationTargetException Thrown when the one of the claim's methods cannot be invoked.
   * @throws NoSuchMethodException Thrown when one of the claim's method does not exist.
   * @throws IllegalAccessException Thrown on illegal access invoking the claim's method.
   * @return A LocalDate object of the claim's through date.
   */
  public LocalDate getThroughDate()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    LocalDate date = (LocalDate) claimClass.getMethod("getDateThrough").invoke(claim);
    return date == null ? LocalDate.MAX : date;
  }
}
