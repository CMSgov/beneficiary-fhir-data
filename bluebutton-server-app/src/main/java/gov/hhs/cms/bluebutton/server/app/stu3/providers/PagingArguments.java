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

	/*
	 * @return Returns true if the pageSize or startIndex is present (i.e. paging is
	 * requested), false if they are not present, and throws an
	 * IllegalArgumentException if the arguments are mismatched.
	 */
	public boolean isPagingRequested() {
		if (pageSize.isPresent())
			return true;
		else if (!pageSize.isPresent() && !startIndex.isPresent())
			return false;
		else
			// It's better to let clients requesting mismatched options know they goofed
			// than to try and guess their intent.
			throw new IllegalArgumentException(
					String.format("Mismatched paging arguments: pageSize='%s', startIndex='%s'", pageSize, startIndex));
	}

	/*
	 * @return Returns the pageSize as an integer. Note: the pageSize must exist at
	 * this point, otherwise paging would not have been requested.
	 */
	public int getPageSize() {
		if (!isPagingRequested())
			throw new BadCodeMonkeyException();
		return pageSize.get();
	}

	/*
	 * @return Returns the startIndex as an integer. If the startIndex is not set,
	 * return 0.
	 */
	public int getStartIndex() {
		if (!isPagingRequested())
			throw new BadCodeMonkeyException();
		if (startIndex.isPresent()) {
			return startIndex.get();
		}
		return 0;
	}

	/*
	 * @return Returns the serverBase.
	 */
	public String getServerBase() {
		return serverBase;
	}
}