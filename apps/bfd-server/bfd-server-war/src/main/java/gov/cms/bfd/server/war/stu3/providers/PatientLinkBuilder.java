package gov.cms.bfd.server.war.stu3.providers;

import ca.uhn.fhir.rest.api.Constants;
import org.hl7.fhir.dstu3.model.Bundle;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/** A link builder for Patient resources using bene-id cursors */
public final class PatientLinkBuilder implements LinkBuilder {
  private final UriComponents components;

  public PatientLinkBuilder(String requestString) {
    components = UriComponentsBuilder.fromUriString(requestString).build();
    components.getQueryParams().getFirst(Constants.PARAM_COUNT);
  }

  @Override
  public boolean isPagingRequested() {
    return false;
  }

  @Override
  public int getPageSize() {
    return Integer.MAX_VALUE;
  }

  @Override
  public boolean isFirstPage() {
    return true;
  }

  public String getCursor() {
    return "";
  }

  @Override
  public void addLinks(Bundle to) {}
}
