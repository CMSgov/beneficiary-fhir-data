package gov.cms.bfd.server.war.commons;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.List;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Patient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/** A link builder for Patient resources using bene-id cursors */
public final class PatientLinkBuilder implements LinkBuilder {
  private final UriComponents components;
  private final Integer count;
  private final String cursor;

  public static final String PARAM_CURSOR = "cursor";

  public PatientLinkBuilder(String requestString) {
    components = UriComponentsBuilder.fromUriString(requestString).build();
    count = extractCountParam(components);
    cursor = extractCursorParam(components);
  }

  @Override
  public boolean isPagingRequested() {
    return count != null;
  }

  @Override
  public int getPageSize() {
    return isPagingRequested() ? count : Integer.MAX_VALUE;
  }

  @Override
  public boolean isFirstPage() {
    return cursor == null || !isPagingRequested();
  }

  @Override
  public void addLinks(Bundle to) {
    List<BundleEntryComponent> entries = to.getEntry();
    if (!isPagingRequested()) return;

    to.addLink(
        new Bundle.BundleLinkComponent()
            .setRelation(Constants.LINK_SELF)
            .setUrl(components.toUriString()));
    to.addLink(
        new Bundle.BundleLinkComponent().setRelation(Constants.LINK_FIRST).setUrl(buildUrl("")));

    if (entries.size() == getPageSize() && entries.size() > 0) {
      Patient lastPatient = (Patient) entries.get(entries.size() - 1).getResource();
      String lastPatientId = lastPatient.getId();
      to.addLink(
          new Bundle.BundleLinkComponent()
              .setRelation(Constants.LINK_NEXT)
              .setUrl(buildUrl(lastPatientId)));
    }
  }

  @Override
  public void addLinks(org.hl7.fhir.r4.model.Bundle to) {
    List<org.hl7.fhir.r4.model.Bundle.BundleEntryComponent> entries = to.getEntry();
    if (!isPagingRequested()) return;

    to.addLink(
        new org.hl7.fhir.r4.model.Bundle.BundleLinkComponent()
            .setRelation(Constants.LINK_SELF)
            .setUrl(components.toUriString()));
    to.addLink(
        new org.hl7.fhir.r4.model.Bundle.BundleLinkComponent()
            .setRelation(Constants.LINK_FIRST)
            .setUrl(buildUrl("")));

    if (entries.size() == getPageSize() && entries.size() > 0) {
      org.hl7.fhir.r4.model.Patient lastPatient =
          (org.hl7.fhir.r4.model.Patient) entries.get(entries.size() - 1).getResource();
      String lastPatientId = lastPatient.getId();
      to.addLink(
          new org.hl7.fhir.r4.model.Bundle.BundleLinkComponent()
              .setRelation(Constants.LINK_NEXT)
              .setUrl(buildUrl(lastPatientId)));
    }
  }

  public String getCursor() {
    return cursor;
  }

  private Integer extractCountParam(UriComponents components) {
    String countText = components.getQueryParams().getFirst(Constants.PARAM_COUNT);
    if (countText == null) return null;
    try {
      return Integer.parseInt(countText);
    } catch (NumberFormatException ex) {
      throw new InvalidRequestException("Invalid _count parameter: " + countText);
    }
  }

  private String extractCursorParam(UriComponents components) {
    String cursorText = components.getQueryParams().getFirst(PARAM_CURSOR);
    if (cursorText != null && cursorText.length() == 0) return null;
    return cursorText;
  }

  private String buildUrl(String cursor) {
    MultiValueMap<String, String> params = components.getQueryParams();
    if (!cursor.isEmpty()) {
      params = new LinkedMultiValueMap<>(params);
      params.set(PARAM_CURSOR, cursor);
    }
    return UriComponentsBuilder.newInstance()
        .uriComponents(components)
        .replaceQueryParams(params)
        .build()
        .toUriString();
  }
}
