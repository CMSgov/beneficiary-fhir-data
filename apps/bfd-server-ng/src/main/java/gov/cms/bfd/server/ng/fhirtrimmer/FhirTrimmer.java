package gov.cms.bfd.server.ng.fhirtrimmer;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.hapi.fluentpath.FhirPathR4;
import org.hl7.fhir.r4.model.Resource;

import java.util.List;
import java.util.Map;

/**
 * Final attempt at the barber shop.
 */
public class FhirTrimmer {

    FhirContext fhirContext;
    Map<BasisProfile, List<FhirPathR4>> profileMap;

    /**
     * Constructor.
     * @param context shared FhirContext
     * @param profileMap the profile map generated on startup
     */
    public FhirTrimmer(FhirContext context, Map<BasisProfile, List<FhirPathR4>> profileMap){
        this.fhirContext = context;
        this.profileMap = profileMap;
    }


    /**
     * Where the magic happens for the fourth or fifth time.
     * @param base the base resource to be trimmed
     * @param profile the profile to trim it to
     * @return the trimmed resource
     */
    public Resource trim(Resource base, BasisProfile profile){
        return base;
    }
}
