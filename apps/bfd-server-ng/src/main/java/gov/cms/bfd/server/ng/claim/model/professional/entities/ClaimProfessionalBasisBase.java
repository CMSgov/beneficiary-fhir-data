package gov.cms.bfd.server.ng.claim.model.professional.entities;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimItemBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimRecordType;
import gov.cms.bfd.server.ng.claim.model.common.ClaimRelatedCondition;
import gov.cms.bfd.server.ng.claim.model.common.ClaimSourceId;
import gov.cms.bfd.server.ng.claim.model.common.MetaSourceSk;
import gov.cms.bfd.server.ng.util.SequenceGenerator;
import java.util.Collections;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

import java.util.List;
import java.util.Optional;
import java.util.SortedSet;

/** Shared base for regular profile professional claims. */
public abstract class ClaimProfessionalBasisBase extends ClaimProfessionalBase {
    @Override
    AdjudicationChargeBase getAdjudicationCharge() {
        return null;
    }

    @Override
    List<ExplanationOfBenefit.SupportingInformationComponent> buildSubclassSupportingInfo() {
        return List.of();
    }

    @Override
    void addSubclassAdjudication(ExplanationOfBenefit eob) {

    }

    @Override
    void addSubclassCareTeam(ExplanationOfBenefit eob, SequenceGenerator sequenceGenerator) {

    }

    @Override
    Optional<ClaimRecordType> getClaimRecordTypeOptional() {
        return Optional.empty();
    }

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
    public Optional<ClaimRelatedCondition> getClaimRelatedCondition() {
        return Optional.empty();
    }
}
