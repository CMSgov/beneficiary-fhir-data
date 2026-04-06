package gov.cms.bfd.server.ng.beneficiary.model;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Represents a single audit record for a patient match attempt.
 *
 * @param clientIp client's IP address
 * @param clientName client name
 * @param timestamp timestamp when the audit record was created
 * @param combinationsEvaluated the list of evaluated match combinations
 * @param finalDetermination the final determination if a match was found
 */
public record PatientMatchAuditRecord(
    String clientIp,
    String clientName,
    String clientId,
    Instant timestamp,
    List<MatchCombinationResult> combinationsEvaluated,
    Optional<FinalDetermination> finalDetermination) {}
