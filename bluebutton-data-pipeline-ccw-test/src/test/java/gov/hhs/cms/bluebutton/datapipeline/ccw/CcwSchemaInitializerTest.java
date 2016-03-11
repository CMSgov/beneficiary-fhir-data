package gov.hhs.cms.bluebutton.datapipeline.ccw;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import com.justdavis.karl.misc.datasources.hsql.HsqlCoordinates;
import com.justdavis.karl.misc.datasources.schema.IDataSourceSchemaManager;

import gov.hhs.cms.bluebutton.datapipeline.ccw.schema.CcwSchemaInitializer;

/**
 * Unit tests for {@link CcwSchemaInitializer}.
 */
@ContextConfiguration(classes = { SpringConfigForTests.class })
public final class CcwSchemaInitializerTest {
	@ClassRule
	public static final SpringClassRule springClassRule = new SpringClassRule();

	@Rule
	public final SpringMethodRule springMethodRule = new SpringMethodRule();

	@Inject
	private IDataSourceSchemaManager schemaManager;

	/**
	 * Verifies that {@link CcwSchemaInitializer} works correctly under normal
	 * circumstances.
	 */
	@Test
	public void testThings() {
		CcwSchemaInitializer schemaIniter = new CcwSchemaInitializer(schemaManager);
		Assert.assertNotNull(schemaIniter);

		// Really just need to make sure this doesn't blow up.
		schemaIniter.initializeSchema(new HsqlCoordinates("jdbc:hsqldb:mem:tests"));
	}
}
