package gov.cms.bfd.server.ng.claim.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Unit tests for ICD code formatting rules implemented in IcdIndicator. */
class IcdIndicatorTest {
  private static final String ICD10_F1010 = "F10.10";
  private static final String ICD9_123_45 = "123.45";
  private static final String ICD9_PROC_12_345 = "12.345";

  @Test
  void icd10FormatsDiagnosisCorrectly() {
    var formatted = IcdIndicator.ICD_10.formatCode("F1010");
    assertEquals(ICD10_F1010, formatted);

    var shortCode = IcdIndicator.ICD_10.formatCode("A12");
    assertEquals("A12", shortCode);
  }

  @Test
  void icd9NumericDiagnosisFormatting() {
    var formatted = IcdIndicator.ICD_9.formatCode("12345");
    assertEquals(ICD9_123_45, formatted);
  }

  @Test
  void icd9EDiagnosisFormatting() {
    var formatted = IcdIndicator.ICD_9.formatCode("E0000");
    assertEquals("E000.0", formatted);
  }

  @Test
  void icd9VDiagnosisFormatting() {
    var formatted = IcdIndicator.ICD_9.formatCode("V1234");
    assertEquals("V12.34", formatted);
  }

  @Test
  void icd9ProcedureFormatting() {
    var formatted = IcdIndicator.ICD_9.formatProcedureCode("12345");
    assertEquals(ICD9_PROC_12_345, formatted);

    var shortProc = IcdIndicator.ICD_9.formatProcedureCode("12");
    assertEquals("12", shortProc);
  }

  @Test
  void formattedCodesRemainUnchanged() {
    assertEquals(ICD9_123_45, IcdIndicator.ICD_9.formatCode(ICD9_123_45));
    assertEquals(ICD10_F1010, IcdIndicator.ICD_10.formatCode(ICD10_F1010));
    assertEquals(ICD9_PROC_12_345, IcdIndicator.ICD_9.formatProcedureCode(ICD9_PROC_12_345));
  }

  @Test
  void defaultIndicatorReturnsRawCode() {
    var raw = "ABC123";
    var formatted = IcdIndicator.DEFAULT.formatCode(raw);
    assertEquals(raw, formatted);
  }

  @Test
  void defaultIndicatorReturnsRawProcedureCode() {
    var rawProcedureCode = "XYZ789";
    var formatted = IcdIndicator.DEFAULT.formatProcedureCode(rawProcedureCode);
    assertEquals(rawProcedureCode, formatted);
  }

  @Test
  void icd9UnknownPatternReturnsRaw() {
    var raw = "ABC123";
    var formatted = IcdIndicator.ICD_9.formatCode(raw);
    assertEquals(raw, formatted);
  }
}
