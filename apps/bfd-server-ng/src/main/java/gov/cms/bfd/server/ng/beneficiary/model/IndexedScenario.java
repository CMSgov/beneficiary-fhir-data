package gov.cms.bfd.server.ng.beneficiary.model;

import java.util.List;

/**
 * Represents a single scenario used during patient matching.
 *
 * @param combinationIndex the index of the combination
 * @param entries the criteria used to patient match for this combination
 */
public record IndexedScenario(String combinationIndex, List<PatientMatchParameter> entries) {}
