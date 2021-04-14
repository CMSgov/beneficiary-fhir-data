package gov.cms.bfd.server.war.commons;

import java.util.Arrays;
import java.util.List;

public interface CommonHeaders {
  /**
   * The header key used to determine whether include mbi mbi and/or hicn values in response. See
   * {@link #RequestHeaders.getValue(<header-name>)} for details.
   */
  public static final String HEADER_NAME_INCLUDE_IDENTIFIERS = "IncludeIdentifiers";

  /** The List of valid values for the {@link #HEADER_NAME_INCLUDE_IDENTIFIERS} header. */
  public static final List<String> VALID_HEADER_VALUES_INCLUDE_IDENTIFIERS =
      Arrays.asList("true", "false", "hicn", "mbi");

  /**
   * The header key used to determine whether or not tax numbers should be included in responses.
   *
   * <p>Should be set to <code>"true"</code> if {@link CarrierClaimColumn#TAX_NUM} or {@link
   * DMEClaimColumn#TAX_NUM} should be mapped and included in the results, <code>"false"</code> if
   * not. Defaults to <code>"false"</code>.
   */
  public static final String HEADER_NAME_INCLUDE_TAX_NUMBERS = "IncludeTaxNumbers";

  /**
   * The header key used to determine whether include derived addresses fields in response. See
   * {@link #RequestHeaders.getValue(<header-name>)} for details.
   */
  public static final String HEADER_NAME_INCLUDE_ADDRESS_FIELDS = "IncludeAddressFields";

  public static final List<String> FHIR_REQUEST_HEADERS =
      Arrays.asList(
          HEADER_NAME_INCLUDE_ADDRESS_FIELDS,
          HEADER_NAME_INCLUDE_IDENTIFIERS,
          HEADER_NAME_INCLUDE_TAX_NUMBERS);
}
