package gov.cms.bfd.server.ng.trimmer;

import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.SingleValidationMessage;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Base;

/** Old version of FhirTrimmer that used FhirValidator to validate, too slow for production. */
public class FhirTrimmer_Validation {

  private static final Pattern NODE_PATTERN = Pattern.compile("^([a-zA-Z0-9]+)(?:\\[(\\d+)])?$");

  private static final List<String> VIOLATION_KEYWORDS =
      List.of("not allowed", "max allowed", "maximum allowed", "unknown extension");

  private final FhirValidator validator;

  /**
   * Constructor that takes a validator.
   *
   * @param validator the FhirValidator instance
   */
  public FhirTrimmer_Validation(FhirValidator validator) {
    this.validator = Objects.requireNonNull(validator, "FhirValidator cannot be null");
  }

  /**
   * Trim the resource with the instantiated validator.
   *
   * @param resource resource to be trimmed
   * @return the trimmed resource
   */
  public IBaseResource trim(IBaseResource resource) {
    if (resource == null) {
      return null;
    }

    var result = validator.validateWithResult(resource);

    var pathsToRemove =
        result.getMessages().stream()
            .filter(this::isValidStructuralViolation)
            .map(SingleValidationMessage::getLocationString)
            .sorted(Comparator.reverseOrder())
            .toList();

    pathsToRemove.forEach(path -> removeElementByPath((Base) resource, path));

    return resource;
  }

  private boolean isValidStructuralViolation(SingleValidationMessage msg) {
    String location = msg.getLocationString();
    if (location == null || location.isBlank()) {
      return false;
    }

    String message = msg.getMessage();
    if (message == null) {
      return false;
    }

    String lowerMsg = message.toLowerCase();
    return VIOLATION_KEYWORDS.stream().anyMatch(lowerMsg::contains);
  }

  private void removeElementByPath(Base root, String fhirPath) {
    String[] parts = fhirPath.split("\\.");
    if (parts.length == 0) {
      return;
    }

    var startIndex = parts[0].equals(root.fhirType()) ? 1 : 0;

    var current = root;
    for (int i = startIndex; i < parts.length - 1; i++) {
      current = navigateChild(current, parts[i]);
      if (current == null) {
        return;
      }
    }

    deleteElement(current, parts[parts.length - 1]);
  }

  private Base navigateChild(Base parent, String pathPart) {
    var matcher = NODE_PATTERN.matcher(pathPart);
    if (!matcher.matches()) {
      return null;
    }

    var name = matcher.group(1);
    var property = parent.getChildByName(name);

    if (property != null && property.hasValues()) {
      var values = property.getValues();
      var indexStr = matcher.group(2);

      if (indexStr != null) {
        var index = Integer.parseInt(indexStr);
        if (index < values.size()) return values.get(index);
      } else if (!values.isEmpty()) {
        return values.getFirst();
      }
    }
    return null;
  }

  private void deleteElement(Base parent, String targetPart) {
    var matcher = NODE_PATTERN.matcher(targetPart);
    if (!matcher.matches()) {
      return;
    }

    var name = matcher.group(1);
    var property = parent.getChildByName(name);
    if (property == null || !property.hasValues()) {
      return;
    }

    var values = property.getValues();
    var indexStr = matcher.group(2);

    if (indexStr != null) {
      var index = Integer.parseInt(indexStr);
      if (index < values.size()) {
        parent.removeChild(name, values.get(index));
      }
    } else {
      values.forEach(val -> parent.removeChild(name, val));
    }
  }
}
