package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.FhirUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;

/**
 * Represents the HCPCS code information for a claim line. This class is embeddable in JPA entities.
 */
@Getter
@Embeddable
public class ClaimLineHcpcsCode {

  /**
   * HCPCS code associated with the claim line. This is mapped to the database column {@code
   * clm_line_hcpcs_cd}. May be empty if no code is provided.
   */
  @Column(name = "clm_line_hcpcs_cd") // SAMHSA
  private Optional<String> hcpcsCode;

  /**
   * Converts the HCPCS code to a FHIR {@link Coding} object.
   *
   * @return an {@link Optional} containing the FHIR {@link Coding} if the HCPCS code is present;
   *     otherwise, an empty {@link Optional}.
   */
  Optional<Coding> toFhir() {
    if (hcpcsCode.isEmpty()) {
      return Optional.empty();
    }
    var hcpcs = hcpcsCode.get();
    return Optional.of(
        new Coding().setSystem(FhirUtil.getHcpcsSystem(hcpcs.substring(0, 1))).setCode(hcpcs));
  }
}
