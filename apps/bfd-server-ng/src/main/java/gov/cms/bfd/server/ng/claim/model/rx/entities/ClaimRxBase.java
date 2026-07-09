package gov.cms.bfd.server.ng.claim.model.rx.entities;

import gov.cms.bfd.server.ng.claim.model.common.ClaimItemBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimRelatedCondition;
import gov.cms.bfd.server.ng.claim.model.common.ClaimSourceId;
import gov.cms.bfd.server.ng.claim.model.common.MetaSourceSk;
import gov.cms.bfd.server.ng.claim.model.common.entities.ClaimBase;

import java.util.Collections;
import java.util.Optional;
import java.util.SortedSet;

/** Shared base for professional claim types (NCH and Shared Systems). */
public class ClaimRxBase extends ClaimBase {
    @Override
    public ClaimSourceId getClaimSourceId() {
        return null;
    }

    @Override
    public MetaSourceSk getMetaSourceSk() {
        return null;
    }

    @Override
    public SortedSet<ClaimItemBase> getItems() {
        return Collections.emptySortedSet();
    }

    @Override
    public Optional<Integer> getDrgCode() {
        return Optional.empty();
    }

    @Override
    public Optional<ClaimRelatedCondition> getClaimRelatedCondition() {
        return Optional.empty();
    }
}
