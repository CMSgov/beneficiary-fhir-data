package gov.hhs.cms.bluebutton.datapipeline.ccw.test;

import java.util.LinkedList;
import java.util.List;

import org.junit.Rule;
import org.junit.rules.ExternalResource;

/**
 * <p>
 * Any {@link AutoCloseable} instances registered with a
 * {@link TearDownAcceptor} will have their XXX methods called after every test
 * case. This is intended for use as a JUnit {@link Rule}, as follows:
 * </p>
 * 
 * <pre><code>
 * public final class MyTest {
 * 	{@literal @}Rule
 * 	public final TearDownAcceptor tearDownAcceptor = new TearDownAcceptor();
 * 
 * 	{@literal @}Test
 * 	public void normalUsage() {
 * 		AutoCloseable foo = // ...
 * 		tearDownAcceptor.register(foo);
 * 		
 * 		// Do stuff...
 * 		
 * 		// foo will be .close()'d automatically after the test case
 * 	}
 * }
 * </code></pre>
 */
public final class TearDownAcceptor extends ExternalResource {
	private final List<AutoCloseable> tearDowns = new LinkedList<>();

	/**
	 * @param tearDown
	 *            the {@link AutoCloseable} to call
	 *            {@link AutoCloseable#close()} on at the end of the test case
	 */
	public void register(AutoCloseable tearDown) {
		tearDowns.add(tearDown);
	}

	/**
	 * @see org.junit.rules.ExternalResource#after()
	 */
	@Override
	protected void after() {
		for (AutoCloseable tearDown : tearDowns) {
			try {
				tearDown.close();
			} catch (Exception e) {
				// Wrap and rethrow.
				throw new RuntimeException("Unable to tear down test resource.", e);
			}
		}
	}
}
