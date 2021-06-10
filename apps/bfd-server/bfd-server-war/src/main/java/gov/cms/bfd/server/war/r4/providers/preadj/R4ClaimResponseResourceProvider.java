package gov.cms.bfd.server.war.r4.providers.preadj;

import ca.uhn.fhir.rest.server.IResourceProvider;
import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.server.war.r4.providers.preadj.common.ResourceTypeV2;
import java.util.Optional;
import org.hl7.fhir.r4.model.ClaimResponse;
import org.springframework.stereotype.Component;

/** This FHIR {@link IResourceProvider} adds support for R4 {@link ClaimResponse} resources. */
@Component
public class R4ClaimResponseResourceProvider extends AbstractR4ResourceProvider<ClaimResponse> {

  /**
   * Specifies the exact resource configuration class to use for parsing.
   *
   * @param typeText String to parse representing the claim type.
   * @return The parsed {@link ClaimResponseTypeV2} type.
   */
  @VisibleForTesting
  Optional<ResourceTypeV2<ClaimResponse>> parseClaimType(String typeText) {
    return ClaimResponseTypeV2.parse(typeText);
  }
}
