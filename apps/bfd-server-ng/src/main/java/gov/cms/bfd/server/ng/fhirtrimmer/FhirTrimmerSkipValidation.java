package gov.cms.bfd.server.ng.fhirtrimmer;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Property;
import org.hl7.fhir.r4.model.Resource;

/** FhirTrimmer concept that skips validation, version 2. */
public class FhirTrimmerSkipValidation {

  private static final Pattern NODE_PATTERN = Pattern.compile("^([a-zA-Z0-9]+)(?:\\[(\\d+)])?$");
  private static final Pattern INDEX_PATTERN = Pattern.compile("\\[(\\d+)]");
  private final Map<String, List<String>> profilePathMap;

  /**
   * Constructor, loads in a profile path map.
   *
   * @param profilePathMap a map of profiles to a list of FhirPaths to remove
   */
  public FhirTrimmerSkipValidation(Map<String, List<String>> profilePathMap) {
    this.profilePathMap = Objects.requireNonNull(profilePathMap, "Profile cache cannot be null");
  }

  /**
   * Where the magic happens.
   *
   * @param resource the resource to be trimmed
   * @return the trimmed resource
   */
  public IBaseResource trim(IBaseResource resource) {
    if (resource == null) {
      return null;
    }

    // 1. If it's a Bundle, recursively trim each nested resource within the entries
    if (resource instanceof org.hl7.fhir.r4.model.Bundle bundle) {
      if (bundle.hasEntry()) {
        bundle.getEntry().stream()
            .filter(org.hl7.fhir.r4.model.Bundle.BundleEntryComponent::hasResource)
            .forEach(entry -> trim(entry.getResource()));
      }
      return bundle;
    }

    if (!(resource instanceof Resource baseResource)) {
      return resource;
    }

    // 2. Process single resources (or nested resources unpacked from the bundle)
    List<CanonicalType> profiles = baseResource.getMeta().getProfile();

    for (CanonicalType profile : profiles) {
      var profileUrl = profile.getValue();
      List<String> pathsToRemove = this.profilePathMap.get(profileUrl);

      if (pathsToRemove != null) {
        List<String> sortedPaths =
            pathsToRemove.stream()
                .sorted((a, b) -> padIndices(b).compareTo(padIndices(a)))
                .toList();

        for (var path : sortedPaths) {
          removeElement(baseResource, path);
        }
      }
    }

    return resource;
  }

  private String padIndices(String path) {
    Matcher m = INDEX_PATTERN.matcher(path);
    StringBuilder sb = new StringBuilder();
    while (m.find()) {
      m.appendReplacement(sb, "[" + String.format("%08d", Integer.parseInt(m.group(1))) + "]");
    }
    m.appendTail(sb);
    return sb.toString();
  }

  private void removeElement(Base root, String fhirPath) {
    String[] parts = fhirPath.split("\\.");
    if (parts.length == 0) {
      return;
    }

    int start = parts[0].equals(root.fhirType()) ? 1 : 0;
    var current = root;

    for (int i = start; i < parts.length - 1; i++) {
      current = processElement(current, parts[i], false);
      if (current == null) {
        return;
      }
    }

    processElement(current, parts[parts.length - 1], true);
  }

  private Base processElement(Base parent, String part, boolean delete) {
    Matcher matcher = NODE_PATTERN.matcher(part);
    if (!matcher.matches()) {
      return null;
    }

    String name = matcher.group(1);
    Property property = parent.getChildByName(name);

    if (property == null || !property.hasValues()) {
      return null;
    }

    List<Base> values = property.getValues();
    String indexGroup = matcher.group(2);

    return delete
            ? handleDeletion(parent, name, values, indexGroup)
            : handleRetrieval(values, indexGroup);
  }

  private Base handleDeletion(Base parent, String name, List<Base> values, String indexGroup) {
    if (indexGroup != null) {
      int index = Integer.parseInt(indexGroup);
      if (index < values.size()) {
        parent.removeChild(name, values.get(index));
      }
    } else {
      values.forEach(value -> parent.removeChild(name, value));
    }
    return null;
  }

  private Base handleRetrieval(List<Base> values, String indexGroup) {
    int index = (indexGroup != null) ? Integer.parseInt(indexGroup) : 0;
    return (index < values.size()) ? values.get(index) : null;
  }
}
