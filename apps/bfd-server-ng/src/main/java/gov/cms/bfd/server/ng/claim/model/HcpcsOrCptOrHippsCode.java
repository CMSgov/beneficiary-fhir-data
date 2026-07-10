package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;

/** Represents the HCPCS, CPT and HIPPS code information for a prior authorization line. */
@Getter
@Embeddable
public class HcpcsOrCptOrHippsCode {

  @Column(name = "hcpcs_or_cpt_or_hipps")
  private String hcpcsOrCptOrHipps;

  /**
   * Determines the corresponding coding systems for the applicable codes.
   *
   * @return the coding systems
   */
  public List<String> getCodingSystems() {
    var systems = new ArrayList<String>();

    if (Character.isDigit(hcpcsOrCptOrHipps.charAt(0))) {
      systems.add(SystemUrls.AMA_CPT);
    } else {
      systems.add(SystemUrls.CMS_HCPCS);
    }

    if (!Character.isDigit(hcpcsOrCptOrHipps.charAt(1))) {
      systems.add(SystemUrls.CMS_HIPPS);
    }

    return systems;
  }

  CodeableConcept toFhir() {
    return new CodeableConcept()
        .setCoding(
            getCodingSystems().stream()
                .map(system -> new Coding().setSystem(system).setCode(hcpcsOrCptOrHipps))
                .toList());
  }
}
