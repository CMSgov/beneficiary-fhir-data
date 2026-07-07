package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.regex.Pattern;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;

@Getter
@Embeddable
class HcpcsOrCptOrHippsCode {

  @Column(name = "hcpcs_or_cpt_or_hipps")
  private String hcpcsOrCptOrHipps;

  private static final Pattern IS_INTEGER = Pattern.compile("\\d+");

  CodeableConcept toFhir() {
    var codeableConcept = new CodeableConcept();
    var code = hcpcsOrCptOrHipps.substring(0, 1);

    if (IS_INTEGER.matcher(code).matches()) {
      codeableConcept.addCoding(
          new Coding().setSystem(SystemUrls.AMA_CPT).setCode(hcpcsOrCptOrHipps));
    } else {
      codeableConcept.addCoding(
          new Coding().setSystem(SystemUrls.CMS_HCPCS).setCode(hcpcsOrCptOrHipps));
    }

    var hippsCode = hcpcsOrCptOrHipps.substring(1, 2);

    if (!IS_INTEGER.matcher(hippsCode).matches()) {
      codeableConcept.addCoding(
          new Coding().setSystem(SystemUrls.CMS_HIPPS).setCode(hcpcsOrCptOrHipps));
    }
    return codeableConcept;
  }
}
