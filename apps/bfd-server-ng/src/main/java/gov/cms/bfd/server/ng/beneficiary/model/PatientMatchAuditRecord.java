package gov.cms.bfd.server.ng.beneficiary.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public record PatientMatchAuditRecord(
    Optional<String> clientIp,
    Optional<String> clientName,
    Optional<String> clientId,
    LocalDate timestamp,
    List<MatchCombinationResult> combinationsEvaluated,
    Optional<FinalDetermination> finalDetermination) {}
