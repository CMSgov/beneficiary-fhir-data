package gov.cms.bfd.server.ng;

import java.time.ZonedDateTime;
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

  public static Bundle singleOrDefaultBundle(
      Optional<Resource> resource, Supplier<ZonedDateTime> batchLastUpdated) {
    if (resource.isEmpty()) {
      return defaultBundle(batchLastUpdated);
    }
    return resource.map(FhirUtil::singleBundle).orElseGet(() -> defaultBundle(batchLastUpdated));
  }

  public static Bundle getBundle(Stream<Resource> resources) {
    return new Bundle()
        .setEntry(resources.map(r -> new Bundle.BundleEntryComponent().setResource(r)).toList());
  }

  private static Bundle defaultBundle(Supplier<ZonedDateTime> batchLastUpdated) {
    var bundle = new Bundle();
    bundle.setMeta(new Meta().setLastUpdated(DateUtil.toDate(batchLastUpdated.get())));
    return bundle;
  }

  private static Bundle singleBundle(Resource resource) {
    var lastUpdated = resource.getMeta().getLastUpdated();
    var bundle = getBundle(Stream.of(resource));
    bundle.setMeta(new Meta().setLastUpdated(lastUpdated));
    return bundle;
  }
}
