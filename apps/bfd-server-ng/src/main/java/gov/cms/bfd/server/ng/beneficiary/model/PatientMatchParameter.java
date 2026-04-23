package gov.cms.bfd.server.ng.beneficiary.model;

import java.util.List;

/**
 * A parameter for searching the database for patient matching.
 *
 * @param values list of valid values to search for in the database
 * @param name parameter/field name
 */
public record PatientMatchParameter(List<Object> values, String name) {}
