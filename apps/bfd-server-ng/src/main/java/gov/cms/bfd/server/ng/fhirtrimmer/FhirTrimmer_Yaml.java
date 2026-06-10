package gov.cms.bfd.server.ng.fhirtrimmer;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.Property;

import java.util.List;
import java.util.Map;

public class FhirTrimmer_Yaml {

    private Map<String, List<String>> profileMap;

    public FhirTrimmer_Yaml(Map<String, List<String>> profileMap) {
        this.profileMap = profileMap;
    }

    public IBaseResource Trim(IBaseResource resource, String profile) {
        return new Claim() {
            @Override
            public String fhirType() {
                return "";
            }

            @Override
            protected void listChildren(List<Property> result) {

            }

            @Override
            public String getIdBase() {
                return "";
            }

            @Override
            public void setIdBase(String value) {

            }
        };
    }
}
