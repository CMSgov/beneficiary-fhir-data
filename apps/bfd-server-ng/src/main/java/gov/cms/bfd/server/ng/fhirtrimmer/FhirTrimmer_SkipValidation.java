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

/** Attempt number two skipping validation, much faster. Not workable though. */
public class FhirTrimmer_SkipValidation {

  private static final Pattern NODE_PATTERN = Pattern.compile("^([a-zA-Z0-9]+)(?:\\[(\\d+)])?$");
  private final Map<String, List<String>> profilePathMap;

  /**
   * Constructor that takes the mapping for profile and elements to remove.
   *
   * @param profilePathMap the map for a profile and a list of paths to remove
   */
  public FhirTrimmer_SkipValidation(Map<String, List<String>> profilePathMap) {
    this.profilePathMap = Objects.requireNonNull(profilePathMap, "Profile cache cannot be null");
  }

  /**
   * Second iteration of FhirTrim.
   *
   * @param resource The resource to trim
   * @return The trimmed resource
   */
  public IBaseResource trim(IBaseResource resource) {
    if (!(resource instanceof Resource baseResource)) {
      return resource;
    }

    List<CanonicalType> profiles = baseResource.getMeta().getProfile();

    for (CanonicalType profile : profiles) {
      var profileUrl = profile.getValue();
      List<String> pathsToRemove = this.profilePathMap.get(profileUrl);

      if (pathsToRemove != null) {
        for (var path : pathsToRemove) {
          removeElement(baseResource, path);
        }
      }
    }

    return resource;
  }

  private void removeElement(Base root, String fhirPath) {
    String[] parts = fhirPath.split("\\.");
    if (parts.length == 0) {
      return;
    }

    int start;
    if (parts[0].equals(root.fhirType())) {
      start = 1;
    } else {
      start = 0;
    }

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

    if (delete) {
      if (matcher.group(2) != null) { // element in array
        int index = Integer.parseInt(matcher.group(2));
        if (index < values.size()) {
          parent.removeChild(name, values.get(index));
        }
      } else { // regular element
        for (Base val : values) {
          parent.removeChild(name, val);
        }
      }
      return null;
    } else {
      int index;
      if (matcher.group(2) != null) {
        index = Integer.parseInt(matcher.group(2));
      } else {
        index = 0;
      }

      if (index < values.size()) {
        return values.get(index);
      } else {
        return null;
      }
    }
  }
}
