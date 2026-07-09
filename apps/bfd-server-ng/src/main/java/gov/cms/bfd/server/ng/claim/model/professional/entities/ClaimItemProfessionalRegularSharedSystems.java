package gov.cms.bfd.server.ng.claim.model.professional.entities;

import gov.cms.bfd.server.ng.claim.model.common.ClaimItemBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimItemId;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineHcpcsCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimProcedureBase;

import java.util.Optional;

/** LineItem for a professional claim, regular profile, sourced from shared systems. */
public class ClaimItemProfessionalRegularSharedSystems implements ClaimItemBase {
    @Override
    public ClaimItemId getClaimItemId() {
        return null;
    }

    @Override
    public Optional<ClaimProcedureBase> getProcedure() {
        return Optional.empty();
    }

    @Override
    public Optional<ClaimLineHcpcsCode> getClaimLineHcpcsCode() {
        return Optional.empty();
    }

    @Override
    public ClaimLineBase getClaimLine() {
        return null;
    }
}
