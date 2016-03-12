package gov.hhs.cms.bluebutton.datapipeline.ccw.extract;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.jdo.JDOQLTypedQuery;
import javax.jdo.PersistenceManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.CurrentBeneficiary;

/**
 * Represents the "extract" portion of the CMS Blue Button's
 * extract-transform-load data pipeline: pulls out {@link CurrentBeneficiary}
 * records from the Chronic Conditions Warehouse (CCW), along with all of their
 * associated claims data.
 */
public class CcwExtractor {
	private static final Logger LOGGER = LoggerFactory.getLogger(CcwExtractor.class);

	private final PersistenceManager pm;

	/**
	 * Constructs a new {@link CcwExtractor} instance.
	 * 
	 * @param pm
	 *            the (injected) {@link PersistenceManager} to use
	 */
	@Inject
	public CcwExtractor(PersistenceManager pm) {
		this.pm = pm;
	}

	/**
	 * @return a {@link Stream} of all {@link CurrentBeneficiary}s in the CCW
	 */
	public Stream<CurrentBeneficiary> extractAllBeneficiaries() {
		// Create the query that will be run.
		JDOQLTypedQuery<CurrentBeneficiary> query = pm.newJDOQLTypedQuery(CurrentBeneficiary.class);

		/*
		 * Set the various DataNucleus extension properties required to keep the
		 * query's results from being all loaded/cached into memory.
		 */
		// query.datastoreReadTimeoutMillis(-1);
		query.extension("datanucleus.query.resultCacheType", "none");
		query.extension("datanucleus.rdbms.query.resultSetType", "forward-only");
		query.extension("datanucleus.rdbms.query.fetchDirection", "forward");
		query.extension("datanucleus.rdbms.query.resultSetConcurrency", "read-only");

		/*
		 * Run the query. Given the settings above, the result list should be
		 * lazy-loaded, as long as we don't do something dumb like call size()
		 * on it (which would require seeking to the end of the list).
		 */
		LOGGER.trace("Querying for beneficiaries...");
		List<CurrentBeneficiary> beneficiariesLazyList = query.executeList();
		LOGGER.trace("Query for beneficiaries completed.");

		/*
		 * Convert the result List to an iterator. Looking at the source, this
		 * should be an instance of DataNucleus' QueryResultIterator.
		 */
		Iterator<CurrentBeneficiary> beneficiariesIter = beneficiariesLazyList.iterator();

		return StreamSupport.stream(
				Spliterators.spliteratorUnknownSize(beneficiariesIter, Spliterator.ORDERED | Spliterator.NONNULL),
				false);
	}
}
