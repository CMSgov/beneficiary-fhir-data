package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import java.util.Optional;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;

/** Clinical Trial number. * */
public class ClinicalTrialNumber {
  @Column(name = "clm_clncl_tril_num")
  private Optional<String> clinicalTrailNum;

  Optional<Extension> toFhir() {
      return clinicalTrailNum.map(s -> new Extension()
              .setUrl(SystemUrls.BLUE_BUTTON_STRUCTURE_DEFINITION_CLAIM_CLINICAL_TRIAL_NUMBER)
              .setValue(new StringType(s)));
  }
}
