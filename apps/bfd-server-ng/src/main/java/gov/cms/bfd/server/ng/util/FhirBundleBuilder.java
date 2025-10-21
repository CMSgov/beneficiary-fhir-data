package gov.cms.bfd.server.ng.util;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Resource;

/**
 * Builder for creating FHIR Bundles with configurable options such as full URLs and lastUpdated.
 */
public class FhirBundleBuilder {

  private final Stream<Resource> resources;
  private Supplier<ZonedDateTime> batchLastUpdated;
  private boolean includeFullUrls = false;

  private FhirBundleBuilder(Stream<Resource> resources) {
    this.resources = resources;
  }

  /**
   * Initialize a builder from a stream of FHIR resources.
   *
   * @param resources the stream of resources to include in the bundle
   * @return a new builder instance
   */
  public static FhirBundleBuilder fromResources(Stream<Resource> resources) {
    return new FhirBundleBuilder(resources);
  }

  /**
   * Sets the lastUpdated timestamp for the bundle.
   *
   * @param batchLastUpdated supplier for last updated timestamp
   * @return the builder
   */
  public FhirBundleBuilder withLastUpdated(Supplier<ZonedDateTime> batchLastUpdated) {
    this.batchLastUpdated = batchLastUpdated;
    return this;
  }

  /**
   * Configures the builder to include full URLs using resource IDs.
   *
   * @param include whether to include full URLs
   * @return the builder
   */
  public FhirBundleBuilder includeFullUrls(boolean include) {
    this.includeFullUrls = include;
    return this;
  }

  /**
   * Builds the bundle.
   *
   * @return a FHIR bundle
   */
  public Bundle build() {
    List<Resource> resourceList = resources.toList();

    if (resourceList.isEmpty()) {
      return defaultBundle();
    }

    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.COLLECTION);
    bundle.setEntry(
        resourceList.stream()
            .map(
                r -> {
                  var entry = new Bundle.BundleEntryComponent().setResource(r);
                  if (includeFullUrls) {
                    // Use UUID or existing ID if present
                    String idPart = r.getIdElement().getIdPart();
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
