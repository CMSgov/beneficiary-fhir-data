package gov.cms.bfd.pipeline.sharedutils.npi_fda;

import gov.cms.bfd.model.rif.npi_fda.FDAData;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Class to download FDA data and persist it into the database. */
public class LoadFdaDataFiles extends LoadDataFiles<FDAData> {
  private static final Logger LOGGER = LoggerFactory.getLogger(LoadFdaDataFiles.class);
  private static final String TABLE_NAME = "fda_data";
  private static final String NDC_URL = "https://www.accessdata.fda.gov/cder/ndctext.zip";

  /** Product NDC column name. */
  private static final String PRODUCT_NDC_COLUMN = "PRODUCTNDC";

  /** Product Proprietary name column. */
  private static final String PROPRIETARY_NAME_COLUMN = "PROPRIETARYNAME";

  /** Product substance name column. */
  private static final String SUBSTANCE_NAME_COLUMN = "SUBSTANCENAME";

  /** CSV Delimiter. */
  private static final String DELIMITER = "\t";

  @Override
  Integer persistResource() throws IOException {
    return downloadDataFile(NDC_URL);
  }

  @Override
  boolean resolveFileName(String name) {
    return name.equalsIgnoreCase("product.txt");
  }

  @Override
  FDAData getDataFromCsv(CSVRecord csvRecord) {
    String productNdc = csvRecord.get(PRODUCT_NDC_COLUMN);
    String proprietaryName = csvRecord.get(PROPRIETARY_NAME_COLUMN);
    String substanceName = csvRecord.get(SUBSTANCE_NAME_COLUMN);
    // We want to split the manufacturer and ingredient portions of the code out, so we can pad
    // them.
    // 0002-0152 Becomes 00002 and 0152. It appears that the ingredient portion is always 4
    // characters,
    // so may not necessarily need to be padded, but the operation is kept:q
    // in out of an abundance of
    // caution.
    String nationalDrugCodeManufacturer =
        StringUtils.leftPad(productNdc.substring(0, productNdc.indexOf("-")), 5, '0');
    // Get the ingredient portion of the code, and pat it with zeros, E.G. 0152
    String nationalDrugCodeIngredient =
        StringUtils.leftPad(productNdc.substring(productNdc.indexOf("-") + 1), 4, '0');
    return FDAData.builder()
        .code(String.format("%s-%s", nationalDrugCodeManufacturer, nationalDrugCodeIngredient))
        .display(String.format("%s - %s", proprietaryName, substanceName))
        .build();
  }

  /**
   * Constructor.
   *
   * @param entityManager the EntityManager to use for database operations;
   * @param batchSize The number of records saved before committing a transaction.
   * @param runInterval How often to run the job, in days.
   */
  public LoadFdaDataFiles(EntityManager entityManager, int batchSize, int runInterval) {
    super(TABLE_NAME, entityManager, batchSize, runInterval, LOGGER, DELIMITER);
  }
}
