package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.ClaimFilterOptions;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import java.util.List;
import lombok.Getter;
import org.hl7.fhir.r4.model.Extension;

/** Claim line, professional, regular profile base. */
@MappedSuperclass
@Getter
abstract class ClaimLineProfessionalRegular extends ClaimLineProfessionalBase {

  @Embedded ExtensionsProfessional extensions;

  @Override
  public List<Extension> getExtensions(ClaimFilterOptions options) {
    return extensions.toFhir();
  }
}
