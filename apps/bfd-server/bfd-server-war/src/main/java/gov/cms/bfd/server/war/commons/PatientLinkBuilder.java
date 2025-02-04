package gov.cms.bfd.server.war.commons;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Patient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/** A link builder for Patient resources using bene-id cursors. */
public final class PatientLinkBuilder implements LinkBuilder {
  /**
   * Maximum page size is one less than the maximum integer value to allow clients to request one
   * more than the page size as a means to see if an additional page is necessary without having an
   * integer overflow.
   */
  public static final int MAX_PAGE_SIZE = Integer.MAX_VALUE - 1;

  /** The request details. */
  private final RequestDetails requestDetails;

  /** The count. */
  private final Optional<Integer> count;

  /** The cursor value. */
  private final Optional<Long> cursor;

  /** If there is another page for this link. */
  private final boolean hasAnotherPage;

  /** Represents the text for the cursor parameter. */
  public static final String PARAM_CURSOR = "cursor";

  /**
   * Instantiates a new Patient link builder.
   *
   * @param requestDetails the request details
   */
  public PatientLinkBuilder(RequestDetails requestDetails) {
    this.requestDetails = requestDetails;
    count =
        StringUtils.parseIntegersFromRequest(requestDetails, Constants.PARAM_COUNT).stream()
            .findFirst();
    cursor = StringUtils.parseLongsFromRequest(requestDetails, PARAM_CURSOR).stream().findFirst();
    hasAnotherPage = false; // Don't really know, so default to false
    validate();
  }

  /**
   * Instantiates a new Patient link builder from an existing builder.
   *
   * @param prev the previous builder
   * @param hasAnotherPage if there is another page
   */
  public PatientLinkBuilder(PatientLinkBuilder prev, boolean hasAnotherPage) {
    requestDetails = prev.requestDetails;
    count = prev.count;
    cursor = prev.cursor;
    this.hasAnotherPage = hasAnotherPage;
    validate();
  }

  /**
   * Check that the page size is valid.
   *
   * @throws InvalidRequestException (http 400 error) if the page size is invalid
   */
  private void validate() {
    if (getPageSize() <= 0) {
      throw new InvalidRequestException("Value for pageSize cannot be zero or negative: %s");
    }
    if (getPageSize() > MAX_PAGE_SIZE) {
      throw new InvalidRequestException("Page size must be less than " + MAX_PAGE_SIZE);
    }
  }

  /** {@inheritDoc} */
  @Override
  public boolean isPagingRequested() {
    return count.isPresent();
  }

  /** {@inheritDoc} */
  @Override
  public int getPageSize() {
    return isPagingRequested() ? count.get() : MAX_PAGE_SIZE;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isFirstPage() {
    return cursor.isEmpty() || !isPagingRequested();
  }

  /** {@inheritDoc} */
  @Override
  public void addLinks(Bundle to) {
    List<BundleEntryComponent> entries = to.getEntry();
    if (!isPagingRequested()) {
      return;
    }
    to.addLink(
        new Bundle.BundleLinkComponent()
            .setRelation(Constants.LINK_SELF)
            .setUrl(requestDetails.getCompleteUrl()));
    to.addLink(
        new Bundle.BundleLinkComponent().setRelation(Constants.LINK_FIRST).setUrl(buildUrl(null)));

    if (hasAnotherPage) {
      Patient lastPatient = (Patient) entries.getLast().getResource();
      Long lastPatientId = StringUtils.parseLongOrBadRequest(lastPatient.getId(), PARAM_CURSOR);
      to.addLink(
          new Bundle.BundleLinkComponent()
              .setRelation(Constants.LINK_NEXT)
              .setUrl(buildUrl(lastPatientId)));
    }
  }

  /** {@inheritDoc} */
  @Override
  public void addLinks(org.hl7.fhir.r4.model.Bundle to) {
    List<org.hl7.fhir.r4.model.Bundle.BundleEntryComponent> entries = to.getEntry();
    if (!isPagingRequested()) {
      return;
    }
    to.addLink(
        new org.hl7.fhir.r4.model.Bundle.BundleLinkComponent()
            .setRelation(Constants.LINK_SELF)
            .setUrl(requestDetails.getCompleteUrl()));
    to.addLink(
        new org.hl7.fhir.r4.model.Bundle.BundleLinkComponent()
            .setRelation(Constants.LINK_FIRST)
            .setUrl(buildUrl(null)));

    if (entries.size() == getPageSize() && !entries.isEmpty()) {
      org.hl7.fhir.r4.model.Patient lastPatient =
          (org.hl7.fhir.r4.model.Patient) entries.getLast().getResource();
      Long lastPatientId = Long.parseLong(lastPatient.getId());
      to.addLink(
          new org.hl7.fhir.r4.model.Bundle.BundleLinkComponent()
              .setRelation(Constants.LINK_NEXT)
              .setUrl(buildUrl(lastPatientId)));
    }
  }

  /**
   * Gets the {@link #cursor}.
   *
   * @return the cursor
   */
  public Long getCursor() {
    return cursor.orElse(null);
  }

  /**
   * Builds the url string using the cursor.
   *
   * @param cursor the cursor
   * @return the url string
   */
  private String buildUrl(Long cursor) {
    UriComponents components =
        UriComponentsBuilder.fromUriString(requestDetails.getCompleteUrl()).build();
    MultiValueMap<String, String> params = components.getQueryParams();
    if (cursor != null) {
      params = new LinkedMultiValueMap<>(params);
      params.set(PARAM_CURSOR, String.valueOf(cursor));
    }
    return UriComponentsBuilder.newInstance()
        .uriComponents(components)
        .replaceQueryParams(params)
        .build()
        .toUriString();
  }

  /**
   * Get the value that should be passed as the max size for a query using paging. This value should
   * be at least as big as the page size to ensure a full page but include at least one additional
   * record as a way to determine whether another page will be needed. In practice this means
   * returning one more than the page size.
   *
   * @return the query max size
   */
  public int getQueryMaxSize() {
    return getPageSize() + 1;
  }
}
