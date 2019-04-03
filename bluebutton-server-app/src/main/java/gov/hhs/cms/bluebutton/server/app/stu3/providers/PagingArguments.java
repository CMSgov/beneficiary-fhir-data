package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.Optional;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleLinkComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;

/*
 * PagingArguments encapsulates the arguments related to paging for the 
 * {@link ExplanationOfBenefit}, {@link Patient}, and {@link Coverage} requests.
 */
public final class PagingArguments {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExplanationOfBenefitResourceProvider.class);

	private final Optional<Integer> pageSize;
	private final Optional<Integer> startIndex;
	private final String serverBase;

	public PagingArguments(RequestDetails requestDetails) {
		pageSize = parseIntegerParameters(requestDetails, "_count");
		startIndex = parseIntegerParameters(requestDetails, "startIndex");
		serverBase = requestDetails.getServerBaseForRequest();
	}

	/**
	 * @param requestDetails
	 *            the {@link RequestDetails} containing additional parameters for
	 *            the URL in need of parsing out
	 * @param parameterToParse
	 *            the parameter to parse from requestDetails
	 * @return Returns the parsed parameter as an Integer, null if the parameter is
	 *         not found.
	 */
	private Optional<Integer> parseIntegerParameters(RequestDetails requestDetails, String parameterToParse) {
		if (requestDetails.getParameters().containsKey(parameterToParse)) {
			try {
				return Optional.of(Integer.parseInt(requestDetails.getParameters().get(parameterToParse)[0]));
			} catch (NumberFormatException e) {
				LOGGER.warn("Invalid argument in request URL: " + parameterToParse + ". Cannot parse to Integer.", e);
				throw new InvalidRequestException(
						"Invalid argument in request URL: " + parameterToParse + ". Cannot parse to Integer.");
			}
		}
		return Optional.empty();
	}

	/**
	 * @return Returns true if the pageSize either startIndex is present (i.e.
	 *         paging is requested), false if neither present.
	 */
	public boolean isPagingRequested() {
		if (pageSize.isPresent() || startIndex.isPresent())
			return true;
		return false;
	}

	/**
	 * @return Returns the pageSize as an integer. Note: if the pageSize does not
	 *         exist but the startIndex does (paging is requested) default to
	 *         pageSize of 10.
	 * @throws InvalidRequestException
	 *             HTTP 400: indicates a pageSize less than 0 was provided
	 */
	public int getPageSize() {
		if (!isPagingRequested())
			throw new BadCodeMonkeyException();
		if (!pageSize.isPresent())
			return 10;
		if (pageSize.isPresent())
			if (pageSize.get() < 0)
				throw new InvalidRequestException(String.format(
						"HTTP 400 Bad Request: Value for startIndex cannot be negative: pageSize %s", pageSize.get()));
		return pageSize.get();
	}

	/**
	 * @return Returns the startIndex as an integer. If the startIndex is not set,
	 *         return 0.
	 * @throws InvalidRequestException
	 *             HTTP 400: indicates a startIndex less than 0 was provided
	 */
	public int getStartIndex() {
		if (!isPagingRequested())
			throw new BadCodeMonkeyException();
		if (startIndex.isPresent()) {
			if (startIndex.get() < 0) {
				throw new InvalidRequestException(
						String.format("HTTP 400 Bad Request: Value for startIndex cannot be negative: startIndex %s",
								startIndex.get()));
			}
			return startIndex.get();
		}
		return 0;
	}

	/**
	 * @return Returns the serverBase.
	 */
	public String getServerBase() {
		return serverBase;
	}
	
	/**
	 * @param bundle
	 *            the {@link Bundle} to which links are being added
	 * @param resource
	 *            the {@link String} the resource being provided by the paging link
	 * @param searchByDesc
	 *            the {@link String} field the search is being performed on
	 * @param identifier
	 *            the {@link String} identifier being searched for
	 * @param numTotalResults
	 *            the number of total resources matching the
	 *            {@link Beneficiary#getBeneficiaryId()}
	 */
	public void addPagingLinks(Bundle bundle, String resource, String searchByDesc, String identifier,
			int numTotalResults) {

		Integer pageSize = getPageSize();
		Integer startIndex = getStartIndex();

		bundle.addLink(new BundleLinkComponent().setRelation(Constants.LINK_FIRST)
				.setUrl(createPagingLink(resource, searchByDesc, identifier, 0, pageSize)));

		if (startIndex + pageSize < numTotalResults) {
			bundle.addLink(new BundleLinkComponent().setRelation(Constants.LINK_NEXT)
					.setUrl(createPagingLink(resource, searchByDesc, identifier, startIndex + pageSize, pageSize)));
		}

		if (startIndex > 0) {
			bundle.addLink(new BundleLinkComponent().setRelation(Constants.LINK_PREVIOUS)
					.setUrl(createPagingLink(resource, searchByDesc, identifier, Math.max(startIndex - pageSize, 0),
							pageSize)));
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
		bundle.addLink(new BundleLinkComponent().setRelation(Constants.LINK_LAST)
				.setUrl(createPagingLink(resource, searchByDesc, identifier, lastIndex, pageSize)));
	}

	/**
	 * @return Returns the URL string for a paging link.
	 */
	private String createPagingLink(String resource, String descriptor, String id, int startIndex, int theCount) {
		StringBuilder b = new StringBuilder();
		b.append(serverBase + resource);
		b.append(Constants.PARAM_COUNT + "=" + theCount);
		b.append("&startIndex=" + startIndex);
		b.append("&" + descriptor + "=" + id);

		return b.toString();
	}
}