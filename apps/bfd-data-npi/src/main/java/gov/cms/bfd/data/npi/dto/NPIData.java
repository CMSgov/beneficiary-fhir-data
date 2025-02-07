package gov.cms.bfd.data.npi.dto;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** DTO used to supply the server with NPI enrichment information. */
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NPIData {
  /** Provider or Org npi. */
  String npi;

  /** Entity Type. Will be 1 for Provider, or 2 for Organization. */
  String entityTypeCode;

  /** Organization name. */
  String providerOrganizationName;

  /** Taxonomy code. */
  String taxonomyCode;

  /** Taxonomy display. */
  String taxonomyDisplay;

  /** Provider name prefix. */
  String providerNamePrefix;

  /** Provider first name. */
  String providerFirstName;

  /** Provider middle name. */
  String providerMiddleName;

  /** Provider last name. */
  String providerLastName;

  /** Provider suffix. */
  String providerNameSuffix;

  /** Provider credential. */
  String providerCredential;
}
