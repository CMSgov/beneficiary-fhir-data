package gov.cms.bfd.server.ng.coverage;

import gov.cms.bfd.server.ng.beneficiary.model.Beneficiary;
import gov.cms.bfd.server.ng.beneficiary.model.BeneficiaryEntitlement;
import gov.cms.bfd.server.ng.beneficiary.model.BeneficiaryEntitlementReason;
import gov.cms.bfd.server.ng.beneficiary.model.BeneficiaryStatus;
import gov.cms.bfd.server.ng.beneficiary.model.BeneficiaryThirdParty;
import java.util.Optional;
import lombok.Builder;
import lombok.Getter;

/**
 * A Service Object (SO) or Data Transfer Object (DTO) that aggregates all necessary data pieces
 * from various beneficiary-related tables required to construct a comprehensive FHIR Coverage
 * resource.
 */
@Getter
@Builder
public class CoverageDetails {

  private final Beneficiary beneficiary;

  private final Optional<BeneficiaryThirdParty> thirdPartyDetails;
  private final Optional<BeneficiaryStatus> currentStatus;
  private final Optional<BeneficiaryEntitlement> partEntitlement;
  private final Optional<BeneficiaryEntitlementReason> currentEntitlementReason;
}
