package gov.cms.bfd.server.ng.fhirtrimmer;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Utility class to remove elements from a FHIR resource based on a FHIRPath expression.
 */
public class FhirTrimmerAI {

    private final FhirContext fhirContext;

    /**
     * Initializes the trimmer with a reusable application FhirContext.
     */
    public FhirTrimmerAI(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    /**
     * Evaluates a FHIRPath expression against a resource and removes all matching elements.
     * This operation modifies the provided resource in-place.
     *
     * @param resource           The FHIR resource instance to prune.
     * @param fhirPathExpression The FHIRPath expression identifying the nodes to delete.
     */
    public void trim(IBaseResource resource, String fhirPathExpression) {
        if (resource == null || fhirPathExpression == null || fhirPathExpression.trim().isEmpty()) {
            return;
        }

        // 1. Evaluate the FHIRPath query to identify target nodes
        List<IBase> targets = fhirContext.newFhirPath().evaluate(resource, fhirPathExpression, IBase.class);
        if (targets.isEmpty()) {
            return; // Nothing matches the criteria, exit early
        }

        // 2. Map targets using reference/identity tracking for precision pruning
        Set<IBase> targetsToRemove = Collections.newSetFromMap(new IdentityHashMap<>());
        targetsToRemove.addAll(targets);

        // 3. Track visited nodes to guarantee defense against object cycles/contained structures
        Set<IBase> visited = Collections.newSetFromMap(new IdentityHashMap<>());

        // 4. Begin recursive depth-first tree traversal
        trimElement(resource, targetsToRemove, visited);
    }

    private void trimElement(IBase element, Set<IBase> targetsToRemove, Set<IBase> visited) {
        if (element == null || !visited.add(element)) {
            return;
        }

        // Scan public accessors of the current FHIR model POJO
        for (Method method : element.getClass().getMethods()) {
            String methodName = method.getName();

            // Filter for standard getters; bypass 'getOrCreate' extensions to prevent lazy instantiation
            if (methodName.startsWith("get") && !methodName.startsWith("getOrCreate") && method.getParameterCount() == 0) {
                Class<?> returnType = method.getReturnType();

                // Case A: The property is a repeating list (e.g., Patient.name, Patient.telecom)
                if (List.class.isAssignableFrom(returnType)) {
                    try {
                        List<?> list = (List<?>) method.invoke(element);
                        if (list != null) {
                            // Evict elements matching targeted instances by object reference identity
                            list.removeIf(item -> targetsToRemove.contains(item));

                            // Deeply scan remaining active items in the list
                            for (Object item : list) {
                                if (item instanceof IBase) {
                                    trimElement((IBase) item, targetsToRemove, visited);
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Safely absorb security or unmodifiable list reflection issues
                    }
                }
                // Case B: The property is a singular element (e.g., Patient.birthDate, Observation.valueQuantity)
                else if (IBase.class.isAssignableFrom(returnType)) {
                    try {
                        Object child = method.invoke(element);
                        if (child != null) {
                            if (targetsToRemove.contains(child)) {
                                // Match located! Dynamically map and invoke the corresponding setter to nullify it
                                String setterName = "set" + methodName.substring(3);
                                for (Method m : element.getClass().getMethods()) {
                                    if (m.getName().equals(setterName) && m.getParameterCount() == 1) {
                                        Class<?> paramType = m.getParameterTypes()[0];
                                        // Guard against setting primitives with null and verify type assignment safety
                                        if (!paramType.isPrimitive() && paramType.isAssignableFrom(returnType)) {
                                            m.invoke(element, (Object) null);
                                            break;
                                        }
                                    }
                                }
                            } else {
                                // No match at this node, drill deeper down the subtree
                                trimElement((IBase) child, targetsToRemove, visited);
                            }
                        }
                    } catch (Exception e) {
                        // Safely absorb reflection issues
                    }
                }
            }
        }
    }
}