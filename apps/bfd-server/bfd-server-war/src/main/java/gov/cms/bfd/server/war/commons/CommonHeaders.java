package gov.cms.bfd.server.war.commons;

import java.util.Arrays;
import java.util.List;

/** A collection of common headers. */
public interface CommonHeaders {
  /**
   * The header key used to determine whether include mbi mbi and/or hicn values in response. See
   * {@link RequestHeaders#getValue(String)} for details.
   */
  public static final String HEADER_NAME_INCLUDE_IDENTIFIERS = "IncludeIdentifiers";

  /** The List of valid values for the {@link #HEADER_NAME_INCLUDE_IDENTIFIERS} header. */
  public static final List<String> VALID_HEADER_VALUES_INCLUDE_IDENTIFIERS =
      Arrays.asList("true", "false", "hicn", "mbi");

  /**
   * Ignored. Kept only to track usage of the {@code IncludeTaxNumbers} header/query parameter via
   * MDC.
   */
  public static final String HEADER_NAME_INCLUDE_TAX_NUMBERS = "IncludeTaxNumbers";

  /**
   * The header key used to determine whether include derived addresses fields in response. See
   * {@link RequestHeaders#getValue(String)} for details.
   */
  public static final String HEADER_NAME_INCLUDE_ADDRESS_FIELDS = "IncludeAddressFields";

  /** A list of common FHIR request headers. */
  public static final List<String> FHIR_REQUEST_HEADERS =
      Arrays.asList(
          HEADER_NAME_INCLUDE_ADDRESS_FIELDS,
          HEADER_NAME_INCLUDE_IDENTIFIERS,
          HEADER_NAME_INCLUDE_TAX_NUMBERS);
}
