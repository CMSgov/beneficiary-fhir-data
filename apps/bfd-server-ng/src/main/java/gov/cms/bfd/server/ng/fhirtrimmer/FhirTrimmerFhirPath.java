package gov.cms.bfd.server.ng.fhirtrimmer;

import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.context.BaseRuntimeElementDefinition;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Resource;

/** Third try using FhirPath. */
public class FhirTrimmerFhirPath {

  private final FhirContext fhirContext;
  private final IFhirPath fhirPathEngine;
  private final Map<String, List<String>> profilePathMap;

  /**
   * Constructor.
   *
   * @param profilePathMap map of profile name and blacklist of fhirpaths
   * @param fhirContext context shared between trimmers
   */
  public FhirTrimmerFhirPath(Map<String, List<String>> profilePathMap, FhirContext fhirContext) {
    this.profilePathMap = Objects.requireNonNull(profilePathMap, "profilePathMap cannot be null");
    this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext cannot be null");
    this.fhirPathEngine = fhirContext.newFhirPath();
  }

  /**
   * FHIR HAIRCUT.
   *
   * @param resource the resource to trim
   * @param profile the profile to trim to
   * @return the trimmed resource
   */
  public IBaseResource trim(IBaseResource resource, String profile) {
    if (resource == null) {
      return null;
    }

    if (!(resource instanceof Resource baseResource)) {
      return resource;
    }

    List<String> pathsToRemove = profilePathMap.get(profile);

    for (String fhirPath : pathsToRemove) {
      removeElements(baseResource, fhirPath);
    }

    return resource;
  }

  /**
   * Remove all elements matched by the given FhirPath expression from the resource.
   *
   * @param resource the resource to trim
   * @param fhirPath the FhirPath expression identifying elements to remove
   */
  private void removeElements(Base resource, String fhirPath) {
    List<IBase> matches = fhirPathEngine.evaluate(resource, fhirPath, IBase.class);
    if (matches.isEmpty()) {
      return;
    }

    int splitIdx = findParentPathSplit(fhirPath);
    String parentPath = splitIdx >= 0 ? fhirPath.substring(0, splitIdx) : fhirPath;

    List<IBase> parents = fhirPathEngine.evaluate(resource, parentPath, IBase.class);

    for (IBase parentNode : parents) {
      if (!(parentNode instanceof Base parent)) {
        continue;
      }
      removeMatchesFromParent(parent, matches);
    }
  }

  /**
   * For a given parent, walks its runtime child definitions to find which named child slot should
   * die.
   *
   * @param parent the parent node
   * @param matches the matched nodes to remove
   */
  private void removeMatchesFromParent(Base parent, List<IBase> matches) {
    BaseRuntimeElementDefinition<?> def = fhirContext.getElementDefinition(parent.getClass());

    for (BaseRuntimeChildDefinition childDef : def.getChildren()) {
      List<?> values = childDef.getAccessor().getValues(parent);
      if (values.isEmpty()) {
        continue;
      }

      for (IBase match : matches) {
        if (!(match instanceof Base baseMatch)) {
          continue;
        }
        if (values.contains(baseMatch)) {
          parent.removeChild(childDef.getElementName(), baseMatch);
        }
      }
    }
  }

  /**
   * Finds the index of the dot separating a FhirPath's parent collection path from its final
   * segment, skipping dots nested inside parentheses (e.g. {@code where(value.exists())}).
   *
   * @param fhirPath fhir path
   * @return the index of the splitting dot, or -1 if none found
   */
  private int findParentPathSplit(String fhirPath) {
    int depth = 0;
    for (int i = fhirPath.length() - 1; i >= 0; i--) {
      char c = fhirPath.charAt(i);
      if (c == ')') {
        depth++;
      } else if (c == '(') {
        depth--;
      } else if (c == '.' && depth == 0) {
        return i;
      }
    }
    return -1;
  }
}
