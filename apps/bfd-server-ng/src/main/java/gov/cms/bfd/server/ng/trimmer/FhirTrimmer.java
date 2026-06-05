package gov.cms.bfd.server.ng.trimmer;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.FhirTerser;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Property;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FhirTrimmer {

    private final FhirContext ctx;
    private final FhirValidator validator;

    public FhirTrimmer(FhirContext ctx, FhirValidator validator) {
        this.ctx = ctx;
        this.validator = validator;
    }

    public IBaseResource trim(IBaseResource resource) {
        // 1. Validate the resource against the configured Profiles
        ValidationResult result = validator.validateWithResult(resource);

        // 2. Extract paths of forbidden elements
        List<String> pathsToRemove = result.getMessages().stream()
                .filter(msg -> isStructuralViolation(msg.getMessage()))
                .map(SingleValidationMessage::getLocationString)
                .filter(loc -> loc != null && !loc.isEmpty())
                // CRITICAL: Sort descending. Deleting index [1] before [0] prevents index shifting.
                .sorted((a, b) -> b.compareTo(a))
                .toList();

        // 3. Prune elements
        for (String fhirPath : pathsToRemove) {
            pruneByPath((Base) resource, fhirPath);
        }

        return resource;
    }

    /**
     * Filters for messages indicating an element is forbidden by the profile.
     * You will need to fine-tune these substrings based on your specific IG.
     */
    private boolean isStructuralViolation(String message) {
        String lowerMsg = message.toLowerCase();
        return lowerMsg.contains("is not allowed") ||
                lowerMsg.contains("max allowed = 0") ||
                lowerMsg.contains("unknown extension");
    }

    /**
     * Navigates to the parent element and removes the invalid child.
     */
    private void pruneByPath(Base resource, String fhirPath) {
        // Example path: "Claim.item[1].extension[0]"
        // Group 1 (Parent): "Claim.item[1]"
        // Group 2 (Element): "extension"
        // Group 3 (Index): "0" (or null if not a list)
        Pattern pattern = Pattern.compile("^(.*?)\\.?([a-zA-Z]+)(?:\\[(\\d+)\\])?$");
        Matcher matcher = pattern.matcher(fhirPath);

        if (!matcher.matches()) return;

        String parentPath = matcher.group(1);
        String elementName = matcher.group(2);
        String indexStr = matcher.group(3);

        // Strip the root resource name (e.g., "Claim.") for Terser compatibility
        String resourceName = resource.fhirType();
        if (parentPath.startsWith(resourceName)) {
            parentPath = parentPath.replaceFirst(resourceName + "\\.?", "");
        }

        FhirTerser terser = ctx.newTerser();

        try {
            // Resolve parent(s) using Terser. If path is empty, the resource itself is the parent.
            List<IBase> parents = parentPath.isEmpty() ?
                    List.of(resource) : terser.getValues(resource, parentPath);

            for (IBase p : parents) {
                Base parentBase = (Base) p;
                Property property = parentBase.getNamedProperty(elementName.hashCode(), elementName, false);

                if (property == null || !property.hasValues()) continue;

                if (indexStr != null) {
                    // It is a list. Remove the item at the specific index.
                    int index = Integer.parseInt(indexStr);
                    List<Base> existingValues = property.getValues();
                    if (index < existingValues.size()) {
                        existingValues.remove(index);
                    }
                } else {
                    // It is a single element (e.g., max = 0). Clear it entirely.
                    property.getValues().clear();
                }
            }
        } catch (Exception e) {
            // Log warning: Failed to parse or prune specific path
        }
    }
}