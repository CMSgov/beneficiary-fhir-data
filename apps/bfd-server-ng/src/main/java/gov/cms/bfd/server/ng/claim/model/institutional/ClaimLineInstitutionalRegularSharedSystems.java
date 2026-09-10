package gov.cms.bfd.server.ng.claim.model.institutional;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

/** Claim line institutional regular shared systems. */
@Embeddable
@Getter
@AttributeOverride(name = "trackingNumber", column = @Column(name = "clm_line_pa_uniq_trkng_num"))
public class ClaimLineInstitutionalRegularSharedSystems extends ClaimLineInstitutionalBase {}
