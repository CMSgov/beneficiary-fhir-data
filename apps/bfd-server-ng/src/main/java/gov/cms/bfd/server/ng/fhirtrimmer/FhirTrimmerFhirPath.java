package gov.cms.bfd.server.ng.fhirtrimmer;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.hapi.ctx.FhirR4;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Resource;

/** Third try using HAPI FhirPath. */
public class FhirTrimmerFhirPath {

  private final Map<String, List<String>> profilePathMap;
  private final IFhirPath fhirPathEngine;

  /**
   * Constructor.
   *
   * @param profilePathMap map of profile URL to list of FhirPath expressions to remove
   * @param fhirContext the FhirContext instance
   */
  public FhirTrimmerFhirPath(Map<String, List<String>> profilePathMap, FhirContext fhirContext) {
    this.profilePathMap = Objects.requireNonNull(profilePathMap, "profilePathMap cannot be null");
    this.fhirPathEngine = new FhirR4().createFhirPathExecutor(fhirContext);
  }

  /**
   * Trim the resource using FhirPath evaluation.
   *
   * @param resource resource to trim
   * @param profile the profile we're trimming to
   * @return trimmed resource
   */
  public IBaseResource trim(IBaseResource resource, String profile) {
    if (resource == null) {
      return null;
    }

    if (!(resource instanceof Resource baseResource)) {
      return resource;
    }

    List<String> pathsToRemove = profilePathMap.get(profile);

    for (String path : pathsToRemove) {
      removeByFhirPath(baseResource, path);
    }

    return resource;
  }

  private void removeByFhirPath(Resource resource, String fhirPath) {
    // Evaluate the path to get matching nodes
    List<IBase> matches = fhirPathEngine.evaluate(resource, fhirPath, IBase.class);
    if (matches.isEmpty()) {
      return;
    }

    // Derive parent path for child removal
    int lastDot = fhirPath.lastIndexOf('.');
    if (lastDot < 0) {
      return;
    }

    String parentPath = fhirPath.substring(0, lastDot);
    String childName = stripPredicate(fhirPath.substring(lastDot + 1));

    List<IBase> parents = fhirPathEngine.evaluate(resource, parentPath, IBase.class);

    for (IBase parent : parents) {
      if (!(parent instanceof Base baseParent)) {
        continue;
      }
      for (IBase match : matches) {
        if (!(match instanceof Base baseMatch)) {
          continue;
        }
        baseParent.removeChild(childName, baseMatch);
      }
    }
  }

  /**
   * Strips FhirPath predicates from a path. Does not work if there are multiple predicates.
   *
   * @param segment the path
   * @return the stripped path
   */
  private String stripPredicate(String segment) {
    int paren = segment.indexOf('(');
    return paren >= 0 ? segment.substring(0, paren) : segment;
  }
}
