package gov.cms.bfd.server.ng.fhirtrimmer;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;
import java.util.EnumMap;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;

/** Fourth try is the charm, MAYBE. Nope. */
public class FhirTrimmer {

  private final IFhirPath fhirPathEngine;
  private final BasisProfileMap basisProfileMap;

  /**
   * Constructor.
   *
   * @param basisProfileMap map of resource type -> profile -> list of fhirpaths
   * @param fhirContext context shared between trimmers
   */
  public FhirTrimmer(BasisProfileMap basisProfileMap, FhirContext fhirContext) {
    this.basisProfileMap = basisProfileMap;
    this.fhirPathEngine = fhirContext.newFhirPath();
  }

  /**
   * Removes elements from a resource based on a profile.
   *
   * @param resource the resource to trim
   * @param profile the profile to trim to
   * @return the trimmed resource
   */
  public IBaseResource trim(IBaseResource resource, BasisProfile profile) {
    if (resource == null) {
      return null;
    }
    if (!(resource instanceof Resource baseResource)) {
      return resource;
    }

    ResourceType resourceType = baseResource.getResourceType();
    EnumMap<BasisProfile, List<String>> profileMap =
        basisProfileMap.getBlackList().get(resourceType);
    List<String> pathsToRemove = profileMap == null ? null : profileMap.get(profile);

    if (pathsToRemove == null || pathsToRemove.isEmpty()) {
      return resource;
    }

    for (String fhirPath : pathsToRemove) {
      removeElements(baseResource, fhirPath);
    }

    return resource;
  }

  private void removeElements(Base resource, String fhirPath) {
    List<IBase> matches = fhirPathEngine.evaluate(resource, fhirPath, IBase.class);
    if (matches.isEmpty()) {
      return;
    }

    int splitIndex = findParentPathSplit(fhirPath);
    // No parent segment, nothing to remove
    if (splitIndex < 0) {
      return;
    }
    String parentPath = fhirPath.substring(0, splitIndex);

    // Check if FhirPaths evaluate properly
    List<IBase> parents = fhirPathEngine.evaluate(resource, parentPath, IBase.class);

    for (IBase parentNode : parents) {
      if (parentNode instanceof Base parent) {
        removeMatchesFromParent(parent, matches);
      }
    }
  }

  /**
   * Walks the resource for matches.
   *
   * @param parent the parent resource
   * @param matches the matches to remove
   */
  private void removeMatchesFromParent(Base parent, List<IBase> matches) {

    for (var match : matches) {
      if (match instanceof Base baseMatch) {
        parent.removeChild(baseMatch.fhirType(), baseMatch);
      }
    }
  }

  /**
   * Finds the index of the dot separating the final segment of a FHIRPath.
   *
   * @param fhirPath F H I R P A T H (but a string)
   * @return the index of the splitting dot, or -1 if none found (for parent)
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
