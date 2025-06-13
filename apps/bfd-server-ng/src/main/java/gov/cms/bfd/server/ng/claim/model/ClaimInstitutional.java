package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SequenceGenerator;
import gov.cms.bfd.server.ng.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

import java.util.List;

@Getter
@Entity
@Table(name = "claim_institutional", schema = "idr")
public class ClaimInstitutional {
  @Embedded private ClaimInstitutionalSupportingInfo supportingInfo;
  @Embedded private PpsDrgWeight ppsDrgWeight;
  @Embedded private ClaimInstitutionalExtensions extensions;
  @Embedded private BenefitBalanceInstitutional benefitBalanceInstitutional;
}
