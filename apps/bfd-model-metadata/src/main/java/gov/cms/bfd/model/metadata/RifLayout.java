package gov.cms.bfd.model.metadata;

/**
 * A {@link Struct} implementation for Research Interchange Format (RIF) files from the Chronic
 * Conditions Warehouse (CCW) at CMS. RIF files are basically CSV files with the following
 * characteristics:
 *
 * <ul>
 *   <li>Pipe characters <code>'|'</code> are used as field separators.
 *   <li>Line break characters <code>'\n'</code> are used as record separators.
 *   <li>One level of nesting/hierarchy is supported, for claim "headers" and claim "trailers" (i.e.
 *       claim lines or revenue centers), where the header record and columns are repeated for each
 *       trailer in a claim, and the trailer columns are appended after the header columns.
 * </ul>
 */
public final class RifLayout implements Struct {}
