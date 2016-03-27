package gov.hhs.cms.bluebutton.datapipeline.sampledata.persist;

import java.util.stream.Stream;

import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.CurrentBeneficiary;

/**
 * Can process (large amounts of) sample data and persist it to a mock copy of
 * the CCW schema.
 */
public class SampleDataPersister {
	private final MetricRegistry metrics;
	private final PersistenceManager pm;

	/**
	 * Constructs a new {@link SampleDataPersister} instance.
	 * 
	 * @param metrics
	 *            the {@link MetricRegistry} to use
	 * @param pm
	 *            the {@link PersistenceManager} to use
	 */
	public SampleDataPersister(MetricRegistry metrics, PersistenceManager pm) {
		this.metrics = metrics;
		this.pm = pm;
	}

	/**
	 * Persists the specified {@link CurrentBeneficiary}s to a mock CCW schema.
	 * 
	 * @param beneficiariesStream
	 *            the {@link CurrentBeneficiary}s to be persisted
	 */
	public void persist(Stream<CurrentBeneficiary> beneficiariesStream) {
		Timer timerPersistBeneficiary = metrics.timer(MetricRegistry.name(getClass(), "beneficiary.tx"));
		beneficiariesStream.forEach(b -> {
			/*
			 * For now, we'll take the naive approach and commit each
			 * CurrentBeneficiary (and its claims) in a separate transaction.
			 */
			Transaction tx = pm.currentTransaction();
			Timer.Context timerContextPersistBeneficiary = timerPersistBeneficiary.time();
			try {
				tx.begin();
				pm.makePersistent(b);
				tx.commit();
			} finally {
				timerContextPersistBeneficiary.stop();
				if (tx.isActive())
					tx.rollback();
			}
		});
	}
}
