package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;

/*
 * PagingArguments encapsulates the arguments related to paging for the 
 * {@link ExplanationOfBenefit} request.
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
}