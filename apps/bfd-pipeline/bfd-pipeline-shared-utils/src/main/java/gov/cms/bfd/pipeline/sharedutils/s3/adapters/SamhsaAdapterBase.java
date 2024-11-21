package gov.cms.bfd.pipeline.sharedutils.s3.adapters;

import gov.cms.bfd.pipeline.sharedutils.model.SamhsaFields;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class SamhsaAdapterBase<TClaim, TClaimLine> {
    TClaim claim;
    List<TClaimLine> claimLines;
    String table;
    String linesTable;
    abstract public List<SamhsaFields> getFields() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException;

    List<SamhsaFields> samhsaFields = new ArrayList<>();
    Class claimClass;
    Class claimLineClass;
    public SamhsaAdapterBase(TClaim claim, List<TClaimLine> claimLines) {
        this.claimLines = claimLines;
        this.claim = claim;
        this.claimClass = claim.getClass();
        if (claimLines != null ) {
            claimLineClass = claimLines.getFirst().getClass();
        }
    }
    void getIcdDiagnosisCodes(int count) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        for (int i = 1; i <= count; i++) {
            Optional<String> code = (Optional<String>) claimClass.getMethod("getDiagnosis" + i + "Code").invoke(claim);
            if (code.isPresent()) {
                SamhsaFields field = SamhsaFields.builder()
                        .code(code.get())
                        .column("icd_dgns_cd" + i)
                        .build();
                samhsaFields.add(field);
            }
        }
    }
    void getDiagnosisAdmittingCodes(int count) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        for (int i = 1; i <= count; i++) {
            Optional<String> code = (Optional<String>) claimClass.getMethod("getDiagnosisAdmission" + i + "Code").invoke(claim);
            if (code.isPresent()) {
                SamhsaFields field = SamhsaFields.builder()
                        .code(code.get())
                        .column("rsn_visit_cd" + i)
                        .build();
                samhsaFields.add(field);
            }
        }
    }

    void getProcedureCodes(int count) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        for (int i = 1; i <= count; i++) {
            Optional<String> code = (Optional<String>) claimClass.getMethod("getProcedure" + i + "Code").invoke(claim);
            if (code.isPresent()) {
                SamhsaFields field = SamhsaFields.builder()
                        .code(code.get())
                        .column("icd_prcdr_cd" + i)
                        .build();
                samhsaFields.add(field);
            }
        }
    }

    void getPrincipalDiagnosis() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        getCode("getDiagnosisPrincipalCode","prncpal_dgns_cd");
    }
    void getDiagnosisFirstCode() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        getCode("getDiagnosisExternalFirstCode", "fst_dgns_e_cd");
    }
    void getDiagnosisExternalCodes(int count) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        for (int i = 1; i <= count; i++) {
            Optional<String> code = (Optional<String>) claimClass.getMethod("getDiagnosisExternal" + i + "Code").invoke(claim);
            if (code.isPresent()) {
                SamhsaFields field = SamhsaFields.builder()
                        .code(code.get())
                        .column("icd_dgns_e_cd" + i)
                        .build();
                samhsaFields.add(field);
            }
        }
    }
    void getHcpcsCode() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        claimLinesCode("getHcpcsCode", "rev_cntr_apc_hipps_cd");
    }
    void claimLinesCode(String method, String column) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (claimLineClass == null) {
            return;
        }
        for (TClaimLine claimLine: claimLines) {
            Optional<String> code =  (Optional<String>) claimLineClass.getMethod(method).invoke(claimLine);
            short lineNum = (short) claimLineClass.getMethod("getLineNumber").invoke(claimLine);
            if (code.isPresent()) {
                SamhsaFields field = SamhsaFields.builder()
                        .column(column)
                        .code(code.get())
                        .lineNum(lineNum)
                        .build();
                samhsaFields.add(field);
            }
        }

    }
    void getApcOrHippsCode() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        claimLinesCode("getApcOrHippsCode", "rev_cntr_apc_hipps_cd");
    }
    void getDiagnosisCode() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        claimLinesCode("getDiagnosisCode","line_icd_dgns_cd");
    }
    void getCode(String method, String column) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Optional<String> code =  (Optional<String>) claimClass.getMethod(method).invoke(claim);
        if (code.isPresent()) {
            SamhsaFields field = SamhsaFields.builder()
                    .column(column)
                    .code(code.get())
                    .build();
            samhsaFields.add(field);
        }
    }
    void getClaimDRGCode() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        getCode("getDiagnosisRelatedGroupCd", "clm_drg_cd");
    }
    void getDiagnosisAdmittingCode() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        getCode("getDiagnosisAdmittingCode", "prncpal_dgns_cd");
    }
    public LocalDate getFromDate() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        LocalDate date = (LocalDate) claimClass.getMethod("getDateFrom").invoke(claim);
        return date == null? LocalDate.MAX: date;
    }

    public LocalDate getThroughDate() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        LocalDate date = (LocalDate) claimClass.getMethod("getDateThrough").invoke(claim);
        return date == null? LocalDate.MAX: date;
    }

}
