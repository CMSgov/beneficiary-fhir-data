package gov.hhs.cms.bluebutton.datapipeline.sampledata;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.justdavis.karl.misc.datasources.IDataSourceCoordinates;
import com.justdavis.karl.misc.datasources.schema.IDataSourceSchemaManager;

/**
 * Can be used to create a dev/test schema in a database that is analogous to
 * the CCW (for the purposes of this system).
 * 
 * @see SampleDataLoader
 */
@Component
public final class CcwSchemaInitializer {
	private final IDataSourceSchemaManager schemaManager;

	/**
	 * Constructs a new instance of the {@link CcwSchemaInitializer}.
	 * 
	 * @param schemaManager
	 *            the {@link IDataSourceSchemaManager} to use
	 */
	@Inject
	public CcwSchemaInitializer(IDataSourceSchemaManager schemaManager) {
		this.schemaManager = schemaManager;
	}

	/**
	 * Initializes the CCW schema in the specified DB, which should be empty.
	 * 
	 * @param coords
	 *            the {@link IDataSourceCoordinates} of the DB to populate
	 */
	public void initializeSchema(IDataSourceCoordinates coords) {
		schemaManager.createOrUpgradeSchema(coords);
	}
}
