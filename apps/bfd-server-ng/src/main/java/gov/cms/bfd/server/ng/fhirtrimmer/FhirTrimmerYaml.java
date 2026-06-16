package gov.cms.bfd.server.ng.fhirtrimmer;

import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseResource;

/** The FhirTrimmer (3rd attempt) that reads in the YAML like we're supposed to. */
public class FhirTrimmerYaml {

  // private Map<String, List<String>> profileMap;

  /**
   * Constructor.
   *
   * @param profileMap the structure of profiles to whitelist or blacklist, depending on
   *     implementation
   */
  public FhirTrimmerYaml(Map<String, List<String>> profileMap) {
    // this.profileMap = profileMap;
  }

  /**
   * Where the magic happens.
   *
   * @param resource the resource to be trimmed
   * @param profile the haircut (profile) to give it
   * @return the trimmed resource
   */
  public IBaseResource trim(IBaseResource resource, String profile) {

    return resource;
  }
}
