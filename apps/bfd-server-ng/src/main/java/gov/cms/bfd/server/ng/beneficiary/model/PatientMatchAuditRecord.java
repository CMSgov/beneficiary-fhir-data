package gov.cms.bfd.server.ng.beneficiary.model;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record PatientMatchAuditRecord(
    String clientIp,
    String clientName,
    String clientId,
    Instant timestamp,
    List<MatchCombinationResult> combinationsEvaluated,
    Optional<FinalDetermination> finalDetermination) {}
