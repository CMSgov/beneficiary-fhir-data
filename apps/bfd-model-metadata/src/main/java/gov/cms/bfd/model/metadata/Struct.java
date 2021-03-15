package gov.cms.bfd.model.metadata;

/**
 * Models data representations, where each concrete implementation represents a format such as FHIR,
 * CSV, etc. and each instance of that represents a particular object schema in that format, e.g. a
 * FHIR <code>Patient</code> resource, a CSV file for beneficiary data, etc.
 *
 * <p>Each {@link Struct} is composed of one or more unique {@link StructField}s.
 *
 * <p>Implementations of {@link Struct} are required to be immutable. It's strongly recommended that
 * each implementation be paired with a separate "builder" factory.
 */
public interface Struct {}
