package gov.cms.bfd.server.ng.fhirtrimmer;

import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.List;
import java.util.Map;

public class FhirTrimmer_Yaml {

    private Map<String, List<String>> profileMap;

    public FhirTrimmer_Yaml(Map<String, List<String>> profileMap) {
        this.profileMap = profileMap;
    }

    public IBaseResource Trim(IBaseResource resource, String profile) {



        return resource;
    }
}
