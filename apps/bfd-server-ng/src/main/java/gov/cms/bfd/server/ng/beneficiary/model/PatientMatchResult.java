package gov.cms.bfd.server.ng.beneficiary.model;

import java.util.List;
import java.util.Optional;

public record PatientMatchResult(
    List<MatchCombinationResult> combinations,
    Optional<FinalDetermination> finalDetermination,
    Optional<Beneficiary> matchedBeneficiary) {}
