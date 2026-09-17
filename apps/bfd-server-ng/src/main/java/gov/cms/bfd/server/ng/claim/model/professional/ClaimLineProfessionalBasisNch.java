package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.ClaimFilterOptions;
import gov.cms.bfd.server.ng.claim.model.common.AdjudicationEmbedded;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Extension;

/** clm_line data for Professional-Basis-NCH. */
@Embeddable
@Getter
public class ClaimLineProfessionalBasisNch extends ClaimLineProfessionalBasis {

  @Embedded private ClaimLineProfessionalNchCore nchCore;
  @Embedded private ExtensionsProfessionalAllNch extensionsNch;

  @Override
  Optional<AdjudicationEmbedded> getClaimLineAdjudication() {
    return Optional.empty();
  }

  @Override
  void populateProductAndQuantity(ExplanationOfBenefit.ItemComponent line) {
    nchCore.populateProductAndQuantity(line, getHcpcsCode(), getServiceUnitQuantity());
  }

  @Override
  public List<Extension> getExtensions(ClaimFilterOptions options) {
    var extensions = new ArrayList<>(super.getExtensions(options));
    extensions.addAll(extensionsNch.toFhir());
    return extensions;
  }
}
