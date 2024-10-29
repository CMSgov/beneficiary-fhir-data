package gov.cms.bfd.data.npi.dto;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NPIData {
  String npi;
  String entityTypeCode;
  String providerOrganizationName;
  String taxonomyCode;
  String taxonomyDisplay;
  String providerNamePrefix;
  String providerFirstName;
  String providerMiddleName;
  String providerLastName;
  String providerNameSuffix;
  String providerCredential;
}
