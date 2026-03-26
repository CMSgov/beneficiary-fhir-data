package gov.cms.bfd.server.ng.beneficiary.model;

import java.util.List;

public record IndexedScenario(String combinationIndex, List<PatientMatchParameter> entries) {}
