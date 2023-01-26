package gov.cms.bfd.server.war.commons;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.http.client.utils.URIBuilder;
import org.hl7.fhir.dstu3.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PagingArguments encapsulates the arguments related to paging for the ExplanationOfBenefit,
 * Patient, and Coverage requests.
 */
public final class OffsetLinkBuilder implements LinkBuilder {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ExplanationOfBenefitResourceProvider.class);

  /** The page size for paging. */
  private final Optional<Integer> pageSize;
  /** The start index for paging. */
  private final Optional<Integer> startIndex;
  /** The server url base. */
  private final String serverBase;
  /** The resource to use with the base url. */
  private final String resource;
  /** The request details. */
  private final RequestDetails requestDetails;
  /**
   * The number of results to return during paging; the default of -1 returns the bundle size by
   * default.
   */
  private int numTotalResults = -1;

  /**
   * Instantiates a new Offset link builder.
   *
   * @param requestDetails the request details
   * @param resource the resource
   */
  public OffsetLinkBuilder(RequestDetails requestDetails, String resource) {
    this.pageSize = parseIntegerParameters(requestDetails, Constants.PARAM_COUNT);
    this.startIndex = parseIntegerParameters(requestDetails, "startIndex");
    this.serverBase = requestDetails.getServerBaseForRequest();
    this.resource = resource;
    this.requestDetails = requestDetails;
  }

  /**
   * Returns the parsed parameter as an Integer.
   *
   * @param requestDetails the {@link RequestDetails} containing additional parameters for the URL
   *     in need of parsing out
   * @param parameterToParse the parameter to parse from requestDetails
   * @return the parsed parameter as an Integer, empty {@link Optional} if the parameter is not
   *     found
   */
  private Optional<Integer> parseIntegerParameters(
      RequestDetails requestDetails, String parameterToParse) {

    if (requestDetails.getParameters().containsKey(parameterToParse)) {
      try {
        return Optional.of(
            Integer.parseInt(requestDetails.getParameters().get(parameterToParse)[0]));
      } catch (NumberFormatException e) {
        LOGGER.warn(
            "Invalid argument in request URL: " + parameterToParse + ". Cannot parse to Integer.",
            e);
        throw new InvalidRequestException(
            "Invalid argument in request URL: " + parameterToParse + ". Cannot parse to Integer.");
      }
    }
    return Optional.empty();
  }

  /** {@inheritDoc} */
  @Override
  public boolean isPagingRequested() {
    return pageSize.isPresent() || startIndex.isPresent();
  }

  /** {@inheritDoc} */
  @Override
  public int getPageSize() {
    if (!isPagingRequested()) throw new BadCodeMonkeyException();
    if (!pageSize.isPresent()) return 10;
    if (pageSize.get() < 0) {
      throw new InvalidRequestException(
          String.format(
              "HTTP 400 Bad Request: Value for startIndex cannot be negative: pageSize %s",
              pageSize.get()));
    }
    return pageSize.get();
  }

  /** {@inheritDoc} */
  @Override
  public boolean isFirstPage() {
    return getStartIndex() == 0;
  }

  /**
   * Gets the start index.
   *
   * @return the startIndex, if not set, returns 0
   * @throws InvalidRequestException HTTP 400: indicates a startIndex less than 0 was provided
   */
  public int getStartIndex() {
    if (!isPagingRequested()) throw new BadCodeMonkeyException();
    if (startIndex.isPresent()) {
      if (startIndex.get() < 0) {
        throw new InvalidRequestException(
            String.format(
                "HTTP 400 Bad Request: Value for startIndex cannot be negative: startIndex %s",
                startIndex.get()));
      }
      return startIndex.get();
    }
    return 0;
  }

  /**
   * Sets the {@link #numTotalResults}.
   *
   * @param numTotalResults the num total results
   * @return the total
   */
  public LinkBuilder setTotal(int numTotalResults) {
    this.numTotalResults = numTotalResults;
    return this;
  }

  /**
   * Add next, first, last, and previous links to a bundle.
   *
   * @param toBundle to add the links
   */
  public void addLinks(Bundle toBundle) {
    Integer pageSize = getPageSize();
    Integer startIndex = getStartIndex();
    int total = numTotalResults == -1 ? toBundle.getEntry().size() : numTotalResults;
    toBundle.addLink(
        new Bundle.BundleLinkComponent()
            .setRelation(Constants.LINK_FIRST)
            .setUrl(createPageLink(0)));

    if (startIndex + pageSize < total) {
      toBundle.addLink(
          new Bundle.BundleLinkComponent()
              .setRelation(Constants.LINK_NEXT)
              .setUrl(createPageLink(startIndex + pageSize)));
    }

    if (!isFirstPage()) {
      toBundle.addLink(
          new Bundle.BundleLinkComponent()
              .setRelation(Constants.LINK_PREVIOUS)
              .setUrl(createPageLink(Math.max(startIndex - pageSize, 0))));
    }

    /*
     * This formula rounds numTotalResults down to the nearest multiple of pageSize that's less than
     * and not equal to numTotalResults
     */
    int lastIndex;
    try {
      lastIndex = (total - 1) / pageSize * pageSize;
    } catch (ArithmeticException e) {
      throw new InvalidRequestException(String.format("Invalid pageSize '%s'", pageSize));
    }
    toBundle.addLink(
        new Bundle.BundleLinkComponent()
            .setRelation(Constants.LINK_LAST)
            .setUrl(createPageLink(lastIndex)));
  }

  /**
   * Add next, first, last, and previous links to a bundle.
   *
   * @param toBundle to add the links
   */
  public void addLinks(org.hl7.fhir.r4.model.Bundle toBundle) {
    Integer pageSize = getPageSize();
    Integer startIndex = getStartIndex();
    int total = numTotalResults == -1 ? toBundle.getEntry().size() : numTotalResults;
    toBundle.addLink(
        new org.hl7.fhir.r4.model.Bundle.BundleLinkComponent()
            .setRelation(Constants.LINK_FIRST)
            .setUrl(createPageLink(0)));

    if (startIndex + pageSize < total) {
      toBundle.addLink(
          new org.hl7.fhir.r4.model.Bundle.BundleLinkComponent()
              .setRelation(Constants.LINK_NEXT)
              .setUrl(createPageLink(startIndex + pageSize)));
    }

    if (!isFirstPage()) {
      toBundle.addLink(
          new org.hl7.fhir.r4.model.Bundle.BundleLinkComponent()
              .setRelation(Constants.LINK_PREVIOUS)
              .setUrl(createPageLink(Math.max(startIndex - pageSize, 0))));
    }

    /*
     * This formula rounds numTotalResults down to the nearest multiple of pageSize that's less than
     * and not equal to numTotalResults
     */
    int lastIndex;
    try {
      lastIndex = (total - 1) / pageSize * pageSize;
    } catch (ArithmeticException e) {
      throw new InvalidRequestException(String.format("Invalid pageSize '%s'", pageSize));
    }
    toBundle.addLink(
        new org.hl7.fhir.r4.model.Bundle.BundleLinkComponent()
            .setRelation(Constants.LINK_LAST)
            .setUrl(createPageLink(lastIndex)));
  }

  /**
   * Build the link string.
   *
   * @param startIndex start index
   * @return the link requested
   */
  private String createPageLink(int startIndex) {

    // Get a copy of all request parameters.
    Map<String, String[]> params = new HashMap<>(requestDetails.getParameters());

    // Add in paging related changes.
    params.put("startIndex", new String[] {String.valueOf(startIndex)});
    params.put("_count", new String[] {String.valueOf(getPageSize())});

    try {
      // Setup URL base and resource.
      URIBuilder uri = new URIBuilder(serverBase + resource);

      // Create query parameters by iterating thru all params entry sets. Handle multi values for
      // the same parameter key.
      ArrayList<String> queryParams = new ArrayList<String>();
      for (Map.Entry<String, String[]> paramSet : params.entrySet()) {
        for (String param : paramSet.getValue()) {
          uri.addParameter(paramSet.getKey(), param);
        }
      }
      return uri.build().toString();
    } catch (URISyntaxException e) {
      throw new InvalidRequestException("Invalid URI:" + e);
    }
  }
}
