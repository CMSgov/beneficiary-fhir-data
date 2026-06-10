package gov.cms.bfd.server.ng.util;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.web.util.UriComponentsBuilder;

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
   * adds previous link if an offset exists add a next link if the stream contains at least one more
   * than the limit empty.
   *
   * @param resources resources
   * @param batchLastUpdated last updated
   * @param requestDetails request details
   * @param limit record count
   * @param offset start index
   * @return bundle
   */
  public static Bundle bundleOrDefault(
      Stream<? extends Resource> resources,
      Supplier<ZonedDateTime> batchLastUpdated,
      Optional<RequestDetails> requestDetails,
      Optional<Integer> limit,
      Optional<Integer> offset) {
    var bundle = getBundle(resources, requestDetails, limit, offset);

    if (bundle.getEntry().isEmpty()) {
      return defaultBundle(batchLastUpdated);
    }
    return bundle;
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
    return bundleOrDefault(
        resources, batchLastUpdated, Optional.empty(), Optional.empty(), Optional.empty());
  }

  /**
   * Creates a bundle from the resource, returning a default bundle with lastUpdated populated if
   * empty.
   *
   * @param resource resource
   * @param batchLastUpdated last updated
   * @param requestDetails request details
   * @param limit record count
   * @param offset start index
   * @return bundle
   */
  public static Bundle bundleOrDefault(
      Optional<Resource> resource,
      Supplier<ZonedDateTime> batchLastUpdated,
      Optional<RequestDetails> requestDetails,
      Optional<Integer> limit,
      Optional<Integer> offset) {
    return resource
        .map(
            value ->
                bundleOrDefault(Stream.of(value), batchLastUpdated, requestDetails, limit, offset))
        .orElseGet(() -> defaultBundle(batchLastUpdated));
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
    return bundleOrDefault(
        resource, batchLastUpdated, Optional.empty(), Optional.empty(), Optional.empty());
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

  private static Bundle getBundle(
      Stream<? extends Resource> resources,
      Optional<RequestDetails> requestDetails,
      Optional<Integer> limit,
      Optional<Integer> offset) {

    record Page(List<Bundle.BundleEntryComponent> items, boolean hasMore) {}

    var page =
        resources
            .map(r -> new Bundle.BundleEntryComponent().setResource(r))
            .collect(
                Collectors.teeing(
                    Collectors.toList(),
                    Collectors.counting(),
                    // collecting and counting to see if we do have a next
                    // limits the stream to only return the requested limit
                    (list, count) ->
                        new Page(
                            trimEntriesToLimit(list, count, limit),
                            determineHasMore(count, limit)
                        )
                )
            );

    var bundle = new Bundle().setEntry(page.items());

    // if we do not have a request we cannot build the links
    requestDetails.ifPresent(
        details -> applyBundleLinks(bundle, details, page.hasMore(), offset, limit));

    return bundle;
  }

  private static List<Bundle.BundleEntryComponent> trimEntriesToLimit(List<Bundle.BundleEntryComponent> entries,long count,Optional<Integer> limit){
    if(limit.isPresent() && count > limit.get().longValue()) {
      return entries.subList(0, limit.get());
    }
    return entries;
  }

  private static boolean determineHasMore(long count,Optional<Integer> limit){
    return limit.isPresent() && count > limit.get().longValue();
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

  private static void applyBundleLinks(
      Bundle bundle,
      RequestDetails requestDetails,
      boolean hasMore,
      Optional<Integer> offset,
      Optional<Integer> limit) {
    // check if a link is needed
    if (hasMore) {
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
  }

  private static String buildLinkURL(RequestDetails requestDetails, Integer offset) {
    var uriBuilder = UriComponentsBuilder.fromUriString(requestDetails.getCompleteUrl());

    // Remove offset and startIndex parameters
    uriBuilder.replaceQueryParam(OFFSET_PARAM);
    uriBuilder.replaceQueryParam(START_INDEX_PARAM);

    // Add the new offset if it's not 0
    if (offset != 0) {
      uriBuilder.queryParam(OFFSET_PARAM, offset);
    }

    return uriBuilder.build().toUriString();
  }
}
