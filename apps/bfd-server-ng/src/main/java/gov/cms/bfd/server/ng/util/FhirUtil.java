package gov.cms.bfd.server.ng.util;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import java.time.ZonedDateTime;
import java.util.ArrayList;
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
  private static final String OFFSET_PARAM = "_offset";
  private static final String START_INDEX_PARAM = "startIndex";

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
      Stream<? extends Resource> resources, Supplier<ZonedDateTime> batchLastUpdated) {
    var bundle = getBundle(resources);

    if (bundle.getEntry().isEmpty()) {
      return defaultBundle(batchLastUpdated);
    }
    return bundle;
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
        .map(value -> bundleOrDefault(Stream.of(value), batchLastUpdated))
        .orElseGet(() -> defaultBundle(batchLastUpdated));
  }

  /**
   * Builds the bundle and includes full urls to every entry in the bundle.
   *
   * @param resources resources
   * @param batchLastUpdated last updated
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
    bundle.setMeta(new Meta().setLastUpdated(DateUtil.toDate(batchLastUpdated.get())));

    return bundle;
  }

  private static Bundle getBundle(Stream<? extends Resource> resources) {
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

  /**
   * Returns a bundle updated with previous and next links. This method assumes you have at least
   * one addition item in your bundle than the limit to know to add the updated next link
   *
   * @param requestDetails current request
   * @param offset current offset
   * @param limit requested limit
   * @param bundle bundle
   * @return bundle
   */
  public static Bundle applyBundleLinks(
      RequestDetails requestDetails,
      Optional<Integer> offset,
      Optional<Integer> limit,
      Bundle bundle) {
    // check if a link is needed
    if (limit.isPresent() && limit.get() < bundle.getEntry().size()) {
      // remove the extra entry that let us know we had next
      bundle.setEntry(bundle.getEntry().subList(0, limit.get()));
      var nextOffset = Math.max(0, offset.orElse(0) + limit.orElse(0));
      bundle
          .addLink()
          .setRelation(Constants.LINK_NEXT)
          .setUrl(buildLinkURL(requestDetails, nextOffset));
    }
    // check if a previous link is needed
    if (offset.isPresent() && offset.get() > 0) {
      // get previous offset
      var previousOffset = Math.max(0, offset.get() - limit.orElse(0));
      bundle
          .addLink()
          .setRelation(Constants.LINK_PREVIOUS)
          .setUrl(buildLinkURL(requestDetails, previousOffset));
    }
    return bundle;
  }

  private static String buildLinkURL(RequestDetails requestDetails, Integer offset) {
    String baseUrl = requestDetails.getCompleteUrl();
    String[] urlParts = baseUrl.split("\\?", 2);
    String path = urlParts[0];

    List<String> retainedParams = new ArrayList<>();
    if (urlParts.length == 2 && !urlParts[1].isBlank()) {
      for (String param : urlParts[1].split("&")) {
        if (param.isBlank()) {
          continue;
        }
        String key = param.split("=", 2)[0];
        if (!OFFSET_PARAM.equals(key) && !START_INDEX_PARAM.equals(key)) {
          retainedParams.add(param);
        }
      }
    }

    if (offset != 0) {
      retainedParams.add(OFFSET_PARAM + "=" + offset);
    }

    if (retainedParams.isEmpty()) {
      return path;
    }
    return path + "?" + String.join("&", retainedParams);
  }
}
