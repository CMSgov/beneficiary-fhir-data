package gov.cms.bfd.server.war.commons;

import gov.cms.bfd.server.war.stu3.providers.TransformerUtils;
import java.util.Objects;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;

/** Models a icdCode code entry in a claim. */
public abstract class IcdCode {
  /** The {@link Coding#getSystem()} used for ICD-9 diagnosis codes. */
  public static final String CODING_SYSTEM_ICD_9 = "http://hl7.org/fhir/sid/icd-9-cm";

  /** The {@link Coding#getSystem()} used for ICD-10 diagnosis codes. */
  public static final String CODING_SYSTEM_ICD_10 = "http://hl7.org/fhir/sid/icd-10";

  private final String icdCode;
  private final Character icdVersionCode;

  /**
   * Constructs a new {@link IcdCode}.
   *
   * @param icdCode the ICD code of the icdCode, if any
   * @param icdVersionCode the CCW encoding (per <a href=
   *     "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prncpal_dgns_vrsn_cd.txt">
   *     CCW Data Dictionary: PRNCPAL_DGNS_VRSN_CD</a> and other similar fields) of the code's ICD
   *     version, if any
   */
  protected IcdCode(Optional<String> icdCode, Optional<Character> icdVersionCode) {
    Objects.requireNonNull(icdCode);
    Objects.requireNonNull(icdVersionCode);

    this.icdCode = icdCode.get();
    this.icdVersionCode = icdVersionCode.orElse(null);
  }

  /**
   * @param codeableConcept the {@link CodeableConcept} to check
   * @return <code>true</code> if the specified {@link CodeableConcept} contains a {@link Coding}
   *     that matches this {@link IcdCode}, <code>false</code> if not
   */
  public boolean isContainedIn(CodeableConcept codeableConcept) {
    return codeableConcept.getCoding().stream()
            .filter(c -> icdCode.equals(c.getCode()))
            .filter(c -> getFhirSystem().equals(c.getSystem()))
            .count()
        != 0;
  }

  /** @return a {@link CodeableConcept} that contains this {@link IcdCode} */
  public CodeableConcept toCodeableConcept() {
    CodeableConcept codeableConcept = new CodeableConcept();
    Coding coding = codeableConcept.addCoding();

    String system = getFhirSystem();
    coding.setSystem(system);

    coding.setCode(icdCode);
    coding.setDisplay(TransformerUtils.retrieveIcdCodeDisplay(icdCode));

    return codeableConcept;
  }

  /** @return the ICD code textual value */
  public String getCode() {
    return icdCode;
  }

  /** @return the version of this {@link IcdCode} */
  public Character getVersion() {
    return icdVersionCode;
  }

  /**
   * @return the <a href= "https://www.hl7.org/fhir/terminologies-systems.html"> FHIR Coding
   *     system</a> value for this {@link IcdCode}' {@link #icdVersionCode} value
   */
  public String getFhirSystem() {
    String system;
    if (icdVersionCode == null || icdVersionCode.equals('9')) system = CODING_SYSTEM_ICD_9;
    else if (icdVersionCode.equals('0')) system = CODING_SYSTEM_ICD_10;
    else system = String.format("http://hl7.org/fhir/sid/unknown-icd-version/%s", icdVersionCode);
    return system;
  }
}
