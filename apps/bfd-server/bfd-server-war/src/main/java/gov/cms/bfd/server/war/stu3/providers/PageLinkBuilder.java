package gov.cms.bfd.server.war.stu3.providers;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;
import java.util.Optional;
import java.util.Set;
import org.hl7.fhir.dstu3.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * PagingArguments encapsulates the arguments related to paging for the
 * {@link ExplanationOfBenefit}, {@link Patient}, and {@link Coverage} requests.
 */
public final class PageLinkBuilder {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ExplanationOfBenefitResourceProvider.class);

  private final Optional<Integer> pageSize;
  private final Optional<Integer> startIndex;
  private final String serverBase;

  private final String resource;
  private final String searchByDesc;
  private final String identifier;
  private final DateRangeParam lastUpdated;
  private final String excludeSamhsa;
  private final Set<ClaimType> claimTypes;

  public PageLinkBuilder(
      RequestDetails requestDetails,
      String resource,
      String searchByDesc,
      String identifier,
      DateRangeParam lastUpdated,
      Set<ClaimType> claimTypes,
      String excludeSamhsa) {
    this.pageSize = parseIntegerParameters(requestDetails, Constants.PARAM_COUNT);
    this.startIndex = parseIntegerParameters(requestDetails, "startIndex");
    this.serverBase = requestDetails.getServerBaseForRequest();
    this.resource = resource;
    this.searchByDesc = searchByDesc;
    this.identifier = identifier;
    this.lastUpdated = lastUpdated;
    this.claimTypes = claimTypes;
    this.excludeSamhsa = excludeSamhsa;
  }

  public PageLinkBuilder(
      RequestDetails requestDetails,
      String resource,
      String searchByDesc,
      String identifier,
      DateRangeParam lastUpdate) {
    this(requestDetails, resource, searchByDesc, identifier, lastUpdate, null, null);
  }

  /**
   * @param requestDetails the {@link RequestDetails} containing additional parameters for the URL
   *     in need of parsing out
   * @param parameterToParse the parameter to parse from requestDetails
   * @return Returns the parsed parameter as an Integer, empty if the parameter is not found.
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

  /**
   * @return Returns true if the pageSize either startIndex is present (i.e. paging is requested),
   *     false if neither present.
   */
  public boolean isPagingRequested() {
    return pageSize.isPresent() || startIndex.isPresent();
  }

  /**
   * @return Returns the pageSize as an integer. Note: if the pageSize does not exist but the
   *     startIndex does (paging is requested) default to pageSize of 10.
   * @throws InvalidRequestException HTTP 400: indicates a pageSize less than 0 was provided
   */
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

  /**
   * @return Returns the startIndex as an integer. If the startIndex is not set, return 0.
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
   * Add next, first, last, and previous links to a bundle
   *
   * @param toBundle to add the links
   * @param numTotalResults from the search
   */
  public void addPageLinks(Bundle toBundle, int numTotalResults) {
    Integer pageSize = getPageSize();
    Integer startIndex = getStartIndex();

    toBundle.addLink(
        new Bundle.BundleLinkComponent()
            .setRelation(Constants.LINK_FIRST)
            .setUrl(createPageLink(0)));

    if (startIndex + pageSize < numTotalResults) {
      toBundle.addLink(
          new Bundle.BundleLinkComponent()
              .setRelation(Constants.LINK_NEXT)
              .setUrl(createPageLink(startIndex + pageSize)));
    }

    if (startIndex > 0) {
      toBundle.addLink(
          new Bundle.BundleLinkComponent()
              .setRelation(Constants.LINK_PREVIOUS)
              .setUrl(createPageLink(Math.max(startIndex - pageSize, 0))));
    }

    /*
     * This formula rounds numTotalResults down to the nearest multiple of pageSize
     * that's less than and not equal to numTotalResults
     */
    int lastIndex;
    try {
      lastIndex = (numTotalResults - 1) / pageSize * pageSize;
    } catch (ArithmeticException e) {
      throw new InvalidRequestException(String.format("Invalid pageSize '%s'", pageSize));
    }
    toBundle.addLink(
        new Bundle.BundleLinkComponent()
            .setRelation(Constants.LINK_LAST)
            .setUrl(createPageLink(lastIndex)));
  }

  /**
   * Build the link string
   *
   * @param startIndex start index
   * @return the link requested
   */
  private String createPageLink(int startIndex) {
    StringBuilder b = new StringBuilder();
    b.append(serverBase);
    b.append(resource);
    b.append(Constants.PARAM_COUNT + "=" + getPageSize());
    b.append("&startIndex=" + startIndex);
    b.append("&" + searchByDesc + "=" + identifier);

    // Add the lastUpdated parameters if present
    if (lastUpdated != null) {
      DateParam lowerBound = lastUpdated.getLowerBound();
      if (lowerBound != null && !lowerBound.isEmpty()) {
        b.append(
            "&"
                + Constants.PARAM_LASTUPDATED
                + "="
                + lowerBound.getPrefix().getValue()
                + lowerBound.getValueAsString());
      }
      DateParam upperBound = lastUpdated.getUpperBound();
      if (upperBound != null && !upperBound.isEmpty()) {
        b.append(
            "&"
                + Constants.PARAM_LASTUPDATED
                + "="
                + upperBound.getPrefix().getValue()
                + upperBound.getValueAsString());
      }
    }

    // Add the "type" parameter if present
    if (claimTypes != null) {
      b.append("&type=" + claimTypes.toString().toLowerCase().replaceAll("[\\[\\] ]", ""));
    }

    // Add the "excludeSamhsa" parameter if present
    if (excludeSamhsa != null) b.append("&excludeSAMHSA=" + excludeSamhsa);

    return b.toString();
  }
}
