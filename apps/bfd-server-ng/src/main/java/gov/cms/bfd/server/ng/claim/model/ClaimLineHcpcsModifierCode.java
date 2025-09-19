package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.FhirUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;

@Embeddable
class ClaimLineHcpcsModifierCode {
  @Column(name = "hcpcs_1_mdfr_cd")
  private Optional<String> hcpcsModifierCode1;

  @Column(name = "hcpcs_2_mdfr_cd")
  private Optional<String> hcpcsModifierCode2;

  @Column(name = "hcpcs_3_mdfr_cd")
  private Optional<String> hcpcsModifierCode3;

  @Column(name = "hcpcs_4_mdfr_cd")
  private Optional<String> hcpcsModifierCode4;

  @Column(name = "hcpcs_5_mdfr_cd")
  private Optional<String> hcpcsModifierCode5;

  CodeableConcept toFhir() {
    var coding =
        Stream.of(
                hcpcsModifierCode1,
                hcpcsModifierCode2,
                hcpcsModifierCode3,
                hcpcsModifierCode4,
                hcpcsModifierCode5)
            .flatMap(Optional::stream)
            .map(c -> new Coding().setSystem(FhirUtil.getHcpcsSystem(c)).setCode(c))
            .toList();
    return new CodeableConcept().setCoding(coding);
  }
}
