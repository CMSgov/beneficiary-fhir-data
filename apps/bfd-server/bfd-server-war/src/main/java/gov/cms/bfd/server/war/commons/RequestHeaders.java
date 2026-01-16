package gov.cms.bfd.server.war.commons;

import static gov.cms.bfd.server.war.commons.StringUtils.splitOnCommas;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A per-request instance that holds all resource (FHIR) request headers, such as:
 * "includeIdentifiers" {@link CommonHeaders#HEADER_NAME_INCLUDE_IDENTIFIERS} "includeAddressFields"
 * {@link CommonHeaders#HEADER_NAME_INCLUDE_ADDRESS_FIELDS} which serve as part of BFD API.
 */
public class RequestHeaders {
  /** The request details. */
  RequestDetails requestDetails;

  /** A map of all the header names and values. */
  Map<String, Object> headerNVs = new HashMap<String, Object>();

  /**
   * Instantiates a new Request header object.
   *
   * @param requestDetails the request details
   */
  private RequestHeaders(RequestDetails requestDetails) {
    this.requestDetails = requestDetails;
    // parse headers
    CommonHeaders.FHIR_REQUEST_HEADERS.forEach(
        (h) -> {
          String v = this.requestDetails.getHeader(h);
          if (h.equals(CommonHeaders.HEADER_NAME_INCLUDE_ADDRESS_FIELDS)) {
            this.headerNVs.put(h, returnIncludeAddressFieldsValue(v));
          }
          if (h.equals(CommonHeaders.HEADER_NAME_INCLUDE_IDENTIFIERS)) {
            this.headerNVs.put(h, returnIncludeIdentifiersValues(v));
          }
        });
  }

  /** instantiate an empty RH, used by tests. */
  private RequestHeaders() {}

  /**
   * Currently used by internal code to create a RH instance with ad hoc header/value pairs for
   * testing code.
   *
   * @param hdrValues the header/value pairs
   */
  private RequestHeaders(String... hdrValues) {
    // convert h1 v1, h2 v2, ... into nv pairs
    if (hdrValues == null || hdrValues.length % 2 != 0) {
      throw new IllegalArgumentException(
          "Header Value string not valid - must be in pairs like h1, v1, h2, v2, ...: "
              + hdrValues);
    }
    Map<String, String> nvPairs = new HashMap<String, String>();
    String curKey = null;
    for (String s : hdrValues) {
      if (curKey == null) {
        curKey = s;
        String pv = nvPairs.put(curKey, "");
        if (pv != null) {
          throw new IllegalArgumentException(
              "Header Value string not valid - duplicate header name / value detected: "
                  + hdrValues);
        }
      } else {
        nvPairs.put(curKey, s);
        curKey = null;
      }
    }
    CommonHeaders.FHIR_REQUEST_HEADERS.forEach(
        (h) -> {
          String v = nvPairs.get(h);
          if (h.equals(CommonHeaders.HEADER_NAME_INCLUDE_ADDRESS_FIELDS)) {
            this.headerNVs.put(h, returnIncludeAddressFieldsValue(v));
          }
          if (h.equals(CommonHeaders.HEADER_NAME_INCLUDE_IDENTIFIERS)) {
            this.headerNVs.put(h, returnIncludeIdentifiersValues(v));
          }
        });
  }

  /**
   * Get a {@link RequestHeaders} instance extracting headers (API headers) from request details.
   *
   * @param requestDetails source to extract API headers
   * @return the {@link RequestHeaders} instance
   */
  public static RequestHeaders getHeaderWrapper(RequestDetails requestDetails) {
    return new RequestHeaders(requestDetails);
  }

  /**
   * Get an empty {@link RequestHeaders}.
   *
   * @return a {@link RequestHeaders} instance with no headers set
   */
  public static RequestHeaders getHeaderWrapper() {
    return new RequestHeaders();
  }

  /**
   * Gets a {@link RequestHeaders} instance extracting headers (API headers) from hdrValues.
   *
   * @param hdrValues a literal string in the form of h1, v1, h2, v2, ...
   * @return a {@link RequestHeaders} instance based on hdrValues
   */
  public static RequestHeaders getHeaderWrapper(String... hdrValues) {
    return new RequestHeaders(hdrValues);
  }

  /**
   * Get header value by using a given header name.
   *
   * @param <T> the header value type
   * @param hdrName the header name
   * @return the header value cast to its type
   */
  public <T> T getValue(String hdrName) {
    Object v = this.headerNVs.get(hdrName);
    if (v != null) {
      return (T) v;
    }
    return null;
  }

  /**
   * Public helper for iterating over a headers name and value pairs.
   *
   * @return a map of header name value pairs
   */
  public Map<String, Object> getNVPairs() {
    return this.headerNVs;
  }

  /**
   * Helper for iterating over a headers name and value pairs with an exclusion list (in the form of
   * delimited string).
   *
   * @param excludeHeaders list of headers to exclude when getting header/value pairs map
   * @return the map of all header/value with headers in exclude list removed
   */
  public Map<String, Object> getNVPairs(String... excludeHeaders) {
    Map<String, Object> nvs = new HashMap<String, Object>();
    List<String> excluded = Arrays.asList(excludeHeaders);
    this.headerNVs.forEach(
        (k, v) -> {
          if (!excluded.contains(k)) {
            // can validate dup too
            nvs.put(k, v);
          }
        });
    return nvs;
  }

  /**
   * Check if request contains header includeIdentifiers {@link
   * CommonHeaders#HEADER_NAME_INCLUDE_IDENTIFIERS} and if the value is 'hicn' or 'true'.
   *
   * @return {@code true} if the header is present and has value 'hicn' or 'true', {@code false}
   *     otherwise
   */
  public boolean isHICNinIncludeIdentifiers() {
    List<String> v = this.getValue(CommonHeaders.HEADER_NAME_INCLUDE_IDENTIFIERS);
    return v == null ? false : (v.contains("hicn") || v.contains("true"));
  }

  /**
   * Check if request contains header includeIdentifiers {@link
   * CommonHeaders#HEADER_NAME_INCLUDE_IDENTIFIERS} and if the value is 'mbi' or 'true'.
   *
   * @return {@code true} if the header is present and has value 'mbi' or 'true', {@code false}
   *     otherwise
   */
  public boolean isMBIinIncludeIdentifiers() {
    List<String> v = this.getValue(CommonHeaders.HEADER_NAME_INCLUDE_IDENTIFIERS);
    return v == null ? false : (v.contains("mbi") || v.contains("true"));
  }

  /**
   * Return the value from the VALID_HEADER_VALUES_INCLUDE_IDENTIFIERS header translated into a
   * {@link Boolean}.
   *
   * @param headerValue the header string value
   * @return the header value, returns false unless the value is "TRUE" (case-insensitive)
   */
  public static Boolean returnIncludeAddressFieldsValue(String headerValue) {
    return "TRUE".equalsIgnoreCase(headerValue);
  }

  /**
   * Return a valid List of values for the IncludeIdentifiers header.
   *
   * @param headerValues a String value containing the value of header
   *     VALID_HEADER_VALUES_INCLUDE_IDENTIFIERS
   * @return List of validated header values against the VALID_HEADER_VALUES_INCLUDE_IDENTIFIERS
   *     list
   */
  public static List<String> returnIncludeIdentifiersValues(String headerValues) {
    if (headerValues == null
        || headerValues.isEmpty()
        || headerValues.trim().replaceAll("^\\[|\\]$", "").isEmpty()) return Arrays.asList("");
    else {
      // Return values split on a comma with any whitespace, valid, distict, and sort
      return Arrays.asList(
              splitOnCommas(headerValues.trim().replaceAll("^\\[|\\]$", "").toLowerCase()))
          .stream()
          .peek(
              c -> {
                if (!CommonHeaders.VALID_HEADER_VALUES_INCLUDE_IDENTIFIERS.contains(c))
                  throw new InvalidRequestException(
                      "Unsupported "
                          + CommonHeaders.HEADER_NAME_INCLUDE_IDENTIFIERS
                          + " Header Value: |"
                          + c
                          + "|, "
                          + headerValues);
              })
          .distinct()
          .sorted()
          .collect(Collectors.toList());
    }
  }
}
