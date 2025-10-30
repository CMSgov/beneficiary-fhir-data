package gov.cms.bfd.server.ng.util;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Resource;

/** FHIR-related utility methods. */
public class FhirUtil {
  private FhirUtil() {}

  private static final Pattern IS_INTEGER = Pattern.compile("\\d+");

  /**
   * Adds a data absent reason of the coding is empty.
   *
   * @param codeableConcept codeable concept
   * @return modified codeable concept
   */
  public static CodeableConcept checkDataAbsent(CodeableConcept codeableConcept) {
    if (codeableConcept.getCoding().isEmpty()) {
      return codeableConcept.addCoding(
          new Coding()
              .setSystem(SystemUrls.HL7_DATA_ABSENT)
              .setCode("not-applicable")
              .setDisplay("Not Applicable"));
    }
    return codeableConcept;
  }

  /**
   * Returns the matching HCPCS system.
   *
   * @param code HCPCS code
   * @return system
   */
  public static String getHcpcsSystem(String code) {
    if (IS_INTEGER.matcher(code).matches()) {
      return SystemUrls.AMA_CPT;
    } else {
      return SystemUrls.CMS_HCPCS;
    }
  }

  /**
   * Creates a bundle from the resource, returning a default bundle with lastUpdated populated if
   * empty.
   *
   * @param resources resources
   * @param batchLastUpdated last updated
   * @return bundle
   */
  public static Bundle bundleOrDefault(
      Stream<Resource> resources, Supplier<ZonedDateTime> batchLastUpdated) {
    return bundleOrDefault(resources.toList(), batchLastUpdated);
  }

  /**
   * Creates a bundle from the resource, returning a default bundle with lastUpdated populated if
   * empty.
   *
   * @param resources resources
   * @param batchLastUpdated last updated
   * @return bundle
   */
  public static Bundle bundleOrDefault(
      List<Resource> resources, Supplier<ZonedDateTime> batchLastUpdated) {
    if (resources.isEmpty()) {
      return defaultBundle(batchLastUpdated);
    }
    return getBundle(resources.stream());
  }

  /**
   * Creates a bundle from the resource, returning a default bundle with lastUpdated populated if
   * empty.
   *
   * @param resource resource
   * @param batchLastUpdated last updated
   * @return bundle
   */
  public static Bundle bundleOrDefault(
      Optional<Resource> resource, Supplier<ZonedDateTime> batchLastUpdated) {
    return resource
        .map(value -> bundleOrDefault(List.of(value), batchLastUpdated))
        .orElseGet(() -> defaultBundle(batchLastUpdated));
  }

  /**
   * Builds the bundle and includes full urls to every entry in the bundle.
   *
   * @return a FHIR bundle
   */
  public static Bundle bundleWithFullUrls(
      Stream<? extends Resource> resources, Supplier<ZonedDateTime> batchLastUpdated) {
    var resourceList = resources.toList();

    if (resourceList.isEmpty()) {
      return defaultBundle(batchLastUpdated);
    }

    var bundle =
        new Bundle()
            .setType(Bundle.BundleType.COLLECTION)
            .setEntry(
                resourceList.stream()
                    .map(
                        r ->
                            new Bundle.BundleEntryComponent()
                                .setResource(r)
                                .setFullUrl("urn:uuid:" + r.getIdElement().getIdPart()))
                    .toList());

    if (batchLastUpdated != null) {
      bundle.setMeta(new Meta().setLastUpdated(DateUtil.toDate(batchLastUpdated.get())));
    }

    return bundle;
  }

  private static Bundle getBundle(Stream<Resource> resources) {
    return new Bundle()
        .setEntry(resources.map(r -> new Bundle.BundleEntryComponent().setResource(r)).toList());
  }

  /**
   * Returns a default bundle.
   *
   * @param batchLastUpdated last updated
   * @return bundle
   */
  public static Bundle defaultBundle(Supplier<ZonedDateTime> batchLastUpdated) {
    var bundle = new Bundle();
    bundle.setMeta(new Meta().setLastUpdated(DateUtil.toDate(batchLastUpdated.get())));
    return bundle;
  }
}
