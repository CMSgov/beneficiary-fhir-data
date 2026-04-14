package gov.cms.bfd.server.ng.beneficiary.model;

import java.util.List;
import java.util.Optional;

/**
 * Represents the result of a patient match attempt.
 *
 * @param combinations the list of evaluated match combinations
 * @param finalDetermination the final determination if a match was found
 * @param matchedBeneficiary the final matched beneficiary if any
 */
public record PatientMatchResult(
    List<MatchCombinationResult> combinations,
    Optional<FinalDetermination> finalDetermination,
    Optional<Beneficiary> matchedBeneficiary) {}
