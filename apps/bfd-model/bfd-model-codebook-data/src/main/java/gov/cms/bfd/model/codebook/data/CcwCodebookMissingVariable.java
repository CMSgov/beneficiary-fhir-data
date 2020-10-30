package gov.cms.bfd.model.codebook.data;

import gov.cms.bfd.model.codebook.model.Value;
import gov.cms.bfd.model.codebook.model.ValueGroup;
import gov.cms.bfd.model.codebook.model.Variable;
import gov.cms.bfd.model.codebook.model.VariableType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public enum CcwCodebookMissingVariable implements CcwCodebookInterface {
  MBI_EFFCTV_DT,
  /**
   * The {@code MBI_EFFCTV_DT}
   *
   * <ul>
   *   <li><strong>Label:</strong> MBI Effective Date
   *   <li><strong>Description:</strong>
   *       <p>MBI EFFECTIVE DATE
   *   <li><strong>Short Name:</strong> MBI_EFFCTV_DT
   *   <li><strong>Long Name:</strong> MBI_EFFECTIVE_DATE
   *   <li><strong>Type:</strong> DATE
   *   <li><strong>Length:</strong> 8
   *   <li><strong>Source:</strong> CMS Enrollment Database (EDB)
   *   <li><strong>Value Format:</strong> MM/DD/YYYY
   *   <li><strong>Coded Values?:</strong> false
   *   <li><strong>Comment:</strong>
   *       <p>THE EFFECTIVE DATE WHEN AN MBI IS ASSIGNED TO A BENEFICIARY.
   * </ul>
   */
  MBI_END_DT,
  /**
   * The {@code MBI_END_DT}
   *
   * <ul>
   *   <li><strong>Label:</strong> MBI End Date
   *   <li><strong>Description:</strong>
   *       <p>MBI EFFECTIVE DATE
   *   <li><strong>Short Name:</strong> MBI_END_DT
   *   <li><strong>Long Name:</strong> MBI_END_DATE
   *   <li><strong>Type:</strong> DATE
   *   <li><strong>Length:</strong> 8
   *   <li><strong>Source:</strong> CMS Enrollment Database (EDB)
   *   <li><strong>Value Format:</strong> MM/DD/YYYY
   *   <li><strong>Coded Values?:</strong> false
   *   <li><strong>Comment:</strong>
   *       <p>THE END DATE WHEN AN MBI IS NO LONGER ACTIVE (INACTIVATED) FOR A BENEFICIARY DUE TO
   *       BEING COMPROMISED OR INVOLVED IN A CROSS REFERENCE ACTION.
   * </ul>
   */
  BENE_LINK_KEY_NUM,
  /**
   * The {@code BENE_LINK_KEY_NUM}
   *
   * <ul>
   *   <li><strong>Codebook:</strong> Master Beneficiary Summary File - Base With Medicare Part
   *       A/B/D (May 2017, Version 1.0)
   *   <li><strong>Label:</strong> Beneficiary Link Key
   *   <li><strong>Description:</strong>
   *       <p>Beneficiary Link Key Number
   *   <li><strong>Short Name:</strong> BENE_LINK_KEY_NUM
   *   <li><strong>Long Name:</strong> BENEFICIARY_LINK KEY_NUMBER
   *   <li><strong>Type:</strong> NUM
   *   <li><strong>Length:</strong>38
   *   <li><strong>Source:</strong> CMS Enrollment Database (EDB)
   *   <li><strong>Value Format:</strong> 0-12
   *   <li><strong>Coded Values?:</strong> false
   *   <li><strong>Comment:</strong>
   *       <p>AN ARTIFICIAL, UNIQUE NUMBER THAT IS ASSIGNED TO EACH EDB RECORD. THE NMUD SYSTEM USES
   *       THIS NUMBER TO LINK TOGETHER ALL MEDICARE
   *       <p>CLAIM BILLS THAT BELONG TO A SINGLE BENEFICIARY.
   * </ul>
   */
  CARR_CLM_CNTL_NUM,
  /**
   * The {@code CARR_CLM_CNTL_NUM}
   *
   * <ul>
   *   <li><strong>Codebook:</strong> Master Beneficiary Summary File - Base With Medicare Part
   *       A/B/D (May 2017, Version 1.0)
   *   <li><strong>Label:</strong> Carrier Claim Control Number
   *   <li><strong>Description:</strong>
   *       <p>Carrier Claim Control Number
   *   <li><strong>Short Name:</strong> CARR_CLM_CNTL_NUM
   *   <li><strong>Long Name:</strong> CARR_CLM_CNTL_NUM
   *   <li><strong>Type:</strong> CHAR
   *   <li><strong>Length:</strong>23
   *   <li><strong>Source:</strong> CWF
   *   <li><strong>Value Format:</strong> 0-12
   *   <li><strong>Coded Values?:</strong> false
   *   <li><strong>Comment:</strong>
   *       <p>Unique control number assigned by a carrier to a non-institutional claim.
   * </ul>
   */
  FI_DOC_CLM_CNTL_NUM,
  /**
   * The {@code FI_DOC_CLM_CNTL_NUM}
   *
   * <ul>
   *   <li><strong>Codebook:</strong> Master Beneficiary Summary File - Base With Medicare Part
   *       A/B/D (May 2017, Version 1.0)
   *   <li><strong>Label:</strong> FI Document Claim Control Number
   *   <li><strong>Description:</strong>
   *       <p>FI Document Claim Control Number
   *   <li><strong>Short Name:</strong> FI_DOC_CLM_CNTL_NUM
   *   <li><strong>Long Name:</strong> FI_DOC_CLM_CNTL_NUM
   *   <li><strong>Type:</strong> CHAR
   *   <li><strong>Length:</strong>23
   *   <li><strong>Source:</strong> CWF
   *   <li><strong>Value Format:</strong> 0-12
   *   <li><strong>Coded Values?:</strong> false
   *   <li><strong>Comment:</strong>
   *       <p>Unique control number assigned by an ntermediary to an institutional claim.
   * </ul>
   */
  FI_ORIG_CLM_CNTL_NUM;
  /**
   * The {@code FI_ORIG_CLM_CNTL_NUM}
   *
   * <ul>
   *   <li><strong>Codebook:</strong> Master Beneficiary Summary File - Base With Medicare Part
   *       A/B/D (May 2017, Version 1.0)
   *   <li><strong>Label:</strong> FI Original Claim Control Number
   *   <li><strong>Description:</strong>
   *       <p>FI Original Claim Control Number
   *   <li><strong>Short Name:</strong> FI_ORIG_CLM_CNTL_NUM
   *   <li><strong>Long Name:</strong> FI_ORIG_CLM_CNTL_NUM
   *   <li><strong>Type:</strong> CHAR
   *   <li><strong>Length:</strong>23
   *   <li><strong>Source:</strong> CWF
   *   <li><strong>Value Format:</strong> 0-12
   *   <li><strong>Coded Values?:</strong> false
   *   <li><strong>Comment:</strong>
   *       <p>Effective with Version G, the original intermediary control number (ICN) which is
   *       <p>claims, representing the ICN of the original transaction now being adjusted.
   * </ul>
   */
  private Map<String, Variable> VARIABLES_BY_ID = buildVariablesMappedById();

  public static Map<String, Variable> buildVariablesMappedById() {
    Map<String, Variable> variablesMappedById = new LinkedHashMap<>();

    variablesMappedById.put(
        "MBI_EFFCTV_DT",
        new Variable(
            "MBI_EFFCTV_DT",
            "MBI Effective Date",
            getList("MBI EFFECTIVE DATE"),
            "MBI_EFFCTV_DT",
            "MBI_EFFECTIVE_DATE",
            VariableType.DATE,
            8,
            "CMS Enrollment Database (EDB)",
            "MM/DD/YYYY",
            getValueGroup("MBI Effective Date", ""),
            getList("THE EFFECTIVE DATE WHEN AN MBI IS ASSIGNED TO A BENEFICIARY.")));

    variablesMappedById.put(
        "MBI_END_DT",
        new Variable(
            "MBI_END_DT",
            "MBI End Date",
            getList("MBI End DATE"),
            "MBI_END_DT",
            "MBI_END_DATE",
            VariableType.DATE,
            8,
            "CMS Enrollment Database (EDB)",
            "MM/DD/YYYY",
            getValueGroup("MBI End Date", ""),
            getList(
                "THE END DATE WHEN AN MBI IS NO LONGER ACTIVE (INACTIVATED) FOR A BENEFICIARY DUE TO BEING COMPROMISED OR INVOLVED IN A CROSS REFERENCE ACTION.")));

    variablesMappedById.put(
        "FI_DOC_CLM_CNTL_NUM",
        new Variable(
            "FI_DOC_CLM_CNTL_NUM",
            "Document Claim Control Number",
            getList("Document Claim Control Number"),
            "FI_DOC_CLM_CNTL_NUM",
            "FI_DOC_CLM_CNTL_NUM",
            VariableType.CHAR,
            23,
            "CMS Enrollment Database (EDB)",
            "",
            getValueGroup("Document Claim Control Number", ""),
            getList(
                "Unique control number assigned by an ntermediary to an institutional claim.")));

    variablesMappedById.put(
        "FI_ORIG_CLM_CNTL_NUM",
        new Variable(
            "FI_ORIG_CLM_CNTL_NUM",
            "FI Original Claim Control Number",
            getList("FI Original Claim Control Number"),
            "FI_ORIG_CLM_CNTL_NUM",
            "FI_ORIG_CLM_CNTL_NUM",
            VariableType.CHAR,
            23,
            "CWF",
            "",
            getValueGroup("FI Original Claim Control Number", ""),
            getList("Effective with Version G, the original intermediary control number (ICN)")));

    variablesMappedById.put(
        "CARR_CLM_CNTL_NUM",
        new Variable(
            "CARR_CLM_CNTL_NUM",
            "Carrier Claim Control Number",
            getList("Carrier Claim Control Number"),
            "CARR_CLM_CNTL_NUM",
            "CARR_CLM_CNTL_NUM",
            VariableType.CHAR,
            23,
            "CWF",
            "",
            getValueGroup("Carrier Claim Control Number", ""),
            getList("Unique control number assigned by a carrier to a non-institutional claim.")));

    return variablesMappedById;
  }

  public static List<String> getList(String addString) {
    List<String> listOfStrings = new ArrayList<String>();
    listOfStrings.add(addString);
    return listOfStrings;
  }

  public static List<ValueGroup> getValueGroup(String description, String code) {
    List<String> descriptionList = new ArrayList<String>();
    descriptionList.add(description);

    List<Value> values = new ArrayList<Value>();
    values.add(new Value(code, description));

    List<ValueGroup> valueGroups = new ArrayList<ValueGroup>();
    valueGroups.add(new ValueGroup(descriptionList, values));
    return valueGroups;
  }

  /**
   * @return the {@link Variable} data (parsed from a codebook PDF) for this {@link
   *     CcwCodebookVariable} constant
   */
  public Variable getVariable() {
    return VARIABLES_BY_ID.get(this.name());
  }
}
