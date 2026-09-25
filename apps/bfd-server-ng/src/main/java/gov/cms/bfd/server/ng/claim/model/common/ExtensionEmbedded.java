package gov.cms.bfd.server.ng.claim.model.common;

import java.util.List;
import org.hl7.fhir.r4.model.Extension;

/** Interface for any embeddable that composes a list of Extensions. */
public interface ExtensionEmbedded {

  /**
   * Shared toFhir() signature.
   *
   * @return the list of extensions
   */
  List<Extension> toFhir();
}
