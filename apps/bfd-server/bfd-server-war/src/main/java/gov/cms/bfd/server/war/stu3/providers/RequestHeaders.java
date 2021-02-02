package gov.cms.bfd.server.war.stu3.providers;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A per request instance holds all resource (FHIR) request headers, such as: "includeIdentifiers"
 * {@link PatientResourceProvider#HEADER_NAME_INCLUDE_IDENTIFIERS} "includeAddressFields" {@link
 * PatientResourceProvider#HEADER_NAME_INCLUDE_ADDRESS_FIELDS} which serve as part of BFD API
 */
public class RequestHeaders {
  RequestDetails requestDetails;
  Map<String, Object> headerNVs = new HashMap<String, Object>();

  private RequestHeaders(RequestDetails requestDetails) {
    this.requestDetails = requestDetails;
    // parse headers
    PatientResourceProvider.FHIR_REQUEST_HEADERS.forEach(
        (h) -> {
          String v = this.requestDetails.getHeader(h);
          if (h.equals(PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS)) {
            this.headerNVs.put(h, PatientResourceProvider.returnIncludeAddressFieldsValue(v));
          }
          if (h.equals(PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS)) {
            this.headerNVs.put(h, PatientResourceProvider.returnIncludeIdentifiersValues(v));
          }
        });
  }

  /**
   * instantiate an empty RH, used by tests
   *
   * @param requestDetails
   */
  private RequestHeaders() {}

  /**
   * currently used by internal code to create a RH instance with ad hoc header/value pairs for
   * testing code
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
      if (s.startsWith("[")) {
        String stackStr = "CALLSTACK:";
        for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
          stackStr = stackStr + ste.toString();
        }
        throw new InvalidRequestException(
            "got header value with BRACKET: " + s + ", CALLSTACK:" + stackStr);
      }
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
    PatientResourceProvider.FHIR_REQUEST_HEADERS.forEach(
        (h) -> {
          String v = nvPairs.get(h);
          if (h.equals(PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS)) {
            this.headerNVs.put(h, PatientResourceProvider.returnIncludeAddressFieldsValue(v));
          }
          if (h.equals(PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS)) {
            this.headerNVs.put(h, PatientResourceProvider.returnIncludeIdentifiersValues(v));
          }
        });
  }

  /**
   * get RH (RequestHeaders) instance extracting headers (API headers) from request details
   *
   * @param requestDetails source to extract API headers
   * @return a RH instance
   */
  public static RequestHeaders getHeaderWrapper(RequestDetails requestDetails) {
    return new RequestHeaders(requestDetails);
  }

  /**
   * get an empty RH
   *
   * @return RH instance with no headers at all
   */
  public static RequestHeaders getHeaderWrapper() {
    return new RequestHeaders();
  }

  /**
   * get RH (RequestHeaders) instance extracting headers (API headers) from hdrValues
   *
   * @param hdrValues a literal string in the form of h1, v1, h2, v2, ...
   * @return a RH instance based on hdrValues
   */
  public static RequestHeaders getHeaderWrapper(String... hdrValues) {
    return new RequestHeaders(hdrValues);
  }

  public <T> T getValue(String hdrName) {
    Object v = this.headerNVs.get(hdrName);
    if (v != null) {
      return (T) v;
    }
    return null;
  }

  public Map<String, Object> getNVPairs() {
    return this.headerNVs;
  }

  /**
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

  public boolean isHICNinIncludeIdentifiers() {
    List<String> v = this.getValue(PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS);
    return v == null ? false : (v.contains("hicn") || v.contains("true"));
  }

  public boolean isMBIinIncludeIdentifiers() {
    List<String> v = this.getValue(PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS);
    return v == null ? false : (v.contains("mbi") || v.contains("true"));
  }
}
