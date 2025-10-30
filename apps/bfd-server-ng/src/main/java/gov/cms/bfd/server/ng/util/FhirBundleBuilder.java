package gov.cms.bfd.server.ng.util;

import java.time.ZonedDateTime;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Getter;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Resource;

/**
 * Builder for creating FHIR Bundles with configurable options such as full URLs and lastUpdated.
 */
@Getter
@Builder(builderClassName = "Builder", builderMethodName = "internalBuilder", setterPrefix = "with")
public class FhirBundleBuilder {

  private final Stream<Resource> resources;
  private final Supplier<ZonedDateTime> batchLastUpdated;
  private final boolean includeFullUrls;

  /**
   * Initialize a builder from a stream of FHIR resources.
   *
   * @param resources the stream of resources to include in the bundle
   * @return a new builder instance
   */
  public static Builder fromResources(Stream<Resource> resources) {
    return internalBuilder().withResources(resources);
  }

  /**
   * Builds the bundle.
   *
   * @return a FHIR bundle
   */
  public Bundle toBundle() {
    var resourceList = resources.toList();

    if (resourceList.isEmpty()) {
      return defaultBundle();
    }

    var bundle = new Bundle();
    bundle.setType(Bundle.BundleType.COLLECTION);
    bundle.setEntry(
        resourceList.stream()
            .map(
                r -> {
                  var entry = new Bundle.BundleEntryComponent().setResource(r);
                  if (includeFullUrls) {
                    // Use UUID or existing ID if present
                    var idPart = r.getIdElement().getIdPart();
                    if (idPart == null || idPart.isEmpty()) {
                      idPart = java.util.UUID.randomUUID().toString();
                      r.setId(idPart);
                    }
                    entry.setFullUrl("urn:uuid:" + idPart);
                  }
                  return entry;
                })
            .toList());

    if (batchLastUpdated != null) {
      bundle.setMeta(new Meta().setLastUpdated(DateUtil.toDate(batchLastUpdated.get())));
    }

    return bundle;
  }

  private Bundle defaultBundle() {
    var bundle = new Bundle();
    if (batchLastUpdated != null) {
      bundle.setMeta(new Meta().setLastUpdated(DateUtil.toDate(batchLastUpdated.get())));
    }
    return bundle;
  }
}
